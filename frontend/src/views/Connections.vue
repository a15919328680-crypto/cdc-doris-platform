<template>
  <div class="page-container">
    <div class="header">
      <div>
        <h1 class="page-title">数据库连接</h1>
        <p class="page-desc">管理 MySQL 和 Doris 数据库连接配置</p>
      </div>
      <el-button type="primary" size="large" @click="showAdd = true" class="add-btn">
        <el-icon><Plus /></el-icon>
        <span>添加连接</span>
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card total">
        <div class="stat-icon"><el-icon><Connection /></el-icon></div>
        <div class="stat-content">
          <div class="stat-label">连接总数</div>
          <div class="stat-value">{{list.length}}</div>
        </div>
      </div>
      <div class="stat-card source">
        <div class="stat-icon"><el-icon><Upload /></el-icon></div>
        <div class="stat-content">
          <div class="stat-label">SOURCE</div>
          <div class="stat-value">{{list.filter(x=>x.role==='SOURCE').length}}</div>
        </div>
      </div>
      <div class="stat-card sink">
        <div class="stat-icon"><el-icon><Download /></el-icon></div>
        <div class="stat-content">
          <div class="stat-label">SINK</div>
          <div class="stat-value">{{list.filter(x=>x.role==='SINK').length}}</div>
        </div>
      </div>
      <div class="stat-card both">
        <div class="stat-icon"><el-icon><Link /></el-icon></div>
        <div class="stat-content">
          <div class="stat-label">BOTH</div>
          <div class="stat-value">{{list.filter(x=>x.role==='BOTH').length}}</div>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <div class="card-header">
        <h3>连接列表</h3>
        <div>
          <el-button type="danger" :disabled="selectedIds.length===0" @click="batchDel">
            批量删除 ({{selectedIds.length}})
          </el-button>
        </div>
      </div>
      <el-table :data="list" style="width:100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{row}">
            <el-tag size="small" :type="row.type==='MYSQL'?'warning':'success'" effect="light">{{row.type}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机" min-width="150" />
        <el-table-column prop="port" label="端口" width="90" />
        <el-table-column prop="role" label="角色" width="90">
          <template #default="{row}">
            <el-tag size="small" :type="row.role==='SOURCE'?'':row.role==='SINK'?'success':''" effect="flat">{{row.role}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="testConn(row.id)">测试</el-button>
            <el-button size="small" type="danger" plain @click="del(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加连接对话框 -->
    <el-dialog v-model="showAdd" title="添加数据库连接" width="560px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px" label-position="left">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="例如：MySQL 生产库" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.type" style="width:100%">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="Doris" value="DORIS" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.role" style="width:100%">
            <el-option label="SOURCE (数据源)" value="SOURCE" />
            <el-option label="SINK (目标库)" value="SINK" />
            <el-option label="BOTH (两者兼有)" value="BOTH" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item label="主机" required>
              <el-input v-model="form.host" placeholder="例如：192.168.1.100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="端口" required>
              <el-input-number v-model="form.port" :min="1" :max="65535" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" required>
              <el-input v-model="form.username" placeholder="root" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" required>
              <el-input v-model="form.password" type="password" show-password placeholder="••••••" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showAdd=false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
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
const selectedIds = ref([])
const form = ref({ name: '', type: 'MYSQL', host: 'localhost', port: 3306, username: 'root', password: '', role: 'BOTH' })

const load = async () => { list.value = await request.get('/connections') }

const handleSelectionChange = (val) => {
  selectedIds.value = val.map(x => x.id)
}

const submit = async () => {
  await request.post('/connections', form.value)
  ElMessage.success('添加成功')
  showAdd.value = false
  load()
}

const testConn = async (id) => {
  try {
    const res = await request.post(`/connections/${id}/test`)
    if (res.success) ElMessage.success('✓ ' + res.message)
    else ElMessage.warning(res.message)
  } catch (e) {
    ElMessage.error('测试失败：' + e.message)
  }
}

const del = async (id) => {
  await ElMessageBox.confirm('确定删除此连接？', '提示', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  await request.delete(`/connections/${id}`)
  ElMessage.success('删除成功')
  load()
}

const batchDel = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 项？`, '警告', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  await request.post('/connections/batch-delete', selectedIds.value)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.page-desc {
  color: #64748b;
  font-size: 14px;
  margin: 0;
}

.add-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid #f1f5f9;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}

.stat-card.total .stat-icon { background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff; }
.stat-card.source .stat-icon { background: linear-gradient(135deg, #10b981, #059669); color: #fff; }
.stat-card.sink .stat-icon { background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff; }
.stat-card.both .stat-icon { background: linear-gradient(135deg, #64748b, #475569); color: #fff; }

.stat-content { flex: 1; }
.stat-label { color: #64748b; font-size: 13px; margin-bottom: 8px; }
.stat-value { font-size: 32px; font-weight: 700; color: #1e293b; }

/* 表格卡片 */
.table-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: #1e293b;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .page-title { font-size: 22px; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .stat-card { padding: 16px; }
  .stat-icon { width: 44px; height: 44px; font-size: 20px; }
  .stat-value { font-size: 24px; }
  .table-card { padding: 16px; }
}

@media (max-width: 480px) {
  .stats-grid { grid-template-columns: 1fr; }
  .header { flex-direction: column; align-items: flex-start; }
}
</style>
