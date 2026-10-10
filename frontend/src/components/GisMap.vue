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
  }>(),
  { baseType: "vec", center: [101.72, 26.58], zoom: 11 }
);

const emit = defineEmits<{ (e: "ready", map: L.Map): void }>();

const mapContainer = ref<HTMLElement>();
let map: L.Map | null = null;
let tileLayers: L.TileLayer[] = [];

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
  emit("ready", map);
});

// 底图类型切换
watch(
  () => props.baseType,
  (type) => loadTiles(type)
);

onBeforeUnmount(() => {
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
