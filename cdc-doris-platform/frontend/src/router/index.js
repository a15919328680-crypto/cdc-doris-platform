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
        component: () => import('@/views/dashboard/Index.vue')
      },
      {
        path: '/mysql-source',
        name: 'MysqlSource',
        meta: { title: 'MySQL 数据源', icon: 'Database' },
        component: () => import('@/views/mysql/Index.vue')
      },
      {
        path: '/doris-target',
        name: 'DorisTarget',
        meta: { title: 'Doris 目标库', icon: 'DataLine' },
        component: () => import('@/views/doris/Index.vue')
      },
      {
        path: '/sync-task',
        name: 'SyncTask',
        meta: { title: '同步任务管理', icon: 'Connection' },
        component: () => import('@/views/task/Index.vue')
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
