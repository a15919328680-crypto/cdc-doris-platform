<template>
  <div class="layout">
    <!-- 移动端顶部导航 -->
    <header class="mobile-header" v-if="isMobile">
      <el-button text size="large" @click="drawer = true" class="menu-btn">
        <el-icon :size="24"><Menu /></el-icon>
      </el-button>
      <span class="title">Flink CDC 平台</span>
    </header>

    <!-- 桌面端侧边栏 -->
    <aside class="sidebar" v-else>
      <div class="logo">
        <el-icon :size="28"><Cpu /></el-icon>
        <span>Flink CDC</span>
      </div>
      <nav class="nav-menu">
        <router-link to="/connections" class="nav-item" active-class="active">
          <el-icon><Connection /></el-icon>
          <span>数据库连接</span>
        </router-link>
        <router-link to="/tasks" class="nav-item" active-class="active">
          <el-icon><Document /></el-icon>
          <span>YAML 任务</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <div class="version">v3.0</div>
      </div>
    </aside>

    <!-- 移动端抽屉菜单 -->
    <el-drawer v-model="drawer" direction="ltr" size="280px" :show-close="false" class="mobile-drawer">
      <template #header>
        <div class="drawer-logo">
          <el-icon :size="24"><Cpu /></el-icon>
          <span>Flink CDC 平台</span>
        </div>
      </template>
      <nav class="nav-menu">
        <router-link to="/connections" class="nav-item" @click="drawer = false">
          <el-icon><Connection /></el-icon>
          <span>数据库连接</span>
        </router-link>
        <router-link to="/tasks" class="nav-item" @click="drawer = false">
          <el-icon><Document /></el-icon>
          <span>YAML 任务</span>
        </router-link>
      </nav>
    </el-drawer>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
const isMobile = ref(window.innerWidth <= 768)
const drawer = ref(false)

onMounted(() => {
  window.addEventListener('resize', () => {
    isMobile.value = window.innerWidth <= 768
  })
})
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.layout {
  display: flex;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background: #f5f7fa;
  overflow: hidden;
}

/* 桌面端侧边栏 */
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.logo {
  height: 70px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 12px;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
}

.nav-menu {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  color: #cbd5e1;
  text-decoration: none;
  border-radius: 12px;
  transition: all 0.3s ease;
  font-size: 15px;
  font-weight: 500;
}

.nav-item:hover {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
  transform: translateX(4px);
}

.nav-item.active {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.version {
  text-align: center;
  color: #64748b;
  font-size: 12px;
}

/* 移动端顶部导航 */
.mobile-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  display: flex;
  align-items: center;
  padding: 0 16px;
  z-index: 1000;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.menu-btn {
  color: #fff !important;
  padding: 8px !important;
}

.title {
  margin-left: 16px;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

/* 移动端抽屉 */
:deep(.mobile-drawer .el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.drawer-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #1e293b;
  font-size: 20px;
  font-weight: 700;
}

/* 主内容区 */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #f5f7fa;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .main-content {
    padding: 16px;
    padding-top: 76px;
  }
  
  .sidebar {
    display: none;
  }
}

/* 平板适配 */
@media (max-width: 1024px) and (min-width: 769px) {
  .sidebar {
    width: 200px;
  }
  
  .logo {
    font-size: 18px;
    padding: 0 16px;
  }
  
  .nav-item {
    padding: 12px 14px;
    font-size: 14px;
  }
}

/* 滚动条美化 */
:deep(.el-drawer) {
  border-radius: 0;
}

.main-content::-webkit-scrollbar {
  width: 8px;
}

.main-content::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.main-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.main-content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
