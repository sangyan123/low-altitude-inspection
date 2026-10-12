import request from "../utils/request";

export interface EmergencyTarget {
  id?: number | null;
  emergencyEventId?: number | null;
  targetName: string;
  targetType?: number | null;
  lng?: number | string | null;
  lat?: number | string | null;
  foundByDevice?: number | null;
  foundByDeviceName?: string;
  foundTime?: string;
  status?: number | null;
  statusName?: string;
  tracking?: number | null;
  shared?: number | null;
  remark?: string;
  createTime?: string;
}

export interface EmergencyEvent {
  id?: number | null;
  eventName: string;
  eventType?: number | null;
  level?: number | null;
  reportUserId?: number | null;
  reporterName?: string;
  lng?: number | string | null;
  lat?: number | string | null;
  locationDesc?: string;
  description?: string;
  status?: number | null;
  createTime?: string;
  endTime?: string;
  closeRemark?: string;
  assignedDeviceId?: number | null;
  assignedDeviceName?: string;
  dispatchUserId?: number | null;
  dispatchUserName?: string;
  dispatchTime?: string;
  dispatchRemark?: string;
  updateTime?: string;
  targetCount?: number;
  trackingCount?: number;
  targets?: EmergencyTarget[];
}

export function getEmergencyPage(params: any) {
  return request.get("/emergency/page", { params });
}

export function getEmergencyDetail(id: number) {
  return request.get(`/emergency/${id}`);
}

export function createEmergency(data: EmergencyEvent) {
  return request.post("/emergency", data);
}

export function updateEmergency(data: EmergencyEvent) {
  return request.put("/emergency", data);
}

export function dispatchEmergency(id: number, data: { deviceId: number; remark?: string }) {
  return request.post(`/emergency/${id}/dispatch`, data);
}

export function finishEmergency(id: number, data?: { remark?: string }) {
  return request.post(`/emergency/${id}/finish`, data || {});
}

export function deleteEmergency(id: number) {
  return request.delete(`/emergency/${id}`);
}

export function getEmergencyTargets(eventId: number) {
  return request.get(`/emergency/${eventId}/target/list`);
}

export function createEmergencyTarget(data: EmergencyTarget) {
  return request.post("/emergency/target", data);
}

export function updateEmergencyTarget(data: EmergencyTarget) {
  return request.put("/emergency/target", data);
}

export function setEmergencyTargetTracking(id: number, tracking: boolean) {
  return request.post(`/emergency/target/${id}/tracking`, { tracking });
}

export function deleteEmergencyTarget(id: number) {
  return request.delete(`/emergency/target/${id}`);
}