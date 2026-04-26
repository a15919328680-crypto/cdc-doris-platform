<template>
  <el-container class="layout">
    <el-aside v-show="!isMobile" width="200px">
      <div class="logo">CDC 平台 v3.0</div>
      <el-menu router :default-active="$route.path" background-color="#1e293b" text-color="#cbd5e1" active-text-color="#3b82f6">
        <el-menu-item index="/connections">数据库连接</el-menu-item>
        <el-menu-item index="/tasks">YAML 任务</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header v-show="!isMobile"><h3>Flink CDC 同步平台</h3></el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
const isMobile = ref(window.innerWidth < 768)
onMounted(() => window.addEventListener('resize', () => isMobile.value = window.innerWidth < 768))
</script>

<style scoped>
.layout { height: 100vh; }
.logo { height: 50px; display: flex; align-items: center; justify-content: center; background: #0f172a; color: #fff; font-weight: bold; }
.el-header { background: #fff; border-bottom: 1px solid #e5e7eb; display: flex; align-items: center; }
.el-header h3 { margin: 0; font-size: 16px; }
.el-main { background: #f3f4f6; padding: 10px; }
@media (max-width: 768px) { .el-header { display: none; } }
</style>
