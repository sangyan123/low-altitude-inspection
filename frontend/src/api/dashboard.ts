import request from "../utils/request";

export interface DashboardDevice {
  id: number;
  deviceName: string;
  deviceSn: string;
  model?: string | null;
  status?: number | null;
  battery?: number | string | null;
  lng?: number | string | null;
  lat?: number | string | null;
  altitude?: number | string | null;
  deptId?: number | null;
  totalFlightCount?: number | null;
}

export interface DashboardStats {
  deviceTotal: number;
  onlineCount: number;
  flyingCount: number;
  chargingCount: number;
  offlineCount: number;
  maintainCount: number;
  locatedCount: number;
  avgBattery?: number | string | null;

  patrolTotal: number;
  patrolPending: number;
  patrolActive: number;
  patrolCompleted: number;
  patrolCancelled: number;

  transportTotal: number;
  transportPending: number;
  transportActive: number;
  transportArrived: number;
  transportSigned: number;
  transportCancelled: number;

  planTotal: number;
  planPending: number;
  planApproved: number;
  planRejected: number;

  eventTotal: number;
  eventPending: number;
  eventDispatched: number;
  eventHandled: number;
  eventVerified: number;
  eventClosed: number;
  todayEventCount: number;
  activeEventCount: number;
  todayTaskCount: number;
  activeTaskCount: number;
  devices: DashboardDevice[];
}

export function getDashboardStats() {
  return request.get<DashboardStats>("/dashboard/stats");
}
