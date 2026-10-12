<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.transportType" placeholder="运输类型" clearable style="width: 140px" @change="loadData">
        <el-option label="血液运输" :value="1" />
        <el-option label="疾控样本" :value="2" />
        <el-option label="救援物资" :value="3" />
      </el-select>
      <el-select v-model="query.deptId" placeholder="发起部门" clearable style="width: 150px" @change="loadData">
        <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="loadData">
        <el-option label="待起飞" :value="0" />
        <el-option label="运输中" :value="1" />
        <el-option label="已到达" :value="2" />
        <el-option label="已签收" :value="3" />
        <el-option label="已取消" :value="4" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="搜索编号/物资/接收人" style="width: 200px" clearable @clear="loadData" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openCreate">新建运输</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="taskNo" label="任务编号" width="180" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.transportType)" size="small">{{ typeName(row.transportType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="originName" label="起运点" min-width="120" show-overflow-tooltip />
      <el-table-column prop="destName" label="目的地" min-width="120" show-overflow-tooltip />
      <el-table-column prop="cargoName" label="物资" width="110" show-overflow-tooltip />
      <el-table-column prop="cargoWeightKg" label="重量(kg)" width="90" />
      <el-table-column prop="receiverName" label="接收人" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" v-if="row.status === 0" type="primary" @click="handleDepart(row)">起飞</el-button>
          <el-button size="small" v-if="row.status === 1" type="warning" @click="handleArrive(row)">到达</el-button>
          <el-button size="small" v-if="row.status === 2" type="success" @click="handleSign(row)">签收</el-button>
          <el-button size="small" v-if="row.status <= 0" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm v-if="row.status <= 0" title="确定删除该任务？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
          <span v-if="row.status >= 3" style="color: #c0c4cc">已完成</span>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑运输任务' : '新建运输任务'" width="620px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="运输类型" required>
              <el-select v-model="form.transportType" style="width: 100%">
                <el-option label="血液运输" :value="1" />
                <el-option label="疾控样本" :value="2" />
                <el-option label="救援物资" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发起部门" required>
              <el-select v-model="form.deptId" style="width: 100%">
                <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="起运经度" required>
              <el-input-number v-model="form.originLng" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="起运纬度" required>
              <el-input-number v-model="form.originLat" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="起运点名">
              <el-input v-model="form.originName" placeholder="如：市中心血站" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="目的经度" required>
              <el-input-number v-model="form.destLng" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目的纬度" required>
              <el-input-number v-model="form.destLat" :precision="6" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目的地名">
              <el-input v-model="form.destName" placeholder="如：市人民医院" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="物资名称" required>
              <el-input v-model="form.cargoName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="重量(kg)">
              <el-input-number v-model="form.cargoWeightKg" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="接收人">
              <el-input v-model="form.receiverName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="接收电话">
              <el-input v-model="form.receiverPhone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="分配设备">
          <el-select v-model="form.deviceId" clearable style="width: 100%" placeholder="选择载重满足的设备">
            <el-option v-for="d in deviceOptions" :key="d.id" :label="`${d.deviceName}（载重${d.maxPayloadKg ?? '-'}kg）`" :value="d.id" />
          </el-select>
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
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getDeptList, getDevicePage } from "../../api/system";
import {
  getTransportPage, createTransport, updateTransport, deleteTransport,
  departTransport, arriveTransport, signTransport,
} from "../../api/task";

const list = ref<any[]>([]);
const deptList = ref<any[]>([]);
const deviceOptions = ref<any[]>([]);
const total = ref(0);
const loading = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);

const query = reactive({
  current: 1,
  size: 10,
  transportType: null as number | null,
  deptId: null as number | null,
  status: null as number | null,
  keyword: "",
});

const emptyForm = {
  id: null as number | null,
  transportType: 1,
  deptId: null as number | null,
  originLng: null as number | null,
  originLat: null as number | null,
  originName: "",
  destLng: null as number | null,
  destLat: null as number | null,
  destName: "",
  cargoName: "",
  cargoWeightKg: null as number | null,
  receiverName: "",
  receiverPhone: "",
  deviceId: null as number | null,
  remark: "",
};
const form = reactive({ ...emptyForm });


const typeName = (t: number) => ({ 1: "血液运输", 2: "疾控样本", 3: "救援物资" }[t] || "-");
const typeTag = (t: number) => ({ 1: "danger", 2: "warning", 3: "primary" }[t] || "info");
const statusName = (s: number) => ({ 0: "待起飞", 1: "运输中", 2: "已到达", 3: "已签收", 4: "已取消" }[s] || "-");
const statusTag = (s: number) => ({ 0: "warning", 1: "primary", 2: "success", 3: "success", 4: "info" }[s] || "info");

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getTransportPage(query);
    list.value = res.data.records;
    total.value = res.data.total;
  } finally {
    loading.value = false;
  }
}

async function loadRefs() {
  const [deptRes, devRes]: any[] = await Promise.all([
    getDeptList(),
    getDevicePage({ current: 1, size: 100 }),
  ]);
  deptList.value = deptRes.data;
  deviceOptions.value = devRes.data.records;
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
  if (!form.transportType) { ElMessage.warning("请选择运输类型"); return; }
  if (!form.deptId) { ElMessage.warning("请选择发起部门"); return; }
  if (form.originLng == null || form.originLat == null || form.destLng == null || form.destLat == null) {
    ElMessage.warning("请填写起运点和目的地坐标（地图选点待接入高德地图）");
    return;
  }
  if (!form.cargoName) { ElMessage.warning("请填写物资名称"); return; }
  if (isEdit.value) {
    await updateTransport(form);
  } else {
    await createTransport(form);
  }
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  loadData();
}

async function handleDepart(row: any) {
  await departTransport(row.id);
  ElMessage.success("已起飞");
  loadData();
}

async function handleArrive(row: any) {
  await arriveTransport(row.id);
  ElMessage.success("已到达");
  loadData();
}

async function handleSign(row: any) {
  await signTransport(row.id);
  ElMessage.success("签收成功");
  loadData();
}

async function handleDelete(id: number) {
  await deleteTransport(id);
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
