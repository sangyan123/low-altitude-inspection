import request from "../utils/request";

export interface DataResult {
  id?: number | null;
  flightRecordId?: number | null;
  dataType?: number | null;
  fileName?: string;
  fileUrl?: string;
  thumbnailUrl?: string;
  fileSizeMb?: number | string | null;
  modelFormat?: string;
  captureTime?: string;
  lng?: number | string | null;
  lat?: number | string | null;
  deptId?: number | null;
  deptName?: string;
  authScope?: string;
  submitUserId?: number | null;
  submitUserName?: string;
  remark?: string;
  createTime?: string;
  updateTime?: string;
  dataTypeName?: string;
  categoryName?: string;
}

export function uploadDataFile(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/file/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

export function getDataResultPage(params: any) {
  return request.get("/data/result/page", { params });
}

export function getDataResultDetail(id: number) {
  return request.get(`/data/result/${id}`);
}

export function createDataResult(data: DataResult) {
  return request.post("/data/result", data);
}

export function updateDataResult(data: DataResult) {
  return request.put("/data/result", data);
}

export function deleteDataResult(id: number) {
  return request.delete(`/data/result/${id}`);
}