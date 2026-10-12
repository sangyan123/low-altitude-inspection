<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.deptId" placeholder="所属部门" clearable style="width: 150px" @change="handleFilterChange">
        <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
      </el-select>
      <el-select v-model="query.deviceType" placeholder="设备类型" clearable style="width: 140px" @change="handleFilterChange">
        <el-option label="固定机巢" :value="1" />
        <el-option label="车载机巢" :value="2" />
        <el-option label="单兵手持" :value="3" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="handleFilterChange">
        <el-option label="离线" :value="0" />
        <el-option label="在线" :value="1" />
        <el-option label="飞行中" :value="2" />
        <el-option label="充电中" :value="3" />
        <el-option label="维护中" :value="4" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="搜索名称/序列号/型号" style="width: 220px" clearable @clear="handleFilterChange" @keyup.enter="handleFilterChange" />
      <el-button type="primary" @click="handleFilterChange">查询</el-button>
      <el-button type="success" @click="openCreate">新增设备</el-button>
    </div>

    <el-tabs v-model="activeTab" class="view-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="表格视图" name="table">
        <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="deviceSn" label="序列号" width="150" />
      <el-table-column prop="deviceName" label="设备名称" min-width="130" />
      <el-table-column prop="model" label="型号" width="110" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ typeName(row.deviceType) }}</template>
      </el-table-column>
      <el-table-column label="所属部门" width="120">
        <template #default="{ row }">{{ deptMap[row.deptId] || '未分配' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="电量" width="130">
        <template #default="{ row }">
          <el-progress :percentage="Number(row.battery || 0)" :color="batteryColor" :stroke-width="14" text-inside />
        </template>
      </el-table-column>
      <el-table-column label="通信链路" width="100">
        <template #default="{ row }">
          <el-tag :type="commTag(row.commMode)" size="small">{{ commName(row.commMode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalFlightCount" label="累计架次" width="90" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该设备？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @change="loadData"
    />
      </el-tab-pane>

      <el-tab-pane label="地图视图" name="map">
        <div v-if="activeTab === 'map'" class="device-map">
          <GisMap :base-type="baseType" @ready="onMapReady" />

          <div class="map-switch">
            <el-radio-group v-model="baseType" size="small">
              <el-radio-button value="vec">矢量</el-radio-button>
              <el-radio-button value="img">影像</el-radio-button>
            </el-radio-group>
          </div>

          <div class="map-summary">
            <div class="summary-title">设备分布</div>
            <div class="summary-value">{{ mapStats.total }}</div>
            <div class="summary-sub">已定位 {{ mapStats.located }} 台</div>
          </div>

          <div v-if="selectedDevice" class="map-detail">
            <div class="detail-header">
              <div class="detail-name">
                <span class="status-dot" :style="{ background: statusColor(selectedDevice.status) }"></span>
                {{ selectedDevice.deviceName }}
              </div>
              <el-icon class="detail-close" @click="selectedDevice = null"><Close /></el-icon>
            </div>
            <div class="detail-row"><span>序列号</span><b>{{ selectedDevice.deviceSn }}</b></div>
            <div class="detail-row"><span>型号</span><b>{{ selectedDevice.model || "-" }}</b></div>
            <div class="detail-row"><span>类型</span><b>{{ typeName(selectedDevice.deviceType) }}</b></div>
            <div class="detail-row"><span>所属部门</span><b>{{ deptMap[selectedDevice.deptId] || "未分配" }}</b></div>
            <div class="detail-row">
              <span>状态</span>
              <el-tag :type="statusTag(selectedDevice.status)" size="small">
                {{ statusName(selectedDevice.status) }}
              </el-tag>
            </div>
            <div class="detail-row">
              <span>电量</span>
              <el-progress class="battery-progress" :percentage="Number(selectedDevice.battery || 0)" :color="batteryColor" :stroke-width="10" />
            </div>
            <div class="detail-row"><span>通信链路</span><b>{{ commName(selectedDevice.commMode) }}</b></div>
            <div class="detail-row">
              <span>坐标</span>
              <b>{{ selectedDevice.lng ?? "-" }}, {{ selectedDevice.lat ?? "-" }}</b>
            </div>
            <div class="detail-row"><span>累计架次</span><b>{{ selectedDevice.totalFlightCount ?? 0 }}</b></div>
          </div>

          <div class="map-legend">
            <div v-for="item in legendItems" :key="item.status" class="legend-item">
              <span class="status-dot" :style="{ background: item.color }"></span>
              {{ item.label }}
            </div>
          </div>

          <div v-if="mapLoading" class="map-loading" v-loading="true" element-loading-text="设备加载中" />
          <div v-else-if="mapDevices.length === 0" class="map-empty">暂无符合条件的设备</div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑设备' : '新增设备'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="序列号" required>
              <el-input v-model="form.deviceSn" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" required>
              <el-input v-model="form.deviceName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="型号">
              <el-input v-model="form.model" placeholder="如 大疆M30T" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型">
              <el-select v-model="form.deviceType" style="width: 100%">
                <el-option label="固定机巢" :value="1" />
                <el-option label="车载机巢" :value="2" />
                <el-option label="单兵手持" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-select v-model="form.deptId" clearable style="width: 100%">
                <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通信链路">
              <el-select v-model="form.commMode" style="width: 100%">
                <el-option label="4G" :value="1" />
                <el-option label="5G" :value="2" />
                <el-option label="自组网" :value="3" />
                <el-option label="中继模式" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最大续航">
              <el-input-number v-model="form.maxFlightTime" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大载重">
              <el-input-number v-model="form.maxPayloadKg" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="当前经度">
              <el-input-number
                v-model="form.lng"
                :min="73"
                :max="135.35"
                :precision="6"
                :step="0.001"
                :controls="false"
                placeholder="如 101.735000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="当前纬度">
              <el-input-number
                v-model="form.lat"
                :min="3.86"
                :max="53.55"
                :precision="6"
                :step="0.001"
                :controls="false"
                placeholder="如 26.585000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import L from "leaflet";
import GisMap from "../../components/GisMap.vue";
import { createDeviceMarkerIcon } from "../../utils/deviceMarker";
import { getDeptList, getDevicePage, createDevice, updateDevice, deleteDevice } from "../../api/system";

const list = ref<any[]>([]);
const deptList = ref<any[]>([]);
const total = ref(0);
const loading = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);

const activeTab = ref("table");
const baseType = ref<"vec" | "img">("vec");
const mapDevices = ref<any[]>([]);
const mapLoading = ref(false);
const selectedDevice = ref<any | null>(null);
let map: L.Map | null = null;
const markers = new Map<number, L.Marker>();

const query = reactive({
  current: 1, size: 10,
  deptId: null as number | null,
  deviceType: null as number | null,
  status: null as number | null,
  keyword: "",
});

const emptyForm = {
  id: null as number | null, deviceSn: "", deviceName: "", model: "",
  deviceType: 1, deptId: null as number | null, commMode: 1,
  maxFlightTime: 30, maxPayloadKg: 3,
  lng: null as number | null, lat: null as number | null,
  remark: "",
};
const form = reactive({ ...emptyForm });

const deptMap = computed(() => {
  const m: Record<number, string> = {};
  deptList.value.forEach((d) => (m[d.id] = d.deptName));
  return m;
});

const typeName = (t: number) => ({ 1: "固定机巢", 2: "车载机巢", 3: "单兵手持" }[t] || "-");
const statusName = (s: number) => ({ 0: "离线", 1: "在线", 2: "飞行中", 3: "充电中", 4: "维护中" }[s] || "-");
const statusTag = (s: number) => ({ 0: "info", 1: "success", 2: "primary", 3: "warning", 4: "danger" }[s] || "info");
const commName = (c: number) => ({ 0: "离线", 1: "4G", 2: "5G", 3: "自组网", 4: "中继" }[c] || "-");
const commTag = (c: number) => ({ 0: "info", 1: "success", 2: "success", 3: "warning", 4: "warning" }[c] || "info");

const batteryColor = (percentage: number) => {
  if (percentage > 60) return "#67c23a";
  if (percentage > 20) return "#e6a23c";
  return "#f56c6c";
};

const statusColor = (status: number) =>
  ({ 0: "#909399", 1: "#67c23a", 2: "#409eff", 3: "#e6a23c", 4: "#f56c6c" }[status] || "#909399");

const legendItems = [
  { status: 1, label: "在线", color: "#67c23a" },
  { status: 2, label: "飞行中", color: "#409eff" },
  { status: 3, label: "充电中", color: "#e6a23c" },
  { status: 0, label: "离线", color: "#909399" },
  { status: 4, label: "维护中", color: "#f56c6c" },
];

const mapStats = computed(() => ({
  total: mapDevices.value.length,
  located: mapDevices.value.filter((d) => d.lng != null && d.lat != null).length,
}));

function handleFilterChange() {
  loadData();
  if (activeTab.value === "map" && map) {
    loadMapData();
  }
}

function handleTabChange(name: string | number) {
  if (name === "map" && map) {
    loadMapData();
  }
}

function onMapReady(mapInstance: L.Map) {
  map = mapInstance;
  loadMapData();
}

async function loadMapData() {
  if (!map) return;
  mapLoading.value = true;
  try {
    const res: any = await getDevicePage({
      ...query,
      current: 1,
      size: 1000,
    });
    mapDevices.value = res.data.records || [];
    renderMarkers();
  } finally {
    mapLoading.value = false;
  }
}

function clearMarkers() {
  markers.forEach((marker) => marker.remove());
  markers.clear();
}

function renderMarkers() {
  if (!map) return;
  clearMarkers();

  const points: L.LatLngExpression[] = [];
  for (const device of mapDevices.value) {
    if (device.lng == null || device.lat == null) continue;
    const position: L.LatLngExpression = [Number(device.lat), Number(device.lng)];
    const marker = L.marker(position, { icon: createDeviceMarkerIcon(device) }).addTo(map);
    marker.on("click", () => {
      selectedDevice.value = device;
    });
    markers.set(device.id, marker);
    points.push(position);
  }

  if (points.length === 1) {
    map.setView(points[0], 15);
  } else if (points.length > 1) {
    map.fitBounds(L.latLngBounds(points).pad(0.18));
  }
}

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getDevicePage(query);
    list.value = res.data.records;
    total.value = res.data.total;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, emptyForm);
  dialogVisible.value = true;
}

