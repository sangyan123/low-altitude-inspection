<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon size="24"><Position /></el-icon>
        <span>一网统飞平台</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#1a2a4a"
        text-color="#a0b4d0"
        active-text-color="#409eff"
        unique-opened
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon><span>首页大屏</span>
        </el-menu-item>
        <el-menu-item index="/flight-situation">
          <el-icon><Location /></el-icon><span>飞行态势</span>
        </el-menu-item>
        <el-sub-menu index="task">
          <template #title>
            <el-icon><Tickets /></el-icon><span>任务管理</span>
              <el-drawer v-model="notificationVisible" title="消息通知" size="520px">
      <div class="drawer-toolbar">
        <span>未读消息：{{ unreadCount }}</span>
        <el-button size="small" :disabled="unreadCount === 0" @click="handleMarkAllRead">全部已读</el-button>
      </div>
      <div
        v-for="item in recentNotifications"
        :key="item.id"
        class="drawer-item"
        :class="{ unread: item.isRead === 0 }"
        @click="handleRead(item.id)"
      >
        <div class="drawer-title">{{ item.title }}</div>
        <div class="drawer-content">{{ item.content || "-" }}</div>
        <div class="drawer-time">{{ item.createTime }}</div>
      </div>
      <el-button style="width: 100%; margin-top: 16px" @click="router.push('/notifications')">进入消息中心</el-button>
    </el-drawer>
