<template>
  <div class="route-map-editor">
    <div class="editor-toolbar">
      <div class="toolbar-info">
        <el-tag size="small">航点 {{ waypoints.length }}</el-tag>
        <el-tag size="small" type="success">航程 {{ totalKm }} km</el-tag>
        <el-tag v-if="!waypoints.length" size="small" type="info">点击地图添加航点</el-tag>
      </div>
      <div class="toolbar-actions">
        <el-button size="small" :disabled="!waypoints.length" @click="undoLast">撤销上一点</el-button>
        <el-button size="small" type="danger" plain :disabled="!waypoints.length" @click="clearAll">清空</el-button>
      </div>
    </div>
    <div ref="mapEl" class="editor-map"></div>
    <div v-if="waypoints.length" class="waypoint-list">
      <div v-for="(wp, idx) in waypoints" :key="idx" class="waypoint-row">
        <span class="wp-no">{{ idx + 1 }}</span>
        <span class="wp-lnglat">{{ wp.lng.toFixed(6) }}, {{ wp.lat.toFixed(6) }}</span>
        <el-input-number
          v-model="wp.alt"
          :min="0"
          :max="5000"
          :step="10"
          size="small"
          controls-position="right"
          style="width: 100px"
        />
        <span class="wp-alt-unit">m</span>
        <el-button size="small" type="danger" text @click="removePoint(idx)">删除</el-button>
      </div>
    </div>
    <div v-else class="waypoint-empty">暂无航点，在地图上单击即可添加，拖动标点微调位置</div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import L from "leaflet";
import { getMapSource } from "../utils/mapSource";
import { distanceMeters } from "../utils/coord";

interface Waypoint { lng: number; lat: number; alt: number }

const props = defineProps<{ modelValue: string }>();
const emit = defineEmits<{
  (e: "update:modelValue", value: string): void;
  (e: "distance-change", km: number, count: number): void;
}>();

const mapEl = ref<HTMLDivElement>();
const waypoints = ref<Waypoint[]>([]);
const totalKm = ref(0);

let map: L.Map | null = null;
let polyline: L.Polyline | null = null;
let markers: L.Marker[] = [];
let suppressEmit = false;
let resizeObserver: ResizeObserver | null = null;
let pendingFitBounds = false;

function round6(v: number): number {
  return Math.round(v * 1e6) / 1e6;
}

function serialize(): string {
  return JSON.stringify(waypoints.value.map((wp) => ({ lng: round6(wp.lng), lat: round6(wp.lat), alt: wp.alt })));
}

function createIcon(idx: number): L.DivIcon {
  return L.divIcon({
    className: "route-wp-icon",
    html: '<div class="route-wp-marker">' + (idx + 1) + "</div>",
    iconSize: [24, 24],
    iconAnchor: [12, 12],
  });
}

function recomputeDistance() {
  let meters = 0;
  for (let i = 1; i < waypoints.value.length; i++) {
    const a = waypoints.value[i - 1];
    const b = waypoints.value[i];
    meters += distanceMeters(a.lng, a.lat, b.lng, b.lat);
  }
  totalKm.value = Math.round((meters / 1000) * 100) / 100;
  emit("distance-change", totalKm.value, waypoints.value.length);
}

function syncLine() {
  if (!polyline) return;
  polyline.setLatLngs(waypoints.value.map((wp) => [wp.lat, wp.lng] as L.LatLngExpression));
  recomputeDistance();
}

function redraw() {
  if (!map) return;
  resizeObserver?.disconnect();
  resizeObserver = null;
  markers.forEach((m) => map?.removeLayer(m));
  markers = [];
  if (polyline) {
    map.removeLayer(polyline);
    polyline = null;
  }
  const latlngs: L.LatLngExpression[] = [];
  waypoints.value.forEach((wp, idx) => {
    latlngs.push([wp.lat, wp.lng]);
    const marker = L.marker([wp.lat, wp.lng], { icon: createIcon(idx), draggable: true }).addTo(map!);
    marker.on("drag", () => {
      const ll = marker.getLatLng();
      wp.lat = ll.lat;
      wp.lng = ll.lng;
      syncLine();
    });
    marker.on("dragend", () => emitUpdate());
    markers.push(marker);
  });
  if (latlngs.length >= 2) {
    polyline = L.polyline(latlngs, { color: "#409eff", weight: 3, dashArray: "6 6" }).addTo(map!);
  }
  recomputeDistance();
}

