<template>
  <div class="dashboard">
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="s in statCards" :key="s.label">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: s.color }">
              <el-icon size="28"><component :is="s.icon" /></el-icon>
            </div>
            <div>
              <div class="stat-value">{{ s.value }}</div>
              <div class="stat-label">{{ s.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <el-card>
          <template #header>无人机状态分布</template>
          <div ref="statusChartRef" style="height: 320px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>部门信息</template>
          <el-table :data="deptList" size="small" stripe>
            <el-table-column prop="deptName" label="部门" />
            <el-table-column prop="leader" label="负责人" width="100" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import * as echarts from "echarts";
import { getDeptList, getDevicePage } from "../../api/system";

const deptList = ref<any[]>([]);
const statusChartRef = ref<HTMLElement>();
const statCards = ref([
  { label: "在线设备", value: 0, icon: "Monitor", color: "#409eff" },
  { label: "飞行中", value: 0, icon: "Position", color: "#67c23a" },
  { label: "充电中", value: 0, icon: "Lightning", color: "#e6a23c" },
  { label: "离线设备", value: 0, icon: "CircleClose", color: "#909399" },
]);

onMounted(async () => {
  const deptRes: any = await getDeptList();
  deptList.value = deptRes.data.filter((d: any) => d.parentId !== 0);

  try {
    const devRes: any = await getDevicePage({ current: 1, size: 1000 });
    const devices = devRes.data.records;
    const counts = [0, 0, 0, 0];
    devices.forEach((d: any) => {
      if (d.status === 1 || d.status === 2) counts[0]++;
      if (d.status === 2) counts[1]++;
      if (d.status === 3) counts[2]++;
      if (d.status === 0 || d.status === 4) counts[3]++;
    });
    statCards.value[0].value = counts[0];
    statCards.value[1].value = counts[1];
    statCards.value[2].value = counts[2];
    statCards.value[3].value = counts[3];

    const chart = echarts.init(statusChartRef.value!);
    chart.setOption({
      tooltip: { trigger: "item" },
      legend: { bottom: 0 },
      series: [{
        type: "pie",
        radius: ["40%", "65%"],
        label: { show: true, formatter: "{b}: {c}" },
        data: [
          { value: counts[0], name: "在线" },
          { value: counts[1], name: "飞行中" },
          { value: counts[2], name: "充电中" },
          { value: counts[3], name: "离线/维护" },
        ],
      }],
    });
    window.addEventListener("resize", () => chart.resize());
  } catch {
    // device API not ready
  }
});
</script>

<style scoped>
.stat-row { margin-bottom: 0; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; }
.stat-label { font-size: 13px; color: #909399; }
.dashboard { min-height: 100%; }
</style>
