<template>
  <div class="dashboard-container">
    <!-- 欢迎头部 -->
    <div class="welcome-header">
      <div>
        <h1 class="welcome-title">👋 欢迎回来</h1>
        <p class="welcome-subtitle">Flink CDC 实时数据同步平台 · 让数据流动更简单</p>
      </div>
      <el-button type="primary" size="large" @click="$router.push('/tasks')" class="quick-create-btn">
        <el-icon><Plus /></el-icon>
        <span>创建同步任务</span>
      </el-button>
    </div>

    <!-- 核心统计卡片 -->
    <div class="stats-row">
      <el-card shadow="hover" class="stat-card primary">
        <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
          <el-icon><Folder /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-label">数据源总数</div>
          <div class="stat-value">{{stats.connections}}</div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card success">
        <div class="stat-icon" style="background: linear-gradient(135deg, #0ba360 0%, #3cba92 100%)">
          <el-icon><Server /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-label">Flink 集群</div>
          <div class="stat-value">{{stats.clusters}}</div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card warning">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-label">CDC 任务总数</div>
          <div class="stat-value">{{stats.tasks}}</div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card running">
        <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
          <el-icon><VideoPlay /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-label">运行中任务</div>
          <div class="stat-value">{{stats.running}}</div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card error">
        <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-label">异常任务</div>
          <div class="stat-value">{{stats.errors}}</div>
        </div>
      </el-card>

      <el-card shadow="hover" class="stat-card info">
        <div class="stat-icon" style="background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-label">24h 新增任务</div>
          <div class="stat-value">{{stats.todayTasks}}</div>
        </div>
      </el-card>
    </div>

    <!-- 图表和快捷入口 -->
    <el-row :gutter="20" style="margin-top: 24px">
      <el-col :span="12" :xs="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>📊 任务状态分布</span>
            </div>
          </template>
          <div ref="pieChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>

      <el-col :span="12" :xs="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>📈 近 7 天任务趋势</span>
            </div>
          </template>
          <div ref="lineChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 异常任务列表 -->
    <el-card shadow="hover" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>⚠️ 异常任务（需立即处理）</span>
          <el-button type="primary" link @click="$router.push('/tasks')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="errorTasks" style="width:100%" :row-key="row => row.id">
        <el-table-column prop="task_name" label="任务名称" min-width="150" />
        <el-table-column label="数据流向" min-width="200">
          <template #default="{row}">
            <span style="font-size: 13px">{{row.source_database}} → {{row.target_database}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="error_message" label="错误信息" min-width="300" show-overflow-tooltip />
        <el-table-column prop="last_update" label="最后更新" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="$router.push('/tasks')">查看</el-button>
            <el-button size="small" type="danger" plain>重启</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="errorTasks.length === 0" description="暂无异常任务，一切正常" :image-size="80" />
    </el-card>

    <!-- 快捷入口 -->
    <el-card shadow="hover" style="margin-top: 20px; margin-bottom: 20px">
      <template #header>
        <span>⚡ 快捷入口</span>
      </template>
      <div class="quick-actions">
        <el-card shadow="hover" class="action-card" @click="$router.push('/clusters')">
          <div class="action-icon">🖥️</div>
          <div class="action-label">Flink 集群</div>
        </el-card>
        <el-card shadow="hover" class="action-card" @click="$router.push('/tasks')">
          <div class="action-icon">📝</div>
          <div class="action-label">同步任务</div>
        </el-card>
        <el-card shadow="hover" class="action-card" @click="$router.push('/connections')">
          <div class="action-icon">🔗</div>
          <div class="action-label">数据源</div>
        </el-card>
        <el-card shadow="hover" class="action-card" @click="$router.push('/jars')">
          <div class="action-icon">📦</div>
          <div class="action-label">JAR 包</div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import request from '../utils/request'
import * as echarts from 'echarts'

const stats = reactive({
  connections: 0,
  clusters: 0,
  tasks: 0,
  running: 0,
  errors: 0,
  todayTasks: 0
})

const errorTasks = ref([])
const pieChartRef = ref(null)
const lineChartRef = ref(null)

const loadStats = async () => {
  try {
    const [conns, clusters, tasks] = await Promise.all([
      request.get('/connections'),
      request.get('/clusters'),
      request.get('/tasks')
    ])
    
    stats.connections = conns.length || 0
    stats.clusters = clusters.length || 0
    stats.tasks = tasks.length || 0
    stats.running = tasks.filter(t => t.status === 'RUNNING').length || 0
    stats.errors = tasks.filter(t => t.status === 'ERROR').length || 0
    
    // 今日新增（简化处理）
    stats.todayTasks = tasks.filter(t => {
      const date = new Date(t.create_time)
      const now = new Date()
      return date.getDate() === now.getDate() && 
             date.getMonth() === now.getMonth() && 
             date.getFullYear() === now.getFullYear()
    }).length || 0
    
    // 异常任务
    errorTasks.value = tasks.filter(t => t.status === 'ERROR').slice(0, 5) || []
    
    // 初始化图表
    initPieChart()
    initLineChart()
  } catch (e) {
    console.error('Load stats failed', e)
  }
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  
  const chart = echarts.init(pieChartRef.value)
  const statusCount = {
    '运行中': stats.running,
    '已停止': stats.tasks - stats.running - stats.errors,
    '异常': stats.errors
  }
  
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: '0' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { show: true, fontSize: 14 },
      data: [
        { value: statusCount['运行中'], name: '运行中', itemStyle: { color: '#10b981' } },
        { value: statusCount['已停止'], name: '已停止', itemStyle: { color: '#64748b' } },
        { value: statusCount['异常'], name: '异常', itemStyle: { color: '#ef4444' } }
      ]
    }]
  })
}

