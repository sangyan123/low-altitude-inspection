<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索角色名称/编码/描述" style="width: 240px" clearable @clear="loadData" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openCreate">新增角色</el-button>
    </div>
    <el-table :data="filteredList" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="roleCode" label="角色编码" width="150" />
      <el-table-column prop="remark" label="描述" min-width="200" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该角色？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="filteredList.length === 0 && !loading" style="text-align: center; color: #909399; padding: 20px">
      没有匹配的角色
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称" required>
          <el-input v-model="form.roleName" placeholder="如：部门管理员" />
        </el-form-item>
        <el-form-item label="角色编码" required>
          <el-input v-model="form.roleCode" placeholder="如：dept_admin" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getRoleList, createRole, updateRole, deleteRole } from "../../api/system";

const list = ref<any[]>([]);
const loading = ref(false);
const keyword = ref("");
const dialogVisible = ref(false);
const isEdit = ref(false);

const form = reactive({
  id: null as number | null,
  roleName: "",
  roleCode: "",
  remark: "",
  status: 1,
});

/** 前端按关键词过滤（角色数量少，无需后端分页） */
const filteredList = computed(() => {
  const kw = keyword.value.trim();
  if (!kw) return list.value;
  return list.value.filter(
    (r) =>
      r.roleName.includes(kw) ||
      (r.roleCode || "").includes(kw) ||
      (r.remark || "").includes(kw)
  );
});

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getRoleList();
    list.value = res.data;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, { id: null, roleName: "", roleCode: "", remark: "", status: 1 });
  dialogVisible.value = true;
}

function openEdit(row: any) {
  isEdit.value = true;
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.roleName || !form.roleCode) {
    ElMessage.warning("请填写角色名称和编码");
    return;
  }
  if (isEdit.value) {
    await updateRole(form);
  } else {
    await createRole(form);
  }
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  loadData();
}

async function handleDelete(id: number) {
  await deleteRole(id);
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
