<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.deptId" placeholder="所属部门" clearable style="width: 150px" @change="loadData">
        <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
      </el-select>
      <el-select v-model="query.deviceType" placeholder="设备类型" clearable style="width: 140px" @change="loadData">
        <el-option label="固定机巢" :value="1" />
        <el-option label="车载机巢" :value="2" />
        <el-option label="单兵手持" :value="3" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="loadData">
        <el-option label="离线" :value="0" />
        <el-option label="在线" :value="1" />
        <el-option label="飞行中" :value="2" />
        <el-option label="充电中" :value="3" />
        <el-option label="维护中" :value="4" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="搜索名称/序列号/型号" style="width: 220px" clearable @clear="loadData" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openCreate">新增设备</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="deviceSn" label="序列号" width="150" />
      <el-table-column prop="deviceName" label="设备名称" min-width="130" />
      <el-table-column prop="model" label="型号" width="110" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ typeName(row.deviceType) }}</template>
      </el-table-column>
      <el-table-column label="所属部门" width="120">
        <template #default="{ row }">{{ deptMap[row.deptId] || '未分配' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="电量" width="130">
        <template #default="{ row }">
          <el-progress :percentage="Number(row.battery || 0)" :color="batteryColor" :stroke-width="14" text-inside />
        </template>
      </el-table-column>
      <el-table-column label="通信链路" width="100">
        <template #default="{ row }">
          <el-tag :type="commTag(row.commMode)" size="small">{{ commName(row.commMode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalFlightCount" label="累计架次" width="90" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该设备？" @confirm="handleDelete(row.id)">
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑设备' : '新增设备'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="序列号" required>
              <el-input v-model="form.deviceSn" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" required>
              <el-input v-model="form.deviceName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="型号">
              <el-input v-model="form.model" placeholder="如 大疆M30T" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型">
              <el-select v-model="form.deviceType" style="width: 100%">
                <el-option label="固定机巢" :value="1" />
                <el-option label="车载机巢" :value="2" />
                <el-option label="单兵手持" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-select v-model="form.deptId" clearable style="width: 100%">
                <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通信链路">
              <el-select v-model="form.commMode" style="width: 100%">
                <el-option label="4G" :value="1" />
                <el-option label="5G" :value="2" />
                <el-option label="自组网" :value="3" />
                <el-option label="中继模式" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最大续航">
              <el-input-number v-model="form.maxFlightTime" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大载重">
              <el-input-number v-model="form.maxPayloadKg" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getDeptList, getDevicePage, createDevice, updateDevice, deleteDevice } from "../../api/system";

const list = ref<any[]>([]);
const deptList = ref<any[]>([]);
const total = ref(0);
const loading = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);

const query = reactive({
  current: 1, size: 10,
  deptId: null as number | null,
  deviceType: null as number | null,
  status: null as number | null,
  keyword: "",
});

const emptyForm = {
  id: null as number | null, deviceSn: "", deviceName: "", model: "",
  deviceType: 1, deptId: null as number | null, commMode: 1,
  maxFlightTime: 30, maxPayloadKg: 3, remark: "",
};
const form = reactive({ ...emptyForm });

const deptMap = computed(() => {
  const m: Record<number, string> = {};
  deptList.value.forEach((d) => (m[d.id] = d.deptName));
  return m;
});

const typeName = (t: number) => ({ 1: "固定机巢", 2: "车载机巢", 3: "单兵手持" }[t] || "-");
const statusName = (s: number) => ({ 0: "离线", 1: "在线", 2: "飞行中", 3: "充电中", 4: "维护中" }[s] || "-");
const statusTag = (s: number) => ({ 0: "info", 1: "success", 2: "primary", 3: "warning", 4: "danger" }[s] || "info");
const commName = (c: number) => ({ 0: "离线", 1: "4G", 2: "5G", 3: "自组网", 4: "中继" }[c] || "-");
const commTag = (c: number) => ({ 0: "info", 1: "success", 2: "success", 3: "warning", 4: "warning" }[c] || "info");

const batteryColor = (percentage: number) => {
  if (percentage > 60) return "#67c23a";
  if (percentage > 20) return "#e6a23c";
  return "#f56c6c";
};

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getDevicePage(query);
    list.value = res.data.records;
    total.value = res.data.total;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, emptyForm);
  dialogVisible.value = true;
}

function openEdit(row: any) {
  isEdit.value = true;
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.deviceSn || !form.deviceName) {
    ElMessage.warning("请填写序列号和设备名称");
    return;
  }
  if (isEdit.value) {
    await updateDevice(form);
  } else {
    await createDevice(form);
  }
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  loadData();
}

async function handleDelete(id: number) {
  await deleteDevice(id);
  ElMessage.success("删除成功");
  loadData();
}

onMounted(() => {
  loadDepts();
  loadData();
});

async function loadDepts() {
  const res: any = await getDeptList();
  deptList.value = res.data;
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
</style>
