# Sora AI — Setup Guide

## Ei project a 2 ta part ase

1. **`app/`** — Native Android app (Kotlin). Eta hocche tomar APK, jeta phone a install hobe. Eta WebView a chat UI dekhay, mic listen kore, ar accessibility service diye phone control kore.
2. **`server/`** — Termux a run hobe. Flask server jeta Ollama er sathe kotha bole ar chat history SQLite a save kore.

Termux + APK duitai same phone a thakte hobe, ar Termux a server চালু থাকা lagbe jokhon APK app ta use korba (karon APK 127.0.0.1:5000 a request pathay, jeta Termux serve kore).

---

## Step 1 — Termux side

```bash
pkg install python -y
pip install -r server/requirements.txt
```

Ollama already install kora ase (screenshot a dekha gese). Server chalu korte:

```bash
bash server/start_server.sh
```

Eta Ollama (port 11434) ar Flask bridge (port 5000) duitai chalu korbe. Eta background a rakhar jonno `tmux` use korte paro, noile Termux app minimize korle process kill hoye jete pare — `pkg install tmux` kore `tmux new -s sora` er moddhe run korle beshi stable thakbe.

---

## Step 2 — APK build kora (GitHub Actions diye, phone a heavy build na kore)

1. GitHub a notun ekta repo banao.
2. Ei `SoraAI` folder ta push koro oi repo a (Termux theke `git` use kore, tumi already PAT auth setup janoo).
3. GitHub repo er **Actions** tab a giye dekhbe "Build APK" workflow automatically run hocche (push korar por).
4. Workflow shesh hole, oi run er "Artifacts" section theke `sora-ai-debug-apk` download korte parba — eta tomar APK.
5. APK ta phone a download kore install koro (Unknown sources install allow korte hobe).

Eta diye buildozer/local Gradle er heavy download/compile phone a korte hobe na — build hobe GitHub er cloud machine a, free.

---

## Step 3 — Permissions on the phone

App open korar por:

- **Microphone** permission allow koro (popup ashbe).
- **Notification** permission allow koro (background indicator dekhanor jonno).
- Voice-control feature (home/back, screen text a click) use korte chaile: phone Settings → Accessibility → "Sora AI" খুঁজে on koro. App er ভিতরেও ekta button thakte pare (`openAccessibilitySettings()` bridge) jeta direct oi page a niye jay.

---

## Notes

- **Speech-to-text**: Ekhon Android er built-in `SpeechRecognizer` use kora hoyeche (`EXTRA_PREFER_OFFLINE = true`). Kichu phone a eta fully offline kaj kore, kichu phone a Google app er offline language pack download na thakle internet lagte pare. Pura offline guarantee korte chaile next step a **Vosk** (`alphacephei/vosk-android`) integrate kora jay — chhoto (~50MB) fully-offline model, kintu setup ektu beshi (native model file load korte hobe). Bolle oi version ta banaiya debo.
- **Chat history** ekhon Termux server er SQLite (`chats.db`) a thake — tai app reload/restart hole o history thake, age er moto memory-only thaka na.
- **Delete**: chat list a ✕ button a tap korle oi chat permanently delete hoye jay (Gemini er moto).
- **Scroll**: messages container properly `overflow-y: auto` set kora ase, notun message ashar por auto bottom a scroll hoy.
