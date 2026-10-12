import { defineStore } from "pinia";
import type { SysNotification } from "../api/notification";

interface NotificationState {
  connected: boolean;
  connecting: boolean;
  unreadCount: number;
  recent: SysNotification[];
}

export const useNotificationStore = defineStore("notification", {
  state: (): NotificationState => ({
    connected: false,
    connecting: false,
    unreadCount: 0,
    recent: [],
  }),
  actions: {
    setConnected(value: boolean) {
      this.connected = value;
    },
    setConnecting(value: boolean) {
      this.connecting = value;
    },
    setUnreadCount(value: number) {
      this.unreadCount = Number(value || 0);
    },
    setRecent(value: SysNotification[]) {
      this.recent = value;
    },
    prependNotification(notification: SysNotification) {
      this.recent = [notification, ...this.recent].slice(0, 20);
      if (notification.isRead === 0) this.unreadCount += 1;
    },
    markRead(id: number) {
      const item = this.recent.find((item) => item.id === id);
      if (item && item.isRead === 0) {
        item.isRead = 1;
        this.unreadCount = Math.max(0, this.unreadCount - 1);
      }
    },
    markAllRead() {
      this.recent.forEach((item) => {
        item.isRead = 1;
      });
      this.unreadCount = 0;
    },
  },
});