const initLineChart = () => {
  if (!lineChartRef.value) return
  
  const chart = echarts.init(lineChartRef.value)
  const days = []
  const data = []
  
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    days.push(`${d.getMonth() + 1}/${d.getDate()}`)
    // 模拟数据
    data.push(Math.floor(Math.random() * 10))
  }
  
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: days,
      boundaryGap: false
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      smooth: true,
      data: data,
      itemStyle: { color: '#3b82f6' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
          { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
        ])
      }
    }]
  })
}

onMounted(() => {
  loadStats()
  window.addEventListener('resize', () => {
    const pieChart = echarts.getInstanceByDom(pieChartRef.value)
    const lineChart = echarts.getInstanceByDom(lineChartRef.value)
    pieChart?.resize()
    lineChart?.resize()
  })
})
</script>

<style scoped>
.dashboard-container { max-width: 1400px; margin: 0 auto; padding: 24px 0; }
.welcome-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; flex-wrap: wrap; gap: 16px; }
.welcome-title { font-size: 28px; font-weight: 700; color: #1e293b; margin: 0; }
.welcome-subtitle { color: #64748b; font-size: 14px; margin: 8px 0 0 0; }
.quick-create-btn { background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); border: none; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3); }
.stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }
.stat-card { border-radius: 16px; overflow: hidden; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; padding: 24px; gap: 16px; }
.stat-icon { width: 64px; height: 64px; border-radius: 16px; display: flex; align-items: center; justify-content: center; font-size: 32px; color: #fff; flex-shrink: 0; }
.stat-content { flex: 1; }
.stat-label { color: #64748b; font-size: 13px; margin-bottom: 8px; }
.stat-value { font-size: 32px; font-weight: 700; color: #1e293b; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header span { font-size: 16px; font-weight: 600; color: #1e293b; }
.quick-actions { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 16px; }
.action-card { text-align: center; padding: 24px 16px; cursor: pointer; transition: all 0.3s ease; border-radius: 12px; }
.action-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1); }
.action-icon { font-size: 40px; margin-bottom: 12px; }
.action-label { font-size: 14px; color: #64748b; font-weight: 500; }
@media (max-width: 768px) {
  .welcome-header { flex-direction: column; align-items: flex-start; }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .quick-actions { grid-template-columns: repeat(2, 1fr); }
}
</style>
