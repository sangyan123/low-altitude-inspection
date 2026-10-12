import request from "../utils/request";

export interface AiAlgorithm {
  id?: number | null;
  algoName: string;
  algoCategory: string;
  algoDesc?: string;
  modelVersion?: string;
  status?: number;
  createTime?: string;
}

export interface AiRecognitionResult {
  id: number;
  flightRecordId?: number | null;
  algoId: number;
  algoName?: string;
  algoCategory?: string;
  imageUrl: string;
  resultType?: string;
  confidence?: number | string;
  lng?: number | string | null;
  lat?: number | string | null;
  status: number;
  confirmUserId?: number | null;
  confirmTime?: string;
  createTime: string;
  sourceType: number;
  provider?: string;
  modelName?: string;
  bbox?: string;
  rawResponse?: string;
  errorMessage?: string;
}

export function uploadImage(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/file/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

export function getAlgorithmList(params?: { keyword?: string; status?: number }) {
  return request.get("/ai/algorithm/list", { params });
}

export function createAlgorithm(data: AiAlgorithm) {
  return request.post("/ai/algorithm", data);
}

export function updateAlgorithm(data: AiAlgorithm) {
  return request.put("/ai/algorithm", data);
}

export function deleteAlgorithm(id: number) {
  return request.delete(`/ai/algorithm/${id}`);
}

export function getRecognitionPage(params: any) {
  return request.get("/ai/recognition/page", { params });
}

export function createRecognition(data: {
  algoId: number;
  imageUrl: string;
  flightRecordId?: number | null;
  lng?: number | null;
  lat?: number | null;
  provider?: string;
}) {
  return request.post("/ai/recognition", data);
}

export function confirmRecognition(id: number) {
  return request.post(`/ai/recognition/${id}/confirm`);
}

export function falseReportRecognition(id: number) {
  return request.post(`/ai/recognition/${id}/false-report`);
}

export function deleteRecognition(id: number) {
  return request.delete(`/ai/recognition/${id}`);
}