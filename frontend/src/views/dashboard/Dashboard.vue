<template>
  <div class="dashboard" v-loading="loading">
    <el-row :gutter="12" class="stat-row">
      <el-col :xs="12" :sm="8" :md="4" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card-box">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: card.color }">
              <el-icon size="26"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-meta">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-note">{{ card.note }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="content-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="map-card">
          <template #header>
            <div class="card-header">
              <span>低空设备态势</span>
              <el-radio-group v-model="baseType" size="small">
                <el-radio-button value="vec">矢量</el-radio-button>
                <el-radio-button value="img">影像</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <div class="map-shell">
            <GisMap :base-type="baseType" :zoom="12" @ready="onMapReady" />

            <div class="map-summary">
              <div class="summary-title">设备分布</div>
              <div class="summary-value">{{ stats?.deviceTotal ?? 0 }}</div>
              <div class="summary-note">已定位 {{ stats?.locatedCount ?? 0 }} 台</div>
            </div>

            <div v-if="selectedDevice" class="map-detail">
              <div class="detail-header">
                <div class="device-name">
                  <span class="status-dot" :style="{ background: statusColor(selectedDevice.status) }"></span>
                  {{ selectedDevice.deviceName }}
                </div>
                <el-icon class="detail-close" @click="selectedDevice = null"><Close /></el-icon>
              </div>
              <div class="detail-row"><span>序列号</span><b>{{ selectedDevice.deviceSn }}</b></div>
              <div class="detail-row"><span>型号</span><b>{{ selectedDevice.model || "-" }}</b></div>
              <div class="detail-row"><span>所属部门</span><b>{{ selectedDevice.deptId == null ? "未分配" : deptMap[selectedDevice.deptId] }}</b></div>
              <div class="detail-row">
                <span>状态</span>
                <el-tag :type="statusTag(selectedDevice.status)" size="small">{{ statusName(selectedDevice.status) }}</el-tag>
              </div>
              <div class="detail-row">
                <span>电量</span>
                <el-progress
                  class="battery-progress"
                  :percentage="Number(selectedDevice.battery || 0)"
                  :color="batteryColor"
                  :stroke-width="9"
                />
              </div>
              <div class="detail-row"><span>坐标</span><b>{{ selectedDevice.lng ?? "-" }}, {{ selectedDevice.lat ?? "-" }}</b></div>
              <div class="detail-row"><span>累计架次</span><b>{{ selectedDevice.totalFlightCount ?? 0 }}</b></div>
            </div>

            <div class="map-legend">
              <div v-for="item in legendItems" :key="item.status" class="legend-item">
                <span class="status-dot" :style="{ background: item.color }"></span>
                {{ item.label }}
              </div>
            </div>

            <div v-if="!stats?.locatedCount" class="map-empty">暂无已定位设备</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <div class="right-column">
          <el-card shadow="never" class="chart-card">
            <template #header>无人机状态分布</template>
            <div ref="statusChartRef" class="status-chart"></div>
          </el-card>

          <el-card shadow="never" class="overview-card">
            <template #header>任务概览</template>
            <div class="overview-list">
              <div class="overview-item">
                <div class="overview-label">今日任务</div>
                <div class="overview-value">{{ stats?.todayTaskCount ?? 0 }}</div>
              </div>
              <div class="overview-item">
                <div class="overview-label">执行中任务</div>
                <div class="overview-value">{{ stats?.activeTaskCount ?? 0 }}</div>
              </div>
              <div class="overview-item">
                <div class="overview-label">巡检执行中</div>
                <div class="overview-value">{{ stats?.patrolActive ?? 0 }}</div>
              </div>
              <div class="overview-item">
                <div class="overview-label">运输执行中</div>
                <div class="overview-value">{{ stats?.transportActive ?? 0 }}</div>
              </div>
              <div class="overview-item">
                <div class="overview-label">飞行计划待审</div>
                <div class="overview-value">{{ stats?.planPending ?? 0 }}</div>
              </div>
              <div class="overview-item">
                <div class="overview-label">事件总数</div>
                <div class="overview-value">{{ stats?.eventTotal ?? 0 }}</div>
              </div>
              <div class="overview-item">
                <div class="overview-label">待处置事件</div>
                <div class="overview-value">{{ stats?.eventPending ?? 0 }}</div>
              </div>
              <div class="overview-item">
                <div class="overview-label">事件已销号</div>
                <div class="overview-value">{{ stats?.eventClosed ?? 0 }}</div>
              </div>
              <div class="overview-item">
                <div class="overview-label">今日事件</div>
                <div class="overview-value">{{ stats?.todayEventCount ?? 0 }}</div>
              </div>
              <div class="overview-item">
                <div class="overview-label">平均电量</div>
                <div class="overview-value">{{ avgBatteryText }}</div>
              </div>
            </div>

            <div class="task-progress">
              <div class="progress-title">巡检完成率</div>
              <el-progress :percentage="patrolRate" :stroke-width="10" />
              <div class="progress-title">运输签收率</div>
              <el-progress :percentage="transportRate" :stroke-width="10" />
              <div class="progress-title">计划通过率</div>
              <el-progress :percentage="planRate" :stroke-width="10" />
              <div class="progress-title">事件销号率</div>
              <el-progress :percentage="eventRate" :stroke-width="10" />
            </div>
          </el-card>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="bottom-row">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>部门信息</template>
          <el-table :data="deptList" size="small" stripe height="240">
            <el-table-column prop="deptName" label="部门" min-width="140" />
            <el-table-column prop="deptCode" label="编码" width="140" />
            <el-table-column prop="leader" label="负责人" width="120" />
            <el-table-column prop="phone" label="联系电话" width="150" />
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? "启用" : "停用" }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import * as echarts from "echarts";
import L from "leaflet";
import GisMap from "../../components/GisMap.vue";
import { createDeviceMarkerIcon, getDeviceStatusColor } from "../../utils/deviceMarker";
import { getDeptList } from "../../api/system";
import { getDashboardStats, type DashboardDevice, type DashboardStats } from "../../api/dashboard";
import { subscribeToMessageType } from "../../utils/websocket";

