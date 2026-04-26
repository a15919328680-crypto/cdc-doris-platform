<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div class="stats-icon total">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ stats.totalTasks }}</div>
              <div class="stats-label">总任务数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div class="stats-icon running">
              <el-icon><VideoPlay /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ stats.runningCount }}</div>
              <div class="stats-label">运行中</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div class="stats-icon stopped">
              <el-icon><VideoPause /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ stats.stoppedCount }}</div>
              <div class="stats-label">已停止</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stats-card" shadow="hover">
          <div class="stats-content">
            <div class="stats-icon failed">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ stats.failedCount }}</div>
              <div class="stats-label">异常任务</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>同步数据总量趋势</span>
            </div>
          </template>
          <div ref="syncChartRef" class="chart"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>最近运行日志</span>
            </div>
          </template>
          <div class="log-list">
            <div v-for="(log, index) in recentLogs" :key="index" class="log-item">
              <el-tag :type="log.level === 'ERROR' ? 'danger' : 'info'" size="small">
                {{ log.level }}
              </el-tag>
              <span class="log-message">{{ log.message }}</span>
              <span class="log-time">{{ log.time }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>任务状态总览</span>
            </div>
          </template>
          <el-table :data="tasks" style="width: 100%" :height="300">
            <el-table-column prop="taskName" label="任务名称" />
            <el-table-column prop="sourceTable" label="源表" width="150">
              <template #default="{ row }">
                {{ row.sourceDatabase }}.{{ row.sourceTable }}
              </template>
            </el-table-column>
            <el-table-column prop="targetTable" label="目标表" width="150">
              <template #default="{ row }">
                {{ row.targetDatabase }}.{{ row.targetTable }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="syncCount" label="同步条数" width="120" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import api from '@/api'

const stats = ref({
  totalTasks: 0,
  runningCount: 0,
  stoppedCount: 0,
  failedCount: 0,
  totalSyncCount: 0
})

const tasks = ref([])
const syncChartRef = ref(null)
let syncChart = null
let timer = null

const recentLogs = ref([
  { level: 'INFO', message: '任务 task_001 启动成功', time: '10:23:45' },
  { level: 'INFO', message: '任务 task_002 完成全量同步', time: '10:22:30' },
  { level: 'WARN', message: '任务 task_003 同步延迟较高', time: '10:21:15' },
  { level: 'INFO', message: '任务 task_001 checkpoint 保存成功', time: '10:20:00' }
])

const getStatusType = (status) => {
  const types = {
    RUNNING: 'success',
    STOPPED: 'info',
    CREATED: 'info',
    FAILED: 'danger',
    CANCELLED: 'warning'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    RUNNING: '运行中',
    STOPPED: '已停止',
    CREATED: '未启动',
    FAILED: '失败',
    CANCELLED: '已取消'
  }
  return texts[status] || status
}

const fetchStats = async () => {
  try {
    const res = await api.task.getOverview()
    if (res.success) {
      stats.value = res
    }
    const tasksRes = await api.task.list()
    tasks.value = tasksRes.slice(0, 10)
    updateChart()
  } catch (error) {
    ElMessage.error('获取数据失败')
  }
}

const initChart = () => {
  if (!syncChartRef.value) return
  syncChart = echarts.init(syncChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['10:00', '10:05', '10:10', '10:15', '10:20', '10:25']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '同步条数',
        type: 'line',
        smooth: true,
        data: [1000, 3200, 5100, 7800, 10200, 15600],
        areaStyle: {
          color: 'rgba(59, 130, 246, 0.1)'
        },
        itemStyle: {
          color: '#3b82f6'
        }
      }
    ]
  }
  syncChart.setOption(option)
}

const updateChart = () => {
  if (syncChart) {
    syncChart.resize()
  }
}

onMounted(() => {
  fetchStats()
  initChart()
  timer = setInterval(fetchStats, 10000)
  
  window.addEventListener('resize', () => {
    syncChart?.resize()
  })
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
  syncChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 10px;
}

.stats-row {
  margin-bottom: 20px;
}

.stats-card {
  border-radius: 8px;
}

.stats-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
}

.stats-icon.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stats-icon.running {
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
}

.stats-icon.stopped {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
}

.stats-icon.failed {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.stats-info {
  flex: 1;
}

.stats-value {
  font-size: 32px;
  font-weight: bold;
  color: #1f2937;
}

.stats-label {
  font-size: 14px;
  color: #6b7280;
  margin-top: 5px;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: bold;
}

.chart {
  height: 300px;
}

.log-list {
  height: 300px;
  overflow-y: auto;
}

.log-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
  font-size: 13px;
}

.log-message {
  flex: 1;
  color: #374151;
}

.log-time {
  color: #9ca3af;
  font-size: 12px;
}
</style>
