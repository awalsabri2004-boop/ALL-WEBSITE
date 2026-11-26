# Checkout Java Backend

Simple Java backend for `checkout-order-site` using the JDK built-in `com.sun.net.httpserver.HttpServer` (no Maven required).

## Run

From this folder:

```bash
javac CheckoutServer.java
java CheckoutServer
```

Then open http://localhost:8080

## What it does

- Serves `../index.html` and static files from the checkout site folder.
- Accepts `POST /api/checkout` with form-encoded or JSON data.
- Saves orders to `orders.json` in this folder.
- Masks card numbers before saving (keeps only last 4 digits).
