<template>
  <div ref="mapContainer" class="gis-map"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { L, createTiandituLayers } from "../utils/tianditu";

const props = withDefaults(
  defineProps<{
    /** 天地图Key */
    tk: string;
    /** 底图类型：vec矢量 / img影像 */
    baseType?: "vec" | "img";
    center?: [number, number];
    zoom?: number;
  }>(),
  { baseType: "vec", center: [101.72, 26.58], zoom: 11 }
);

const emit = defineEmits<{ (e: "ready", map: L.Map): void }>();

const mapContainer = ref<HTMLElement>();
let map: L.Map | null = null;

onMounted(() => {
  if (!mapContainer.value) return;
  const layers = createTiandituLayers(props.tk, props.baseType);
  map = L.map(mapContainer.value, {
    center: props.center,
    zoom: props.zoom,
    zoomControl: true,
    attributionControl: false,
  });
  layers.forEach((layer) => layer.addTo(map!));
  emit("ready", map);
});

// 底图类型切换
watch(
  () => props.baseType,
  (type) => {
    if (!map) return;
    map.eachLayer((layer) => map!.removeLayer(layer));
    createTiandituLayers(props.tk, type).forEach((layer) => layer.addTo(map!));
  }
);

onBeforeUnmount(() => {
  map?.remove();
  map = null;
});

defineExpose({ getMap: () => map });
</script>

<style scoped>
.gis-map {
  width: 100%;
  height: 100%;
  min-height: 400px;
}
</style>
