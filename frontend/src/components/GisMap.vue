<template>
  <div ref="mapContainer" class="gis-map"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import L from "leaflet";
import { getMapSource } from "../utils/mapSource";
import { toMapCoord } from "../utils/coord";

const props = withDefaults(
  defineProps<{
    /** 底图类型：vec矢量 / img影像 */
    baseType?: "vec" | "img";
    /** 初始中心点，业务坐标（WGS-84）：[经度, 纬度] */
    center?: [number, number];
    zoom?: number;
    /** 初始化后自动定位到当前设备位置 */
    autoLocate?: boolean;
  }>(),
  { baseType: "vec", center: () => [101.72, 26.58] as [number, number], zoom: 11 }
);

const emit = defineEmits<{ (e: "ready", map: L.Map): void }>();

const mapContainer = ref<HTMLElement>();
let map: L.Map | null = null;
let tileLayers: L.TileLayer[] = [];
let resizeObserver: ResizeObserver | null = null;

function loadTiles(baseType: "vec" | "img") {
  if (!map) return;
  tileLayers.forEach((layer) => map!.removeLayer(layer));
  tileLayers = [];
  const source = getMapSource(baseType);
  tileLayers = source.tileUrls.map(
    (url) =>
      L.tileLayer(url, {
        subdomains: source.subdomains,
        maxZoom: source.maxZoom,
      }).addTo(map!)
  );
}

onMounted(() => {
  if (!mapContainer.value) return;
  // Leaflet center 格式为 [纬度, 经度]
  const [lng, lat] = toMapCoord(props.center[0], props.center[1], getMapSource(props.baseType).coordSystem);
  map = L.map(mapContainer.value, {
    center: [lat, lng],
    zoom: props.zoom,
    zoomControl: true,
    attributionControl: false,
  });
  loadTiles(props.baseType);

  // Tab 切换或容器尺寸变化时，Leaflet 必须重新计算尺寸，否则底图只会渲染初始尺寸
  resizeObserver = new ResizeObserver(() => {
    map?.invalidateSize();
  });
  resizeObserver.observe(mapContainer.value);
  window.setTimeout(() => map?.invalidateSize(), 120);
  if (props.autoLocate && navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        if (!map) return; // 组件已卸载，忽略异步回调
        // 浏览器返回WGS-84坐标
        const [lng, lat] = toMapCoord(pos.coords.longitude, pos.coords.latitude, getMapSource(props.baseType).coordSystem);
        map.setView([lat, lng], 13);
        L.circle([lat, lng], { radius: pos.coords.accuracy || 500, color: "#409eff", weight: 1, fillOpacity: 0.08 }).addTo(map);
      },
      () => {/* 用户拒绝或定位失败，保持默认中心 */ },
      { enableHighAccuracy: true, timeout: 8000 }
    );
  }
  emit("ready", map);
});

// 底图类型切换
watch(
  () => props.baseType,
  (type) => loadTiles(type)
);

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  resizeObserver = null;
  map?.remove();
  map = null;
  tileLayers = [];
});

defineExpose({ getMap: () => map });
</script>

<style scoped>
.gis-map {
  position: absolute;
  inset: 0;
}
</style>

