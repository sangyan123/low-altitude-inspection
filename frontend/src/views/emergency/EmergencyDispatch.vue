<template>
  <ModulePlaceholder
    title="应急调度"
    icon="WarningFilled"
    description="面向洪涝、山火、地质灾害、事故救援和搜救任务，提供应急上报、无人机调度、目标定位和通信中继能力。"
    :plans="plans"
  />
</template>

<script setup lang="ts">
import ModulePlaceholder from "../../components/ModulePlaceholder.vue";

const plans = [
  { title: "应急事件", description: "管理事件等级、发生位置、上报人和处置状态。", icon: "Warning" },
  { title: "搜救定位", description: "标记受困人员、火点、险情点并跟踪目标状态。", icon: "Location" },
  { title: "通信中继", description: "规划无人机中继点位，保障现场图像和指挥链路。", icon: "Connection" },
];
</script>