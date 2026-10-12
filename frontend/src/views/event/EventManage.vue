<template>
  <ModulePlaceholder
    title="事件管理"
    icon="Warning"
    description="面向无人机巡查发现的问题构建“识别 → 事件 → 派单 → 处置 → 核验 → 销号”闭环管理能力。"
    :plans="plans"
  />
</template>

<script setup lang="ts">
import ModulePlaceholder from "../../components/ModulePlaceholder.vue";

const plans = [
  { title: "事件台账", description: "统一展示AI识别、人工上报产生的事件及处置状态。", icon: "Tickets" },
  { title: "处置流转", description: "支持派单、处置反馈、核验通过/驳回和销号。", icon: "Guide" },
  { title: "AI联动", description: "识别结果一键推送为事件，保留图片、坐标和原始结果。", icon: "Picture" },
];
</script>