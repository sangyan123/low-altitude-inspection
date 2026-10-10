<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.taskType" placeholder="任务类型" clearable style="width: 130px" @change="loadData">
        <el-option label="常态巡检" :value="1" />
        <el-option label="临时任务" :value="2" />
        <el-option label="应急任务" :value="3" />
      </el-select>
      <el-select v-model="query.sceneType" placeholder="场景类型" clearable filterable allow-create style="width: 140px" @change="loadData">
        <el-option v-for="s in sceneOptions" :key="s" :label="s" :value="s" />
      </el-select>
      <el-select v-model="query.deptId" placeholder="提报部门" clearable style="width: 150px" @change="loadData">
        <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="loadData">
        <el-option label="待派单" :value="0" />
        <el-option label="执行中" :value="1" />
        <el-option label="已完成" :value="2" />
        <el-option label="已取消" :value="3" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="搜索任务名称/编号" style="width: 200px" clearable @clear="loadData" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openCreate">新建任务</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="taskNo" label="任务编号" width="180" />
      <el-table-column prop="taskName" label="任务名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.taskType)" size="small">{{ typeName(row.taskType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sceneType" label="场景" width="110" />
      <el-table-column label="提报部门" width="130">
        <template #default="{ row }">{{ deptMap[row.deptId] || '-' }}</template>
      </el-table-column>
      <el-table-column label="优先级" width="80">
        <template #default="{ row }">
          <el-tag :type="prioTag(row.priority)" size="small">{{ prioName(row.priority) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="planStartTime" label="计划开始" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" :disabled="row.status !== 0" @click="openDispatch(row)">派单</el-button>
          <el-popconfirm title="确定删除该任务？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑任务' : '新建巡检任务'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="任务名称" required>
              <el-input v-model="form.taskName" placeholder="如：河道日常巡查" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务类型">
              <el-select v-model="form.taskType" style="width: 100%">
                <el-option label="常态巡检" :value="1" />
                <el-option label="临时任务" :value="2" />
                <el-option label="应急任务" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="场景类型" required>
              <el-select v-model="form.sceneType" filterable allow-create default-first-option style="width: 100%" placeholder="选择或输入场景">
                <el-option v-for="s in sceneOptions" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提报部门" required>
              <el-select v-model="form.deptId" style="width: 100%">
                <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="航线模板">
              <el-select v-model="form.routeId" clearable style="width: 100%" placeholder="可选">
                <el-option v-for="r in routeList" :key="r.id" :label="r.routeName" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option label="紧急" :value="1" />
                <el-option label="普通" :value="2" />
                <el-option label="低" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="计划时间" required>
          <el-date-picker
            v-model="planRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="目标区域">
          <el-input v-model="areaText" placeholder="地图框选功能待接入高德地图，可暂填区域描述" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- 派单 -->
    <el-dialog v-model="dispatchVisible" title="任务派单" width="460px">
      <el-form label-width="80px">
        <el-form-item label="任务">
          <span>{{ dispatchTask?.taskName }}</span>
        </el-form-item>
        <el-form-item label="无人机" required>
          <el-select v-model="dispatchDeviceId" placeholder="选择可用设备" style="width: 100%">
            <el-option v-for="d in deviceOptions" :key="d.id" :label="`${d.deviceName}（${d.model || '未知型号'}，电量${d.battery ?? '-'}%）`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="飞手" required>
          <el-select v-model="dispatchUserId" placeholder="选择飞手" style="width: 100%">
            <el-option v-for="u in pilotOptions" :key="u.id" :label="u.realName || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDispatch">确认派单</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getDeptList } from "../../api/system";
import { getDevicePage } from "../../api/system";
import { getPatrolPage, createPatrol, updatePatrol, deletePatrol, dispatchPatrol } from "../../api/task";
import { getRouteList } from "../../api/task";

const list = ref<any[]>([]);
const deptList = ref<any[]>([]);
const routeList = ref<any[]>([]);
const deviceOptions = ref<any[]>([]);
const pilotOptions = ref<any[]>([]);
const total = ref(0);
const loading = ref(false);
const dialogVisible = ref(false);
const dispatchVisible = ref(false);
const isEdit = ref(false);
const dispatchTask = ref<any>(null);
const dispatchDeviceId = ref<number | null>(null);
const dispatchUserId = ref<number | null>(null);
const planRange = ref<[string, string] | null>(null);
const areaText = ref("");

const sceneOptions = ["森林防火", "河道巡查", "道路巡查", "航道巡查", "林地巡查", "工地扬尘", "渔政巡河", "耕地核查", "治安巡防"];

const query = reactive({
  current: 1,
  size: 10,
  taskType: null as number | null,
  sceneType: "",
  deptId: null as number | null,
  status: null as number | null,
  keyword: "",
});

const emptyForm = {
  id: null as number | null,
  taskName: "",
  taskType: 1,
  sceneType: "",
  deptId: null as number | null,
  routeId: null as number | null,
  priority: 2,
  remark: "",
};
const form = reactive({ ...emptyForm });

const deptMap = computed(() => {
  const m: Record<number, string> = {};
  deptList.value.forEach((d) => (m[d.id] = d.deptName));
  return m;
});

const typeName = (t: number) => ({ 1: "常态巡检", 2: "临时任务", 3: "应急任务" }[t] || "-");
const typeTag = (t: number) => ({ 1: "primary", 2: "warning", 3: "danger" }[t] || "info");
const prioName = (p: number) => ({ 1: "紧急", 2: "普通", 3: "低" }[p] || "-");
const prioTag = (p: number) => ({ 1: "danger", 2: "info", 3: "success" }[p] || "info");
const statusName = (s: number) => ({ 0: "待派单", 1: "执行中", 2: "已完成", 3: "已取消" }[s] || "-");
const statusTag = (s: number) => ({ 0: "warning", 1: "primary", 2: "success", 3: "info" }[s] || "info");

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getPatrolPage(query);
    list.value = res.data.records;
    total.value = res.data.total;
  } finally {
    loading.value = false;
  }
}

async function loadRefs() {
  const [deptRes, routeRes]: any[] = await Promise.all([getDeptList(), getRouteList()]);
  deptList.value = deptRes.data;
  routeList.value = routeRes.data;
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, emptyForm);
  planRange.value = null;
  areaText.value = "";
  dialogVisible.value = true;
}

function openEdit(row: any) {
  isEdit.value = true;
  Object.assign(form, row);
  planRange.value = row.planStartTime && row.planEndTime ? [row.planStartTime, row.planEndTime] : null;
  areaText.value = "";
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.taskName) { ElMessage.warning("请输入任务名称"); return; }
  if (!form.sceneType) { ElMessage.warning("请选择场景类型"); return; }
  if (!form.deptId) { ElMessage.warning("请选择提报部门"); return; }
  const payload: any = { ...form };
  if (planRange.value) {
    payload.planStartTime = planRange.value[0];
    payload.planEndTime = planRange.value[1];
  }
  if (isEdit.value) {
    await updatePatrol(payload);
  } else {
    payload.submitUserId = Number(localStorage.getItem("userId") || 0) || null;
    await createPatrol(payload);
  }
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  loadData();
}

async function openDispatch(row: any) {
  dispatchTask.value = row;
  dispatchDeviceId.value = null;
  dispatchUserId.value = null;
  // 在线设备
  const devRes: any = await getDevicePage({ current: 1, size: 100, status: 1 });
  deviceOptions.value = devRes.data.records;
  // 飞手（复用用户列表接口，前端过滤角色为飞手的；简化：取全部用户）
  pilotOptions.value = [];
  // 从部门树接口拿用户不现实，这里直接调用户分页接口
  const { getUserPage } = await import("../../api/system");
  const userRes: any = await getUserPage({ current: 1, size: 100 });
  pilotOptions.value = userRes.data.records;
  dispatchVisible.value = true;
}

async function handleDispatch() {
  if (!dispatchDeviceId.value) { ElMessage.warning("请选择无人机"); return; }
  if (!dispatchUserId.value) { ElMessage.warning("请选择飞手"); return; }
  await dispatchPatrol(dispatchTask.value.id, dispatchDeviceId.value, dispatchUserId.value);
  ElMessage.success("派单成功");
  dispatchVisible.value = false;
  loadData();
}

async function handleDelete(id: number) {
  await deletePatrol(id);
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
  flex-wrap: wrap;
}
</style>
