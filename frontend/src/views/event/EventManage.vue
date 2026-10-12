<template>
  <div class="event-manage">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.eventType" placeholder="事件类型" clearable filterable style="width: 170px" @change="loadData">
          <el-option v-for="item in eventTypes" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.eventSource" placeholder="来源" clearable style="width: 130px" @change="loadData">
          <el-option label="AI识别" :value="1" />
          <el-option label="人工上报" :value="2" />
        </el-select>
        <el-select v-model="query.deptId" placeholder="处置部门" clearable filterable style="width: 170px" @change="loadData">
          <el-option v-for="item in depts" :key="item.id" :label="item.deptName" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="loadData">
          <el-option label="待处置" :value="0" />
          <el-option label="已派单" :value="1" />
          <el-option label="已处置" :value="2" />
          <el-option label="已核验" :value="3" />
          <el-option label="已销号" :value="4" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="loadData"
        />
        <el-input
          v-model="query.keyword"
          placeholder="搜索编号/类型/描述"
          clearable
          style="width: 220px"
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button type="success" @click="openCreate">人工上报</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="eventNo" label="事件编号" width="170" />
        <el-table-column prop="eventType" label="事件类型" min-width="130" />
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="row.eventSource === 1 ? 'primary' : 'info'" size="small">
              {{ sourceName(row.eventSource) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
              fit="cover"
              class="event-image"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="事件描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="deptName" label="处置部门" width="130">
          <template #default="{ row }">{{ row.deptName || "未派单" }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 0" size="small" type="primary" @click="openDispatch(row)">派单</el-button>
              <el-button v-if="row.status === 1" size="small" type="warning" @click="openHandle(row)">处置</el-button>
              <el-button v-if="row.status === 2" size="small" type="success" @click="openVerify(row)">核验</el-button>
              <el-button v-if="row.status === 3" size="small" type="primary" @click="openClose(row)">销号</el-button>
              <el-popconfirm v-if="row.status === 0" title="确定删除该事件？" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @change="loadData"
      />
    </el-card>

    <el-drawer v-model="detailVisible" title="事件详情" size="640px">
      <div v-if="detail" class="detail-container">
        <div class="detail-section">
          <div class="section-title">基础信息</div>
          <div class="info-grid">
            <div class="info-item"><span>事件编号</span><b>{{ detail.eventNo }}</b></div>
            <div class="info-item"><span>事件类型</span><b>{{ detail.eventType }}</b></div>
            <div class="info-item"><span>事件来源</span><b>{{ sourceName(detail.eventSource) }}</b></div>
            <div class="info-item"><span>当前状态</span><b>{{ statusName(detail.status) }}</b></div>
            <div class="info-item"><span>处置部门</span><b>{{ detail.deptName || "未派单" }}</b></div>
            <div class="info-item"><span>上报人</span><b>{{ detail.submitUserName || "-" }}</b></div>
            <div class="info-item"><span>坐标</span><b>{{ detail.lng ?? "-" }}, {{ detail.lat ?? "-" }}</b></div>
            <div class="info-item"><span>创建时间</span><b>{{ detail.createTime || "-" }}</b></div>
          </div>
          <div class="description">{{ detail.description || "-" }}</div>
        </div>

        <div v-if="detail.imageUrl" class="detail-section">
          <div class="section-title">AI识别图片</div>
          <el-image
            :src="detail.imageUrl"
            :preview-src-list="[detail.imageUrl]"
            preview-teleported
            fit="contain"
            class="detail-image"
          />
          <div class="ai-meta">
            <el-tag size="small">{{ detail.resultType || detail.eventType }}</el-tag>
            <el-tag size="small" type="warning">置信度 {{ confidenceText(detail.confidence) }}</el-tag>
            <el-tag size="small" type="info">{{ detail.provider || "-" }} / {{ detail.modelName || "-" }}</el-tag>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">处置时间线</div>
          <el-timeline>
            <el-timeline-item
              v-for="log in detail.handleLogs || []"
              :key="log.id"
              :timestamp="log.createTime"
              placement="top"
            >
              <div class="timeline-title">{{ log.action }}</div>
              <div class="timeline-meta">{{ log.operatorName || "-" }}</div>
              <div v-if="log.remark" class="timeline-remark">{{ log.remark }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div v-if="detail.rawResponse" class="detail-section">
          <div class="section-title">AI原始返回</div>
          <pre>{{ prettyJson(detail.rawResponse) }}</pre>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="createVisible" title="人工上报事件" width="620px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="事件类型" required>
          <el-select v-model="createForm.eventType" filterable allow-create placeholder="选择或输入事件类型" style="width: 100%">
            <el-option v-for="item in eventTypes" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件描述" required>
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input-number v-model="createForm.lng" :min="73" :max="135.35" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度">
              <el-input-number v-model="createForm.lat" :min="3.86" :max="53.55" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="处置部门">
          <el-select v-model="createForm.deptId" clearable filterable placeholder="可先留空，派单时选择" style="width: 100%">
            <el-option v-for="item in depts" :key="item.id" :label="item.deptName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="actionVisible" :title="actionTitle" width="520px">
      <el-form label-width="90px">
        <el-form-item v-if="actionType === 'dispatch'" label="处置部门" required>
          <el-select v-model="actionForm.deptId" filterable placeholder="选择处置部门" style="width: 100%">
            <el-option v-for="item in depts" :key="item.id" :label="item.deptName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="actionType === 'verify'" label="核验结果" required>
          <el-radio-group v-model="actionForm.result">
            <el-radio-button :value="1">通过</el-radio-button>
            <el-radio-button :value="2">不通过</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="actionType === 'handle' ? '处置说明' : '备注'" :required="actionType === 'handle'">
          <el-input v-model="actionForm.remark" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitAction">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  getEventPage,
  getEventDetail,
  createEvent,
  dispatchEvent,
  handleEvent,
  verifyEvent,
  closeEvent,
  deleteEvent,
  loadEventDepts,
  type EventInfo,
  type EventQuery,
} from "../../api/event";

const list = ref<EventInfo[]>([]);
const depts = ref<any[]>([]);
const loading = ref(false);
const total = ref(0);
const detail = ref<EventInfo | null>(null);
const detailVisible = ref(false);
const createVisible = ref(false);
const actionVisible = ref(false);
const actionLoading = ref(false);

const eventTypes = [
  "火点识别",
  "违建识别",
  "非法采砂识别",
  "河道边坡坍塌识别",
  "道路破损识别",
  "船舶违停识别",
  "违规捕鱼识别",
  "毁坏林地识别",
  "裸土扬尘识别",
  "垃圾堆放识别",
  "人群聚集识别",
  "山体滑坡识别",
  "人工巡查事件",
];

const query = reactive<EventQuery>({
  current: 1,
  size: 10,
  eventType: undefined,
  eventSource: null,
  deptId: null,
  status: null,
  keyword: "",
  startDate: undefined,
  endDate: undefined,
});

const dateRange = ref<[string, string] | null>(null);

const emptyCreate = {
  eventType: "",
  description: "",
  lng: null as number | null,
  lat: null as number | null,
  deptId: null as number | null,
  remark: "",
};
const createForm = reactive({ ...emptyCreate });

const actionType = ref<"dispatch" | "handle" | "verify" | "close">("dispatch");
const actionTarget = ref<EventInfo | null>(null);
const actionForm = reactive({
  deptId: null as number | null,
  result: 1,
  remark: "",
});

const actionTitle = computed(() => ({
  dispatch: "事件派单",
  handle: "事件处置",
  verify: "事件核验",
  close: "事件销号",
}[actionType.value]));

const sourceName = (value?: number) => ({ 1: "AI识别", 2: "人工上报" }[Number(value)] || "-");
const statusName = (value: number) =>
  ({ 0: "待处置", 1: "已派单", 2: "已处置", 3: "已核验", 4: "已销号" }[Number(value)] || "-");
const statusTag = (value: number) =>
  ({ 0: "warning", 1: "primary", 2: "success", 3: "success", 4: "info" }[Number(value)] || "info");

const confidenceText = (value: number | string | null | undefined) => {
  const num = Number(value ?? 0);
  return num ? `${(num * 100).toFixed(1)}%` : "-";
};

const prettyJson = (value?: string) => {
  if (!value) return "-";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
};

async function loadData() {
  loading.value = true;
  try {
    const params: EventQuery = {
      ...query,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    };
    const res: any = await getEventPage(params);
    list.value = res.data.records || [];
    total.value = res.data.total;
  } finally {
    loading.value = false;
  }
}

async function openDetail(row: EventInfo) {
  const res: any = await getEventDetail(row.id);
  detail.value = res.data;
  detailVisible.value = true;
}

function openCreate() {
  Object.assign(createForm, emptyCreate);
  createVisible.value = true;
}

async function handleCreate() {
  if (!createForm.eventType) {
    ElMessage.warning("请选择或输入事件类型");
    return;
  }
  if (!createForm.description) {
    ElMessage.warning("请填写事件描述");
    return;
  }
  await createEvent(createForm);
  ElMessage.success("事件上报成功");
  createVisible.value = false;
  query.current = 1;
  loadData();
}

function openDispatch(row: EventInfo) {
  actionTarget.value = row;
  actionType.value = "dispatch";
  actionForm.deptId = row.deptId ?? null;
  actionForm.remark = "";
  actionVisible.value = true;
}

function openHandle(row: EventInfo) {
  actionTarget.value = row;
  actionType.value = "handle";
  actionForm.remark = "";
  actionVisible.value = true;
}

function openVerify(row: EventInfo) {
  actionTarget.value = row;
  actionType.value = "verify";
  actionForm.result = 1;
  actionForm.remark = "";
  actionVisible.value = true;
}

function openClose(row: EventInfo) {
  actionTarget.value = row;
  actionType.value = "close";
  actionForm.remark = "";
  actionVisible.value = true;
}

async function submitAction() {
  if (!actionTarget.value) return;
  actionLoading.value = true;
  try {
    if (actionType.value === "dispatch") {
      if (!actionForm.deptId) {
        ElMessage.warning("请选择处置部门");
        return;
      }
      await dispatchEvent(actionTarget.value.id, {
        deptId: actionForm.deptId,
        remark: actionForm.remark || undefined,
      });
      ElMessage.success("派单成功");
    } else if (actionType.value === "handle") {
      if (!actionForm.remark) {
        ElMessage.warning("请填写处置说明");
        return;
      }
      await handleEvent(actionTarget.value.id, { remark: actionForm.remark });
      ElMessage.success("处置完成");
    } else if (actionType.value === "verify") {
      await verifyEvent(actionTarget.value.id, {
        result: actionForm.result,
        remark: actionForm.remark || undefined,
      });
      ElMessage.success(actionForm.result === 1 ? "核验通过" : "已退回派单");
    } else {
      await closeEvent(actionTarget.value.id, { remark: actionForm.remark || undefined });
      ElMessage.success("事件已销号");
    }
    actionVisible.value = false;
    loadData();
  } finally {
    actionLoading.value = false;
  }
}

async function handleDelete(id: number) {
  await deleteEvent(id);
  ElMessage.success("删除成功");
  loadData();
}

onMounted(async () => {
  const res: any = await loadEventDepts();
  depts.value = res.data || [];
  loadData();
});
</script>

<style scoped>
.event-manage {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.event-image {
  width: 62px;
  height: 42px;
  border-radius: 4px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.row-actions :deep(.el-button) {
  flex: 0 0 auto;
  padding: 5px 8px;
}

.detail-container {
  padding-right: 6px;
}

.detail-section + .detail-section {
  margin-top: 20px;
}

.section-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-item {
  padding: 8px 10px;
  border-radius: 6px;
  background: #f7f9fc;
  font-size: 12px;
}

.info-item span {
  display: block;
  margin-bottom: 4px;
  color: #909399;
}

.info-item b {
  color: #303133;
  font-weight: 500;
}

.description {
  margin-top: 12px;
  padding: 10px;
  border-radius: 6px;
  background: #f7f9fc;
  color: #606266;
  line-height: 20px;
}

.detail-image {
  width: 100%;
  max-height: 320px;
  border-radius: 6px;
}

.ai-meta {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.timeline-title {
  font-weight: 600;
  color: #303133;
}

.timeline-meta {
  margin: 3px 0;
  font-size: 12px;
  color: #909399;
}

.timeline-remark {
  color: #606266;
  line-height: 18px;
}

.detail-section pre {
  max-height: 220px;
  overflow: auto;
  padding: 10px;
  border-radius: 6px;
  background: #111827;
  color: #e5e7eb;
  font-size: 12px;
  white-space: pre-wrap;
}
</style>