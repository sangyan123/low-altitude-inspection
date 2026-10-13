import request from "../utils/request";

export interface ReportTrendPoint {
  timeLabel: string;
  deviceCount: number;
  patrolCount: number;
  transportCount: number;
  planCount: number;
  aiCount: number;
  eventCount: number;
  emergencyCount: number;
  dataCount: number;
}

export interface DepartmentReport {
  deptId: number;
  deptName: string;
  deviceCount: number;
  patrolCount: number;
  transportCount: number;
  planCount: number;
  eventCount: number;
  dataCount: number;
  totalBusinessCount: number;
}

export interface NameValue {
  name: string;
  value: number;
}

export interface ReportOverview {
  deviceTotal: number;
  deviceOnline: number;
  deviceFlying: number;
  patrolTotal: number;
  patrolCompleted: number;
  patrolCompletionRate: number;
  transportTotal: number;
  transportSigned: number;
  transportSignedRate: number;
  planTotal: number;
  planApproved: number;
  planApprovalRate: number;
  aiRecognitionTotal: number;
  eventTotal: number;
  eventClosed: number;
  eventClosureRate: number;
  emergencyTotal: number;
  emergencyOngoing: number;
  dataResultTotal: number;
  dataMediaTotal: number;
  dataModelTotal: number;
}

export interface ReportData {
  startDate: string;
  endDate: string;
  granularity: string;
  overview: ReportOverview;
  trends: ReportTrendPoint[];
  departments: DepartmentReport[];
  patrolStatus: NameValue[];
  transportStatus: NameValue[];
  eventStatus: NameValue[];
  emergencyType: NameValue[];
  dataType: NameValue[];
}

export function getReport(params: {
  startDate?: string;
  endDate?: string;
  granularity?: string;
  deptId?: number | null;
}) {
  return request.get("/report/overview", { params });
}
export function exportReportExcel(params: {
  startDate?: string;
  endDate?: string;
  granularity?: string;
  deptId?: number | null;
}) {
  return request.get("/report/export/excel", { params, responseType: "blob" });
}

export function exportReportPdf(params: {
  startDate?: string;
  endDate?: string;
  granularity?: string;
  deptId?: number | null;
}) {
  return request.get("/report/export/pdf", { params, responseType: "blob" });
}
