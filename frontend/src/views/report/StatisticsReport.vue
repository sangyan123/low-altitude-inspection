<template>
  <ModulePlaceholder
    title="统计报表"
    icon="TrendCharts"
    description="围绕设备、任务、飞行时长、事件处置和部门使用情况进行多维统计和报表导出。"
    :plans="plans"
  />
</template>

<script setup lang="ts">
import ModulePlaceholder from "../../components/ModulePlaceholder.vue";

const plans = [
  { title: "运行统计", description: "统计设备在线率、任务完成率、飞行架次和时长。", icon: "DataAnalysis" },
  { title: "部门报表", description: "按部门汇总巡查、运输、事件和成果数据。", icon: "OfficeBuilding" },
  { title: "导出中心", description: "支持日报、周报、月报导出及自定义时间范围。", icon: "Download" },
];
</script>