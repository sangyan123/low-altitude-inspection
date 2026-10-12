<template>
  <ModulePlaceholder
    title="影像/视频成果"
    icon="VideoCamera"
    description="统一管理巡查照片、视频、全景影像和任务成果文件，支持按部门、任务、时间筛选。"
    :plans="plans"
  />
</template>

<script setup lang="ts">
import ModulePlaceholder from "../../components/ModulePlaceholder.vue";

const plans = [
  { title: "成果入库", description: "对接任务成果上传、文件元数据和缩略图管理。", icon: "UploadFilled" },
  { title: "媒体预览", description: "提供图片大图、视频播放和任务轨迹关联查看。", icon: "View" },
  { title: "权限控制", description: "按部门、任务归属和授权范围控制成果访问。", icon: "Lock" },
];
</script>