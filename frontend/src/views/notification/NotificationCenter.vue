<template>
  <div class="notification-center">
    <el-card shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="query.isRead" @change="handleFilterChange">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="0">未读</el-radio-button>
          <el-radio-button :value="1">已读</el-radio-button>
        </el-radio-group>
        <el-button :disabled="store.unreadCount === 0" @click="handleMarkAllRead">全部已读</el-button>
        <div class="connection-state">
          <span class="state-dot" :class="store.connected ? 'online' : 'offline'"></span>
          {{ store.connected ? "实时连接已开启" : "实时连接未连接" }}
        </div>
      </div>

      <div v-loading="loading">
        <div
          v-for="item in list"
          :key="item.id"
          class="notification-item"
          :class="{ unread: item.isRead === 0 }"
          @click="handleRead(item)"
        >
          <div class="notification-icon">
            <el-icon size="20"><component :is="typeIcon(item.notifyType)" /></el-icon>
          </div>
          <div class="notification-body">
            <div class="notification-title">
              <span class="title-text">{{ item.title }}</span>
              <el-tag size="small" :type="typeTag(item.notifyType)" effect="plain">
                {{ typeName(item.notifyType) }}
              </el-tag>
              <span v-if="item.isRead === 0" class="unread-dot"></span>
            </div>
            <div class="notification-content">{{ item.content || "-" }}</div>
            <div class="notification-time">{{ item.createTime }}</div>
          </div>
        </div>

        <el-empty v-if="!loading && list.length === 0" description="暂无通知" />
      </div>

      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 18px; justify-content: flex-end"
        @change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  getNotificationPage,
  markNotificationRead,
  markAllNotificationsRead,
  getUnreadCount,
  type SysNotification,
} from "../../api/notification";
import { useNotificationStore } from "../../stores/notification";

const store = useNotificationStore();
const loading = ref(false);
const list = ref<SysNotification[]>([]);
const total = ref(0);
const query = reactive({
  current: 1,
  size: 20,
  isRead: null as number | null,
});

const typeName = (value: number) =>
  ({ 1: "任务", 2: "事件", 3: "告警", 4: "审批", 5: "运输", 6: "应急" }[value] || "通知");
const typeTag = (value: number) =>
  ({ 1: "primary", 2: "warning", 3: "danger", 4: "success", 5: "info", 6: "danger" }[value] || "info");
const typeIcon = (value: number) =>
  ({ 1: "Tickets", 2: "Warning", 3: "AlarmClock", 4: "DocumentChecked", 5: "Box", 6: "WarningFilled" }[value] || "Bell");

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getNotificationPage(query);
    list.value = res.data.records || [];
    total.value = res.data.total;
    store.setRecent(list.value.slice(0, 10));
  } finally {
    loading.value = false;
  }
}

async function loadUnreadCount() {
  const res: any = await getUnreadCount();
  store.setUnreadCount(res.data);
}

function handleFilterChange() {
  query.current = 1;
  loadData();
}

async function handleRead(item: SysNotification) {
  if (item.isRead === 1) return;
  await markNotificationRead(item.id);
  item.isRead = 1;
  store.markRead(item.id);
  const res: any = await getUnreadCount();
  store.setUnreadCount(res.data);
}

async function handleMarkAllRead() {
  await markAllNotificationsRead();
  ElMessage.success("已全部标记为已读");
  list.value.forEach((item) => {
    item.isRead = 1;
  });
  store.markAllRead();
}

onMounted(() => {
  loadData();
  loadUnreadCount();
});
</script>

<style scoped>
.notification-center {
  min-height: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.connection-state {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 13px;
}

.state-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.state-dot.online {
  background: #67c23a;
  box-shadow: 0 0 6px #67c23a;
}

.state-dot.offline {
  background: #909399;
}

.notification-item {
  display: flex;
  gap: 14px;
  padding: 14px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background: #f7f9fc;
}

.notification-item.unread {
  background: #f8fbff;
}

.notification-item.unread .title-text {
  font-weight: 700;
}

.notification-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  color: #303133;
}

.unread-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f56c6c;
}

.notification-content {
  margin-top: 4px;
  color: #606266;
  font-size: 13px;
  line-height: 20px;
}

.notification-time {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}
</style>