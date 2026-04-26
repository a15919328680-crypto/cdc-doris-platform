import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/Layout.vue'

const routes = [{
  path: '/', component: Layout, children: [
    { path: '', redirect: '/connections' },
    { path: 'connections', component: () => import('@/views/Connections.vue') },
    { path: 'tasks', component: () => import('@/views/Tasks.vue') }
  ]
}]

export default createRouter({ history: createWebHistory(), routes })
