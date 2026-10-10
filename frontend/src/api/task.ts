import request from "../utils/request";

// ========= 巡检任务 =========
export function getPatrolPage(params: any) {
  return request.get("/task/patrol/page", { params });
}
export function createPatrol(data: any) {
  return request.post("/task/patrol", data);
}
export function updatePatrol(data: any) {
  return request.put("/task/patrol", data);
}
export function deletePatrol(id: number) {
  return request.delete(`/task/patrol/${id}`);
}
export function dispatchPatrol(id: number, deviceId: number, userId: number) {
  return request.post(`/task/patrol/${id}/dispatch`, { deviceId, userId });
}

// ========= 运输任务 =========
export function getTransportPage(params: any) {
  return request.get("/task/transport/page", { params });
}
export function createTransport(data: any) {
  return request.post("/task/transport", data);
}
export function updateTransport(data: any) {
  return request.put("/task/transport", data);
}
export function deleteTransport(id: number) {
  return request.delete(`/task/transport/${id}`);
}
export function departTransport(id: number) {
  return request.put(`/task/transport/${id}/depart`);
}
export function arriveTransport(id: number) {
  return request.put(`/task/transport/${id}/arrive`);
}
export function signTransport(id: number) {
  return request.put(`/task/transport/${id}/sign`);
}

// ========= 航线库 =========
export function getRouteList(params?: any) {
  return request.get("/task/route/list", { params });
}
export function createRoute(data: any) {
  return request.post("/task/route", data);
}
export function updateRoute(data: any) {
  return request.put("/task/route", data);
}
export function deleteRoute(id: number) {
  return request.delete(`/task/route/${id}`);
}

// ========= 飞行计划 =========
export function getPlanPage(params: any) {
  return request.get("/task/plan/page", { params });
}
export function createPlan(data: any) {
  return request.post("/task/plan", data);
}
export function updatePlan(data: any) {
  return request.put("/task/plan", data);
}
export function deletePlan(id: number) {
  return request.delete(`/task/plan/${id}`);
}
export function approvePlan(id: number, approvalStatus: number, remark?: string) {
  return request.post(`/task/plan/${id}/approve`, { approvalStatus, remark });
}
