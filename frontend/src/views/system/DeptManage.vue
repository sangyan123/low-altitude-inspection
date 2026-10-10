<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索部门/用户名称" style="width: 240px" clearable @clear="loadData" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openCreate(null)">新增部门</el-button>
      <el-button @click="toggleExpand">{{ expandAll ? '全部折叠' : '全部展开' }}</el-button>
    </div>

    <el-table
      :data="treeData"
      v-loading="loading"
      row-key="id"
      :default-expand-all="expandAll"
      :tree-props="{ children: 'children' }"
      border
      :key="tableKey"
    >
      <el-table-column prop="name" label="名称" min-width="300">
        <template #default="{ row }">
          <el-tag v-if="row.isUser" size="small" type="info" style="margin-right: 6px">用户</el-tag>
          <span :style="row.isUser ? 'color: #909399' : ''">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" align="center">
        <template #default="{ row }">{{ row.isUser ? '-' : row.sortOrder }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
      <el-table-column label="操作" width="220" align="center">
        <template #default="{ row }">
          <template v-if="!row.isUser">
            <el-button link type="primary" size="small" @click="openEdit(row)">
              <el-icon><EditPen /></el-icon>&nbsp;修改
            </el-button>
            <el-button link type="success" size="small" @click="openCreate(row.id)">
              <el-icon><Plus /></el-icon>&nbsp;新增
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>&nbsp;删除
            </el-button>
          </template>
          <span v-else style="color: #c0c4cc">—</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            check-strictly
            :render-after-expand="false"
            style="width: 100%"
            placeholder="选择上级部门"
          />
        </el-form-item>
        <el-form-item label="部门名称" required>
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门编码">
          <el-input v-model="form.deptCode" placeholder="选填" :disabled="isEdit" />
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
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getDeptList, createDept, updateDept, deleteDept, getUserPage } from "../../api/system";

interface DeptNode {
  id: number;
  parentId: number;
  deptName: string;
  deptCode: string;
  leader: string;
  phone: string;
  sortOrder: number;
  status: number;
  createTime: string;
  children?: DeptNode[];
}

interface TreeRow {
  id: string | number;
  isUser: boolean;
  name: string;
  sortOrder?: number;
  createTime?: string;
  dept?: DeptNode;
  children?: TreeRow[];
}

const flatList = ref<DeptNode[]>([]);
const allUsers = ref<any[]>([]);
const loading = ref(false);
const keyword = ref("");
const expandAll = ref(true);
const tableKey = ref(0);
const dialogVisible = ref(false);
const isEdit = ref(false);

const form = reactive({
  id: null as number | null,
  parentId: 0 as number | null,
  deptName: "",
  deptCode: "",
  leader: "",
  phone: "",
  sortOrder: 0,
  status: 1,
});

/** 构建树形结构：部门 + 部门下的用户（叶子节点） */
const treeData = computed<TreeRow[]>(() => {
  const kw = keyword.value.trim();
  const depts = kw
    ? flatList.value.filter((d) => d.deptName.includes(kw) || (d.deptCode || "").includes(kw))
    : flatList.value;
  const users = kw
    ? allUsers.value.filter((u) => (u.realName || "").includes(kw) || (u.username || "").includes(kw))
    : allUsers.value;

  const nodeMap = new Map<number, TreeRow>();
  depts.forEach((d) =>
    nodeMap.set(d.id, {
      id: d.id,
      isUser: false,
      name: d.deptName,
      sortOrder: d.sortOrder,
      createTime: d.createTime,
      dept: d,
      children: [],
    })
  );
  const roots: TreeRow[] = [];
  nodeMap.forEach((node) => {
    const parent = node.dept!.parentId ? nodeMap.get(node.dept!.parentId) : undefined;
    if (parent) {
      parent.children!.push(node);
    } else {
      roots.push(node);
    }
  });
  // 把用户挂到所属部门下（姓名显示，不可操作）
  const userTreeRows: TreeRow[] = users.map((u) => ({
    id: `user-${u.id}`,
    isUser: true,
    name: u.realName || u.username,
    createTime: u.createTime,
  }));
  userTreeRows.forEach((uRow) => {
    const user = users.find((u) => `user-${u.id}` === uRow.id)!;
    const parent = nodeMap.get(user.deptId);
    if (parent) {
      parent.children!.push(uRow);
    } else {
      roots.push(uRow); // 部门被过滤掉时用户仍可见
    }
  });
  // 排序：部门在前按 sortOrder，用户排在部门后
  const sortNodes = (nodes: TreeRow[]) => {
    nodes.sort((a, b) => {
      if (a.isUser !== b.isUser) return a.isUser ? 1 : -1;
      return (a.sortOrder ?? 0) - (b.sortOrder ?? 0);
    });
    nodes.forEach((n) => n.children && sortNodes(n.children));
  };
  sortNodes(roots);
  // 空children不显示展开箭头
  const clean = (nodes: TreeRow[]) => {
    nodes.forEach((n) => {
      if (n.children && n.children.length === 0) delete n.children;
      else if (n.children) clean(n.children);
    });
  };
  clean(roots);
  return roots;
});

