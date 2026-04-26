<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>YAML 任务</h2>
      <el-button type="primary" size="small" @click="showAdd=true">+ 创建</el-button>
    </div>
    <el-table :data="list" style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="taskName" label="任务名称" />
      <el-table-column label="源→目标" width="200">
        <template #default="{row}">{{row.source_database}} → {{row.target_database}}</template>
      </el-table-column>
      <el-table-column prop="parallelism" label="并行度" width="80" />
      <el-table-column prop="status" label="状态" width="100"><template #default="{row}"><el-tag size="small">{{row.status}}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{row}">
          <el-button size="small" @click="viewYaml(row.id)">YAML</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAdd" title="创建任务" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="任务名称"><el-input v-model="form.taskName" /></el-form-item>
        <el-form-item label="数据源 ID"><el-input-number v-model="form.sourceId" :min="1" /></el-form-item>
        <el-form-item label="目标库 ID"><el-input-number v-model="form.targetId" :min="1" /></el-form-item>
        <el-form-item label="源数据库"><el-input v-model="form.sourceDatabase" /></el-form-item>
        <el-form-item label="目标数据库"><el-input v-model="form.targetDatabase" /></el-form-item>
        <el-form-item label="并行度"><el-input-number v-model="form.parallelism" :min="1" :max="10" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showAdd=false">取消</el-button><el-button type="primary" @click="submit">生成 YAML</el-button></template>
    </el-dialog>

    <el-dialog v-model="showYaml" title="YAML 配置" width="800px">
      <pre style="background:#1e1e1e;color:#d4d4d4;padding:16px;border-radius:6px;overflow:auto;max-height:500px">{{currentYaml}}</pre>
      <template #footer><el-button @click="showYaml=false">关闭</el-button><el-button type="primary" @click="copy">复制</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const list = ref([])
const showAdd = ref(false)
const showYaml = ref(false)
const currentYaml = ref('')
const form = ref({ taskName: '', sourceId: 1, targetId: 2, sourceDatabase: '', targetDatabase: '', parallelism: 2 })

const load = async () => { list.value = await request.get('/tasks') }
const submit = async () => {
  const res = await request.post('/tasks', form.value)
  if (res.success) {
    ElMessage.success('创建成功')
    currentYaml.value = res.yaml
    showYaml.value = true
    showAdd.value = false
    load()
  }
}
const viewYaml = async (id) => {
  const res = await request.get(`/tasks/${id}/yaml`)
  if (res.success) { currentYaml.value = res.yaml; showYaml.value = true }
}
const del = async (id) => { await request.delete(`/tasks/${id}`); load() }
const copy = () => { navigator.clipboard.writeText(currentYaml.value); ElMessage.success('已复制') }

onMounted(load)
</script>