const loading = ref(false);
const baseType = ref<"vec" | "img">("vec");
const stats = ref<DashboardStats | null>(null);
const deptList = ref<any[]>([]);
const selectedDevice = ref<DashboardDevice | null>(null);
const statusChartRef = ref<HTMLElement>();

let map: L.Map | null = null;
let unsubscribers: (() => void)[] = [];
let statusChart: echarts.ECharts | null = null;
let hasFitBounds = false;

const markers = new Map<number, L.Marker>();

const statusColor = (status?: number | null) => getDeviceStatusColor(Number(status ?? 0));
const statusName = (status?: number | null) =>
  ({ 0: "离线", 1: "在线", 2: "飞行中", 3: "充电中", 4: "维护中" }[Number(status ?? 0)] || "-");
const statusTag = (status?: number | null) =>
  ({ 0: "info", 1: "success", 2: "primary", 3: "warning", 4: "danger" }[Number(status ?? 0)] || "info");

const batteryColor = (percentage: number) => {
  if (percentage > 60) return "#67c23a";
  if (percentage > 20) return "#e6a23c";
  return "#f56c6c";
};

const legendItems = [
  { status: 1, label: "在线", color: "#67c23a" },
  { status: 2, label: "飞行中", color: "#409eff" },
  { status: 3, label: "充电中", color: "#e6a23c" },
  { status: 0, label: "离线", color: "#909399" },
  { status: 4, label: "维护中", color: "#f56c6c" },
];

const deptMap = computed(() => {
  const map: Record<number, string> = {};
  deptList.value.forEach((item) => {
    map[item.id] = item.deptName;
  });
  return map;
});

const onlineDeviceCount = computed(() => (stats.value?.onlineCount ?? 0) + (stats.value?.flyingCount ?? 0));
const avgBatteryText = computed(() => {
  const value = Number(stats.value?.avgBattery ?? 0);
  return `${value}%`;
});