/** 上级部门下拉选项（纯部门树，排除自己及子孙；顶级部门下用户不显示） */
const parentOptions = computed(() => {
  const excludeIds = new Set<number>();
  if (isEdit.value && form.id) {
    excludeIds.add(form.id);
    const collectChildren = (id: number) => {
      flatList.value.forEach((d) => {
        if (d.parentId === id) {
          excludeIds.add(d.id);
          collectChildren(d.id);
        }
      });
    };
    collectChildren(form.id);
  }
  const build = (nodes: DeptNode[]): any[] =>
    nodes
      .filter((n) => !excludeIds.has(n.id))
      .map((n) => ({
        value: n.id,
        label: n.deptName,
        children: flatList.value.some((c) => c.parentId === n.id)
          ? build(flatList.value.filter((c) => c.parentId === n.id))
          : undefined,
      }));
  return build(treeData.value.filter((n): n is TreeRow & { dept: DeptNode } => !n.isUser).map((n) => n.dept!));
});

const dialogTitle = computed(() => (isEdit.value ? "修改部门" : "新增部门"));

async function loadData() {
  loading.value = true;
  try {
    const [deptRes, userRes]: any[] = await Promise.all([getDeptList(), getUserPage({ current: 1, size: 1000 })]);
    flatList.value = deptRes.data;
    allUsers.value = userRes.data.records;
    tableKey.value++; // 强制重渲染，确保新增子节点后展开箭头正确显示
  } finally {
    loading.value = false;
  }
}

function openCreate(parentId: number | null) {
  isEdit.value = false;
  Object.assign(form, {
    id: null,
    parentId: parentId ?? 0,
    deptName: "",
    deptCode: "",
    leader: "",
    phone: "",
    sortOrder: 0,
    status: 1,
  });
  dialogVisible.value = true;
}

function openEdit(row: TreeRow) {
  if (row.isUser || !row.dept) return;
  isEdit.value = true;
  Object.assign(form, { ...row.dept });
  dialogVisible.value = true;
}

function toggleExpand() {
  expandAll.value = !expandAll.value;
  tableKey.value++;
}

async function handleSave() {
  if (!form.deptName) {
    ElMessage.warning("请输入部门名称");
    return;
  }
  if (isEdit.value) {
    await updateDept(form as any);
  } else {
    await createDept(form as any);
  }
  ElMessage.success("保存成功");
  dialogVisible.value = false;
  loadData();
}

async function handleDelete(row: TreeRow) {
  if (row.isUser || !row.dept) return;
  const hasChildren = flatList.value.some((d) => d.parentId === row.dept!.id);
  if (hasChildren) {
    ElMessage.warning("该部门下存在子部门，无法删除");
    return;
  }
  const hasUsers = allUsers.value.some((u) => u.deptId === row.dept!.id);
  if (hasUsers) {
    ElMessage.warning("该部门下存在用户，无法删除");
    return;
  }
  try {
    await ElMessageBox.confirm(`确定删除部门「${row.name}」？`, "提示", { type: "warning" });
  } catch {
    return;
  }
  await deleteDept(row.dept.id);
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
