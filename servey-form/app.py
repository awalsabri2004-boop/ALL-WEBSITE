from flask import Flask, render_template, request, jsonify
import sqlite3
import smtplib
import os
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from dotenv import load_dotenv

load_dotenv()  # Loads credentials from .env file

app = Flask(__name__)

# Email Configuration — stored safely in .env
SENDER_EMAIL    = os.getenv("SENDER_EMAIL", "")
SENDER_PASSWORD = os.getenv("SENDER_PASSWORD", "")

def send_confirmation_email(recipient_email, recipient_name):
    try:
        # Setup MIME
        message = MIMEMultipart("alternative")
        message["Subject"] = "Thank you for your submission!"
        message["From"] = SENDER_EMAIL
        message["To"] = recipient_email
        # Email body content
        text = f"Hi {recipient_name},\n\nThank you for submitting the form! We have received your details safely.\n\nBest regards,\nYour Team"
        part = MIMEText(text, "plain")
        message.attach(part)
        # Connect to Gmail SMTP server
        with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
            server.login(SENDER_EMAIL, SENDER_PASSWORD)
            server.sendmail(SENDER_EMAIL, recipient_email, message.as_string())
        print(f"Confirmation email successfully sent to {recipient_email}")
    except Exception as e:
        print(f"Failed to send email: {e}")



@app.route('/')

def index():

    return render_template('index.html')



@app.route('/submit', methods=['POST'])

def submit():

    # 1. Retrieve data from the frontend

    nama = request.form.get('name')

    email = request.form.get('email')

   

    # 2. Connect to SQLite and save data

    conn = sqlite3.connect('survey_data.db')

    cursor = conn.cursor()

    cursor.execute("""

        CREATE TABLE IF NOT EXISTS responses (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            nama TEXT,

            email TEXT

        )

    """)

    cursor.execute("INSERT INTO responses (nama, email) VALUES (?, ?)", (nama, email))

    conn.commit()

    conn.close()

   

    # 3. Send automated email response back to the user

    if email:

        send_confirmation_email(email, nama)

   

    print(f"Data saved and email triggered for: {nama} ({email})")

   

    # 4. Return JSON so the frontend fetch() can read it
    return jsonify({"message": f"Thank you, {nama}! Your data has been saved and a confirmation email has been sent."})



if __name__ == '__main__':

    app.run(debug=True)