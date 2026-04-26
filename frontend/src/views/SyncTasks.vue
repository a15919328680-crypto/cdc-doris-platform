<template>
  <div class="sync-tasks">
    <div class="page-header">
      <h2>同步任务（YAML）</h2>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        <span class="btn-text">创建任务</span>
      </el-button>
    </div>

    <el-table :data="tasks" style="width: 100%" class="responsive-table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="taskName" label="任务名称" min-width="150" />
      <el-table-column prop="syncMode" label="模式" width="80">
        <template #default="{ row }">
          <el-tag size="small">{{ row.syncMode || 'CDC' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="源 → 目标" min-width="200">
        <template #default="{ row }">
          <div class="route-flow">
            <span class="route-text">{{ row.sourceDatabase }}.{{ row.sourceTable }}</span>
            <el-icon class="arrow-icon"><Right /></el-icon>
            <span class="route-text">{{ row.targetDatabase }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="parallelism" label="并行度" width="70" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button size="small" @click="viewYaml(row)">YAML</el-button>
            <el-button size="small" type="success" :disabled="row.status !== 'CREATED'" @click="startTask(row.id)">启动</el-button>
            <el-button size="small" type="warning" :disabled="row.status !== 'RUNNING'" @click="stopTask(row.id)">停止</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="150px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="如：mysql-to-doris-sync" />
        </el-form-item>
        
        <el-form-item label="数据源" prop="sourceId">
          <el-select v-model="form.sourceId" placeholder="选择数据源" style="width: 100%" @change="onSourceChange">
            <el-option
              v-for="source in sources"
              :key="source.id"
              :label="`${source.name} (${source.host}:${source.port})`"
              :value="source.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="目标库" prop="targetId">
          <el-select v-model="form.targetId" placeholder="选择目标库" style="width: 100%">
            <el-option
              v-for="target in targets"
              :key="target.id"
              :label="`${target.name} (${target.host}:${target.port})`"
              :value="target.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="源数据库模式" prop="sourceDatabase">
          <el-input v-model="form.sourceDatabase" placeholder="如：db_1 或 db_*（支持正则）" />
          <div class="form-tip">支持正则表达式，如 db_\d+ 匹配多个分库</div>
        </el-form-item>
        
        <el-form-item label="源表模式" prop="sourceTable">
          <el-input v-model="form.sourceTable" placeholder="如：user_* 或 *（支持正则）" />
          <div class="form-tip">支持正则表达式，如 user_\d+ 匹配分表</div>
        </el-form-item>
        
        <el-form-item label="目标数据库" prop="targetDatabase">
          <el-input v-model="form.targetDatabase" placeholder="如：doris_db" />
        </el-form-item>
        
        <el-form-item label="同步模式" prop="syncMode">
          <el-select v-model="form.syncMode">
            <el-option label="CDC（全量+增量）" value="CDC" />
            <el-option label="全量" value="FULL" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="并行度" prop="parallelism">
          <el-input-number v-model="form.parallelism" :min="1" :max="10" />
        </el-form-item>
        
        <el-form-item label="Checkpoint 间隔" prop="checkpointInterval">
          <el-input-number v-model="form.checkpointInterval" :min="10" :step="10" />
          <span class="form-tip">单位：秒</span>
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- YAML 预览对话框 -->
    <el-dialog v-model="yamlDialogVisible" title="Flink CDC YAML 配置" width="900px">
      <div class="yaml-preview">
        <div class="tip">
          <el-icon><InfoFilled /></el-icon>
          点击复制按钮可以复制 YAML 配置，使用 flink-cdc.sh 提交任务
        </div>
        <pre class="yaml-code">{{ currentYaml }}</pre>
      </div>
      <template #footer>
        <el-button @click="yamlDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="copyYaml">复制 YAML</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Right, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const tasks = ref([])
const sources = ref([])
const targets = ref([])
const dialogVisible = ref(false)
const yamlDialogVisible = ref(false)
const dialogTitle = ref('创建任务')
const submitting = ref(false)
const currentYaml = ref('')
const formRef = ref(null)

const form = ref({
  id: null,
  taskName: '',
  sourceId: null,
  targetId: null,
  sourceDatabase: '',
  sourceTable: '.*',
  targetDatabase: '',
  syncMode: 'CDC',
  parallelism: 2,
  checkpointInterval: 60,
  description: ''
})

const rules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  sourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  targetId: [{ required: true, message: '请选择目标库', trigger: 'change' }],
  sourceDatabase: [{ required: true, message: '请输入源数据库模式', trigger: 'blur' }],
  targetDatabase: [{ required: true, message: '请输入目标数据库', trigger: 'blur' }]
}

const getStatusType = (status) => {
  const types = {
    CREATED: 'info',
    RUNNING: 'success',
    STOPPED: 'warning',
    FAILED: 'danger',
    CANCELLED: 'info'
  }
  return types[status] || 'info'
}

const loadTasks = async () => {
  try {
    tasks.value = await request.get('/task/list')
  } catch (error) {
    ElMessage.error('加载任务失败：' + error.message)
  }
}

const loadConnections = async () => {
  try {
    const [sourcesData, targetsData] = await Promise.all([
      request.get('/connection/sources'),
      request.get('/connection/targets')
    ])
    sources.value = sourcesData
    targets.value = targetsData
  } catch (error) {
    ElMessage.error('加载连接失败：' + error.message)
  }
}

const showAddDialog = () => {
  dialogTitle.value = '创建同步任务'
  form.value = {
    id: null,
    taskName: '',
    sourceId: null,
    targetId: null,
    sourceDatabase: '',
    sourceTable: '.*',
    targetDatabase: '',
    syncMode: 'CDC',
    parallelism: 2,
    checkpointInterval: 60,
    description: ''
  }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  dialogTitle.value = '编辑同步任务'
  form.value = { 
    id: row.id,
    taskName: row.taskName,
    sourceId: row.sourceId,
    targetId: row.targetId,
    sourceDatabase: row.sourceDatabase,
    sourceTable: row.sourceTable,
    targetDatabase: row.targetDatabase,
    syncMode: row.syncMode,
    parallelism: row.parallelism,
    checkpointInterval: row.checkpointInterval,
    description: row.description
  }
  dialogVisible.value = true
}

const viewYaml = async (row) => {
  try {
    const result = await request.get(`/task/${row.id}/yaml`)
    if (result.success) {
      currentYaml.value = result.yaml
      yamlDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取 YAML 失败：' + error.message)
  }
}

const copyYaml = () => {
  navigator.clipboard.writeText(currentYaml.value)
  ElMessage.success('YAML 已复制到剪贴板')
}

const startTask = async (id) => {
  try {
    await ElMessageBox.confirm('确定要启动此任务吗？', '提示', { type: 'warning' })
    const result = await request.post(`/task/${id}/start`)
    if (result.success) {
      ElMessage.success('任务已启动')
      loadTasks()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    if (error.message !== 'cancel') {
      ElMessage.error('启动失败：' + error.message)
    }
  }
}

const stopTask = async (id) => {
  try {
    await ElMessageBox.confirm('确定要停止此任务吗？', '提示', { type: 'warning' })
    const result = await request.post(`/task/${id}/stop`)
    if (result.success) {
      ElMessage.success('任务已停止')
      loadTasks()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    if (error.message !== 'cancel') {
      ElMessage.error('停止失败：' + error.message)
    }
  }
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    
    const url = form.value.id ? '/task/update' : '/task/add'
    const result = await request.post(url, form.value)
    
    if (result.success) {
      ElMessage.success(form.value.id ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadTasks()
      // 显示生成的 YAML
      if (result.yaml) {
        currentYaml.value = result.yaml
        yamlDialogVisible.value = true
      }
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    submitting.value = false
  }
}

const deleteTask = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除此任务吗？', '提示', { type: 'warning' })
    
    const result = await request.delete(`/task/delete/${id}`)
    if (result.success) {
      ElMessage.success('删除成功')
      loadTasks()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    if (error.message !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

const onSourceChange = (sourceId) => {
  const source = sources.value.find(s => s.id === sourceId)
  if (source && source.role === 'BOTH') {
    // 如果数据源是 BOTH 类型，自动填充目标数据库
    form.value.targetDatabase = source.name.replace('MySQL', 'Doris')
  }
}

onMounted(() => {
  loadTasks()
  loadConnections()
})
</script>

<style scoped>
.sync-tasks {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
}

.btn-text {
  margin-left: 4px;
}

.route-flow {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

.arrow-icon {
  color: #409EFF;
}

.form-tip {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

.yaml-preview {
  position: relative;
}

.yaml-preview .tip {
  background: #f0f9ff;
  border: 1px solid #bae0ff;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0958d9;
  font-size: 13px;
}

.yaml-code {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 6px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.5;
  max-height: 500px;
  overflow: auto;
}

.action-buttons {
  display: flex;
  gap: 4px;
}

.responsive-table {
  background: white;
  border-radius: 4px;
}

/* 移动端优化 */
@media screen and (max-width: 768px) {
  .route-text {
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .btn-text {
    display: none;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 4px;
  }
  
  .action-buttons .el-button {
    width: 100%;
  }
  
  .responsive-table ::v-deep(.el-table__header-wrapper) {
    font-size: 12px;
  }
  
  .responsive-table ::v-deep(.el-table__body-wrapper) {
    font-size: 12px;
  }
  
  ::v-deep(.el-dialog) {
    width: 95% !important;
    margin: 10px auto !important;
  }
}
</style>
