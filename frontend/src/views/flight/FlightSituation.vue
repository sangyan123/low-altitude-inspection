<template>
  <div class="flight-page">
    <!-- 左侧设备列表面板 -->
    <div class="side-panel left">
      <div class="panel-title">设备列表（{{ devices.length }}）</div>
      <div
        v-for="d in devices"
        :key="d.id"
        class="device-item"
        :class="{ active: selectedDeviceId === d.id }"
        @click="locateDevice(d)"
      >
        <div class="device-name">
          <span class="status-dot" :class="`s${d.status}`"></span>
          {{ d.deviceName }}
        </div>
        <div class="device-info">
          {{ d.model || "未知型号" }} · 电量 {{ d.battery ?? "-" }}% · {{ commName(d.commMode) }}
        </div>
      </div>
      <div v-if="devices.length === 0" class="empty-tip">暂无设备，请先在无人机管理中添加</div>
    </div>

    <!-- 地图主体 -->
    <div class="map-wrap">
      <GisMap :base-type="baseType" @ready="onMapReady" />
      <!-- 底图切换 -->
      <div class="map-switch">
        <el-radio-group v-model="baseType" size="small">
          <el-radio-button value="vec">矢量</el-radio-button>
          <el-radio-button value="img">影像</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 右侧详情面板 -->
    <div class="side-panel right">
      <div class="panel-title">设备详情</div>
      <template v-if="selected">
        <div class="detail-row"><span>设备名称</span><b>{{ selected.deviceName }}</b></div>
        <div class="detail-row"><span>序列号</span><b>{{ selected.deviceSn }}</b></div>
        <div class="detail-row"><span>型号</span><b>{{ selected.model || "-" }}</b></div>
        <div class="detail-row">
          <span>状态</span>
          <el-tag :type="statusTag(selected.status)" size="small">{{ statusName(selected.status) }}</el-tag>
        </div>
        <div class="detail-row">
          <span>电量</span>
          <el-progress :percentage="Number(selected.battery || 0)" :stroke-width="12" style="flex:1" />
        </div>
        <div class="detail-row"><span>通信链路</span><b>{{ commName(selected.commMode) }}</b></div>
        <div class="detail-row">
          <span>坐标</span>
          <b>{{ selected.lng ?? "-" }}, {{ selected.lat ?? "-" }}</b>
        </div>
        <div class="detail-row"><span>累计架次</span><b>{{ selected.totalFlightCount ?? 0 }}</b></div>
      </template>
      <div v-else class="empty-tip">点击左侧设备或地图标点查看详情</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";
import type L from "leaflet";
import GisMap from "../../components/GisMap.vue";
import { getDevicePage } from "../../api/system";

const baseType = ref<"vec" | "img">("vec");

const devices = ref<any[]>([]);
const selectedDeviceId = ref<number | null>(null);
const selected = ref<any>(null);
let map: L.Map | null = null;
const markers = new Map<number, L.Marker>();
let timer: number | undefined;

const commName = (c: number) => ({ 0: "离线", 1: "4G", 2: "5G", 3: "自组网", 4: "中继" }[c] || "-");
const statusName = (s: number) => ({ 0: "离线", 1: "在线", 2: "飞行中", 3: "充电中", 4: "维护中" }[s] || "-");
const statusTag = (s: number) => ({ 0: "info", 1: "success", 2: "primary", 3: "warning", 4: "danger" }[s] || "info");

const statusColor: Record<number, string> = {
  0: "#909399", 1: "#67c23a", 2: "#409eff", 3: "#e6a23c", 4: "#f56c6c",
};

function deviceIcon(d: any): L.DivIcon {
  const color = statusColor[d.status] || "#909399";
  return L.divIcon({
    className: "",
    html: `<div style="position:relative;text-align:center">
      <div style="width:16px;height:16px;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 0 8px ${color};margin:0 auto"></div>
      <div style="font-size:11px;color:#fff;background:rgba(0,0,0,.55);border-radius:4px;padding:1px 4px;margin-top:2px;white-space:nowrap">${d.deviceName}</div>
    </div>`,
    iconSize: [0, 0],
  });
}

function onMapReady(m: L.Map) {
  map = m;
  refresh();
  timer = window.setInterval(refresh, 3000);
}

async function refresh() {
  try {
    const res: any = await getDevicePage({ current: 1, size: 500 });
    devices.value = res.data.records;
    renderMarkers();
  } catch {
    /* 轮询容错 */
  }
}

function renderMarkers() {
  if (!map) return;
  const seen = new Set<number>();
  for (const d of devices.value) {
    seen.add(d.id);
    if (d.lng == null || d.lat == null) continue;
    const pos: [number, number] = [Number(d.lat), Number(d.lng)];
    let marker = markers.get(d.id);
    if (marker) {
      marker.setLatLng(pos);
      marker.setIcon(deviceIcon(d));
    } else {
      marker = L.marker(pos, { icon: deviceIcon(d) }).addTo(map);
      marker.on("click", () => selectDevice(d));
      markers.set(d.id, marker);
    }
  }
  // 移除已消失的设备标点
  markers.forEach((marker, id) => {
    if (!seen.has(id)) {
      map?.removeLayer(marker);
      markers.delete(id);
    }
  });
  if (selected.value) {
    selected.value = devices.value.find((d) => d.id === selected.value.id) || selected.value;
  }
}

function selectDevice(d: any) {
  selectedDeviceId.value = d.id;
  selected.value = d;
}

function locateDevice(d: any) {
  selectDevice(d);
  if (map && d.lng != null && d.lat != null) {
    map.setView([Number(d.lat), Number(d.lng)], 15);
  }
}

onMounted(() => {});
onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer); // 地图销毁由GisMap组件负责
});
</script>

<style scoped>
.flight-page {
  display: flex;
  gap: 12px;
  height: calc(100vh - 92px);
}
.side-panel {
  width: 240px;
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  overflow-y: auto;
  flex-shrink: 0;
}
.panel-title {
  font-weight: bold;
  margin-bottom: 12px;
  color: #303133;
}
.device-item {
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid transparent;
}
.device-item:hover {
  background: #f5f7fa;
}
.device-item.active {
  background: #ecf5ff;
  border-color: #409eff;
}
.device-name {
  font-size: 14px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 6px;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.status-dot.s0 { background: #909399; }
.status-dot.s1 { background: #67c23a; }
.status-dot.s2 { background: #409eff; }
.status-dot.s3 { background: #e6a23c; }
.status-dot.s4 { background: #f56c6c; }
.device-info {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  margin-left: 14px;
}
.map-wrap {
  flex: 1;
  position: relative;
  border-radius: 6px;
  overflow: hidden;
  min-height: 500px;
}
.map-switch {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1000;
}
.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}
.detail-row span {
  color: #909399;
}
.detail-row b {
  color: #303133;
  font-weight: normal;
}
.empty-tip {
  color: #c0c4cc;
  font-size: 13px;
  text-align: center;
  padding: 20px 0;
}
</style>


