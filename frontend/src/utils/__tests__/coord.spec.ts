import { describe, expect, it } from "vitest";
import { distanceMeters, gcj02ToWgs84, polygonAreaSqM, toMapCoord, wgs84ToGcj02 } from "../coord";

/** 坐标系转换工具单元测试 */
describe("coord 坐标系转换", () => {
  it("中国境外坐标原样返回，不做加密偏移", () => {
    const [lng, lat] = wgs84ToGcj02(-74.006, 40.7128); // 纽约
    expect(lng).toBe(-74.006);
    expect(lat).toBeCloseTo(40.7128);
  });

  it("中国境内坐标发生GCJ-02偏移且量级合理", () => {
    const [lng, lat] = wgs84ToGcj02(116.3975, 39.9087); // 北京
    expect(Math.abs(lng - 116.3975)).toBeGreaterThan(0.001);
    expect(Math.abs(lng - 116.3975)).toBeLessThan(0.01);
    expect(Math.abs(lat - 39.9087)).toBeGreaterThan(0.0005);
    expect(Math.abs(lat - 39.9087)).toBeLessThan(0.01);
  });

  it("GCJ02近似逆变换往返误差在0.0001度（约10米）内", () => {
    const [glng, glat] = wgs84ToGcj02(101.72, 26.58); // 昆明
    const [wlng, wlat] = gcj02ToWgs84(glng, glat);
    expect(Math.abs(wlng - 101.72)).toBeLessThan(0.0001);
    expect(Math.abs(wlat - 26.58)).toBeLessThan(0.0001);
  });

  it("toMapCoord：gcj02底图走加密转换，wgs84底图原样透传", () => {
    const [gcjLng, gcjLat] = toMapCoord(116.3975, 39.9087, "gcj02");
    expect(gcjLng).not.toBe(116.3975);
    const [wgsLng, wgsLat] = toMapCoord(116.3975, 39.9087, "wgs84");
    expect(wgsLng).toBe(116.3975);
    expect(wgsLat).toBe(39.9087);
  });

  it("distanceMeters：同一点距离为0", () => {
    expect(distanceMeters(101.72, 26.58, 101.72, 26.58)).toBe(0);
  });

  it("distanceMeters：昆明—北京直线距离约2100公里", () => {
    const d = distanceMeters(102.712851, 25.040809, 116.404, 39.915);
    expect(d).toBeGreaterThan(2_050_000);
    expect(d).toBeLessThan(2_200_000);
  });

  it("polygonAreaSqM：少于3个点面积为0", () => {
    expect(polygonAreaSqM([])).toBe(0);
    expect(polygonAreaSqM([
      [101.72, 26.58],
      [101.73, 26.59],
    ])).toBe(0);
  });

  it("polygonAreaSqM：赤道0.01°×0.01°正方形约1.24平方公里", () => {
    const area = polygonAreaSqM([
      [0, 0],
      [0.01, 0],
      [0.01, 0.01],
      [0, 0.01],
    ]);
    expect(area).toBeGreaterThan(1_100_000);
    expect(area).toBeLessThan(1_400_000);
  });
});