const statCards = computed(() => [
  { label: "设备总数", value: stats.value?.deviceTotal ?? 0, note: `已定位 ${stats.value?.locatedCount ?? 0} 台`, icon: "Monitor", color: "#409eff" },
  { label: "在线设备", value: onlineDeviceCount.value, note: `含飞行中 ${stats.value?.flyingCount ?? 0} 台`, icon: "Connection", color: "#67c23a" },
  { label: "飞行中", value: stats.value?.flyingCount ?? 0, note: `充电中 ${stats.value?.chargingCount ?? 0} 台`, icon: "Position", color: "#e6a23c" },
  { label: "今日任务", value: stats.value?.todayTaskCount ?? 0, note: "巡检 + 运输", icon: "Calendar", color: "#7367f0" },
  { label: "执行中任务", value: stats.value?.activeTaskCount ?? 0, note: `运输中 ${stats.value?.transportActive ?? 0} 单`, icon: "Tickets", color: "#ff9f43" },
  { label: "待审计划", value: stats.value?.planPending ?? 0, note: `计划总数 ${stats.value?.planTotal ?? 0}`, icon: "DocumentChecked", color: "#f56c6c" },
  { label: "事件总数", value: stats.value?.eventTotal ?? 0, note: `待处置 ${stats.value?.eventPending ?? 0} 起`, icon: "Warning", color: "#f56c6c" },
  { label: "已销号事件", value: stats.value?.eventClosed ?? 0, note: `今日新增 ${stats.value?.todayEventCount ?? 0} 起`, icon: "CircleCheck", color: "#67c23a" },
]);

const patrolRate = computed(() => {
  const total = stats.value?.patrolTotal ?? 0;
  return total ? Math.round(((stats.value?.patrolCompleted ?? 0) / total) * 100) : 0;
});

const transportRate = computed(() => {
  const total = stats.value?.transportTotal ?? 0;
  return total ? Math.round(((stats.value?.transportSigned ?? 0) / total) * 100) : 0;
});

const eventRate = computed(() => {
  const total = stats.value?.eventTotal ?? 0;
  return total ? Math.round(((stats.value?.eventClosed ?? 0) / total) * 100) : 0;
});

const planRate = computed(() => {
  const total = stats.value?.planTotal ?? 0;
  return total ? Math.round(((stats.value?.planApproved ?? 0) / total) * 100) : 0;
});

function onMapReady(mapInstance: L.Map) {
  map = mapInstance;
  renderMarkers();
}

function clearMarkers() {
  markers.forEach((marker) => marker.remove());
  markers.clear();
}

function renderMarkers() {
  if (!map) return;
  clearMarkers();

  const points: L.LatLngExpression[] = [];
  for (const device of stats.value?.devices ?? []) {
    if (device.lng == null || device.lat == null) continue;
    const position: L.LatLngExpression = [Number(device.lat), Number(device.lng)];
    const marker = L.marker(position, { icon: createDeviceMarkerIcon(device) }).addTo(map);
    marker.on("click", () => {
      selectedDevice.value = device;
    });
    markers.set(device.id, marker);
    points.push(position);
  }

  if (!hasFitBounds && points.length > 1) {
    map.fitBounds(L.latLngBounds(points).pad(0.18));
    hasFitBounds = true;
  } else if (!hasFitBounds && points.length === 1) {
    map.setView(points[0], 14);
    hasFitBounds = true;
  }
}

function updateChart() {
  if (!statusChartRef.value) return;
  if (!statusChart) {
    statusChart = echarts.init(statusChartRef.value);
  }

  statusChart.setOption({
    tooltip: { trigger: "item" },
    legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
    series: [{
      type: "pie",
      radius: ["48%", "70%"],
      center: ["50%", "42%"],
      itemStyle: { borderColor: "#fff", borderWidth: 2 },
      label: { show: false },
      data: [
        { value: stats.value?.onlineCount ?? 0, name: "在线", itemStyle: { color: "#67c23a" } },
        { value: stats.value?.flyingCount ?? 0, name: "飞行中", itemStyle: { color: "#409eff" } },
        { value: stats.value?.chargingCount ?? 0, name: "充电中", itemStyle: { color: "#e6a23c" } },
        { value: stats.value?.offlineCount ?? 0, name: "离线", itemStyle: { color: "#909399" } },
        { value: stats.value?.maintainCount ?? 0, name: "维护中", itemStyle: { color: "#f56c6c" } },
      ],
    }],
  });
}

