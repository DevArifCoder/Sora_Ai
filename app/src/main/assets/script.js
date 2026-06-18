const API_BASE = "http://127.0.0.1:5000";

let currentChatId = null;
let isRecording = false;

const sidebar = document.getElementById("sidebar");
const overlay = document.getElementById("overlay");
const chatList = document.getElementById("chatList");
const messagesEl = document.getElementById("messages");
const textInput = document.getElementById("textInput");
const partialBar = document.getElementById("partialBar");
const micBtn = document.getElementById("micBtn");

// ---------- Sidebar open/close ----------
// Important: overlay (faka jaiga) tap korle sidebar bondho hobe,
// kintu kono chat select hobe na. Eta ager bug fix kore -
// list item ar overlay er click handler completely alada.
document.getElementById("menuBtn").addEventListener("click", openSidebar);
overlay.addEventListener("click", closeSidebar);

function openSidebar() {
  sidebar.classList.add("open");
  overlay.classList.add("show");
  loadChats();
}

function closeSidebar() {
  sidebar.classList.remove("open");
  overlay.classList.remove("show");
}

// ---------- Chat list ----------
async function loadChats() {
  try {
    const res = await fetch(`${API_BASE}/chats`);
    const chats = await res.json();
    renderChatList(chats);
  } catch (e) {
    chatList.innerHTML = `<div class="chat-item">Server a connect hocche na. Termux a server চালু আছে?</div>`;
  }
}

function renderChatList(chats) {
  chatList.innerHTML = "";
  chats.forEach((chat) => {
    const item = document.createElement("div");
    item.className = "chat-item" + (chat.id === currentChatId ? " active" : "");

    const title = document.createElement("div");
    title.className = "chat-item-title";
    title.textContent = chat.title || "New chat";
    // Tap on the title/row -> just load that chat. Nothing else.
    title.addEventListener("click", () => {
      loadMessages(chat.id);
      closeSidebar();
    });

    const delBtn = document.createElement("button");
    delBtn.className = "chat-item-delete";
    delBtn.textContent = "✕";
    delBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      deleteChat(chat.id);
    });

    item.appendChild(title);
    item.appendChild(delBtn);
    chatList.appendChild(item);
  });
}

async function deleteChat(chatId) {
  await fetch(`${API_BASE}/chats/${chatId}`, { method: "DELETE" });
  if (chatId === currentChatId) {
    currentChatId = null;
    messagesEl.innerHTML = "";
  }
  loadChats();
}

async function newChat() {
  const res = await fetch(`${API_BASE}/chats`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title: "New chat" }),
  });
  const chat = await res.json();
  currentChatId = chat.id;
  messagesEl.innerHTML = "";
  closeSidebar();
  loadChats();
}

document.getElementById("newChatBtn").addEventListener("click", newChat);

// ---------- Messages ----------
async function loadMessages(chatId) {
  currentChatId = chatId;
  const res = await fetch(`${API_BASE}/chats/${chatId}/messages`);
  const msgs = await res.json();
  messagesEl.innerHTML = "";
  msgs.forEach((m) => appendBubble(m.role, m.content));
  scrollToBottom();
}

function appendBubble(role, text) {
  const bubble = document.createElement("div");
  bubble.className = `msg ${role}`;
  bubble.textContent = text;
  messagesEl.appendChild(bubble);
  scrollToBottom();
  return bubble;
}

function scrollToBottom() {
  // Auto scroll after layout settles
  requestAnimationFrame(() => {
    messagesEl.scrollTop = messagesEl.scrollHeight;
  });
}

async function sendMessage(text) {
  if (!text.trim()) return;

  if (!currentChatId) {
    const res = await fetch(`${API_BASE}/chats`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title: "New chat" }),
    });
    const chat = await res.json();
    currentChatId = chat.id;
  }

  appendBubble("user", text);
  textInput.value = "";
  autoResizeInput();

  const typingBubble = appendBubble("assistant", "...");
  typingBubble.classList.add("typing");

  try {
    const res = await fetch(`${API_BASE}/chats/${currentChatId}/messages`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content: text }),
    });
    const data = await res.json();
    typingBubble.textContent = data.content;
    typingBubble.classList.remove("typing");
  } catch (e) {
    typingBubble.textContent = "Server a connect kora gelo na. Termux a Ollama + server চালু আছে কিনা চেক করো।";
    typingBubble.classList.remove("typing");
  }
  scrollToBottom();
}

document.getElementById("sendBtn").addEventListener("click", () => {
  sendMessage(textInput.value);
});

textInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    sendMessage(textInput.value);
  }
});

textInput.addEventListener("input", autoResizeInput);

function autoResizeInput() {
  textInput.style.height = "auto";
  textInput.style.height = Math.min(textInput.scrollHeight, 110) + "px";
}

// ---------- Mic (native bridge to MainActivity.kt) ----------
micBtn.addEventListener("click", () => {
  if (!isRecording) {
    isRecording = true;
    micBtn.classList.add("recording");
    partialBar.style.display = "block";
    partialBar.textContent = "Shunchi...";
    if (window.Android && Android.startListening) {
      Android.startListening();
    }
  } else {
    stopRecordingUI();
    if (window.Android && Android.stopListening) {
      Android.stopListening();
    }
  }
});

function stopRecordingUI() {
  isRecording = false;
  micBtn.classList.remove("recording");
  partialBar.style.display = "none";
}

// These functions are called directly from MainActivity.kt via evaluateJavascript
function onSpeechPartial(text) {
  partialBar.textContent = text;
}

function onSpeechResult(text) {
  stopRecordingUI();
  if (text && text.trim()) {
    sendMessage(text);
  }
}

function onSpeechError(message) {
  stopRecordingUI();
  partialBar.style.display = "none";
  console.log("Speech error:", message);
}

// ---------- Background mic toggle (top bar dot) ----------
let backgroundOn = false;
document.getElementById("micStatusBtn").addEventListener("click", () => {
  backgroundOn = !backgroundOn;
  const btn = document.getElementById("micStatusBtn");
  if (backgroundOn) {
    btn.style.color = "#1f8e6f";
    if (window.Android && Android.startBackgroundMic) Android.startBackgroundMic();
  } else {
    btn.style.color = "#e7e9ee";
    if (window.Android && Android.stopBackgroundMic) Android.stopBackgroundMic();
  }
});

// ---------- Init ----------
loadChats();
