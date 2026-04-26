<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>数据库连接</h2>
      <el-button type="primary" size="small" @click="showAdd = true">+ 添加</el-button>
    </div>
    <el-table :data="list" style="width:100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{row}"><el-tag size="small" :type="row.type==='MYSQL'?'warning':'success'">{{row.type}}</el-tag></template>
      </el-table-column>
      <el-table-column prop="host" label="主机" />
      <el-table-column prop="port" label="端口" width="70" />
      <el-table-column prop="role" label="角色" width="80">
        <template #default="{row}"><el-tag size="small" effect="plain">{{row.role}}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{row}"><el-button size="small" type="danger" @click="del(row.id)">删除</el-button></template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAdd" title="添加连接" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type"><el-option label="MySQL" value="MYSQL" /><el-option label="Doris" value="DORIS" /></el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role"><el-option label="SOURCE" value="SOURCE" /><el-option label="TARGET" value="TARGET" /><el-option label="BOTH" value="BOTH" /></el-select>
        </el-form-item>
        <el-form-item label="主机"><el-input v-model="form.host" /></el-form-item>
        <el-form-item label="端口"><el-input-number v-model="form.port" :min="1" :max="65535" /></el-form-item>
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showAdd=false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const list = ref([])
const showAdd = ref(false)
const form = ref({ name: '', type: 'MYSQL', host: 'localhost', port: 3306, username: 'root', password: '', role: 'BOTH' })

const load = async () => { list.value = await request.get('/connections') }
const submit = async () => {
  await request.post('/connections', form.value)
  ElMessage.success('添加成功')
  showAdd.value = false
  load()
}
const del = async (id) => { await request.delete(`/connections/${id}`); load() }

onMounted(load)
</script>
