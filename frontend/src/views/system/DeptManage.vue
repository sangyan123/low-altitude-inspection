<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索部门名称" style="width: 240px" clearable @clear="loadData" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openCreate">新增部门</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="deptName" label="部门名称" min-width="150" />
      <el-table-column prop="deptCode" label="部门编码" width="130" />
      <el-table-column prop="leader" label="负责人" width="100" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该部门？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="部门名称" required>
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门编码">
          <el-input v-model="form.deptCode" placeholder="请输入部门编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
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
import { getDeptList, createDept, updateDept, deleteDept } from "../../api/system";

const list = ref<any[]>([]);
const loading = ref(false);
const keyword = ref("");
const dialogVisible = ref(false);
const isEdit = ref(false);

const form = reactive({
  id: null as number | null,
  parentId: 1,
  deptName: "",
  deptCode: "",
  leader: "",
  phone: "",
  sortOrder: 0,
  status: 1,
});

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getDeptList(keyword.value || undefined);
    list.value = res.data;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, { id: null, parentId: 1, deptName: "", deptCode: "", leader: "", phone: "", sortOrder: 0, status: 1 });
  dialogVisible.value = true;
}

function openEdit(row: any) {
  isEdit.value = true;
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.deptName) {
    ElMessage.warning("请输入部门名称");
    return;
  }
  if (isEdit.value) {
    await updateDept(form);
  } else {
    await createDept(form);
  }
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  loadData();
}

async function handleDelete(id: number) {
  await deleteDept(id);
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
