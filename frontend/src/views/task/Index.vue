<template>
  <div class="sync-task-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>同步任务管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新建任务
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="taskName" label="任务名称" width="180" />
        <el-table-column label="源库表" width="180">
          <template #default="{ row }">
            {{ row.sourceDatabase }}.{{ row.sourceTable }}
          </template>
        </el-table-column>
        <el-table-column label="目标库表" width="180">
          <template #default="{ row }">
            {{ row.targetDatabase }}.{{ row.targetTable }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="syncCount" label="同步条数" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="500" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status !== 'RUNNING'" 
              size="small" 
              type="success" 
              @click="handleStart(row)"
            >
              启动
            </el-button>
            <el-dropdown v-else>
              <el-button size="small" type="success">
                运行中 <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleStop(row, true)">优雅停止</el-dropdown-item>
                  <el-dropdown-item @click="handleStop(row, false)">强制停止</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            
            <el-button size="small" @click="handleRestart(row)">重启</el-button>
            <el-button size="small" type="warning" @click="handleReset(row)">重置位点</el-button>
            <el-button size="small" @click="handleViewLogs(row)">日志</el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      @close="handleClose"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="formData.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        
        <el-form-item label="源数据源" prop="sourceId">
          <el-select v-model="formData.sourceId" placeholder="请选择 MySQL 数据源" style="width: 100%" @change="handleSourceChange">
            <el-option
              v-for="item in mysqlSources"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="源数据库" prop="sourceDatabase">
          <el-select v-model="formData.sourceDatabase" placeholder="请先选择数据源" style="width: 100%" :disabled="!formData.sourceId">
            <el-option
              v-for="item in sourceDatabases"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="源表名" prop="sourceTable">
          <el-select v-model="formData.sourceTable" placeholder="请先选择数据库" style="width: 100%" :disabled="!formData.sourceDatabase">
            <el-option
              v-for="item in sourceTables"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="目标库" prop="targetId">
          <el-select v-model="formData.targetId" placeholder="请选择 Doris 目标库" style="width: 100%" @change="handleTargetChange">
            <el-option
              v-for="item in dorisTargets"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="目标数据库" prop="targetDatabase">
          <el-select v-model="formData.targetDatabase" placeholder="请先选择目标库" style="width: 100%" :disabled="!formData.targetId">
            <el-option
              v-for="item in targetDatabases"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="目标表名" prop="targetTable">
          <el-select v-model="formData.targetTable" placeholder="请先选择数据库" style="width: 100%" :disabled="!formData.targetDatabase">
            <el-option
              v-for="item in targetTables"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述信息" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logDialogVisible" title="运行日志" width="1000px">
      <el-input
        v-model="logContent"
        type="textarea"
        :rows="30"
        readonly
        style="font-family: monospace; font-size: 12px;"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const tableData = ref([])
const dialogVisible = ref(false)
const logDialogVisible = ref(false)
const dialogTitle = ref('新建任务')
const submitting = ref(false)
const formRef = ref(null)
const logContent = ref('')

const mysqlSources = ref([])
const dorisTargets = ref([])
const sourceDatabases = ref([])
const sourceTables = ref([])
const targetDatabases = ref([])
const targetTables = ref([])

const formData = ref({
  id: null,
  taskName: '',
  sourceId: null,
  sourceDatabase: '',
  sourceTable: '',
  targetId: null,
  targetDatabase: '',
  targetTable: '',
  description: ''
})

const rules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  sourceId: [{ required: true, message: '请选择源数据源', trigger: 'change' }],
  sourceDatabase: [{ required: true, message: '请选择源数据库', trigger: 'change' }],
  sourceTable: [{ required: true, message: '请选择源表', trigger: 'change' }],
  targetId: [{ required: true, message: '请选择目标库', trigger: 'change' }],
  targetDatabase: [{ required: true, message: '请选择目标数据库', trigger: 'change' }],
  targetTable: [{ required: true, message: '请选择目标表', trigger: 'change' }]
}

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

const fetchAllData = async () => {
  try {
    const [tasks, sources, targets] = await Promise.all([
      api.task.list(),
      api.mysql.list(),
      api.doris.list()
    ])
    tableData.value = tasks
    mysqlSources.value = sources
    dorisTargets.value = targets
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const handleAdd = () => {
  dialogTitle.value = '新建任务'
  formData.value = {
    id: null,
    taskName: '',
    sourceId: null,
    sourceDatabase: '',
    sourceTable: '',
    targetId: null,
    targetDatabase: '',
    targetTable: '',
    description: ''
  }
  sourceDatabases.value = []
  sourceTables.value = []
  targetDatabases.value = []
  targetTables.value = []
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑任务'
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.task.delete(row.id)
    ElMessage.success('删除成功')
    fetchAllData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleStart = async (row) => {
  try {
    await api.task.start(row.id)
    ElMessage.success('任务启动中')
    setTimeout(fetchAllData, 3000)
  } catch (error) {
    ElMessage.error('启动失败')
  }
}

const handleStop = async (row, graceful) => {
  try {
    await api.task.stop(row.id, graceful)
    ElMessage.success(graceful ? '任务优雅停止中' : '任务强制停止中')
    setTimeout(fetchAllData, 3000)
  } catch (error) {
    ElMessage.error('停止失败')
  }
}

const handleRestart = async (row) => {
  try {
    await api.task.restart(row.id)
    ElMessage.success('任务重启中')
    setTimeout(fetchAllData, 5000)
  } catch (error) {
    ElMessage.error('重启失败')
  }
}

const handleReset = async (row) => {
  try {
    await ElMessageBox.confirm('重置位点后将重新全量同步，确定继续？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.task.reset(row.id)
    ElMessage.success('位点重置中，将重新全量同步')
    setTimeout(fetchAllData, 5000)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重置失败')
    }
  }
}

const handleViewLogs = async (row) => {
  try {
    const res = await api.task.getLogs(row.id)
    if (res.success) {
      logContent.value = res.logs || '暂无日志'
      logDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取日志失败')
  }
}

const handleSourceChange = async () => {
  if (!formData.value.sourceId) return
  try {
    const source = mysqlSources.value.find(s => s.id === formData.value.sourceId)
    const tables = await api.mysql.getTables(formData.value.sourceId)
    if (tables.success) {
      const dbs = [...new Set(tables.tables.map(t => t.split('.')[0]))]
      sourceDatabases.value = dbs
    }
  } catch (error) {
    ElMessage.error('获取数据库列表失败')
  }
}

const handleTargetChange = async () => {
  if (!formData.value.targetId) return
  try {
    const dbs = await api.doris.getDatabases(formData.value.targetId)
    if (dbs.success) {
      targetDatabases.value = dbs.databases
    }
  } catch (error) {
    ElMessage.error('获取数据库列表失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    
    if (formData.value.id) {
      await api.task.update(formData.value)
      ElMessage.success('更新成功')
    } else {
      await api.task.add(formData.value)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    fetchAllData()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  formRef.value?.resetFields()
}

onMounted(() => {
  fetchAllData()
})
</script>

<style scoped>
.sync-task-page {
  padding: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: bold;
}
</style>
