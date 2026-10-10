<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.deptId" placeholder="选择部门" clearable style="width: 180px" @change="loadData">
        <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="搜索用户名/姓名/手机号" style="width: 240px" clearable @clear="loadData" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openCreate">新增用户</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column label="所属部门" width="140">
        <template #default="{ row }">{{ deptMap[row.deptId] || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="openResetPwd(row)">重置密码</el-button>
          <el-button size="small" type="primary" plain @click="openAssignRole(row)">分配角色</el-button>
          <el-popconfirm title="确定删除该用户？" @confirm="handleDelete(row.id)">
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

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="所属部门" required>
          <el-select v-model="form.deptId" placeholder="选择部门" style="width: 100%">
            <el-option v-for="d in deptList" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" v-if="!isEdit" required>
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="400px">
      <el-input v-model="newPassword" placeholder="请输入新密码" type="password" show-password />
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getDeptList, getUserPage, createUser, updateUser, deleteUser, resetPassword, getRoleList, getUserRoles, assignRoles } from "../../api/system";

const list = ref<any[]>([]);
const deptList = ref<any[]>([]);
const total = ref(0);
const loading = ref(false);
const dialogVisible = ref(false);
const resetPwdVisible = ref(false);
const isEdit = ref(false);
const resetPwdUserId = ref<number | null>(null);
const newPassword = ref("");
const assignRoleVisible = ref(false);
const assignRoleUserId = ref<number | null>(null);
const roleList = ref<any[]>([]);
const selectedRoleIds = ref<number[]>([]);

const query = reactive({ current: 1, size: 10, deptId: null as number | null, keyword: "" });

const form = reactive({
  id: null as number | null,
  deptId: null as number | null,
  username: "",
  password: "",
  realName: "",
  phone: "",
  status: 1,
});

const deptMap = computed(() => {
  const m: Record<number, string> = {};
  deptList.value.forEach((d) => (m[d.id] = d.deptName));
  return m;
});

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getUserPage(query);
    list.value = res.data.records;
    total.value = res.data.total;
  } finally {
    loading.value = false;
  }
}

async function loadDepts() {
  const res: any = await getDeptList();
  deptList.value = res.data;
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, { id: null, deptId: null, username: "", password: "", realName: "", phone: "", status: 1 });
  dialogVisible.value = true;
}

function openEdit(row: any) {
  isEdit.value = true;
  Object.assign(form, { ...row, password: "" });
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.deptId) { ElMessage.warning("请选择部门"); return; }
  if (!form.username) { ElMessage.warning("请输入用户名"); return; }
  if (isEdit.value) {
    await updateUser(form);
  } else {
    if (!form.password) { ElMessage.warning("请输入密码"); return; }
    await createUser(form);
  }
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  loadData();
}

function openResetPwd(row: any) {
  resetPwdUserId.value = row.id;
  newPassword.value = "";
  resetPwdVisible.value = true;
}

async function handleResetPwd() {
  if (!newPassword.value) { ElMessage.warning("请输入新密码"); return; }
  await resetPassword(resetPwdUserId.value!, newPassword.value);
  ElMessage.success("密码重置成功");
  resetPwdVisible.value = false;
}

async function loadRoles() {
  const res: any = await getRoleList();
  roleList.value = res.data;
}

function openAssignRole(row: any) {
  assignRoleUserId.value = row.id;
  loadRoles().then(async () => {
    const res: any = await getUserRoles(row.id);
    selectedRoleIds.value = res.data;
  });
  assignRoleVisible.value = true;
}

async function handleAssignRole() {
  await assignRoles(assignRoleUserId.value!, selectedRoleIds.value);
  ElMessage.success("角色分配成功");
  assignRoleVisible.value = false;
}

async function handleDelete(id: number) {
  await deleteUser(id);
  ElMessage.success("删除成功");
  loadData();
}

onMounted(() => {
  loadDepts();
  loadData();
});
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
</style>

