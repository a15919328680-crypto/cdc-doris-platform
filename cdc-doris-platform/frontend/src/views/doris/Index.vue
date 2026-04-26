<template>
  <div class="doris-target-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Doris 目标库管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增目标库
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" width="180" />
        <el-table-column prop="feNodes" label="FE 节点" width="250" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="defaultDatabase" label="默认数据库" width="150" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleTest(row)">测试连接</el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleClose"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入目标库名称" />
        </el-form-item>
        <el-form-item label="FE 节点" prop="feNodes">
          <el-input v-model="formData.feNodes" placeholder="例如：192.168.1.100:8030" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="默认数据库">
          <el-input v-model="formData.defaultDatabase" placeholder="请输入默认数据库名" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增目标库')
const submitting = ref(false)
const formRef = ref(null)

const formData = ref({
  id: null,
  name: '',
  feNodes: '',
  username: '',
  password: '',
  defaultDatabase: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  feNodes: [{ required: true, message: '请输入 FE 节点地址', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const fetchData = async () => {
  try {
    tableData.value = await api.doris.list()
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增目标库'
  formData.value = {
    id: null,
    name: '',
    feNodes: '',
    username: '',
    password: '',
    defaultDatabase: '',
    description: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑目标库'
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该目标库吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.doris.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleTest = async (row) => {
  try {
    const res = await api.doris.test({
      feNodes: row.feNodes,
      username: row.username,
      password: row.password
    })
    if (res.success) {
      ElMessage.success('连接成功')
    } else {
      ElMessage.error('连接失败')
    }
  } catch (error) {
    ElMessage.error('连接测试失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    
    if (formData.value.id) {
      await api.doris.update(formData.value)
      ElMessage.success('更新成功')
    } else {
      await api.doris.add(formData.value)
      ElMessage.success('添加成功')
    }
    
    dialogVisible.value = false
    fetchData()
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
  fetchData()
})
</script>

<style scoped>
.doris-target-page {
  padding: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: bold;
}
</style>
