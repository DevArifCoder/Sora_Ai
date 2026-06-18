from flask import Flask, request, jsonify
from flask_cors import CORS
import sqlite3
import requests
import time
import os

app = Flask(__name__)
CORS(app)

DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "chats.db")
OLLAMA_URL = "http://127.0.0.1:11434/api/chat"
MODEL_NAME = "qwen2.5:1.5b"


def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = get_db()
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS chats (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT,
            created_at INTEGER
        )
        """
    )
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            chat_id INTEGER,
            role TEXT,
            content TEXT,
            created_at INTEGER,
            FOREIGN KEY(chat_id) REFERENCES chats(id)
        )
        """
    )
    conn.commit()
    conn.close()


@app.route("/chats", methods=["GET"])
def list_chats():
    conn = get_db()
    rows = conn.execute("SELECT * FROM chats ORDER BY created_at DESC").fetchall()
    conn.close()
    return jsonify([dict(r) for r in rows])


@app.route("/chats", methods=["POST"])
def create_chat():
    data = request.get_json(silent=True) or {}
    title = data.get("title", "New chat")
    conn = get_db()
    cur = conn.execute(
        "INSERT INTO chats (title, created_at) VALUES (?, ?)",
        (title, int(time.time())),
    )
    conn.commit()
    chat_id = cur.lastrowid
    conn.close()
    return jsonify({"id": chat_id, "title": title})


@app.route("/chats/<int:chat_id>", methods=["DELETE"])
def delete_chat(chat_id):
    conn = get_db()
    conn.execute("DELETE FROM messages WHERE chat_id=?", (chat_id,))
    conn.execute("DELETE FROM chats WHERE id=?", (chat_id,))
    conn.commit()
    conn.close()
    return jsonify({"ok": True})


@app.route("/chats/<int:chat_id>/messages", methods=["GET"])
def get_messages(chat_id):
    conn = get_db()
    rows = conn.execute(
        "SELECT * FROM messages WHERE chat_id=? ORDER BY created_at ASC",
        (chat_id,),
    ).fetchall()
    conn.close()
    return jsonify([dict(r) for r in rows])


@app.route("/chats/<int:chat_id>/messages", methods=["POST"])
def post_message(chat_id):
    data = request.get_json(silent=True) or {}
    user_text = (data.get("content") or "").strip()
    if not user_text:
        return jsonify({"error": "empty message"}), 400

    conn = get_db()
    conn.execute(
        "INSERT INTO messages (chat_id, role, content, created_at) VALUES (?, 'user', ?, ?)",
        (chat_id, user_text, int(time.time())),
    )
    conn.commit()

    history_rows = conn.execute(
        "SELECT role, content FROM messages WHERE chat_id=? ORDER BY created_at ASC",
        (chat_id,),
    ).fetchall()
    ollama_messages = [{"role": r["role"], "content": r["content"]} for r in history_rows]

    try:
        resp = requests.post(
            OLLAMA_URL,
            json={"model": MODEL_NAME, "messages": ollama_messages, "stream": False},
            timeout=120,
        )
        resp.raise_for_status()
        reply_text = resp.json().get("message", {}).get("content", "(no reply)")
    except Exception as e:
        reply_text = f"Ollama server a connect kora gelo na: {e}"

    conn.execute(
        "INSERT INTO messages (chat_id, role, content, created_at) VALUES (?, 'assistant', ?, ?)",
        (chat_id, reply_text, int(time.time())),
    )

    chat_row = conn.execute("SELECT title FROM chats WHERE id=?", (chat_id,)).fetchone()
    if chat_row and chat_row["title"] == "New chat":
        new_title = user_text[:40]
        conn.execute("UPDATE chats SET title=? WHERE id=?", (new_title, chat_id))

    conn.commit()
    conn.close()
    return jsonify({"role": "assistant", "content": reply_text})


if __name__ == "__main__":
    init_db()
    app.run(host="127.0.0.1", port=5000)
