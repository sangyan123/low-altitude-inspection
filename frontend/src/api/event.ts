import request from "../utils/request";
import { getDeptList } from "./system";

export interface EventHandleLog {
  id?: number;
  eventId?: number;
  action: string;
  operatorId?: number;
  operatorName?: string;
  remark?: string;
  createTime?: string;
}

export interface EventInfo {
  id: number;
  eventNo: string;
  eventType: string;
  eventSource: number;
  recognitionId?: number | null;
  deptId?: number | null;
  deptName?: string;
  status: number;
  pushSystem?: string;
  pushTime?: string;
  dispatchTime?: string;
  handleTime?: string;
  handleRemark?: string;
  handleUserId?: number | null;
  handleUserName?: string;
  verifyUserId?: number | null;
  verifyUserName?: string;
  verifyTime?: string;
  verifyResult?: number | null;
  verifyRemark?: string;
  closeTime?: string;
  lng?: number | string | null;
  lat?: number | string | null;
  description?: string;
  submitUserId?: number | null;
  submitUserName?: string;
  createTime?: string;
  updateTime?: string;
  imageUrl?: string;
  resultType?: string;
  confidence?: number | string;
  provider?: string;
  modelName?: string;
  rawResponse?: string;
  handleLogs?: EventHandleLog[];
}

export interface EventQuery {
  current: number;
  size: number;
  eventType?: string;
  eventSource?: number | null;
  deptId?: number | null;
  status?: number | null;
  keyword?: string;
  startDate?: string;
  endDate?: string;
}

export function getEventPage(params: EventQuery) {
  return request.get("/event/page", { params });
}

export function getEventDetail(id: number) {
  return request.get(`/event/${id}`);
}

export function createEvent(data: {
  eventType: string;
  description: string;
  lng?: number | null;
  lat?: number | null;
  deptId?: number | null;
  remark?: string;
}) {
  return request.post("/event", data);
}

export function dispatchEvent(id: number, data: { deptId: number; remark?: string }) {
  return request.post(`/event/${id}/dispatch`, data);
}

export function handleEvent(id: number, data: { remark: string }) {
  return request.post(`/event/${id}/handle`, data);
}

export function verifyEvent(id: number, data: { result: number; remark?: string }) {
  return request.post(`/event/${id}/verify`, data);
}

export function closeEvent(id: number, data?: { remark?: string }) {
  return request.post(`/event/${id}/close`, data || {});
}

export function deleteEvent(id: number) {
  return request.delete(`/event/${id}`);
}

export function loadEventDepts() {
  return getDeptList();
}