</template>
          <el-menu-item index="/task/patrol">
            <el-icon><Compass /></el-icon>巡检任务
          </el-menu-item>
          <el-menu-item index="/task/transport">
            <el-icon><Box /></el-icon>运输任务
          </el-menu-item>
          <el-menu-item index="/task/route">
            <el-icon><Guide /></el-icon>航线库
          </el-menu-item>
          <el-menu-item index="/task/plan">
            <el-icon><Calendar /></el-icon>飞行计划
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="ai">
          <template #title>
            <el-icon><MagicStick /></el-icon><span>AI识别中心</span>
          </template>
          <el-menu-item index="/ai/result">
            <el-icon><Picture /></el-icon>识别结果
          </el-menu-item>
          <el-menu-item index="/ai/algorithm">
            <el-icon><Operation /></el-icon>算法管理
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/event">
          <el-icon><Warning /></el-icon><span>事件管理</span>
        </el-menu-item>
        <el-sub-menu index="data">
          <template #title>
            <el-icon><FolderOpened /></el-icon><span>成果中心</span>
          </template>
          <el-menu-item index="/data/media">
            <el-icon><VideoCamera /></el-icon>影像/视频成果
          </el-menu-item>
          <el-menu-item index="/data/model">
            <el-icon><Grid /></el-icon>三维模型
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/emergency">
          <el-icon><WarningFilled /></el-icon><span>应急调度</span>
        </el-menu-item>
        <el-menu-item index="/report">
          <el-icon><TrendCharts /></el-icon><span>统计报表</span>
        </el-menu-item>
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon><span>系统管理</span>
          </template>
          <el-menu-item index="/system/dept">
            <el-icon><OfficeBuilding /></el-icon>部门管理
          </el-menu-item>
          <el-menu-item index="/system/user">
            <el-icon><User /></el-icon>账户管理
          </el-menu-item>
          <el-menu-item index="/system/role">
            <el-icon><Avatar /></el-icon>角色管理
          </el-menu-item>
          <el-menu-item index="/system/device">
            <el-icon><Promotion /></el-icon>无人机管理
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">{{ route.meta.title }}</div>
        <div class="header-right">
          <el-dropdown trigger="click" @visible-change="(isOpen: boolean) => isOpen && loadNotifications()">
            <span class="notify-info">
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
                <el-icon><Bell /></el-icon>
              </el-badge>
              消息通知
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <div class="notification-dropdown">
                <div class="dropdown-head">
                  <span>消息通知</span>
                  <el-button link type="primary" :disabled="unreadCount === 0" @click="handleMarkAllRead">全部已读</el-button>
                </div>
                <div v-if="recentNotifications.length === 0" class="dropdown-empty">暂无新消息</div>
                <div
                  v-for="item in recentNotifications"
                  :key="item.id"
                  class="dropdown-item"
                  :class="{ unread: item.isRead === 0 }"
                  @click="handleRead(item.id)"
                >
                  <div class="item-title">
                    <span v-if="item.isRead === 0" class="dot"></span>
                    {{ item.title }}
                  </div>
                  <div class="item-time">{{ item.createTime }}</div>
                </div>
                <div class="dropdown-footer">
                  <el-button link type="primary" @click="router.push('/notifications')">查看全部</el-button>
                </div>
              </div>
            </template>
          </el-dropdown>

          <el-dropdown>
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              {{ username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { connectNotificationSocket, disconnectNotificationSocket } from "../utils/websocket";
import { getNotificationPage, getUnreadCount, markAllNotificationsRead, markNotificationRead } from "../api/notification";
import { useNotificationStore } from "../stores/notification";

const route = useRoute();
const router = useRouter();
const notificationStore = useNotificationStore();
const { unreadCount } = storeToRefs(notificationStore);
const recentNotifications = ref<any[]>([]);
const notificationVisible = ref(false);

const username = computed(() => {
  try {
    const user = JSON.parse(localStorage.getItem("user") || "{}");
    return user.realName || user.username || "未知用户";
  } catch {
    return "未知用户";
  }
});

async function loadNotifications() {
  const res: any = await getNotificationPage({ current: 1, size: 8, isRead: null });
  recentNotifications.value = res.data.records || [];
  const countRes: any = await getUnreadCount();
  notificationStore.setUnreadCount(countRes.data);
}

async function handleRead(id: number) {
  const item = recentNotifications.value.find((item: any) => item.id === id);
  if (!item || item.isRead === 1) return;
  await markNotificationRead(id);
  item.isRead = 1;
  notificationStore.markRead(id);
  const res: any = await getUnreadCount();
  notificationStore.setUnreadCount(res.data);
}

async function handleMarkAllRead() {
  await markAllNotificationsRead();
  recentNotifications.value.forEach((item: any) => { item.isRead = 1; });
  notificationStore.markAllRead();
  ElMessage.success("已全部标记为已读");
}

onMounted(() => {
  connectNotificationSocket();
  loadNotifications();
});

onBeforeUnmount(() => {
  disconnectNotificationSocket();
});

function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  router.push("/login");
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.sidebar {
  display: flex;
  flex-direction: column;
  background-color: #1a2a4a;
  overflow: hidden;
}
.logo {
  flex: 0 0 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #409eff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.sidebar-menu {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border-right: none;
  contain: layout paint style;
  will-change: auto;
}

.sidebar-menu :deep(.el-sub-menu__title),
.sidebar-menu :deep(.el-menu-item) {
  transition: none !important;
  animation: none !important;
}

.sidebar-menu :deep(.el-menu--inline) {
  transition: none !important;
  animation: none !important;
  transform: none !important;
}

.sidebar-menu :deep(.el-menu-item .el-icon),
.sidebar-menu :deep(.el-sub-menu__title .el-icon) {
  flex-shrink: 0;
}
.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
}
.header-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notify-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
}

.notification-dropdown {
  width: 320px;
  max-height: 420px;
  overflow-y: auto;
}

.dropdown-head,
.dropdown-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
}

.dropdown-head {
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
  color: #303133;
}

.dropdown-footer {
  border-top: 1px solid #f0f0f0;
  justify-content: center;
}

.dropdown-empty {
  padding: 28px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.dropdown-item {
  padding: 10px 12px;
  border-bottom: 1px solid #f5f7fa;
  cursor: pointer;
}

.dropdown-item:hover {
  background: #f7f9fc;
}

.dropdown-item.unread {
  background: #f8fbff;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #303133;
  font-size: 13px;
}

.item-title .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f56c6c;
}

.item-time {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.drawer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  color: #606266;
}

.drawer-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.drawer-item:hover,
.drawer-item.unread {
  background: #f8fbff;
}

.drawer-title {
  font-weight: 600;
  color: #303133;
}

.drawer-content {
  margin-top: 4px;
  color: #606266;
  font-size: 13px;
  line-height: 20px;
}

.drawer-time {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
}
.main-content {
  background: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}
</style>