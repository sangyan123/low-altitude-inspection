<template>
  <div class="statistics-report">
    <el-card shadow="never">
      <div class="toolbar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 280px"
          @change="loadReport"
        />
        <el-select v-model="granularity" style="width: 130px" @change="loadReport">
          <el-option label="按日" value="day" />
          <el-option label="按周" value="week" />
          <el-option label="按月" value="month" />
        </el-select>
        <el-select v-model="deptId" placeholder="全部部门" clearable filterable style="width: 190px" @change="loadReport">
          <el-option v-for="item in depts" :key="item.id" :label="item.deptName" :value="item.id" />
        </el-select>
        <el-button type="primary" @click="loadReport">刷新报表</el-button>
        <el-dropdown @command="handleExport" style="margin-left: 12px">
          <el-button type="success">导出报表<el-icon style="margin-left: 4px"><ArrowDown /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="excel">Excel (.xlsx)</el-dropdown-item>
              <el-dropdown-item command="pdf">PDF 文档 (.pdf)</el-dropdown-item>
              <el-dropdown-item command="csv" divided>CSV (兼容格式)</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <div class="toolbar-right">
          <el-tag effect="plain">{{ report?.startDate }} 至 {{ report?.endDate }}</el-tag>
          <el-tag effect="plain" type="success">{{ granularityName }}</el-tag>
        </div>
      </div>
    </el-card>

    <el-row :gutter="12" class="metric-row">
      <el-col :xs="12" :sm="8" :md="6" v-for="card in metricCards" :key="card.label">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-value">{{ card.value }}</div>
          <div class="metric-label">{{ card.label }}</div>
          <div class="metric-note">{{ card.note }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <template #header>业务数量趋势</template>
          <div ref="trendChartRef" class="chart large-chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never">
          <template #header>事件状态分布</template>
          <div ref="eventChartRef" class="chart large-chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>任务完成情况</template>
          <div ref="taskChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>成果类型分布</template>
          <div ref="dataChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card">
      <template #header>部门业务统计</template>
      <el-table :data="report?.departments || []" v-loading="loading" border stripe size="small">
        <el-table-column prop="deptName" label="部门" min-width="150" />
        <el-table-column prop="deviceCount" label="设备" width="90" />
        <el-table-column prop="patrolCount" label="巡检" width="90" />
        <el-table-column prop="transportCount" label="运输" width="90" />
        <el-table-column prop="planCount" label="计划" width="90" />
        <el-table-column prop="eventCount" label="事件" width="90" />
        <el-table-column prop="dataCount" label="成果" width="90" />
        <el-table-column prop="totalBusinessCount" label="业务合计" width="110" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import * as echarts from "echarts";
import { ElMessage } from "element-plus";
import { getReport, exportReportExcel, exportReportPdf, type ReportData } from "../../api/report";
import { getDeptList } from "../../api/system";

const loading = ref(false);
const depts = ref<any[]>([]);
const report = ref<ReportData | null>(null);
const granularity = ref("day");
const deptId = ref<number | null>(null);
const dateRange = ref<[string, string] | null>(null);

const trendChartRef = ref<HTMLElement>();
const eventChartRef = ref<HTMLElement>();
const taskChartRef = ref<HTMLElement>();
const dataChartRef = ref<HTMLElement>();

let trendChart: echarts.ECharts | null = null;
let eventChart: echarts.ECharts | null = null;
let taskChart: echarts.ECharts | null = null;
let dataChart: echarts.ECharts | null = null;



const granularityName = computed(() => ({ day: "按日", week: "按周", month: "按月" }[granularity.value]));
const metricCards = computed(() => {
  const overview = report.value?.overview;
  return [
    { label: "设备总数", value: overview?.deviceTotal ?? 0, note: `在线 ${overview?.deviceOnline ?? 0}` },
    { label: "巡检任务", value: overview?.patrolTotal ?? 0, note: `完成率 ${overview?.patrolCompletionRate ?? 0}%` },
    { label: "运输任务", value: overview?.transportTotal ?? 0, note: `签收率 ${overview?.transportSignedRate ?? 0}%` },
    { label: "飞行计划", value: overview?.planTotal ?? 0, note: `通过率 ${overview?.planApprovalRate ?? 0}%` },
    { label: "AI识别", value: overview?.aiRecognitionTotal ?? 0, note: "周期内总数" },
    { label: "事件总数", value: overview?.eventTotal ?? 0, note: `销号率 ${overview?.eventClosureRate ?? 0}%` },
    { label: "应急事件", value: overview?.emergencyTotal ?? 0, note: `进行中 ${overview?.emergencyOngoing ?? 0}` },
    { label: "成果文件", value: overview?.dataResultTotal ?? 0, note: `模型 ${overview?.dataModelTotal ?? 0}` },
  ];
});

function initChart(refValue: HTMLElement | undefined, chart: echarts.ECharts | null) {
  if (!refValue) return null;
  return chart ?? echarts.init(refValue);
}

function updateCharts() {
  if (trendChartRef.value) {
    trendChart = initChart(trendChartRef.value, trendChart);
    const trends = report.value?.trends || [];
    trendChart?.setOption({
      tooltip: { trigger: "axis" },
      legend: { bottom: 0, textStyle: { fontSize: 11 } },
      grid: { left: 42, right: 18, top: 32, bottom: 52 },
      xAxis: { type: "category", data: trends.map((item) => item.timeLabel) },
      yAxis: { type: "value", minInterval: 1 },
      series: [
        { name: "巡检", type: "line", smooth: true, data: trends.map((item) => item.patrolCount) },
        { name: "运输", type: "line", smooth: true, data: trends.map((item) => item.transportCount) },
        { name: "AI识别", type: "line", smooth: true, data: trends.map((item) => item.aiCount) },
        { name: "事件", type: "line", smooth: true, data: trends.map((item) => item.eventCount) },
        { name: "应急", type: "line", smooth: true, data: trends.map((item) => item.emergencyCount) },
        { name: "成果", type: "line", smooth: true, data: trends.map((item) => item.dataCount) },
      ],
    });
  }

  if (eventChartRef.value) {
    eventChart = initChart(eventChartRef.value, eventChart);
    eventChart?.setOption({
      tooltip: { trigger: "item" },
      legend: { bottom: 0, textStyle: { fontSize: 11 } },
      series: [{
        type: "pie",
        radius: ["42%", "66%"],
        center: ["50%", "42%"],
        label: { show: false },
        data: (report.value?.eventStatus || []).map((item) => ({ name: item.name, value: item.value })),
      }],
    });
  }

  if (taskChartRef.value) {
    taskChart = initChart(taskChartRef.value, taskChart);
    taskChart?.setOption({
      tooltip: { trigger: "axis" },
      legend: { bottom: 0, textStyle: { fontSize: 11 } },
      grid: { left: 42, right: 18, top: 32, bottom: 44 },
      xAxis: { type: "category", data: ["巡检", "运输", "计划"] },
      yAxis: { type: "value", minInterval: 1 },
      series: [
        { name: "总数", type: "bar", data: [report.value?.overview.patrolTotal ?? 0, report.value?.overview.transportTotal ?? 0, report.value?.overview.planTotal ?? 0] },
        { name: "完成/签收/通过", type: "bar", data: [report.value?.overview.patrolCompleted ?? 0, report.value?.overview.transportSigned ?? 0, report.value?.overview.planApproved ?? 0] },
      ],
    });
  }

  if (dataChartRef.value) {
    dataChart = initChart(dataChartRef.value, dataChart);
    dataChart?.setOption({
      tooltip: { trigger: "item" },
      legend: { bottom: 0, textStyle: { fontSize: 11 } },
      series: [{
        type: "pie",
        radius: ["42%", "66%"],
        center: ["50%", "42%"],
        label: { show: false },
        data: (report.value?.dataType || []).map((item) => ({ name: item.name, value: item.value })),
      }],
    });
  }
}

function handleResize() {
  trendChart?.resize();
  eventChart?.resize();
  taskChart?.resize();
  dataChart?.resize();
}

async function loadReport() {
  loading.value = true;
  try {
    const res: any = await getReport({
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      granularity: granularity.value,
      deptId: deptId.value,
    });
    report.value = res.data;
    updateCharts();
  } finally {
    loading.value = false;
  }
}

function csvCell(value: unknown) {
  const text = String(value ?? "");
  return text.includes(",") || text.includes('"') || text.includes("\n")
    ? `"${text.replace(/"/g, '""')}"`
    : text;
}

async function handleExport(command: string) {
  if (command === "csv") {
    exportCsv();
    return;
  }
  const params = {
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1],
    granularity: granularity.value,
    deptId: deptId.value || null,
  };
  try {
    const res: any = command === "excel"
      ? await exportReportExcel(params)
      : await exportReportPdf(params);
    const blob = res.data as Blob;
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "统计报表_" + (params.startDate || "") + "_" + (params.endDate || "") + "." + command;
    link.click();
    URL.revokeObjectURL(url);
    ElMessage.success("导出成功");
  } catch {
    ElMessage.error("导出失败，请稍后重试");
  }
}

