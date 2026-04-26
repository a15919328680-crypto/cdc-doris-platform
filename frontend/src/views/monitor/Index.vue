<template>
  <div class="monitor-page">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>任务运行状态</span>
          </template>
          <div ref="statusChartRef" style="height: 400px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>同步吞吐量</span>
          </template>
          <div ref="throughputChartRef" style="height: 400px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>任务延迟监控</span>
          </template>
          <el-table :data="taskMonitorData" style="width: 100%">
            <el-table-column prop="taskName" label="任务名称" />
            <el-table-column prop="status" label="状态" />
            <el-table-column prop="delay" label="延迟 (ms)" />
            <el-table-column prop="checkpoint" label="最近检查点" />
            <el-table-column label="监控" width="150">
              <template #default="{ row }">
                <el-tag :type="row.delay < 1000 ? 'success' : row.delay < 5000 ? 'warning' : 'danger'">
                  {{ row.delay < 1000 ? '正常' : row.delay < 5000 ? '偏高' : '严重' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import * as echarts from 'echarts'

const statusChartRef = ref(null)
const throughputChartRef = ref(null)
let statusChart = null
let throughputChart = null

const taskMonitorData = ref([
  { taskName: 'task_001', status: 'RUNNING', delay: 234, checkpoint: '2024-01-15 10:23:45' },
  { taskName: 'task_002', status: 'RUNNING', delay: 1523, checkpoint: '2024-01-15 10:22:30' },
  { taskName: 'task_003', status: 'RUNNING', delay: 389, checkpoint: '2024-01-15 10:21:15' }
])

const initCharts = () => {
  if (statusChartRef.value) {
    statusChart = echarts.init(statusChartRef.value)
    statusChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { top: '5%', left: 'center' },
      series: [{
        name: '任务状态',
        type: 'pie',
        radius: ['40%', '70%'],
        data: [
          { value: 5, name: '运行中' },
          { value: 3, name: '已停止' },
          { value: 1, name: '异常' }
        ]
      }]
    })
  }

  if (throughputChartRef.value) {
    throughputChart = echarts.init(throughputChartRef.value)
    throughputChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00']
      },
      yAxis: { type: 'value' },
      series: [{
        data: [120, 200, 150, 80, 70, 110],
        type: 'bar',
        itemStyle: { color: '#3b82f6' }
      }]
    })
  }
}

onMounted(() => {
  initCharts()
  window.addEventListener('resize', () => {
    statusChart?.resize()
    throughputChart?.resize()
  })
})
</script>

<style scoped>
.monitor-page {
  padding: 10px;
}
</style>
