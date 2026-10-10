<template>
  <el-card>
    <div class="toolbar">
      <el-button type="success" @click="openCreate">新增角色</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="roleCode" label="角色编码" width="150" />
      <el-table-column prop="remark" label="描述" min-width="200" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getRoleList } from "../../api/system";

const list = ref<any[]>([]);
const loading = ref(false);

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getRoleList();
    list.value = res.data;
  } finally {
    loading.value = false;
  }
}

function openCreate() { ElMessage.info("新增角色功能待实现"); }
function openEdit(_row: any) { ElMessage.info("编辑角色功能待实现"); }

onMounted(loadData);
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
</style>
