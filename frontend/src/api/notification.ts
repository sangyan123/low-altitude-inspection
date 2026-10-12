import request from "../utils/request";

export interface SysNotification {
  id: number;
  receiverId: number;
  title: string;
  content?: string;
  notifyType: number;
  relatedId?: number | null;
  isRead: number;
  createTime: string;
}

export function getNotificationPage(params: { current: number; size: number; isRead?: number | null }) {
  return request.get("/notification/page", { params });
}

export function getUnreadCount() {
  return request.get("/notification/unread-count");
}

export function markNotificationRead(id: number) {
  return request.post(`/notification/${id}/read`);
}

export function markAllNotificationsRead() {
  return request.post("/notification/read-all");
}