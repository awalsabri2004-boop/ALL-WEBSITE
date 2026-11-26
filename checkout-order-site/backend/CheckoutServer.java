import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Simple Java backend for the checkout-order-site demo.
 *
 * Run from this folder:
 *   javac CheckoutServer.java
 *   java CheckoutServer
 *
 * Then open: http://localhost:8080
 */
public class CheckoutServer {

    private static final int PORT = 8080;
    private static final Path WEB_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path ORDERS_FILE = Path.of("orders.json").toAbsolutePath().normalize();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/checkout", CheckoutServer::handleCheckout);
        server.createContext("/", CheckoutServer::handleStatic);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("Checkout Java backend running at http://localhost:" + PORT);
        System.out.println("Serving files from: " + WEB_ROOT);
    }

    private static void handleCheckout(HttpExchange exchange) throws IOException {
        addCors(exchange.getResponseHeaders());

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 204, "text/plain; charset=utf-8", "");
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"status\":\"error\",\"message\":\"Method not allowed\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> data = parseBody(body, exchange.getRequestHeaders().getFirst("Content-Type"));

        String cardName = data.getOrDefault("card-name", "");
        String cardNumber = data.getOrDefault("card-number", "");
        String expiry = data.getOrDefault("expiry", "");
        String cvv = data.getOrDefault("cvv", "");

        if (cardName.isBlank() || cardNumber.isBlank() || expiry.isBlank() || cvv.isBlank()) {
            sendJson(exchange, 400, "{\"status\":\"error\",\"message\":\"Missing required fields\"}");
            return;
        }

        String orderJson = "{"
                + "\"receivedAt\":\"" + escapeJson(Instant.now().toString()) + "\","
                + "\"cardName\":\"" + escapeJson(cardName) + "\","
                + "\"cardNumberMasked\":\"" + escapeJson(maskCard(cardNumber)) + "\","
                + "\"expiry\":\"" + escapeJson(expiry) + "\","
                + "\"cvvProvided\":" + (!cvv.isBlank())
                + "}";

        appendOrder(orderJson);
        System.out.println("New checkout order from: " + cardName);
        sendJson(exchange, 200, "{\"status\":\"ok\",\"message\":\"Order received\"}");
    }

    private static void handleStatic(HttpExchange exchange) throws IOException {
        addCors(exchange.getResponseHeaders());

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath == null || requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        // Prevent path traversal
        Path requested = WEB_ROOT.resolve(requestPath.substring(1)).normalize();
        if (!requested.startsWith(WEB_ROOT) || !Files.exists(requested) || Files.isDirectory(requested)) {
            send(exchange, 404, "text/plain; charset=utf-8", "Not found");
            return;
        }

        String contentType = guessContentType(requested);
        byte[] bytes = Files.readAllBytes(requested);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseBody(String body, String contentType) {
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            return parseJsonObject(body);
        }
        return parseFormUrlEncoded(body);
    }

    private static Map<String, String> parseFormUrlEncoded(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isBlank()) return map;
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            String key = urlDecode(pair.substring(0, idx));
            String value = urlDecode(pair.substring(idx + 1));
            map.put(key, value);
        }
        return map;
    }

    // Minimal JSON object parser for flat string values: {"key":"value"}
    private static Map<String, String> parseJsonObject(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null) return map;
        String s = body.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);

        int i = 0;
        while (i < s.length()) {
            while (i < s.length() && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) i++;
            if (i >= s.length() || s.charAt(i) != '"') { i++; continue; }
            i++;
            String key = readJsonString(s, i);
            i = skipJsonString(s, i);
            while (i < s.length() && s.charAt(i) != ':') i++;
            i++; // skip ':'
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            String value;
            if (i < s.length() && s.charAt(i) == '"') {
                i++;
                value = readJsonString(s, i);
                i = skipJsonString(s, i);
            } else {
                int start = i;
                while (i < s.length() && s.charAt(i) != ',' && s.charAt(i) != '}') i++;
                value = s.substring(start, i).trim();
            }
            map.put(key, value);
        }
        return map;
    }

    private static String readJsonString(String s, int start) {
        StringBuilder out = new StringBuilder();
        int i = start;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') break;
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == '"' || n == '\\' || n == '/') { out.append(n); i += 2; continue; }
                if (n == 'n') { out.append('\n'); i += 2; continue; }
                if (n == 'r') { out.append('\r'); i += 2; continue; }
                if (n == 't') { out.append('\t'); i += 2; continue; }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static int skipJsonString(String s, int start) {
        int i = start;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') return i + 1;
            if (c == '\\' && i + 1 < s.length()) { i += 2; continue; }
            i++;
        }
        return i;
    }

    private static synchronized void appendOrder(String orderJson) throws IOException {
        String existing = Files.exists(ORDERS_FILE) ? Files.readString(ORDERS_FILE, StandardCharsets.UTF_8).trim() : "";
        String next;
        if (existing.isEmpty() || existing.equals("[]")) {
            next = "[\n  " + orderJson + "\n]\n";
        } else if (existing.endsWith("]")) {
            next = existing.substring(0, existing.length() - 1).trim();
            if (next.endsWith("[")) {
                next = next + "\n  " + orderJson + "\n]\n";
            } else {
                next = next + ",\n  " + orderJson + "\n]\n";
            }
        } else {
            next = "[\n  " + orderJson + "\n]\n";
        }
        Files.writeString(ORDERS_FILE, next, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String maskCard(String cardNumber) {
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() <= 4) return "****";
        return "**** **** **** " + digits.substring(digits.length() - 4);
    }

    private static String guessContentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private static void addCors(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        send(exchange, status, "application/json; charset=utf-8", json);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
