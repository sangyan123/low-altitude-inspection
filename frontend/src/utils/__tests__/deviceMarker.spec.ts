import { describe, expect, it } from "vitest";
import { createDeviceMarkerIcon, getDeviceStatusColor } from "../deviceMarker";

/** 设备地图标记工具单元测试 */
describe("deviceMarker 状态颜色", () => {
  it("已定义状态返回对应颜色", () => {
    expect(getDeviceStatusColor(0)).toBe("#909399");
    expect(getDeviceStatusColor(1)).toBe("#67c23a");
    expect(getDeviceStatusColor(2)).toBe("#409eff");
    expect(getDeviceStatusColor(3)).toBe("#e6a23c");
    expect(getDeviceStatusColor(4)).toBe("#f56c6c");
  });

  it("未知状态回退离线灰色", () => {
    expect(getDeviceStatusColor(99)).toBe("#909399");
  });
});

describe("createDeviceMarkerIcon", () => {
  it("生成DivIcon：包含状态颜色、设备名称与锚点尺寸", () => {
    const icon = createDeviceMarkerIcon({ deviceName: "巡检一号", status: 2 });
    const html = String(icon.options.html);
    expect(html).toContain("#409eff");
    expect(html).toContain("巡检一号");
    expect(icon.options.iconSize).toEqual([32, 32]);
    expect(icon.options.iconAnchor).toEqual([16, 16]);
  });

  it("名称缺省时显示'未命名无人机'", () => {
    const icon = createDeviceMarkerIcon({ deviceName: null, status: 1 });
    expect(String(icon.options.html)).toContain("未命名无人机");
  });

  it("名称中的HTML字符被转义（防XSS注入）", () => {
    const icon = createDeviceMarkerIcon({
      deviceName: "<img src=x onerror=alert(1)>",
      status: 0,
    });
    const html = String(icon.options.html);
    expect(html).not.toContain("<img");
    expect(html).toContain("&lt;img");
  });

  it("status为null时按离线灰色渲染", () => {
    const icon = createDeviceMarkerIcon({ deviceName: "测试", status: null });
    expect(String(icon.options.html)).toContain("#909399");
  });
});