function handleResize() {
  statusChart?.resize();
}

async function loadStats() {
  const res: any = await getDashboardStats();
  stats.value = res.data;
  selectedDevice.value = stats.value?.devices?.find((item) => item.id === selectedDevice.value?.id) || null;
  renderMarkers();
  updateChart();
}

async function loadDepts() {
  const res: any = await getDeptList();
  deptList.value = res.data.filter((item: any) => item.parentId !== 0);
}

function handleDashboardUpdate(data: any) {
  if (!data) return;
  stats.value = data;
  selectedDevice.value = stats.value?.devices?.find((item: any) => item.id === selectedDevice.value?.id) || null;
  renderMarkers();
  updateChart();
}

onMounted(async () => {
  window.addEventListener("resize", handleResize);
  loadDepts();
  loading.value = true;
  try {
    await loadStats();
  } finally {
    loading.value = false;
  }
  // WebSocket real-time update replaces polling
  unsubscribers.push(subscribeToMessageType("DASHBOARD_UPDATE", handleDashboardUpdate));

});

onBeforeUnmount(() => {
  unsubscribers.forEach((fn) => fn());
  window.removeEventListener("resize", handleResize);
  statusChart?.dispose();
  statusChart = null;
  clearMarkers();
});
</script>

<style scoped>
.dashboard {
  min-height: 100%;
}

.stat-row {
  margin-bottom: 12px;
}

.stat-card-box {
  margin-bottom: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 82px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-value {
  font-size: 26px;
  line-height: 30px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  margin-top: 2px;
  font-size: 13px;
  color: #606266;
}

.stat-note {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.content-row {
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.map-card :deep(.el-card__body) {
  padding: 0;
}

.map-shell {
  position: relative;
  height: calc(100vh - 335px);
  min-height: 560px;
  overflow: hidden;
}

.map-summary {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 1000;
  min-width: 118px;
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.16);
}

.summary-title {
  font-size: 12px;
  color: #909399;
}

.summary-value {
  font-size: 24px;
  line-height: 30px;
  font-weight: 700;
  color: #303133;
}

.summary-note {
  font-size: 12px;
  color: #606266;
}

.map-detail {
  position: absolute;
  top: 88px;
  left: 14px;
  z-index: 1000;
  width: 268px;
  padding: 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.97);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.16);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.device-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.detail-close {
  color: #909399;
  cursor: pointer;
}

.detail-close:hover {
  color: #409eff;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
}

.detail-row:last-child {
  border-bottom: 0;
}

.detail-row span {
  flex-shrink: 0;
  color: #909399;
}

.detail-row b {
  color: #303133;
  font-weight: 400;
  text-align: right;
}

.battery-progress {
  flex: 1;
}

.map-legend {
  position: absolute;
  bottom: 16px;
  left: 14px;
  z-index: 1000;
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.14);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.map-empty {
  position: absolute;
  inset: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
  pointer-events: none;
}

.right-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

.status-chart {
  width: 100%;
  height: 250px;
}

.overview-card {
  flex: 1;
}

.overview-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.overview-item {
  padding: 12px;
  border-radius: 6px;
  background: #f7f9fc;
}

.overview-label {
  font-size: 12px;
  color: #909399;
}

.overview-value {
  margin-top: 4px;
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.task-progress {
  margin-top: 16px;
}

.progress-title {
  margin: 12px 0 6px;
  font-size: 12px;
  color: #606266;
}

.bottom-row {
  margin-bottom: 0;
}
</style>



