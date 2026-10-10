import type { MapCoordSystem } from "./coord";

export type MapProvider = "tianditu" | "amap" | "tencent";
export type BaseMapType = "vec" | "img";

export interface MapSourceConfig {
  provider: MapProvider;
  key: string;
  /** 底图坐标系（决定是否转换WGS-84坐标） */
  coordSystem: MapCoordSystem;
  /** Leaflet瓦片URL模板列表（底图+注记） */
  tileUrls: string[];
  subdomains?: string[];
  maxZoom: number;
}

const env = import.meta.env;

const PROVIDER = (env.VITE_MAP_PROVIDER as MapProvider) || "tianditu";

function tiandituConfig(baseType: BaseMapType): MapSourceConfig {
  const tk = (env.VITE_TIANDITU_KEY as string) || "";
  const t = baseType === "img" ? "img" : "vec";
  const note = baseType === "img" ? "cia" : "cva";
  const url = (layer: string) =>
    `https://t{s}.tianditu.gov.cn/${layer}_w/wmts?layer=${layer}&style=default&tilematrixset=w&Service=WMTS&Request=GetTile&Version=1.0.0&Format=tiles&TileMatrix={z}&TileCol={x}&TileRow={y}&tk=${tk}`;
  return {
    provider: "tianditu",
    key: tk,
    coordSystem: "wgs84",
    tileUrls: [url(t), url(note)],
    subdomains: ["0", "1", "2", "3", "4", "5", "6", "7"],
    maxZoom: 18,
  };
}

function amapConfig(baseType: BaseMapType): MapSourceConfig {
  return {
    provider: "amap",
    key: (env.VITE_AMAP_KEY as string) || "",
    coordSystem: "gcj02",
    tileUrls: [
      `https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=${baseType === "img" ? 6 : 8}&x={x}&y={y}&z={z}`,
    ],
    subdomains: ["1", "2", "3", "4"],
    maxZoom: 18,
  };
}

function tencentConfig(baseType: BaseMapType): MapSourceConfig {
  return {
    provider: "tencent",
    key: (env.VITE_TENCENT_KEY as string) || "",
    coordSystem: "gcj02",
    tileUrls: [
      `https://rt{s}.map.gtimg.com/realtimerender?lang=zh_cn&style=${baseType === "img" ? 6 : 0}&version=1.0&x={x}&y={y}&z={z}&type=vector`,
    ],
    subdomains: ["0", "1", "2"],
    maxZoom: 18,
  };
}

/** 获取地图源配置（含矢量/影像切换） */
export function getMapSource(baseType: BaseMapType = "vec"): MapSourceConfig {
  switch (PROVIDER) {
    case "amap":
      return amapConfig(baseType);
    case "tencent":
      return tencentConfig(baseType);
    case "tianditu":
    default:
      return tiandituConfig(baseType);
  }
}

/** 当前provider（只读） */
export const currentProvider: MapProvider = PROVIDER;

export const providerName: Record<MapProvider, string> = {
  tianditu: "天地图",
  amap: "高德地图",
  tencent: "腾讯地图",
};
