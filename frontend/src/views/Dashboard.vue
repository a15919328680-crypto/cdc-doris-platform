<template>
  <div class="dashboard">
    <el-card class="stat-card">
      <div class="stat-content">
        <div class="stat-icon mysql">
          <el-icon><Connection /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ connectionCount }}</div>
          <div class="stat-label">数据库连接</div>
        </div>
      </div>
    </el-card>

    <el-card class="stat-card">
      <div class="stat-content">
        <div class="stat-icon source">
          <el-icon><Folder /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ sourceCount }}</div>
          <div class="stat-label">数据源</div>
        </div>
      </div>
    </el-card>

    <el-card class="stat-card">
      <div class="stat-content">
        <div class="stat-icon target">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ targetCount }}</div>
          <div class="stat-label">目标库</div>
        </div>
      </div>
    </el-card>

    <el-card class="stat-card">
      <div class="stat-content">
        <div class="stat-icon task">
          <el-icon><Operation /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ taskCount }}</div>
          <div class="stat-label">同步任务</div>
        </div>
      </div>
    </el-card>

    <el-card class="task-card">
      <template #header>
        <div class="card-header">
          <span>最近任务</span>
          <el-button type="primary" size="small" @click="$router.push('/tasks')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentTasks" style="width: 100%">
        <el-table-column prop="taskName" label="任务名称" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="syncCount" label="同步条数" width="100" />
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.updateTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Connection, Folder, DataLine, Operation } from '@element-plus/icons-vue'
import request from '@/utils/request'

const connectionCount = ref(0)
const sourceCount = ref(0)
const targetCount = ref(0)
const taskCount = ref(0)
const recentTasks = ref([])

const getStatusType = (status) => {
  const types = {
    RUNNING: 'success',
    STOPPED: 'info',
    CREATED: 'warning',
    FAILED: 'danger',
    CANCELLED: 'info'
  }
  return types[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const loadDashboard = async () => {
  try {
    const [connections, sources, targets, tasks] = await Promise.all([
      request.get('/connection/list'),
      request.get('/connection/sources'),
      request.get('/connection/targets'),
      request.get('/task/list')
    ])
    
    connectionCount.value = connections.length
    sourceCount.value = sources.length
    targetCount.value = targets.length
    taskCount.value = tasks.length
    recentTasks.value = tasks.slice(0, 5)
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-card {
  margin-bottom: 20px;
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 28px;
  color: white;
}

.stat-icon.mysql {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.source {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.target {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.task {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
