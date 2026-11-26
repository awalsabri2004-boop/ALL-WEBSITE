from flask import Flask, request, jsonify
from flask_cors import CORS
import json
import os
import smtplib
from dotenv import load_dotenv
from datetime import datetime
from email.message import EmailMessage

app = Flask(__name__)
CORS(app)  # allows the website to send data to this server

FILE_NAME = "bookings.json"

load_dotenv()  # reads credentials from a local .env file

# Email Configuration — stored safely in environment variables
MY_EMAIL = os.getenv("SENDER_EMAIL", "")
APP_PASSWORD = os.getenv("SENDER_PASSWORD", "")


def send_confirmation(to_email, name):
    msg = EmailMessage()
    msg["Subject"] = "We got your booking request!"
    msg["From"] = MY_EMAIL
    msg["To"] = to_email
    msg.set_content(
        f"Hi {name},\n\n"
        "Thanks for your booking request at Nomad & Bean. "
        "We'll get back to you within 24 hours.\n\n"
        "- The Camper Cafe Team"
    )

    with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
        server.login(MY_EMAIL, APP_PASSWORD)
        server.send_message(msg)


@app.route("/api/booking", methods=["POST"])
def receive_booking():
    data = request.get_json()
    data["received_at"] = datetime.now().isoformat()

    # 1. Save the booking
    bookings = []
    if os.path.exists(FILE_NAME):
        with open(FILE_NAME, "r", encoding="utf-8") as f:
            bookings = json.load(f)

    bookings.append(data)

    with open(FILE_NAME, "w", encoding="utf-8") as f:
        json.dump(bookings, f, indent=2, ensure_ascii=False)

    print("New booking from:", data.get("name"))

    # 2. Send confirmation email (booking is already saved even if this fails)
    try:
        send_confirmation(data.get("email"), data.get("name"))
    except Exception as e:
        print("Email failed:", e)

    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(port=5000, debug=True)