function openEdit(row: any) {
  isEdit.value = true;
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.deviceSn || !form.deviceName) {
    ElMessage.warning("请填写序列号和设备名称");
    return;
  }
  if ((form.lng == null) !== (form.lat == null)) {
    ElMessage.warning("经度和纬度需要同时填写");
    return;
  }
  if (isEdit.value) {
    await updateDevice(form);
  } else {
    await createDevice(form);
  }
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  loadData();
}

async function handleDelete(id: number) {
  await deleteDevice(id);
  ElMessage.success("删除成功");
  loadData();
}

onMounted(() => {
  loadDepts();
  loadData();
});

async function loadDepts() {
  const res: any = await getDeptList();
  deptList.value = res.data;
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.view-tabs {
  margin-top: 4px;
}

.device-map {
  position: relative;
  height: calc(100vh - 250px);
  min-height: 560px;
  border-radius: 6px;
  overflow: hidden;
}

.map-switch {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 1000;
}

.map-summary {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 1000;
  min-width: 112px;
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.14);
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

.summary-sub {
  font-size: 12px;
  color: #606266;
}

.map-detail {
  position: absolute;
  top: 82px;
  left: 12px;
  z-index: 1000;
  width: 264px;
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

.detail-name {
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
  left: 12px;
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

.map-loading {
  position: absolute;
  inset: 0;
  z-index: 999;
  background: rgba(255, 255, 255, 0.24);
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

</style>
