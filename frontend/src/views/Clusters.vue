<template>
  <div class="page-container">
    <div class="header">
      <div>
        <h1 class="page-title">Flink 集群</h1>
        <p class="page-desc">管理 Flink 集群配置</p>
      </div>
      <el-button type="primary" size="large" @click="showAdd=true" class="add-btn">
        <el-icon><Plus /></el-icon>
        <span>添加集群</span>
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card total">
        <div class="stat-icon"><el-icon><Server /></el-icon></div>
        <div class="stat-content">
          <div class="stat-label">集群总数</div>
          <div class="stat-value">{{list.length}}</div>
        </div>
      </div>
      <div class="stat-card connected">
        <div class="stat-icon"><el-icon><Connection /></el-icon></div>
        <div class="stat-content">
          <div class="stat-label">已连接</div>
          <div class="stat-value">{{list.filter(x=>x.status==='CONNECTED').length}}</div>
        </div>
      </div>
      <div class="stat-card inactive">
        <div class="stat-icon"><el-icon><Switch /></el-icon></div>
        <div class="stat-content">
          <div class="stat-label">未激活</div>
          <div class="stat-value">{{list.filter(x=>x.status==='INACTIVE').length}}</div>
        </div>
      </div>
    </div>

    <!-- 集群列表 -->
    <div class="table-card">
      <el-table :data="list" style="width:100%">
        <el-table-column prop="name" label="集群名称" min-width="150" />
        <el-table-column prop="deploy_mode" label="部署模式" width="120">
          <template #default="{row}">
            <el-tag :type="row.deploy_mode==='STANDALONE'?'success':row.deploy_mode==='DOCKER'?'warning':row.deploy_mode==='KUBERNETES'?'primary':'info'" size="small">
              {{row.deploy_mode}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="连接地址" min-width="200">
          <template #default="{row}">
            <span v-if="row.rest_url">{{row.rest_url}}</span>
            <span v-else-if="row.flink_home">{{row.flink_home}}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="Flink 版本" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="row.status==='CONNECTED'?'success':row.status==='ERROR'?'danger':'info'" size="small">
              {{row.status}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="last_check_time" label="最后检查" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{row}">
            <div style="display:flex;gap:8px">
              <el-button size="small" type="success" @click="testConnection(row)" :loading="row.testing">
                测试连接
              </el-button>
              <el-button size="small" @click="editCluster(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="del(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加/编辑集群对话框 -->
    <el-dialog v-model="showAdd" :title="editMode?'编辑集群':'添加 Flink 集群'" width="800px" :close-on-click-modal="false">
      <el-form :model="form" label-width="140px" label-position="left">
        <el-form-item label="集群名称" required>
          <el-input v-model="form.name" placeholder="例如：生产 Flink 集群" />
        </el-form-item>
        <el-form-item label="部署模式" required>
          <el-select v-model="form.deployMode" placeholder="选择部署模式" style="width:100%" @change="onDeployModeChange">
            <el-option label="Standalone（独立部署）" value="STANDALONE" />
            <el-option label="Docker Compose" value="DOCKER" />
            <el-option label="Kubernetes" value="KUBERNETES" />
            <el-option label="YARN" value="YARN" />
          </el-select>
        </el-form-item>
        
        <!-- REST API 配置 -->
        <el-form-item label="REST API 地址" v-if="['STANDALONE','DOCKER','KUBERNETES'].includes(form.deployMode)">
          <el-input v-model="form.restUrl" placeholder="http://192.168.1.100:8081" />
          <div style="font-size:12px;color:#8492a6;margin-top:4px">Flink UI 的访问地址</div>
        </el-form-item>
        
        <!-- 命令行配置 -->
        <el-form-item label="Flink 安装路径" v-if="form.deployMode==='STANDALONE'">
          <el-input v-model="form.flinkHome" placeholder="/opt/flink" />
          <div style="font-size:12px;color:#8492a6;margin-top:4px">flink 命令所在目录</div>
        </el-form-item>
        
        <!-- K8s 配置 -->
        <el-form-item label="K8s Namespace" v-if="form.deployMode==='KUBERNETES'">
          <el-input v-model="form.k8sNamespace" placeholder="default" />
        </el-form-item>
        
        <!-- Docker Compose 配置 -->
        <el-form-item label="Compose 文件路径" v-if="form.deployMode==='DOCKER'">
          <el-input v-model="form.dockerComposeFile" placeholder="/opt/docker-compose.yml" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showAdd=false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const showAdd = ref(false)
const editMode = ref(false)
const currentId = ref(null)

const form = ref({
  name: '',
  deployMode: 'STANDALONE',
  restUrl: '',
  flinkHome: '',
  k8sNamespace: 'default',
  dockerComposeFile: ''
})

const load = async () => {
  list.value = await request.get('/clusters')
}

const onDeployModeChange = () => {
  // 部署模式变化时清空相关配置
  if (form.value.deployMode !== 'STANDALONE') {
    form.value.flinkHome = ''
  }
  if (form.value.deployMode !== 'KUBERNETES') {
    form.value.k8sNamespace = ''
  }
  if (form.value.deployMode !== 'DOCKER') {
    form.value.dockerComposeFile = ''
  }
}

const testConnection = async (row) => {
  row.testing = true
  try {
    const res = await request.post(`/clusters/${row.id}/test`)
    if (res.success) {
      ElMessage.success(`连接成功！Flink 版本：${res.version || '未知'}`)
      row.status = 'CONNECTED'
      row.version = res.version
    } else {
      ElMessage.error('连接失败：' + res.message)
      row.status = 'ERROR'
    }
  } catch (e) {
    ElMessage.error('测试失败')
    row.status = 'ERROR'
  } finally {
    row.testing = false
  }
}

const editCluster = (row) => {
  form.value = {
    name: row.name,
    deployMode: row.deploy_mode,
    restUrl: row.rest_url,
    flinkHome: row.flink_home,
    k8sNamespace: row.k8s_namespace || 'default',
    dockerComposeFile: row.docker_compose_file
  }
  currentId.value = row.id
  editMode.value = true
  showAdd.value = true
}

const submit = async () => {
  if (!form.value.name) {
    ElMessage.error('请输入集群名称')
    return
  }
  
  let res
  if (editMode.value) {
    res = await request.put(`/clusters/${currentId.value}`, form.value)
  } else {
    res = await request.post('/clusters', form.value)
  }
  
  if (res.success) {
    ElMessage.success(editMode.value ? '更新成功' : '添加成功')
    showAdd.value = false
    load()
    resetForm()
  } else {
    ElMessage.error(res.message)
  }
}

const del = async (id) => {
  await ElMessageBox.confirm('确定删除此集群？', '提示', { type: 'warning' })
  await request.delete(`/clusters/${id}`)
  ElMessage.success('删除成功')
  load()
}

const resetForm = () => {
  form.value = {
    name: '',
    deployMode: 'STANDALONE',
    restUrl: '',
    flinkHome: '',
    k8sNamespace: 'default',
    dockerComposeFile: ''
  }
  editMode.value = false
  currentId.value = null
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.page-container { max-width: 1400px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; flex-wrap: wrap; gap: 16px; }
.page-title { font-size: 28px; font-weight: 700; color: #1e293b; margin: 0 0 4px 0; }
.page-desc { color: #64748b; font-size: 14px; margin: 0; }
.add-btn { background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); border: none; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3); }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 24px; }
.stat-card { background: #fff; border-radius: 16px; padding: 24px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); transition: all 0.3s ease; border: 1px solid #f1f5f9; }
.stat-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08); }
.stat-icon { width: 56px; height: 56px; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 26px; }
.stat-card.total .stat-icon { background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff; }
.stat-card.connected .stat-icon { background: linear-gradient(135deg, #10b981, #059669); color: #fff; }
.stat-card.inactive .stat-icon { background: linear-gradient(135deg, #64748b, #475569); color: #fff; }
.stat-content { flex: 1; }
.stat-label { color: #64748b; font-size: 13px; margin-bottom: 8px; }
.stat-value { font-size: 32px; font-weight: 700; color: #1e293b; }
.table-card { background: #fff; border-radius: 16px; padding: 24px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); border: 1px solid #f1f5f9; }
</style>
