<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="sceneFilter" placeholder="场景类型" clearable style="width: 150px">
        <el-option v-for="s in sceneOptions" :key="s" :label="s" :value="s" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索航线名称" style="width: 200px" clearable />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openCreate">新增航线</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="routeName" label="航线名称" min-width="160" />
      <el-table-column prop="sceneType" label="场景类型" width="120" />
      <el-table-column label="航点数" width="90">
        <template #default="{ row }">{{ pointCount(row.routeData) }}</template>
      </el-table-column>
      <el-table-column prop="estDistanceKm" label="预计航程(km)" width="120" />
      <el-table-column prop="estDurationMin" label="预计时长(分)" width="115" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该航线？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑航线' : '新增航线'" width="1000px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="航线名称" required>
              <el-input v-model="form.routeName" placeholder="如：沱江河道巡检线" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景类型" required>
              <el-select v-model="form.sceneType" filterable allow-create style="width: 100%">
                <el-option v-for="s in sceneOptions" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="航线绘制" required>
          <RouteMapEditor v-model="routeDataText" @distance-change="onDistanceChange" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="预计航程(km)">
              <el-input-number v-model="form.estDistanceKm" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计时长(分)">
              <el-input-number v-model="form.estDurationMin" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
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
import { getRouteList, createRoute, updateRoute, deleteRoute } from "../../api/task";
import RouteMapEditor from "../../components/RouteMapEditor.vue";

const sceneOptions = ["秸秆禁烧", "河道巡查", "工地巡查", "森林防火", "耕地核查", "渔政巡河", "道路巡查"];

const list = ref<any[]>([]);
const loading = ref(false);
const keyword = ref("");
const sceneFilter = ref("");
const dialogVisible = ref(false);
const isEdit = ref(false);
const routeDataText = ref("");

const form = reactive({
  id: null as number | null,
  routeName: "",
  sceneType: "",
  estDistanceKm: null as number | null,
  estDurationMin: null as number | null,
  status: 1,
});

const lastDistanceKm = ref(0);

function onDistanceChange(km: number) {
  lastDistanceKm.value = km;
}

function pointCount(data: string): number {
  try {
    const arr = JSON.parse(data);
    return Array.isArray(arr) ? arr.length : 0;
  } catch {
    return 0;
  }
}

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getRouteList({ sceneType: sceneFilter.value || undefined, keyword: keyword.value || undefined });
    list.value = res.data;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, { id: null, routeName: "", sceneType: "", estDistanceKm: null, estDurationMin: null, status: 1 });
  routeDataText.value = "";
  dialogVisible.value = true;
}

function openEdit(row: any) {
  isEdit.value = true;
  Object.assign(form, row);
  try {
    routeDataText.value = JSON.stringify(JSON.parse(row.routeData), null, 2);
  } catch {
    routeDataText.value = row.routeData;
  }
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.routeName) { ElMessage.warning("请输入航线名称"); return; }
  if (!form.sceneType) { ElMessage.warning("请选择场景类型"); return; }
  let routeData = "";
  try {
    const parsed = JSON.parse(routeDataText.value);
    if (!Array.isArray(parsed)) throw new Error("not array");
    if (parsed.length < 2) {
      ElMessage.warning("请在地图上至少绘制 2 个航点");
      return;
    }
    routeData = JSON.stringify(parsed);
  } catch {
    ElMessage.warning("请在地图上绘制航线（至少 2 个航点）");
    return;
  }
  const payload = {
    ...form,
    routeData,
    estDistanceKm: form.estDistanceKm ?? lastDistanceKm.value,
    estDurationMin: form.estDurationMin ?? Math.max(1, Math.ceil(lastDistanceKm.value * 1.5)),
  };
  if (isEdit.value) {
    await updateRoute(payload);
  } else {
    await createRoute(payload);
  }
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  loadData();
}

async function handleDelete(id: number) {
  await deleteRoute(id);
  ElMessage.success("删除成功");
  loadData();
}

onMounted(loadData);
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
</style>

