<template>
  <div ref="containerRef" class="model-viewer">
    <div v-if="loading" class="viewer-overlay">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <div class="overlay-text">模型加载中…</div>
    </div>
    <div v-else-if="error" class="viewer-overlay error">
      <el-icon :size="32" color="#f56c6c"><CircleCloseFilled /></el-icon>
      <div class="overlay-text">{{ error }}</div>
    </div>
    <div class="viewer-toolbar">
      <el-button-group size="small">
        <el-button :type="wireframe ? 'primary' : ''" @click="toggleWireframe">线框</el-button>
        <el-button @click="resetCamera">复位</el-button>
        <el-button :type="autoRotate ? 'primary' : ''" @click="autoRotate = !autoRotate">自动旋转</el-button>
      </el-button-group>
    </div>
    <div v-if="modelInfo" class="viewer-info">
      {{ modelInfo }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as THREE from "three";
import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader.js";
import { OBJLoader } from "three/examples/jsm/loaders/OBJLoader.js";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";

const props = defineProps<{
  url: string;
  format?: string;
}>();

const containerRef = ref<HTMLDivElement | null>(null);
const loading = ref(true);
const error = ref("");
const wireframe = ref(false);
const autoRotate = ref(true);
const modelInfo = ref("");

let renderer: THREE.WebGLRenderer | null = null;
let scene: THREE.Scene | null = null;
let camera: THREE.PerspectiveCamera | null = null;
let controls: OrbitControls | null = null;
let model: THREE.Object3D | null = null;
let animationId = 0;
let resizeObserver: ResizeObserver | null = null;

function initScene() {
  if (!containerRef.value) return;
  const width = containerRef.value.clientWidth;
  const height = containerRef.value.clientHeight;

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setSize(width, height);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setClearColor(0x1a1a2e, 1);
  containerRef.value.appendChild(renderer.domElement);

  scene = new THREE.Scene();
  scene.add(new THREE.AmbientLight(0xffffff, 0.6));
  const dirLight = new THREE.DirectionalLight(0xffffff, 1.2);
  dirLight.position.set(5, 10, 7);
  scene.add(dirLight);
  const dirLight2 = new THREE.DirectionalLight(0x8899ff, 0.4);
  dirLight2.position.set(-5, -3, -5);
  scene.add(dirLight2);

  // Grid helper
  const gridHelper = new THREE.GridHelper(20, 40, 0x444466, 0x333355);
  gridHelper.position.y = -0.01;
  scene.add(gridHelper);

  camera = new THREE.PerspectiveCamera(50, width / height, 0.01, 10000);
  camera.position.set(5, 5, 5);

  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.autoRotateSpeed = 2.0;

  resizeObserver = new ResizeObserver(() => {
    if (!containerRef.value || !renderer || !camera) return;
    const w = containerRef.value.clientWidth;
    const h = containerRef.value.clientHeight;
    renderer.setSize(w, h);
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
  });
  resizeObserver.observe(containerRef.value);

  animate();
}

function animate() {
  animationId = requestAnimationFrame(animate);
  if (controls) {
    controls.autoRotate = autoRotate.value;
    controls.update();
  }
  if (renderer && scene && camera) {
    renderer.render(scene, camera);
  }
}

function disposeScene() {
  cancelAnimationFrame(animationId);
  resizeObserver?.disconnect();
  controls?.dispose();
  if (model) {
    model.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        child.geometry?.dispose();
        if (Array.isArray(child.material)) {
          child.material.forEach((m) => m.dispose());
        } else {
          child.material?.dispose();
        }
      }
    });
    model = null;
  }
  renderer?.dispose();
  if (renderer?.domElement?.parentElement) {
    renderer.domElement.parentElement.removeChild(renderer.domElement);
  }
  renderer = null;
  scene = null;
  camera = null;
  controls = null;
}

