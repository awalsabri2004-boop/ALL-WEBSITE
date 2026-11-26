from flask import Flask, render_template, request, redirect
import sqlite3

app = Flask(__name__)

def init_db():
    conn = sqlite3.connect("survey.db")
    conn.execute("""
        CREATE TABLE IF NOT EXISTS responses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT,
            answer TEXT
        )
    """)
    conn.commit()
    conn.close()

@app.route("/")
def home():
    return render_template("index.html")

@app.route("/submit", methods=["POST"])
def submit():
    name = request.form["name"]
    answer = request.form["answer"]
    conn = sqlite3.connect("survey.db")
    conn.execute("INSERT INTO responses (name, answer) VALUES (?, ?)", (name, answer))
    conn.commit()
    conn.close()
    return redirect("/results")

@app.route("/results")
def results():
    conn = sqlite3.connect("survey.db")
    rows = conn.execute("SELECT name, answer FROM responses").fetchall()
    conn.close()
    return render_template("results.html", rows=rows)

if __name__ == "__main__":
    init_db()
    app.run(debug=True)