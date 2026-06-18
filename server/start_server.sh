#!/data/data/com.termux/files/usr/bin/bash
# Ei script Termux a run korba: bash start_server.sh

echo "Ollama server starting..."
ollama serve &
sleep 3

echo "Flask bridge server starting on 127.0.0.1:5000 ..."
cd "$(dirname "$0")"
python app.py
