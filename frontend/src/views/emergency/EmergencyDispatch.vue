<template>
  <div class="emergency-dispatch">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.eventType" placeholder="事件类型" clearable style="width: 140px" @change="loadData">
          <el-option label="洪涝" :value="1" />
          <el-option label="山火" :value="2" />
          <el-option label="地质灾害" :value="3" />
          <el-option label="事故" :value="4" />
          <el-option label="搜救" :value="5" />
        </el-select>
        <el-select v-model="query.level" placeholder="事件等级" clearable style="width: 140px" @change="loadData">
          <el-option label="一般" :value="1" />
          <el-option label="较大" :value="2" />
          <el-option label="重大" :value="3" />
          <el-option label="特别重大" :value="4" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="loadData">
          <el-option label="进行中" :value="0" />
          <el-option label="已结束" :value="1" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="loadData"
        />
        <el-input
          v-model="query.keyword"
          placeholder="搜索名称/位置/描述"
          clearable
          style="width: 220px"
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button type="success" @click="openCreate">上报应急事件</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="map-card">
      <template #header>
        <div class="card-header">
          <span>应急调度地图</span>
          <el-checkbox v-model="showDevices" @change="renderMarkers">显示无人机</el-checkbox>
        </div>
      </template>
      <div class="map-shell">
        <GisMap :base-type="baseType" :zoom="12" @ready="onMapReady" />
        <div class="map-switch">
          <el-radio-group v-model="baseType" size="small">
            <el-radio-button value="vec">矢量</el-radio-button>
            <el-radio-button value="img">影像</el-radio-button>
          </el-radio-group>
        </div>

        <div class="map-summary">
          <div class="summary-title">应急调度</div>
          <div class="summary-value">{{ total }}</div>
          <div class="summary-note">
            进行中 {{ emergencyStats.ongoing }} · 已结束 {{ emergencyStats.finished }}
          </div>
        </div>

        <div v-if="selectedEvent" class="map-detail">
          <div class="detail-header">
            <b>{{ selectedEvent.eventName }}</b>
            <el-tag :type="levelTag(selectedEvent.level)" size="small">
              {{ levelName(selectedEvent.level) }}
            </el-tag>
          </div>
          <div class="detail-row"><span>类型</span><b>{{ typeName(selectedEvent.eventType) }}</b></div>
          <div class="detail-row"><span>状态</span><b>{{ statusName(selectedEvent.status) }}</b></div>
          <div class="detail-row"><span>位置</span><b>{{ selectedEvent.locationDesc || "-" }}</b></div>
          <div class="detail-row"><span>调度无人机</span><b>{{ selectedEvent.assignedDeviceName || "未调度" }}</b></div>
          <div class="detail-row"><span>搜救目标</span><b>{{ selectedEvent.targetCount ?? 0 }} 个</b></div>
          <el-button class="detail-button" size="small" type="primary" @click="openDetailDrawer(selectedEvent)">打开详情</el-button>
        </div>

        <div class="map-legend">
          <div class="legend-item"><span class="legend-dot" style="background:#f56c6c"></span>应急事件</div>
          <div class="legend-item"><span class="legend-dot" style="background:#e6a23c"></span>受困人员</div>
          <div class="legend-item"><span class="legend-dot" style="background:#f56c6c"></span>火点</div>
          <div class="legend-item"><span class="legend-dot" style="background:#409eff"></span>险情点</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="table-header">
        <span>应急事件列表</span>
        <span class="table-tip">点击行可在地图中聚焦事件与搜救目标</span>
      </div>
      <el-table
        :data="list"
        v-loading="loading"
        border
        stripe
        highlight-current-row
        @row-click="selectEvent"
      >
        <el-table-column prop="eventName" label="事件名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ typeName(row.eventType) }}</template>
        </el-table-column>
        <el-table-column label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" size="small">{{ levelName(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="locationDesc" label="位置" min-width="160" show-overflow-tooltip />
        <el-table-column label="调度无人机" width="140">
          <template #default="{ row }">{{ row.assignedDeviceName || "未调度" }}</template>
        </el-table-column>
        <el-table-column label="目标/追踪" width="110">
          <template #default="{ row }">{{ row.targetCount ?? 0 }} / {{ row.trackingCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : 'info'" size="small">
              {{ statusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上报时间" width="170" />
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" @click="openDetailDrawer(row)">详情</el-button>
              <el-button v-if="row.status === 0" size="small" type="primary" @click.stop="openDispatch(row)">调度</el-button>
              <el-button v-if="row.status === 0" size="small" type="warning" @click.stop="openEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 0" size="small" type="success" @click.stop="openFinish(row)">结束</el-button>
              <el-popconfirm title="确定删除该应急事件？" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button size="small" type="danger" @click.stop>删除</el-button>
                </template>
              </el-popconfirm>
            </div>
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
    </el-card>

    <el-drawer v-model="detailVisible" :title="detail?.eventName || '应急事件详情'" size="760px">
      <div v-if="detail" class="detail-container">
        <div class="detail-section">
          <div class="section-title">基础信息</div>
          <div class="info-grid">
            <div class="info-item"><span>事件类型</span><b>{{ typeName(detail.eventType) }}</b></div>
            <div class="info-item"><span>事件等级</span><b>{{ levelName(detail.level) }}</b></div>
            <div class="info-item"><span>当前状态</span><b>{{ statusName(detail.status) }}</b></div>
            <div class="info-item"><span>上报人</span><b>{{ detail.reporterName || "-" }}</b></div>
            <div class="info-item"><span>位置描述</span><b>{{ detail.locationDesc || "-" }}</b></div>
            <div class="info-item"><span>坐标</span><b>{{ detail.lng ?? "-" }}, {{ detail.lat ?? "-" }}</b></div>
            <div class="info-item"><span>调度无人机</span><b>{{ detail.assignedDeviceName || "未调度" }}</b></div>
            <div class="info-item"><span>调度时间</span><b>{{ detail.dispatchTime || "-" }}</b></div>
          </div>
          <div class="description">{{ detail.description || "-" }}</div>
          <div v-if="detail.dispatchRemark" class="command-box">
            <b>调度指令：</b>{{ detail.dispatchRemark }}
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">
            搜救目标
            <el-button size="small" type="primary" @click="openTargetCreate">新增目标</el-button>
          </div>
          <el-table :data="detail.targets || []" border size="small">
            <el-table-column prop="targetName" label="目标名称" min-width="120" />
            <el-table-column prop="targetTypeName" label="类型" width="100" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="targetStatusTag(row.status)">{{ row.statusName || "-" }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="foundByDeviceName" label="发现设备" width="120">
              <template #default="{ row }">{{ row.foundByDeviceName || "-" }}</template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button size="small" @click="openTargetEdit(row)">编辑</el-button>
                  <el-button size="small" :type="row.tracking === 1 ? 'warning' : 'success'" @click="toggleTracking(row)">
                    {{ row.tracking === 1 ? "停止" : "追踪" }}
                  </el-button>
                  <el-popconfirm title="确定删除该目标？" @confirm="handleDeleteTarget(row.id)">
                    <template #reference>
                      <el-button size="small" type="danger">删除</el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="detail-section">
          <div class="section-title">处置操作</div>
          <el-button v-if="detail.status === 0" type="primary" @click="openDispatch(detail)">调度无人机</el-button>
          <el-button v-if="detail.status === 0" type="warning" @click="openEdit(detail)">编辑事件</el-button>
          <el-button v-if="detail.status === 0" type="success" @click="openFinish(detail)">结束应急事件</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="eventVisible" :title="isEdit ? '编辑应急事件' : '上报应急事件'" width="720px">
      <el-form :model="eventForm" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="事件名称" required>
              <el-input v-model="eventForm.eventName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="事件类型" required>
              <el-select v-model="eventForm.eventType" style="width: 100%">
                <el-option label="洪涝" :value="1" />
                <el-option label="山火" :value="2" />
                <el-option label="地质灾害" :value="3" />
                <el-option label="事故" :value="4" />
                <el-option label="搜救" :value="5" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="事件等级" required>
              <el-select v-model="eventForm.level" style="width: 100%">
                <el-option label="一般" :value="1" />
                <el-option label="较大" :value="2" />
                <el-option label="重大" :value="3" />
                <el-option label="特别重大" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="位置描述">
              <el-input v-model="eventForm.locationDesc" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="经度" required>
              <el-input-number v-model="eventForm.lng" :min="73" :max="135.35" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" required>
              <el-input-number v-model="eventForm.lat" :min="3.86" :max="53.55" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="事件描述">
          <el-input v-model="eventForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="eventVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEvent">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dispatchVisible" title="调度无人机" width="560px">
      <el-form label-width="100px">
        <el-form-item label="无人机" required>
          <el-select v-model="dispatchForm.deviceId" filterable style="width: 100%">
            <el-option
              v-for="item in devices"
              :key="item.id"
              :label="`${item.deviceName}（${statusName(item.status)}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调度指令">
          <el-input v-model="dispatchForm.remark" type="textarea" :rows="4" placeholder="填写航线要求、任务重点或回传要求" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDispatch">确认调度</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="finishVisible" title="结束应急事件" width="520px">
      <el-form label-width="90px">
        <el-form-item label="结束说明">
          <el-input v-model="finishForm.remark" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishVisible = false">取消</el-button>
        <el-button type="primary" @click="handleFinish">确认结束</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="targetVisible" :title="isTargetEdit ? '编辑搜救目标' : '新增搜救目标'" width="620px">
      <el-form :model="targetForm" label-width="100px">
        <el-form-item label="目标名称" required>
          <el-input v-model="targetForm.targetName" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="目标类型" required>
              <el-select v-model="targetForm.targetType" style="width: 100%">
                <el-option label="受困人员" :value="1" />
                <el-option label="火点" :value="2" />
                <el-option label="险情点" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标状态">
              <el-select v-model="targetForm.status" style="width: 100%">
                <el-option label="待核实" :value="1" />
                <el-option label="已确认" :value="2" />
                <el-option label="已处置" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="经度" required>
              <el-input-number v-model="targetForm.lng" :min="73" :max="135.35" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" required>
              <el-input-number v-model="targetForm.lat" :min="3.86" :max="53.55" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="发现设备">
          <el-select v-model="targetForm.foundByDevice" clearable filterable style="width: 100%">
            <el-option v-for="item in devices" :key="item.id" :label="item.deviceName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="追踪状态">
          <el-radio-group v-model="targetForm.tracking">
            <el-radio-button :value="1">追踪中</el-radio-button>
            <el-radio-button :value="0">已停止</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="targetForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="targetVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveTarget">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import L from "leaflet";
import GisMap from "../../components/GisMap.vue";
import { createDeviceMarkerIcon } from "../../utils/deviceMarker";
import { getDevicePage } from "../../api/system";
import {
  getEmergencyPage,
  getEmergencyDetail,
  createEmergency,
  updateEmergency,
  dispatchEmergency,
  finishEmergency,
  deleteEmergency,
  createEmergencyTarget,
  updateEmergencyTarget,
  setEmergencyTargetTracking,
  deleteEmergencyTarget,
  type EmergencyEvent,
  type EmergencyTarget,
} from "../../api/emergency";
import { subscribeToMessageType } from "../../utils/websocket";

const loading = ref(false);
const list = ref<EmergencyEvent[]>([]);
const devices = ref<any[]>([]);
const total = ref(0);
const baseType = ref<"vec" | "img">("vec");
const showDevices = ref(false);
const selectedEvent = ref<EmergencyEvent | null>(null);

const detail = ref<EmergencyEvent | null>(null);
const detailVisible = ref(false);
const eventVisible = ref(false);
const isEdit = ref(false);
const dispatchVisible = ref(false);
const finishVisible = ref(false);
const targetVisible = ref(false);
const isTargetEdit = ref(false);

const dateRange = ref<[string, string] | null>(null);
let map: L.Map | null = null;
let markers: L.Layer[] = [];
let unsubscribers: (() => void)[] = [];

const query = reactive({
  current: 1,
  size: 10,
  eventType: null as number | null,
  level: null as number | null,
  status: null as number | null,
  keyword: "",
});

const emptyEvent = {
  id: null as number | null,
  eventName: "",
  eventType: 5,
  level: 2,
  lng: null as number | null,
  lat: null as number | null,
  locationDesc: "",
  description: "",
};
const eventForm = reactive({ ...emptyEvent });

const dispatchForm = reactive({
  deviceId: null as number | null,
  remark: "",
});
const finishForm = reactive({ remark: "" });

const emptyTarget = {
  id: null as number | null,
  emergencyEventId: null as number | null,
  targetName: "",
  targetType: 1,
  lng: null as number | null,
  lat: null as number | null,
  foundByDevice: null as number | null,
  foundTime: "",
  status: 1,
  tracking: 1,
  shared: 0,
  remark: "",
};
const targetForm = reactive({ ...emptyTarget });

const emergencyStats = computed(() => ({
  ongoing: list.value.filter((item) => item.status === 0).length,
  finished: list.value.filter((item) => item.status === 1).length,
}));

const typeName = (value?: number | null) =>
  ({ 1: "洪涝", 2: "山火", 3: "地质灾害", 4: "事故", 5: "搜救" }[Number(value)] || "-");
const levelName = (value?: number | null) =>
  ({ 1: "一般", 2: "较大", 3: "重大", 4: "特别重大" }[Number(value)] || "-");
const levelTag = (value?: number | null) =>
  ({ 1: "info", 2: "warning", 3: "danger", 4: "danger" }[Number(value)] || "info");
const statusName = (value?: number | null) =>
  ({ 0: "进行中", 1: "已结束", 2: "在线", 3: "飞行中", 4: "维护中" }[Number(value)] || "-");

const targetStatusTag = (value?: number | null) =>
  ({ 1: "warning", 2: "primary", 3: "success" }[Number(value)] || "info");

function onMapReady(instance: L.Map) {
  map = instance;
  renderMarkers();
}

function clearMarkers() {
  markers.forEach((marker) => map?.removeLayer(marker));
  markers = [];
}

function inlineIcon(color: string, label: string, size = 18): L.DivIcon {
  return L.divIcon({
    className: "",
    iconSize: [0, 0],
    iconAnchor: [0, 0],
    html: `<div style="position:absolute;transform:translate(-50%,-50%);text-align:center">
      <div style="width:${size}px;height:${size}px;margin:0 auto;border:2px solid #fff;border-radius:50%;background:${color};box-shadow:0 0 12px ${color};"></div>
      <div style="margin-top:3px;padding:1px 6px;border:1px solid rgba(255,255,255,.32);border-radius:999px;background:rgba(17,24,39,.78);color:#fff;font-size:11px;line-height:16px;white-space:nowrap;text-shadow:0 1px 2px rgba(0,0,0,.8);">${label}</div>
    </div>`,
  });
}

function renderMarkers() {
  if (!map) return;
  clearMarkers();

  const points: L.LatLngExpression[] = [];
  const event = selectedEvent.value;
  if (event?.lng != null && event.lat != null) {
    const position: L.LatLngExpression = [Number(event.lat), Number(event.lng)];
    const marker = L.marker(position, {
      icon: inlineIcon("#f56c6c", event.eventName, 20),
    }).addTo(map);
    marker.bindPopup(`<b>${event.eventName}</b><br>${event.locationDesc || "应急事件"}`);
    markers.push(marker);
    points.push(position);
  }

  for (const target of event?.targets || []) {
    if (target.lng == null || target.lat == null) continue;
    const color = target.targetType === 2 ? "#f56c6c" : target.targetType === 1 ? "#e6a23c" : "#409eff";
    const position: L.LatLngExpression = [Number(target.lat), Number(target.lng)];
    const marker = L.marker(position, {
      icon: inlineIcon(color, target.targetName, 15),
    }).addTo(map);
    marker.bindPopup(`<b>${target.targetName}</b><br>${target.targetType === 2 ? "火点" : target.targetType === 1 ? "受困人员" : "险情点"}<br>${target.remark || ""}`);
    markers.push(marker);
    points.push(position);
  }

  if (showDevices.value) {
    for (const device of devices.value) {
      if (device.lng == null || device.lat == null) continue;
      const position: L.LatLngExpression = [Number(device.lat), Number(device.lng)];
      const marker = L.marker(position, { icon: createDeviceMarkerIcon(device) }).addTo(map);
      marker.bindPopup(`<b>${device.deviceName}</b><br>${statusName(device.status)}`);
      markers.push(marker);
      points.push(position);
    }
  }

  if (points.length > 1) {
    map.fitBounds(L.latLngBounds(points).pad(0.2));
  } else if (points.length === 1) {
    map.setView(points[0], 15);
  }
}

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getEmergencyPage({
      ...query,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    });
    list.value = res.data.records || [];
    total.value = res.data.total;
    if (!selectedEvent.value && list.value.length) {
      await selectEvent(list.value[0]);
    }
  } finally {
    loading.value = false;
  }
}

async function selectEvent(row: EmergencyEvent) {
  if (row.id == null) return;
  const res: any = await getEmergencyDetail(row.id);
  selectedEvent.value = res.data;
  renderMarkers();
}

function openDetailDrawer(row: EmergencyEvent) {
  detail.value = row;
  detailVisible.value = true;
}

function openCreate() {
  isEdit.value = false;
  Object.assign(eventForm, emptyEvent);
  eventVisible.value = true;
}

function openEdit(row: EmergencyEvent) {
  isEdit.value = true;
  Object.assign(eventForm, row);
  eventVisible.value = true;
}

async function handleSaveEvent() {
  if (!eventForm.eventName) {
    ElMessage.warning("请填写事件名称");
    return;
  }
  if (eventForm.lng == null || eventForm.lat == null) {
    ElMessage.warning("请填写事件坐标");
    return;
  }
  if (isEdit.value) {
    await updateEmergency(eventForm);
  } else {
    const res: any = await createEmergency(eventForm);
    await selectEvent({ id: res.data } as EmergencyEvent);
  }
  ElMessage.success("保存成功");
  eventVisible.value = false;
  loadData();
}

function openDispatch(row: EmergencyEvent) {
  selectedEvent.value = row;
  dispatchForm.deviceId = row.assignedDeviceId ?? null;
  dispatchForm.remark = row.dispatchRemark || "";
  dispatchVisible.value = true;
}

async function handleDispatch() {
  if (!selectedEvent.value?.id) return;
  if (!dispatchForm.deviceId) {
    ElMessage.warning("请选择调度无人机");
    return;
  }
  await dispatchEmergency(selectedEvent.value.id, {
    deviceId: dispatchForm.deviceId,
    remark: dispatchForm.remark || undefined,
  });
  ElMessage.success("调度成功");
  dispatchVisible.value = false;
  const detailRes: any = await getEmergencyDetail(selectedEvent.value.id);
  selectedEvent.value = detailRes.data;
  renderMarkers();
  loadData();
}

function openFinish(row: EmergencyEvent) {
  selectedEvent.value = row;
  finishForm.remark = "";
  finishVisible.value = true;
}

async function handleFinish() {
  if (!selectedEvent.value?.id) return;
  await finishEmergency(selectedEvent.value.id, { remark: finishForm.remark || undefined });
  ElMessage.success("应急事件已结束");
  finishVisible.value = false;
  loadData();
}

async function handleDelete(id: number) {
  await deleteEmergency(id);
  ElMessage.success("删除成功");
  if (selectedEvent.value?.id === id) selectedEvent.value = null;
  renderMarkers();
  loadData();
}

function openTargetCreate() {
  if (!detail.value?.id && !selectedEvent.value?.id) return;
  isTargetEdit.value = false;
  Object.assign(targetForm, emptyTarget);
  targetForm.emergencyEventId = detail.value?.id || selectedEvent.value?.id || null;
  targetVisible.value = true;
}

function openTargetEdit(row: EmergencyTarget) {
  isTargetEdit.value = true;
  Object.assign(targetForm, row);
  targetVisible.value = true;
}

async function handleSaveTarget() {
  if (!targetForm.emergencyEventId) {
    targetForm.emergencyEventId = detail.value?.id || selectedEvent.value?.id || null;
  }
  if (!targetForm.targetName) {
    ElMessage.warning("请填写目标名称");
    return;
  }
  if (targetForm.lng == null || targetForm.lat == null) {
    ElMessage.warning("请填写目标坐标");
    return;
  }
  if (isTargetEdit.value) {
    await updateEmergencyTarget(targetForm);
  } else {
    await createEmergencyTarget(targetForm);
  }
  ElMessage.success("保存成功");
  targetVisible.value = false;
  if (selectedEvent.value?.id) {
    await selectEvent(selectedEvent.value);
    if (detailVisible.value) detail.value = selectedEvent.value;
  }
  loadData();
}

async function toggleTracking(row: EmergencyTarget) {
  await setEmergencyTargetTracking(row.id!, row.tracking !== 1);
  ElMessage.success(row.tracking === 1 ? "已停止追踪" : "已开始追踪");
  if (selectedEvent.value?.id) await selectEvent(selectedEvent.value);
  if (detailVisible.value) detail.value = selectedEvent.value;
  loadData();
}

async function handleDeleteTarget(id: number) {
  await deleteEmergencyTarget(id);
  ElMessage.success("删除成功");
  if (selectedEvent.value?.id) await selectEvent(selectedEvent.value);
  if (detailVisible.value) detail.value = selectedEvent.value;
  loadData();
}

function handleResize() {
  map?.invalidateSize();
}

function handleEmergencyUpdate() {
  if (showDevices.value && selectedEvent.value?.id) {
    selectEvent(selectedEvent.value);
  }
}

onMounted(async () => {
  window.addEventListener("resize", handleResize);
  const res: any = await getDevicePage({ current: 1, size: 500 });
  devices.value = res.data.records || [];
  await loadData();
  // WebSocket real-time update replaces polling
  unsubscribers.push(subscribeToMessageType("EMERGENCY_UPDATE", handleEmergencyUpdate));
});

onBeforeUnmount(() => {
  unsubscribers.forEach((fn) => fn());
  window.removeEventListener("resize", handleResize);
  clearMarkers();
});
</script>

<style scoped>
.emergency-dispatch {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
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
  height: calc(100vh - 430px);
  min-height: 520px;
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
  min-width: 130px;
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

.summary-note {
  font-size: 12px;
  color: #606266;
}

.map-detail {
  position: absolute;
  top: 82px;
  left: 12px;
  z-index: 1000;
  width: 280px;
  padding: 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.97);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.16);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  color: #303133;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
}

.detail-row span {
  color: #909399;
}

.detail-row b {
  color: #303133;
  font-weight: 400;
  text-align: right;
}

.detail-button {
  width: 100%;
  margin-top: 10px;
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

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  font-weight: 600;
}

.table-tip {
  font-size: 12px;
  color: #909399;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: nowrap;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.row-actions :deep(.el-button) {
  flex: 0 0 auto;
  padding: 5px 8px;
}

.detail-container {
  padding-right: 6px;
}

.detail-section + .detail-section {
  margin-top: 22px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-item {
  padding: 8px 10px;
  border-radius: 6px;
  background: #f7f9fc;
  font-size: 12px;
}

.info-item span {
  display: block;
  margin-bottom: 4px;
  color: #909399;
}

.info-item b {
  color: #303133;
  font-weight: 500;
}

.description {
  margin-top: 12px;
  padding: 10px;
  border-radius: 6px;
  background: #f7f9fc;
  color: #606266;
  line-height: 20px;
}

.command-box {
  margin-top: 10px;
  padding: 10px;
  border-left: 3px solid #409eff;
  border-radius: 4px;
  background: #ecf5ff;
  color: #606266;
}
</style>
