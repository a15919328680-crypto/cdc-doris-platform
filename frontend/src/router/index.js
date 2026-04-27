import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import Connections from '../views/Connections.vue'
import Tasks from '../views/Tasks.vue'
import Clusters from '../views/Clusters.vue'
import Jars from '../views/Jars.vue'

const routes = [{
  path: '/', component: Layout, children: [
    { path: '', redirect: '/dashboard' },
    { path: 'dashboard', component: Dashboard },
    { path: 'connections', component: Connections },
    { path: 'tasks', component: Tasks },
    { path: 'clusters', component: Clusters },
    { path: 'jars', component: Jars }
  ]
}]

export default createRouter({ history: createWebHistory(), routes })
