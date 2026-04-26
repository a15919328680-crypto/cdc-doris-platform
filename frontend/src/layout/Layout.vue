<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <h2>CDC 同步平台</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1e293b"
        text-color="#cbd5e1"
        active-text-color="#3b82f6"
      >
        <el-menu-item
          v-for="route in menuRoutes"
          :key="route.path"
          :index="route.path"
        >
          <el-icon><component :is="route.meta.icon" /></el-icon>
          <span>{{ route.meta.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h3>CDC 数据同步运维管理平台</h3>
        </div>
        <div class="header-right">
          <el-tag type="success" size="small">v1.0.0</el-tag>
          <el-divider direction="vertical" />
          <span>服务器状态：</span>
          <el-tag type="success" size="small">正常</el-tag>
          <el-divider direction="vertical" />
          <span>运行任务数：{{ stats.runningCount || 0 }}</span>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'

const route = useRoute()
const stats = ref({ runningCount: 0 })

const activeMenu = computed(() => route.path)

const menuRoutes = computed(() => {
  return route.$router?.options?.routes[0]?.children || []
})

const fetchStats = async () => {
  try {
    const res = await api.task.getOverview()
    if (res.success) {
      stats.value = res
    }
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

onMounted(() => {
  fetchStats()
  setInterval(fetchStats, 10000)
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #1e293b;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #0f172a;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}

.el-menu {
  border-right: none;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left h3 {
  margin: 0;
  font-size: 18px;
  color: #1f2937;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #6b7280;
}

.main-content {
  background-color: #f3f4f6;
  padding: 20px;
}
</style>
