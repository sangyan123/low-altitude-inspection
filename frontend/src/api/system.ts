import request from "../utils/request";

// ========= Auth =========
export function login(data: { username: string; password: string }) {
  return request.post("/auth/login", data);
}

// ========= Dept =========
export function getDeptList(keyword?: string) {
  return request.get("/system/dept/list", { params: { keyword } });
}
export function createDept(data: any) {
  return request.post("/system/dept", data);
}
export function updateDept(data: any) {
  return request.put("/system/dept", data);
}
export function deleteDept(id: number) {
  return request.delete(`/system/dept/${id}`);
}

// ========= User =========
export function getUserPage(params: any) {
  return request.get("/system/user/page", { params });
}
export function createUser(data: any) {
  return request.post("/system/user", data);
}
export function updateUser(data: any) {
  return request.put("/system/user", data);
}
export function deleteUser(id: number) {
  return request.delete(`/system/user/${id}`);
}
export function resetPassword(id: number, password: string) {
  return request.put(`/system/user/${id}/reset-password`, { password });
}

// ========= Role =========
export function getRoleList() {
  return request.get("/system/role/list");
}
export function createRole(data: any) {
  return request.post("/system/role", data);
}
export function updateRole(data: any) {
  return request.put("/system/role", data);
}
export function deleteRole(id: number) {
  return request.delete(`/system/role/${id}`);
}
export function getUserRoles(userId: number) {
  return request.get(`/system/role/user/${userId}`);
}
export function assignRoles(userId: number, roleIds: number[]) {
  return request.post(`/system/role/user/${userId}/assign`, { roleIds });
}

// ========= Device =========
export function getDevicePage(params: any) {
  return request.get("/device/page", { params });
}
export function createDevice(data: any) {
  return request.post("/device", data);
}
export function updateDevice(data: any) {
  return request.put("/device", data);
}
export function deleteDevice(id: number) {
  return request.delete(`/device/${id}`);
}


