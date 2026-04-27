<template>
  <div class="page-container">
    <div class="header">
      <div>
        <h1 class="page-title">JAR 包管理</h1>
        <p class="page-desc">上传和管理 Flink Connector JAR 包</p>
      </div>
      <el-upload
        :http-request="uploadJar"
        :show-file-list="false"
        :before-upload="beforeUpload"
        accept=".jar"
      >
        <el-button type="primary" size="large" class="add-btn">
          <el-icon><Upload /></el-icon>
          <span>上传 JAR</span>
        </el-button>
      </el-upload>
    </div>

    <!-- JAR 包列表 -->
    <div class="table-card">
      <el-table :data="list" style="width:100%">
        <el-table-column prop="name" label="文件名" min-width="200" />
        <el-table-column label="文件大小" width="120">
          <template #default="{row}">{{formatSize(row.file_size)}}</template>
        </el-table-column>
        <el-table-column prop="flink_version" label="Flink 版本" width="120" />
        <el-table-column prop="checksum" label="SHA256" width="300" show-overflow-tooltip />
        <el-table-column prop="upload_time" label="上传时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="danger" plain @click="del(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])

const formatSize = (bytes) => {
  if (!bytes) return '-'
  const kb = bytes / 1024
  if (kb < 1024) return kb.toFixed(1) + ' KB'
  return (kb / 1024).toFixed(1) + ' MB'
}

const beforeUpload = (file) => {
  if (!file.name.endsWith('.jar')) {
    ElMessage.error('只能上传 JAR 文件')
    return false
  }
  return true
}

const uploadJar = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  
  try {
    const res = await request.post('/jars/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.success) {
      ElMessage.success('上传成功')
      load()
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

const load = async () => {
  list.value = await request.get('/jars')
}

const del = async (id) => {
  await ElMessageBox.confirm('确定删除此 JAR 包？', '提示', { type: 'warning' })
  await request.delete(`/jars/${id}`)
  ElMessage.success('删除成功')
  load()
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
.table-card { background: #fff; border-radius: 16px; padding: 24px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); border: 1px solid #f1f5f9; }
</style>
