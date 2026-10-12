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
          <el-dropdown trigger="click">
            <span class="notify-info">
              <el-badge :value="0" :hidden="true">
                <el-icon><Bell /></el-icon>
              </el-badge>
              消息通知
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>暂无新消息</el-dropdown-item>
              </el-dropdown-menu>
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
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();

const username = computed(() => {
  try {
    const user = JSON.parse(localStorage.getItem("user") || "{}");
    return user.realName || user.username || "未知用户";
  } catch {
    return "未知用户";
  }
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
  background-color: #1a2a4a;
  overflow-y: auto;
}
.logo {
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
  border-right: none;
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