function exportCsv() {
  const rows: unknown[][] = [
    ["报表周期", report.value?.startDate, report.value?.endDate, report.value?.granularity],
    [],
    ["指标", "数值"],
    ["设备总数", report.value?.overview.deviceTotal],
    ["巡检任务", report.value?.overview.patrolTotal],
    ["运输任务", report.value?.overview.transportTotal],
    ["AI识别", report.value?.overview.aiRecognitionTotal],
    ["事件总数", report.value?.overview.eventTotal],
    ["应急事件", report.value?.overview.emergencyTotal],
    ["成果文件", report.value?.overview.dataResultTotal],
    [],
    ["部门", "设备", "巡检", "运输", "计划", "事件", "成果", "业务合计"],
  ];

  for (const item of report.value?.departments || []) {
    rows.push([item.deptName, item.deviceCount, item.patrolCount, item.transportCount, item.planCount, item.eventCount, item.dataCount, item.totalBusinessCount]);
  }

  const content = rows.map((row) => row.map(csvCell).join(",")).join("\n");
  const blob = new Blob([`\uFEFF${content}`], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `统计报表_${report.value?.startDate}_${report.value?.endDate}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

onMounted(async () => {
  const end = new Date();
  const start = new Date();
  start.setDate(start.getDate() - 29);
  const format = (date: Date) => date.toISOString().slice(0, 10);
  dateRange.value = [format(start), format(end)];

  window.addEventListener("resize", handleResize);
  const deptRes: any = await getDeptList();
  depts.value = deptRes.data || [];
  await loadReport();
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  trendChart?.dispose();
  eventChart?.dispose();
  taskChart?.dispose();
  dataChart?.dispose();
});
</script>

<style scoped>
.statistics-report {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-right {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.metric-row {
  margin-bottom: 0;
}

.metric-card {
  margin-bottom: 12px;
  text-align: center;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  line-height: 34px;
}

.metric-label {
  margin-top: 4px;
  font-size: 13px;
  color: #606266;
}

.metric-note {
  margin-top: 3px;
  font-size: 12px;
  color: #909399;
}

.chart {
  width: 100%;
  height: 300px;
}

.large-chart {
  height: 360px;
}

.chart-row {
  margin-top: 12px;
}

.table-card {
  margin-top: 12px;
}
</style>



