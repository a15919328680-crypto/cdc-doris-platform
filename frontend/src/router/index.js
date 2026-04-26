import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/Layout.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        meta: { title: '首页大盘', icon: 'Dashboard' },
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: '/connections',
        name: 'Connections',
        meta: { title: '数据库连接', icon: 'Connection' },
        component: () => import('@/views/DataSources.vue')
      },
      {
        path: '/sync-tasks',
        name: 'SyncTasks',
        meta: { title: '同步任务（YAML）', icon: 'Document' },
        component: () => import('@/views/SyncTasks.vue')
      },
      {
        path: '/monitor',
        name: 'Monitor',
        meta: { title: '运行监控', icon: 'TrendCharts' },
        component: () => import('@/views/monitor/Index.vue')
      },
      {
        path: '/system-config',
        name: 'SystemConfig',
        meta: { title: '系统配置', icon: 'Setting' },
        component: () => import('@/views/system/Index.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
