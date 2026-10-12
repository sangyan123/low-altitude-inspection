<template>
  <el-card>
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索算法名称/分类/描述"
        clearable
        style="width: 240px"
        @clear="loadData"
        @keyup.enter="loadData"
      />
      <el-select v-model="status" placeholder="状态" clearable style="width: 120px" @change="loadData">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openCreate">新增算法</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="algoName" label="算法名称" min-width="150" />
      <el-table-column prop="algoCategory" label="分类" width="120">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.algoCategory }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="algoDesc" label="描述" min-width="240" show-overflow-tooltip />
      <el-table-column prop="modelVersion" label="模型版本" width="120">
        <template #default="{ row }">{{ row.modelVersion || "-" }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? "启用" : "停用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该算法？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑算法' : '新增算法'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="算法名称" required>
          <el-input v-model="form.algoName" placeholder="如 火点识别" />
        </el-form-item>
        <el-form-item label="算法分类" required>
          <el-select v-model="form.algoCategory" placeholder="选择或输入分类" allow-create filterable style="width: 100%">
            <el-option label="生态环保" value="生态环保" />
            <el-option label="城管" value="城管" />
            <el-option label="水利应急" value="水利应急" />
            <el-option label="交通治安" value="交通治安" />
            <el-option label="农业" value="农业" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型版本">
          <el-input v-model="form.modelVersion" placeholder="如 v1.0 / YOLOv8n" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="算法描述">
          <el-input v-model="form.algoDesc" type="textarea" :rows="3" />
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
import {
  getAlgorithmList,
  createAlgorithm,
  updateAlgorithm,
  deleteAlgorithm,
  type AiAlgorithm,
} from "../../api/ai";

const list = ref<AiAlgorithm[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);
const keyword = ref("");
const status = ref<number | null>(null);

const emptyForm = {
  id: null as number | null,
  algoName: "",
  algoCategory: "",
  algoDesc: "",
  modelVersion: "",
  status: 1,
};
const form = reactive({ ...emptyForm });

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getAlgorithmList({
      keyword: keyword.value || undefined,
      status: status.value ?? undefined,
    });
    list.value = res.data || [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, emptyForm);
  dialogVisible.value = true;
}

function openEdit(row: AiAlgorithm) {
  isEdit.value = true;
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.algoName) {
    ElMessage.warning("请填写算法名称");
    return;
  }
  if (!form.algoCategory) {
    ElMessage.warning("请选择算法分类");
    return;
  }
  if (isEdit.value) {
    await updateAlgorithm(form);
  } else {
    await createAlgorithm(form);
  }
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  loadData();
}

async function handleDelete(id: number) {
  await deleteAlgorithm(id);
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
  flex-wrap: wrap;
}
</style>