function emitUpdate() {
  if (suppressEmit) return;
  emit("update:modelValue", serialize());
}

function onMapClick(e: L.LeafletMouseEvent) {
  waypoints.value.push({ lng: e.latlng.lng, lat: e.latlng.lat, alt: 100 });
  redraw();
  emitUpdate();
}

function removePoint(idx: number) {
  waypoints.value.splice(idx, 1);
  redraw();
  emitUpdate();
}

function undoLast() {
  if (!waypoints.value.length) return;
  waypoints.value.pop();
  redraw();
  emitUpdate();
}

function clearAll() {
  waypoints.value = [];
  redraw();
  emitUpdate();
}

function loadFromModel(value: string) {
  let loaded = false;
  try {
    const arr = JSON.parse(value);
    if (Array.isArray(arr)) {
      suppressEmit = true;
      waypoints.value = arr.map((p: Record<string, unknown>) => ({
        lng: Number(p.lng) || 0,
        lat: Number(p.lat) || 0,
        alt: Number(p.alt) || 100,
      }));
      redraw();
      loaded = true;
      if (waypoints.value.length && map) {
        // Defer fitting until dialog open animation finishes so Leaflet has the real container size
        pendingFitBounds = true;
      }
    }
  } catch {
    /* 非法JSON忽略 */
  } finally {
    suppressEmit = false;
  }
  return loaded;
}

onMounted(() => {
  if (!mapEl.value) return;
  const source = getMapSource("vec");
  map = L.map(mapEl.value, { center: [26.58, 101.72], zoom: 12, attributionControl: false });
  source.tileUrls.forEach((url) =>
    L.tileLayer(url, { subdomains: source.subdomains, maxZoom: source.maxZoom }).addTo(map!)
  );
  map.on("click", onMapClick);
  loadFromModel(props.modelValue);
  // Dialog opening animation changes container size continuously;
  // keep Leaflet size cache in sync so click coords are correct.
  resizeObserver = new ResizeObserver(() => map?.invalidateSize());
  resizeObserver.observe(mapEl.value);
  // el-dialog open animation (~300ms) scales the container; wait for it to finish
  setTimeout(() => {
    if (!map) return;
    map.invalidateSize();
    if (pendingFitBounds && waypoints.value.length) {
      pendingFitBounds = false;
      map.fitBounds(
        L.latLngBounds(waypoints.value.map((wp) => [wp.lat, wp.lng] as [number, number])).pad(0.35)
      );
    }
  }, 450);
});

watch(
  () => props.modelValue,
  (val) => {
    if (val !== serialize()) loadFromModel(val);
  }
);

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  resizeObserver = null;
  markers.forEach((m) => map?.removeLayer(m));
  markers = [];
  if (polyline && map) map.removeLayer(polyline);
  polyline = null;
  map?.remove();
  map = null;
});
</script>

<style scoped>
.route-map-editor {
  width: 100%;
}
.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.toolbar-info {
  display: flex;
  gap: 8px;
}
.editor-map {
  height: 420px;
  width: 100%;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  cursor: crosshair;
}
.waypoint-list {
  margin-top: 10px;
  max-height: 180px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}
.waypoint-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border-bottom: 1px solid #f2f6fc;
  font-size: 13px;
}
.waypoint-row:last-child {
  border-bottom: none;
}
.wp-no {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.wp-lnglat {
  flex: 1;
  font-family: monospace;
  color: #606266;
}
.wp-alt-unit {
  color: #909399;
  font-size: 12px;
}
.waypoint-empty {
  margin-top: 10px;
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}
</style>

<style>
/* 航点Marker样式（divIcon渲染在scoped之外，需全局） */
.route-wp-icon {
  background: transparent;
  border: none;
}
.route-wp-marker {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #409eff;
  border: 2px solid #fff;
  color: #fff;
  font-size: 12px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.35);
  cursor: grab;
}
.route-wp-marker:active {
  cursor: grabbing;
}
</style>




