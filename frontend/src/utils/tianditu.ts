import L from "leaflet";

/** 天地图图层配置 */
export interface TiandituLayers {
  vec: string; // 矢量底图
  cva: string; // 矢量注记
  img: string; // 影像底图
  cia: string; // 影像注记
}

/** 计算两点间距离（米），Haversine公式，CGCS2000/WGS-84通用 */
export function distanceMeters(
  lng1: number, lat1: number,
  lng2: number, lat2: number
): number {
  const R = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(a));
}

/** 计算多边形面积（平方米），鞋带公式球面修正 */
export function polygonAreaSqM(points: [number, number][]): number {
  // points: [lng, lat][]
  if (points.length < 3) return 0;
  const rad = Math.PI / 180;
  let total = 0;
  for (let i = 0; i < points.length; i++) {
    const [lng1, lat1] = points[i];
    const [lng2, lat2] = points[(i + 1) % points.length];
    total +=
      ((lng2 - lng1) * rad) *
      (2 + Math.sin(lat1 * rad) + Math.sin(lat2 * rad));
  }
  return Math.abs((total * 6378137 * 6378137) / 2);
}

/** 创建天地图底图图层组 */
export function createTiandituLayers(key: string, type: "vec" | "img" = "vec"): L.TileLayer[] {
  const t = type === "img" ? "img" : "vec";
  const note = type === "img" ? "cia" : "cva";
  const base = L.tileLayer(
    `https://t{s}.tianditu.gov.cn/${t}_w/wmts?layer=${t}&style=default&tilematrixset=w&Service=WMTS&Request=GetTile&Version=1.0.0&Format=tiles&TileMatrix={z}&TileCol={x}&TileRow={y}&tk=${key}`,
    { subdomains: ["0", "1", "2", "3", "4", "5", "6", "7"], maxZoom: 18 }
  );
  const annotation = L.tileLayer(
    `https://t{s}.tianditu.gov.cn/${note}_w/wmts?layer=${note}&style=default&tilematrixset=w&Service=WMTS&Request=GetTile&Version=1.0.0&Format=tiles&TileMatrix={z}&TileCol={x}&TileRow={y}&tk=${key}`,
    { subdomains: ["0", "1", "2", "3", "4", "5", "6", "7"], maxZoom: 18 }
  );
  return [base, annotation];
}

export { L };

