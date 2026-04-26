import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    ElMessage.error(error.response?.data?.message || '请求失败')
    return Promise.reject(error)
  }
)

export default {
  mysql: {
    list: () => request.get('/mysql/list'),
    getById: (id) => request.get(`/mysql/${id}`),
    add: (data) => request.post('/mysql/add', data),
    update: (data) => request.put('/mysql/update', data),
    delete: (id) => request.delete(`/mysql/delete/${id}`),
    test: (data) => request.post('/mysql/test', data),
    getTables: (id) => request.get(`/mysql/tables/${id}`)
  },

  doris: {
    list: () => request.get('/doris/list'),
    getById: (id) => request.get(`/doris/${id}`),
    add: (data) => request.post('/doris/add', data),
    update: (data) => request.put('/doris/update', data),
    delete: (id) => request.delete(`/doris/delete/${id}`),
    test: (data) => request.post('/doris/test', data),
    getDatabases: (id) => request.get(`/doris/databases/${id}`),
    getTables: (id, database) => request.get(`/doris/tables/${id}?database=${database}`)
  },

  task: {
    list: () => request.get('/task/list'),
    getById: (id) => request.get(`/task/${id}`),
    add: (data) => request.post('/task/add', data),
    update: (data) => request.put('/task/update', data),
    delete: (id) => request.delete(`/task/delete/${id}`),
    start: (id) => request.post(`/task/start/${id}`),
    stop: (id, graceful = true) => request.post(`/task/stop/${id}?graceful=${graceful}`),
    restart: (id) => request.post(`/task/restart/${id}`),
    reset: (id) => request.post(`/task/reset/${id}`),
    getLogs: (id) => request.get(`/task/logs/${id}`),
    getOverview: () => request.get('/task/stats/overview')
  }
}
