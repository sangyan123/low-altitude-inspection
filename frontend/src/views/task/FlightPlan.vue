<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.deptId" placeholder="申报部门" clearable style="width: 150px" @change="loadData">
        <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
      </el-select>
      <el-select v-model="query.approvalStatus" placeholder="审批状态" clearable style="width: 130px" @change="loadData">
        <el-option label="待审批" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openCreate">新建计划</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="planNo" label="计划编号" width="180" />
      <el-table-column label="申报部门" width="130">
        <template #default="{ row }">{{ deptMap[row.deptId] || '-' }}</template>
      </el-table-column>
      <el-table-column label="关联任务" width="100">
        <template #default="{ row }">{{ row.taskType === 1 ? '巡检' : '运输' }}#{{ row.taskId || '-' }}</template>
      </el-table-column>
      <el-table-column prop="planStartTime" label="计划开始" width="170" />
      <el-table-column prop="planEndTime" label="计划结束" width="170" />
      <el-table-column label="空域校验" width="100">
        <template #default="{ row }">
          <el-tag :type="airTag(row.airspaceCheck)" size="small">{{ airName(row.airspaceCheck) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="approvalTag(row.approvalStatus)" size="small">{{ approvalName(row.approvalStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="approvalRemark" label="审批意见" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" v-if="row.approvalStatus === 0" type="success" @click="openApprove(row)">审批</el-button>
          <el-button size="small" v-if="row.approvalStatus === 0" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm v-if="row.approvalStatus === 0" title="确定删除该计划？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
          <span v-if="row.approvalStatus !== 0" style="color: #c0c4cc">已审批</span>
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

    <!-- 新建/编辑 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑飞行计划' : '新建飞行计划'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="申报部门" required>
          <el-select v-model="form.deptId" style="width: 100%">
            <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联设备">
          <el-select v-model="form.deviceId" clearable style="width: 100%">
            <el-option v-for="d in deviceOptions" :key="d.id" :label="d.deviceName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划时间" required>
          <el-date-picker
            v-model="planRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- 审批 -->
    <el-dialog v-model="approveVisible" title="飞行计划审批" width="460px">
      <el-form label-width="80px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveRemark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApprove">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getDeptList, getDevicePage } from "../../api/system";
import { getPlanPage, createPlan, updatePlan, deletePlan, approvePlan } from "../../api/task";

const list = ref<any[]>([]);
const deptList = ref<any[]>([]);
const deviceOptions = ref<any[]>([]);
const total = ref(0);
const loading = ref(false);
const dialogVisible = ref(false);
const approveVisible = ref(false);
const isEdit = ref(false);
const planRange = ref<[string, string] | null>(null);
const approvePlanId = ref<number | null>(null);
const approveStatus = ref(1);
const approveRemark = ref("");

const query = reactive({
  current: 1,
  size: 10,
  deptId: null as number | null,
  approvalStatus: null as number | null,
});

const form = reactive({
  id: null as number | null,
  deptId: null as number | null,
  deviceId: null as number | null,
});

const deptMap = computed(() => {
  const m: Record<number, string> = {};
  deptList.value.forEach((d) => (m[d.id] = d.deptName));
  return m;
});

const airName = (s: number) => ({ 0: "未校验", 1: "通过", 2: "涉禁飞区" }[s] || "-");
const airTag = (s: number) => ({ 0: "info", 1: "success", 2: "danger" }[s] || "info");
const approvalName = (s: number) => ({ 0: "待审批", 1: "已通过", 2: "已驳回" }[s] || "-");
const approvalTag = (s: number) => ({ 0: "warning", 1: "success", 2: "danger" }[s] || "info");

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getPlanPage(query);
    list.value = res.data.records;
    total.value = res.data.total;
  } finally {
    loading.value = false;
  }
}

async function loadRefs() {
  const [deptRes, devRes]: any[] = await Promise.all([getDeptList(), getDevicePage({ current: 1, size: 100 })]);
  deptList.value = deptRes.data;
  deviceOptions.value = devRes.data.records;
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, { id: null, deptId: null, deviceId: null });
  planRange.value = null;
  dialogVisible.value = true;
}

function openEdit(row: any) {
  isEdit.value = true;
  Object.assign(form, row);
  planRange.value = row.planStartTime && row.planEndTime ? [row.planStartTime, row.planEndTime] : null;
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.deptId) { ElMessage.warning("请选择申报部门"); return; }
  if (!planRange.value) { ElMessage.warning("请选择计划起止时间"); return; }
  const payload: any = { ...form };
  payload.planStartTime = planRange.value[0];
  payload.planEndTime = planRange.value[1];
  if (isEdit.value) {
    await updatePlan(payload);
  } else {
    payload.userId = null;
    await createPlan(payload);
  }
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  loadData();
}

function openApprove(row: any) {
  approvePlanId.value = row.id;
  approveStatus.value = 1;
  approveRemark.value = "";
  approveVisible.value = true;
}

async function handleApprove() {
  await approvePlan(approvePlanId.value!, approveStatus.value, approveRemark.value || undefined);
  ElMessage.success(approveStatus.value === 1 ? "已通过" : "已驳回");
  approveVisible.value = false;
  loadData();
}

async function handleDelete(id: number) {
  await deletePlan(id);
  ElMessage.success("删除成功");
  loadData();
}

onMounted(() => {
  loadData();
  loadRefs();
});
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
</style>
