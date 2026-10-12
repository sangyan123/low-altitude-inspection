import L from "leaflet";

/** 无人机状态颜色，地图与业务列表保持一致 */
export const deviceStatusColors: Record<number, string> = {
  0: "#909399",
  1: "#67c23a",
  2: "#409eff",
  3: "#e6a23c",
  4: "#f56c6c",
};

export function getDeviceStatusColor(status: number): string {
  return deviceStatusColors[status] || "#909399";
}

function escapeHtml(value: unknown): string {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function droneSvg(): string {
  return `<svg
    viewBox="0 0 24 24"
    width="17"
    height="17"
    fill="none"
    stroke="#ffffff"
    stroke-width="1.8"
    stroke-linecap="round"
    aria-hidden="true"
  >
    <path d="M8.4 8.4 15.6 15.6 M15.6 8.4 8.4 15.6" />
    <circle cx="6.2" cy="6.2" r="2.9" />
    <circle cx="17.8" cy="6.2" r="2.9" />
    <circle cx="6.2" cy="17.8" r="2.9" />
    <circle cx="17.8" cy="17.8" r="2.9" />
    <circle cx="12" cy="12" r="2.1" fill="#ffffff" stroke="none" />
  </svg>`;
}

/**
 * 创建带无人机图标和名称标签的地图 Marker。
 * 图标圆心精确锚定设备坐标，标签固定显示在图标下方。
 */
export function createDeviceMarkerIcon(device: {
  deviceName?: string | null;
  status?: number | null;
}): L.DivIcon {
  const color = getDeviceStatusColor(Number(device.status ?? 0));
  const name = escapeHtml(device.deviceName || "未命名无人机");

  const html = `<div style="position:absolute;left:0;top:0;width:32px;height:32px;">
    <div style="
      width:32px;
      height:32px;
      display:flex;
      align-items:center;
      justify-content:center;
      background:${color};
      border:2px solid #ffffff;
      border-radius:50%;
      box-shadow:0 0 0 1px rgba(0,0,0,.08), 0 0 14px ${color};
    ">${droneSvg()}</div>
    <div style="
      position:absolute;
      top:36px;
      left:50%;
      transform:translateX(-50%);
      padding:2px 7px;
      border:1px solid rgba(255,255,255,.32);
      border-radius:999px;
      background:rgba(17,24,39,.78);
      color:#ffffff;
      font-size:11px;
      font-weight:600;
      line-height:16px;
      white-space:nowrap;
      text-shadow:0 1px 2px rgba(0,0,0,.85);
      box-shadow:0 2px 8px rgba(0,0,0,.28);
      pointer-events:none;
    ">${name}</div>
  </div>`;

  return L.divIcon({
    className: "",
    html,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
  });
}