<template>
  <el-container class="layout-container">
    <!-- 移动端顶部导航 -->
    <el-header class="mobile-header" v-if="isMobile">
      <div class="mobile-header-content">
        <el-icon class="menu-icon" @click="drawerVisible = true" v-if="isMobile">
          <Menu />
        </el-icon>
        <h2>CDC 同步平台</h2>
        <el-tag type="success" size="small">v3.0</el-tag>
      </div>
    </el-header>

    <!-- 桌面端侧边栏 -->
    <el-aside :width="isMobile ? '0' : '220px'" class="sidebar" v-show="!isMobile">
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

    <!-- 移动端抽屉菜单 -->
    <el-drawer v-model="drawerVisible" :with-header="false" size="220px" v-if="isMobile">
      <div class="drawer-menu">
        <div class="drawer-logo">
          <h2>CDC 同步平台</h2>
        </div>
        <el-menu
          :default-active="activeMenu"
          router
          @select="drawerVisible = false"
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
      </div>
    </el-drawer>

    <el-container>
      <!-- 桌面端顶部 -->
      <el-header class="header" v-show="!isMobile">
        <div class="header-left">
          <h3>CDC 数据同步运维管理平台</h3>
        </div>
        <div class="header-right">
          <el-tag type="success" size="small">v3.0</el-tag>
          <el-divider direction="vertical" />
          <span>运行任务：{{ stats.runningCount || 0 }}</span>
        </div>
      </el-header>

      <el-main :class="['main-content', { 'mobile-main': isMobile }]">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Menu } from '@element-plus/icons-vue'
import api from '@/api'

const route = useRoute()
const stats = ref({ runningCount: 0 })
const drawerVisible = ref(false)
const isMobile = ref(window.innerWidth < 768)

const activeMenu = computed(() => route.path)

const menuRoutes = computed(() => {
  return route.$router?.options?.routes[0]?.children || []
})

// 监听窗口大小变化
const handleResize = () => {
  isMobile.value = window.innerWidth < 768
}

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
  window.addEventListener('resize', handleResize)
  fetchStats()
  setInterval(fetchStats, 10000)
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* 移动端顶部导航 */
.mobile-header {
  background-color: #1e293b;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 50px;
  padding: 0;
}

.mobile-header-content {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  color: #fff;
}

.mobile-header-content h2 {
  margin: 0;
  font-size: 16px;
  font-weight: bold;
}

.menu-icon {
  font-size: 20px;
  cursor: pointer;
}

/* 侧边栏 */
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

/* 抽屉菜单 */
.drawer-menu {
  background-color: #1e293b;
  height: 100%;
}

.drawer-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #0f172a;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}

.drawer-logo h2 {
  margin: 0;
}

::v-deep(.el-drawer) {
  background-color: #1e293b;
}

::v-deep(.el-drawer__body) {
  padding: 0;
}

/* 桌面端头部 */
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

/* 主内容区 */
.main-content {
  background-color: #f3f4f6;
  padding: 20px;
  overflow-x: auto;
}

.mobile-main {
  padding: 10px;
}

/* 响应式表格优化 */
::v-deep(.el-table) {
  font-size: 13px;
}

::v-deep(.el-table th) {
  padding: 8px 0;
}

::v-deep(.el-table td) {
  padding: 8px 0;
}

/* 移动端按钮优化 */
@media screen and (max-width: 768px) {
  ::v-deep(.el-button) {
    padding: 8px 12px;
    font-size: 13px;
  }
  
  ::v-deep(.el-button--small) {
    padding: 6px 10px;
    font-size: 12px;
  }
  
  /* 表格在小屏幕上可横向滚动 */
  ::v-deep(.el-table) {
    width: 100%;
    overflow-x: auto;
  }
  
  /* 卡片间距调整 */
  .el-card {
    margin-bottom: 10px;
  }
  
  /* 表单标签调整 */
  ::v-deep(.el-form-item__label) {
    font-size: 13px;
  }
  
  /* 对话框调整 */
  ::v-deep(.el-dialog) {
    width: 90% !important;
    margin: 20px auto !important;
  }
}
</style>
