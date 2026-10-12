<template>
  <div class="placeholder-page">
    <el-card shadow="never" class="placeholder-card">
      <div class="placeholder-body">
        <div class="placeholder-icon">
          <el-icon size="34"><component :is="icon" /></el-icon>
        </div>
        <div class="placeholder-info">
          <div class="placeholder-status">模块规划中</div>
          <h2 class="placeholder-title">{{ title }}</h2>
          <p class="placeholder-description">{{ description }}</p>
          <div class="placeholder-actions">
            <el-button type="primary" @click="$router.push('/dashboard')">返回首页大屏</el-button>
            <el-button @click="$router.push('/flight-situation')">查看飞行态势</el-button>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="planning-grid">
        <div v-for="item in plans" :key="item.title" class="planning-item">
          <div class="planning-title">
            <el-icon><component :is="item.icon" /></el-icon>
            {{ item.title }}
          </div>
          <div class="planning-description">{{ item.description }}</div>
          <el-tag size="small" type="info" effect="plain">待接入</el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string;
    description: string;
    icon?: string;
    plans?: Array<{ title: string; description: string; icon: string }>;
  }>(),
  {
    icon: "Box",
    plans: () => [],
  }
);
</script>

<style scoped>
.placeholder-page {
  min-height: 100%;
}

.placeholder-card {
  border-radius: 8px;
}

.placeholder-body {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px 8px;
}

.placeholder-icon {
  width: 76px;
  height: 76px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}

.placeholder-status {
  display: inline-flex;
  padding: 3px 8px;
  margin-bottom: 10px;
  border-radius: 999px;
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.12);
  font-size: 12px;
}

.placeholder-title {
  margin: 0 0 8px;
  color: #303133;
  font-size: 24px;
}

.placeholder-description {
  margin: 0;
  max-width: 680px;
  color: #606266;
  line-height: 22px;
}

.placeholder-actions {
  margin-top: 20px;
}

.planning-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}

.planning-item {
  min-height: 118px;
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #f9fafc;
}

.planning-title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-weight: 600;
  color: #303133;
}

.planning-description {
  min-height: 40px;
  margin: 8px 0 10px;
  color: #909399;
  font-size: 13px;
  line-height: 20px;
}

@media (max-width: 720px) {
  .placeholder-body {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>