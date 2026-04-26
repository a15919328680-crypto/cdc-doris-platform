<template>
  <div class="data-sources">
    <div class="page-header">
      <h2>数据库连接</h2>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        <span class="btn-text">添加连接</span>
      </el-button>
    </div>

    <el-table :data="connections" style="width: 100%" class="responsive-table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="120" />
      <el-table-column prop="type" label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 'MYSQL' ? 'warning' : 'success'" size="small">
            {{ row.type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="80">
        <template #default="{ row }">
          <el-tag effect="plain" size="small" v-if="row.role === 'SOURCE'">数据源</el-tag>
          <el-tag effect="plain" type="success" size="small" v-else-if="row.role === 'TARGET'">目标库</el-tag>
          <el-tag effect="plain" type="info" size="small" v-else>通用</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="host" label="主机" min-width="120" />
      <el-table-column prop="port" label="端口" width="70" />
      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button size="small" @click="testConnection(row)">测试</el-button>
            <el-button size="small" type="primary" @click="showEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteConnection(row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" :fullscreen="isMobile">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="连接名称" prop="name">
          <el-input v-model="form.name" placeholder="如：本地 MySQL、Doris 集群" />
        </el-form-item>
        <el-form-item label="数据库类型" prop="type">
          <el-select v-model="form.type">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="Doris" value="DORIS" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role">
            <el-option label="数据源" value="SOURCE" />
            <el-option label="目标库" value="TARGET" />
            <el-option label="通用" value="BOTH" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机地址" prop="host">
          <el-input v-model="form.host" placeholder="如：localhost 或 192.168.1.100" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="数据库用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="数据库密码" />
          <div v-if="form.type === 'DORIS'" style="color: #999; font-size: 12px; margin-top: 4px;">
            Doris 默认密码为空
          </div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可选，用于备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const isMobile = ref(window.innerWidth < 768)
const connections = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加连接')
const submitting = ref(false)
const formRef = ref(null)

const form = ref({
  id: null,
  name: '',
  type: 'MYSQL',
  host: 'localhost',
  port: 3306,
  username: 'root',
  password: '',
  role: 'BOTH',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入连接名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

const loadConnections = async () => {
  try {
    connections.value = await request.get('/connection/list')
  } catch (error) {
    ElMessage.error('加载连接列表失败：' + error.message)
  }
}

const showAddDialog = () => {
  dialogTitle.value = '添加数据库连接'
  form.value = {
    id: null,
    name: '',
    type: 'MYSQL',
    host: 'localhost',
    port: 3306,
    username: 'root',
    password: '',
    role: 'BOTH',
    description: ''
  }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  dialogTitle.value = '编辑数据库连接'
  form.value = { ...row }
  dialogVisible.value = true
}

const testConnection = async (row) => {
  try {
    const result = await request.post('/connection/test', row)
    if (result.success) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.error('连接测试失败：' + result.message)
    }
  } catch (error) {
    ElMessage.error('连接测试失败：' + error.message)
  }
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    
    const url = form.value.id ? '/connection/update' : '/connection/add'
    const result = await request.post(url, form.value)
    
    if (result.success) {
      ElMessage.success(form.value.id ? '更新成功' : '添加成功')
      dialogVisible.value = false
      loadConnections()
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

const deleteConnection = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除此数据库连接吗？', '提示', {
      type: 'warning'
    })
    
    const result = await request.delete(`/connection/delete/${id}`)
    if (result.success) {
      ElMessage.success('删除成功')
      loadConnections()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    if (error.message !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

onMounted(() => {
  loadConnections()
})
</script>

<style scoped>
.data-sources {
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
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .btn-text {
    display: none;
  }
  
  .page-header h2 {
    font-size: 16px;
  }
  
  .responsive-table ::v-deep(.el-table__header-wrapper) {
    font-size: 12px;
  }
  
  .responsive-table ::v-deep(.el-table__body-wrapper) {
    font-size: 12px;
  }
  
  .responsive-table ::v-deep(.el-table th) {
    padding: 8px 0;
  }
  
  .responsive-table ::v-deep(.el-table td) {
    padding: 8px 0;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 4px;
  }
  
  .action-buttons .el-button {
    width: 100%;
  }
  
  ::v-deep(.el-dialog) {
    width: 95% !important;
    margin: 10px auto !important;
  }
  
  ::v-deep(.el-form-item) {
    margin-bottom: 16px;
  }
  
  ::v-deep(.el-form-item__label) {
    font-size: 13px;
  }
}
</style>
