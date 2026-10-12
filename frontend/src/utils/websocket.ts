import { useNotificationStore } from "../stores/notification";
import { ElNotification } from "element-plus";
import type { SysNotification } from "../api/notification";

let socket: WebSocket | null = null;
let reconnectTimer: number | undefined;
let heartbeatTimer: number | undefined;
let retryCount = 0;

function buildSocketUrl() {
  const token = localStorage.getItem("token");
  if (!token) return "";
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const host = window.location.hostname === "localhost" ? "localhost:8080" : window.location.host;
  return `${protocol}//${host}/api/ws/notification?token=${encodeURIComponent(token)}`;
}

function handleMessage(event: MessageEvent) {
  try {
    const data = JSON.parse(event.data) as Partial<SysNotification> & { type?: string; title?: string };
    const store = useNotificationStore();
    if (data.type === "CONNECTED") {
      store.setConnected(true);
      return;
    }
    if (data.type === "PONG") return;
    if (data.type === "NOTIFICATION") {
      const notification: SysNotification = {
        id: Number(data.id),
        receiverId: 0,
        title: data.title || "系统通知",
        content: data.content,
        notifyType: Number(data.notifyType || 1),
        relatedId: data.relatedId,
        isRead: 0,
        createTime: String(data.createTime || new Date().toLocaleString()),
      };
      store.prependNotification(notification);
      ElNotification({
        title: notification.title,
        message: notification.content || "您有一条新的实时通知",
        type: notification.notifyType === 6 ? "warning" : "info",
      });
    }
  } catch (error) {
    console.warn("Invalid websocket message", error);
  }
}

function scheduleReconnect() {
  if (reconnectTimer) return;
  retryCount += 1;
  const delay = Math.min(30000, 1000 * 2 ** Math.min(retryCount, 5));
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = undefined;
    connectNotificationSocket();
  }, delay);
}

function startHeartbeat() {
  if (heartbeatTimer) window.clearInterval(heartbeatTimer);
  heartbeatTimer = window.setInterval(() => {
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: "PING" }));
    }
  }, 30000);
}

export function connectNotificationSocket() {
  const token = localStorage.getItem("token");
  if (!token || socket || window.location.pathname === "/login") return;

  const url = buildSocketUrl();
  if (!url) return;

  const store = useNotificationStore();
  store.setConnecting(true);
  socket = new WebSocket(url);

  socket.onopen = () => {
    retryCount = 0;
    store.setConnected(true);
    store.setConnecting(false);
    startHeartbeat();
  };

  socket.onmessage = handleMessage;

  socket.onclose = () => {
    socket = null;
    store.setConnected(false);
    scheduleReconnect();
  };

  socket.onerror = () => {
    socket?.close();
  };
}

export function disconnectNotificationSocket() {
  if (reconnectTimer) {
    window.clearTimeout(reconnectTimer);
    reconnectTimer = undefined;
  }
  if (heartbeatTimer) {
    window.clearInterval(heartbeatTimer);
    heartbeatTimer = undefined;
  }
  socket?.close();
  socket = null;
}