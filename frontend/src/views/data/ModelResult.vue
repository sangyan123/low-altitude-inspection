<template>
  <ModulePlaceholder
    title="三维模型"
    icon="Box"
    description="管理倾斜摄影、实景三维、DSM/DOM 及重点区域三维模型成果，为应急和水利场景提供空间底座。"
    :plans="plans"
  />
</template>

<script setup lang="ts">
import ModulePlaceholder from "../../components/ModulePlaceholder.vue";

const plans = [
  { title: "模型目录", description: "管理3D Tiles、OBJ等模型格式、版本和覆盖范围。", icon: "FolderOpened" },
  { title: "三维预览", description: "后续可评估接入Cesium/Three.js进行空间可视化。", icon: "DataLine" },
  { title: "空间分析", description: "支持模型范围、高程、淹没分析等水利应急场景。", icon: "Aim" },
];
</script>