function centerAndFrame(object: THREE.Object3D) {
  if (!camera || !controls) return;
  const box = new THREE.Box3().setFromObject(object);
  const center = box.getCenter(new THREE.Vector3());
  const size = box.getSize(new THREE.Vector3());
  const maxDim = Math.max(size.x, size.y, size.z) || 1;
  const distance = maxDim * 1.6;

  object.position.sub(center);

  camera.position.set(distance * 0.6, distance * 0.6, distance * 0.8);
  camera.far = Math.max(10000, distance * 20);
  camera.near = Math.max(0.001, distance * 0.001);
  camera.updateProjectionMatrix();
  controls.target.set(0, 0, 0);
  controls.update();

  modelInfo.value = `尺寸 ${size.x.toFixed(2)} × ${size.y.toFixed(2)} × ${size.z.toFixed(2)}`;
}

function applyWireframe(object: THREE.Object3D, value: boolean) {
  object.traverse((child) => {
    if (child instanceof THREE.Mesh) {
      if (Array.isArray(child.material)) {
        child.material.forEach((m) => { m.wireframe = value; });
      } else {
        child.material.wireframe = value;
      }
    }
  });
}

function toggleWireframe() {
  wireframe.value = !wireframe.value;
  if (model) applyWireframe(model, wireframe.value);
}

function resetCamera() {
  if (model && camera && controls) {
    // Re-center
    const box = new THREE.Box3().setFromObject(model);
    const center = box.getCenter(new THREE.Vector3());
    const size = box.getSize(new THREE.Vector3());
    const maxDim = Math.max(size.x, size.y, size.z) || 1;
    camera.position.set(maxDim * 0.6, maxDim * 0.6, maxDim * 0.8);
    controls.target.copy(center);
    controls.update();
  }
}

async function loadModel(url: string, format?: string) {
  loading.value = true;
  error.value = "";
  wireframe.value = false;

  const ext = (format || url || "").toLowerCase().split("?")[0];
  const lowerUrl = url.toLowerCase();

  try {
    if (!scene) initScene();
    if (!scene) return;

    // Remove previous model
    if (model) {
      scene.remove(model);
      disposeObject(model);
      model = null;
    }

    if (lowerUrl.endsWith(".glb") || lowerUrl.endsWith(".gltf")) {
      const loader = new GLTFLoader();
      const gltf = await loader.loadAsync(url);
      model = gltf.scene;
      scene.add(model);
      centerAndFrame(model);
    } else if (lowerUrl.endsWith(".obj")) {
      const loader = new OBJLoader();
      const obj = await loader.loadAsync(url);
      // Apply default material to OBJ meshes
      obj.traverse((child) => {
        if (child instanceof THREE.Mesh) {
          child.material = new THREE.MeshPhongMaterial({ color: 0x8899aa, flatShading: false });
        }
      });
      model = obj;
      scene.add(model);
      centerAndFrame(obj);
    } else {
      error.value = `暂不支持在线预览 ${ext || "该格式"}，请下载后使用专业软件查看`;
      loading.value = false;
      return;
    }

    loading.value = false;
  } catch (e: any) {
    console.error("Model load failed", e);
    error.value = e?.message || "模型加载失败，请检查文件格式或网络";
    loading.value = false;
  }
}

function disposeObject(object: THREE.Object3D) {
  object.traverse((child) => {
    if (child instanceof THREE.Mesh) {
      child.geometry?.dispose();
      if (Array.isArray(child.material)) {
        child.material.forEach((m) => m.dispose());
      } else {
        child.material?.dispose();
      }
    }
  });
}

function reinit() {
  disposeScene();
  if (props.url) loadModel(props.url, props.format);
}

onMounted(() => {
  initScene();
  if (props.url) loadModel(props.url, props.format);
});

watch(() => props.url, () => {
  reinit();
});

onBeforeUnmount(() => {
  disposeScene();
});
</script>

<style scoped>
.model-viewer {
  position: relative;
  width: 100%;
  height: 480px;
  border-radius: 8px;
  overflow: hidden;
  background: #1a1a2e;
}
.viewer-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  z-index: 2;
  color: #909399;
  pointer-events: none;
  background: rgba(26, 26, 46, 0.85);
}
.viewer-overlay.error {
  color: #f56c6c;
}
.overlay-text {
  font-size: 14px;
}
.viewer-toolbar {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 3;
}
.viewer-info {
  position: absolute;
  bottom: 10px;
  left: 12px;
  z-index: 3;
  color: #8899aa;
  font-size: 12px;
  pointer-events: none;
}
</style>
