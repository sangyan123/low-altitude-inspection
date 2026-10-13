import { beforeEach, describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useNotificationStore } from "../notification";

const makeNotification = (id: number, isRead = 0) => ({
  id,
  receiverId: 1,
  title: `通知${id}`,
  content: "内容",
  notifyType: 1,
  relatedId: null,
  isRead,
  createTime: "2026-10-13 10:00:00",
});

/** 消息通知中心store单元测试 */
describe("notification store", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it("初始状态：未连接、未读为0、消息为空", () => {
    const store = useNotificationStore();
    expect(store.connected).toBe(false);
    expect(store.connecting).toBe(false);
    expect(store.unreadCount).toBe(0);
    expect(store.recent).toEqual([]);
  });

  it("setUnreadCount：非法值兜底为0", () => {
    const store = useNotificationStore();
    store.setUnreadCount(undefined as unknown as number);
    expect(store.unreadCount).toBe(0);
    store.setUnreadCount(5);
    expect(store.unreadCount).toBe(5);
  });

  it("prependNotification：新未读消息置顶且未读+1", () => {
    const store = useNotificationStore();
    store.setRecent([makeNotification(1)]);
    store.setUnreadCount(1);
    store.prependNotification(makeNotification(2));
    expect(store.recent[0].id).toBe(2);
    expect(store.recent).toHaveLength(2);
    expect(store.unreadCount).toBe(2);
  });

  it("prependNotification：已读消息不增加未读数", () => {
    const store = useNotificationStore();
    store.prependNotification(makeNotification(3, 1));
    expect(store.unreadCount).toBe(0);
  });

  it("recent列表最多保留20条", () => {
    const store = useNotificationStore();
    for (let i = 1; i <= 25; i++) {
      store.prependNotification(makeNotification(i, 1));
    }
    expect(store.recent).toHaveLength(20);
    expect(store.recent[0].id).toBe(25);
  });

  it("markRead：仅第一次标记生效，重复调用不重复扣减", () => {
    const store = useNotificationStore();
    store.prependNotification(makeNotification(1));
    store.markRead(1);
    expect(store.recent[0].isRead).toBe(1);
    expect(store.unreadCount).toBe(0);
    store.markRead(1);
    expect(store.unreadCount).toBe(0);
  });

  it("markRead：不存在的id无副作用", () => {
    const store = useNotificationStore();
    store.setUnreadCount(3);
    store.markRead(999);
    expect(store.unreadCount).toBe(3);
  });

  it("markAllRead：全部置已读且未读清零", () => {
    const store = useNotificationStore();
    store.prependNotification(makeNotification(1));
    store.prependNotification(makeNotification(2));
    store.markAllRead();
    expect(store.unreadCount).toBe(0);
    expect(store.recent.every((n) => n.isRead === 1)).toBe(true);
  });

  it("连接状态标志可切换", () => {
    const store = useNotificationStore();
    store.setConnected(true);
    store.setConnecting(true);
    expect(store.connected).toBe(true);
    expect(store.connecting).toBe(true);
  });
});
