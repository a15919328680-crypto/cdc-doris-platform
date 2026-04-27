import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import Connections from '../views/Connections.vue'
import Tasks from '../views/Tasks.vue'

const routes = [{
  path: '/', component: Layout, children: [
    { path: '', redirect: '/connections' },
    { path: 'connections', component: Connections },
    { path: 'tasks', component: Tasks }
  ]
}]

export default createRouter({ history: createWebHistory(), routes })
