<template>
    <div class="page-container">
      <div class="header">
        <div>
          <h1 class="page-title">同步任务</h1>
          <p class="page-desc">Flink CDC 配置生成与管控</p>
        </div>
        <div style="display:flex;gap:12px;align-items:center">
          <a href="http://localhost:8083" target="_blank" style="text-decoration:none">
            <el-button type="info" size="large">
              <el-icon><Monitor /></el-icon>
              <span>Flink UI</span>
            </el-button>
          </a>
          <el-button type="primary" size="large" @click="showAdd=true" class="add-btn">
            <el-icon><Plus /></el-icon>
            <span>创建任务</span>
          </el-button>
        </div>
      </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card total">
        <div class="stat-icon"><el-icon><Document /></el-icon></div>
        <div class="stat-content">
          <div class="stat-label">任务总数</div>
          <div class="stat-value">{{list.length}}</div>
        </div>
      </div>
      <div class="stat-card running">
        <div class="stat-icon"><el-icon><VideoPlay /></el-icon></div>
        <div class="stat-content">
          <div class="stat-label">运行中</div>
          <div class="stat-value">{{list.filter(x=>x.status==='RUNNING').length}}</div>
        </div>
      </div>
      <div class="stat-card created">
        <div class="stat-icon"><el-icon><File /></el-icon></div>
        <div class="stat-content">
          <div class="stat-label">已创建</div>
          <div class="stat-value">{{list.filter(x=>x.status==='CREATED').length}}</div>
        </div>
      </div>
      <div class="stat-card error">
        <div class="stat-icon"><el-icon><Warning /></el-icon></div>
        <div class="stat-content">
          <div class="stat-label">异常</div>
          <div class="stat-value">{{list.filter(x=>x.status==='ERROR').length}}</div>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <div class="card-header">
        <h3>任务列表</h3>
        <div>
          <el-button type="danger" :disabled="selectedIds.length===0" @click="batchDel" plain>
            批量删除 ({{selectedIds.length}})
          </el-button>
        </div>
      </div>
      <el-table :data="list" style="width:100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="任务 ID" width="80" />
        <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="Flink 任务" min-width="240">
          <template #default="{row}">
            <div style="display:flex;flex-direction:column;gap:6px">
              <div style="display:flex;align-items:center;gap:6px" v-if="row.flink_cluster_id">
                <el-tag size="small" type="success" effect="flat">
                  <el-icon><Server /></el-icon>
                  <span style="margin-left:4px">{{getClusterName(row.flink_cluster_id)}}</span>
                </el-tag>
              </div>
              <div v-if="row.flink_job_id" style="font-family:monospace;font-size:12px;color:#555">
                <el-tag size="small" type="info" effect="plain">{{row.flink_job_id}}</el-tag>
              </div>
              <span v-else style="font-size:12px;color:#999">未提交</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数据流向" min-width="180">
          <template #default="{row}">
            <div style="display:flex;flex-direction:column;gap:6px">
              <div style="display:flex;align-items:center;gap:6px">
                <el-tag size="small" type="warning" effect="light">{{row.source_database}}</el-tag>
                <span style="font-size:12px;color:#999">→</span>
                <el-tag size="small" type="success" effect="light">{{row.target_database}}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="同步表" min-width="120">
          <template #default="{row}">
            <el-tag size="small" type="info">{{row.sourceTable || '.*'}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="parallelism" label="并行度" width="80" align="center" />
        <el-table-column label="Checkpoint" width="100" align="center">
          <template #default="{row}">
            <el-tag size="small" :type="row.checkpointEnabled ? 'success' : 'info'" effect="flat">
              {{row.checkpointEnabled ? '已启用' : '未启用'}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{row}">
            <el-tag size="small" :type="row.status==='RUNNING'?'success':row.status==='ERROR'?'danger':row.status==='STOPPED'?'info':'warning'" effect="light">
              {{row.status}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="420" fixed="right">
          <template #default="{row}">
            <div style="display:flex;gap:6px;flex-wrap:wrap">
              <el-button size="small" :type="row.status==='RUNNING'?'warning':'success'" @click="toggleTask(row)" :loading="row.loading">
                {{row.status==='RUNNING'?'停止':'启动'}}
              </el-button>
              <el-button size="small" type="primary" @click="viewDetail(row.id)" title="查看详情">
                <el-icon><View /></el-icon>
              </el-button>
              <el-button size="small" type="primary" @click="editTask(row.id)" title="编辑">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-dropdown trigger="click" @command="(cmd) => handleCheckpoint(cmd, row)">
                <el-button size="small" type="success">
                  <el-icon><Camera /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="checkpoint">触发 Checkpoint</el-dropdown-item>
                    <el-dropdown-item command="savepoint">触发 Savepoint</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button size="small" type="primary" @click="viewYaml(row.id)" title="YAML">
                <el-icon><Document /></el-icon>
              </el-button>
              <el-button size="small" type="info" @click="viewLogs(row.id)" title="查看日志">
                <el-icon><Document /></el-icon>
              </el-button>
              <el-button size="small" type="danger" plain @click="del(row.id)" title="删除">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建任务对话框 - 专业流程 -->
    <el-dialog v-model="showAdd" title="创建数据同步任务" width="720px" :close-on-click-modal="false">
      <el-steps :active="currentStep" finish-status="success" align-center style="margin-bottom:24px">
        <el-step title="数据源" description="选择源端连接" />
        <el-step title="数据库" description="选择源数据库" />
        <el-step title="数据表" description="选择同步表" />
        <el-step title="目标库" description="选择目标端" />
        <el-step title="配置" description="任务配置" />
      </el-steps>

      <el-form :model="form" label-width="120px" label-position="left">
        <!-- 步骤 1：选择数据源 -->
        <div v-if="currentStep === 0">
          <el-alert type="info" show-icon style="margin-bottom:20px">
            <template #title>请选择数据源连接</template>
          </el-alert>
          <el-form-item label="数据源" required>
            <el-select v-model="form.sourceId" placeholder="请选择数据源" style="width:100%" @change="onSourceChange">
              <el-option v-for="c in sources" :key="c.id" :label="c.name" :value="c.id">
                <div style="display:flex;justify-content:space-between;align-items:center">
                  <div>
                    <div style="font-weight:500">{{c.name}}</div>
                    <div style="font-size:12px;color:#8492a6">{{c.host}}:{{c.port}}</div>
                  </div>
                  <el-tag size="small" :type="c.role==='SOURCE'?'warning':c.role==='SINK'?'success':''" effect="flat">{{c.role}}</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <div v-if="sourceDatabases.loading" style="text-align:center;padding:20px">
            <el-icon class="is-loading"><Loading /></el-icon>
            <div style="margin-top:8px;color:#8492a6">正在获取数据库列表...</div>
          </div>
          <el-form-item label="源数据库" v-else-if="sourceDatabases.list.length > 0" required>
            <el-select v-model="form.sourceDatabase" placeholder="请选择数据库" style="width:100%" @change="onSourceDbChange" filterable>
              <el-option v-for="db in sourceDatabases.list" :key="db" :label="db" :value="db" />
            </el-select>
          </el-form-item>
        </div>

        <!-- 步骤 2：选择数据表 -->
        <div v-if="currentStep === 1">
          <el-alert type="info" show-icon style="margin-bottom:20px">
            <template #title>
              已选择：{{getSourceName(form.sourceId)}} → {{form.sourceDatabase}}
            </template>
          </el-alert>
          <el-form-item label="同步模式" required>
            <el-radio-group v-model="form.tableMode">
              <el-radio label="ALL">整库同步</el-radio>
              <el-radio label="CUSTOM">指定表</el-radio>
            </el-radio-group>
          </el-form-item>
          <div v-if="form.tableMode === 'CUSTOM'">
            <div v-if="tables.loading" style="text-align:center;padding:20px">
              <el-icon class="is-loading"><Loading /></el-icon>
              <div style="margin-top:8px;color:#8492a6">正在获取表列表...</div>
            </div>
            <el-form-item label="数据表" v-else-if="tables.list.length > 0" required>
              <el-select v-model="form.sourceTable" placeholder="请选择数据表（支持多选）" style="width:100%" multiple filterable allow-create default-first-option>
                <el-option v-for="tbl in tables.list" :key="tbl" :label="tbl" :value="tbl" />
              </el-select>
              <div style="font-size:12px;color:#8492a6;margin-top:8px">提示：可以手动输入表名，多个表用逗号分隔</div>
            </el-form-item>
            <el-alert v-else type="warning" show-icon>该数据库没有找到表</el-alert>
          </div>
          <el-alert v-else type="success" show-icon>将同步整库所有表</el-alert>
        </div>

        <!-- 步骤 3：选择目标库 -->
        <div v-if="currentStep === 2">
          <el-alert type="success" show-icon style="margin-bottom:20px">
            <template #title>
              源端：{{getSourceName(form.sourceId)}} → {{form.sourceDatabase}} {{form.tableMode==='ALL'?'(整库)':form.sourceTable}}
            </template>
          </el-alert>
          <el-form-item label="目标数据源" required>
            <el-select v-model="form.targetId" placeholder="请选择目标库" style="width:100%" @change="onTargetChange">
              <el-option v-for="c in targets" :key="c.id" :label="c.name" :value="c.id">
                <div style="display:flex;justify-content:space-between;align-items:center">
                  <div>
                    <div style="font-weight:500">{{c.name}}</div>
                    <div style="font-size:12px;color:#8492a6">{{c.host}}:{{c.port}}</div>
                  </div>
                  <el-tag size="small" :type="c.role==='SINK'?'success':c.role==='SOURCE'?'warning':''" effect="flat">{{c.role}}</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="目标数据库" v-if="targetDatabases.list.length > 0" required>
            <el-select v-model="form.targetDatabase" placeholder="请选择目标数据库" style="width:100%" filterable>
              <el-option v-for="db in targetDatabases.list" :key="db" :label="db" :value="db" />
            </el-select>
            <div style="font-size:12px;color:#8492a6;margin-top:8px">提示：可以输入新数据库名自动创建</div>
          </el-form-item>
          <el-form-item label="目标数据库" v-else>
            <el-input v-model="form.targetDatabase" placeholder="输入目标数据库名称" />
            <div style="font-size:12px;color:#8492a6;margin-top:8px">提示：目标连接暂不可用，将手动创建数据库</div>
          </el-form-item>
        </div>

        <!-- 步骤 4：任务配置 -->
        <div v-if="currentStep === 3">
          <el-alert type="success" show-icon style="margin-bottom:20px">
            <template #title>
              {{getSourceName(form.sourceId)}}.{{form.sourceDatabase}} → {{getTargetName(form.targetId)}}.{{form.targetDatabase}}
            </template>
          </el-alert>
          <el-form-item label="任务名称" required>
            <el-input v-model="form.taskName" placeholder="例如：用户数据同步" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="并行度" required>
                <el-input-number v-model="form.parallelism" :min="1" :max="20" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Checkpoint">
                <el-switch v-model="form.checkpointEnabled" active-text="启用" inactive-text="禁用" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="Checkpoint 间隔" v-if="form.checkpointEnabled">
            <el-input-number v-model="form.checkpointInterval" :step="60000" :min="60000" style="width:100%" />
            <span style="margin-left:12px;color:#8492a6">毫秒 (默认 300000ms = 5 分钟)</span>
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="showAdd=false" v-if="currentStep > 0">取消</el-button>
        <el-button @click="prevStep" v-if="currentStep > 0">上一步</el-button>
        <el-button type="primary" @click="nextStep" :disabled="!canNext">
          {{currentStep === steps.length - 1 ? '生成 YAML' : '下一步'}}
        </el-button>
      </template>
    </el-dialog>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="showDetail" title="任务详情" width="900px" :fullscreen="isMobile">
      <el-descriptions :column="2" border v-if="currentTask">
        <el-descriptions-item label="任务 ID">{{currentTask.id}}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{currentTask.taskName}}</el-descriptions-item>
        <el-descriptions-item label="数据源">{{getSourceName(currentTask.source_id)}}</el-descriptions-item>
        <el-descriptions-item label="目标库">{{getTargetName(currentTask.target_id)}}</el-descriptions-item>
        <el-descriptions-item label="源数据库">{{currentTask.sourceDatabase}}</el-descriptions-item>
        <el-descriptions-item label="目标数据库">{{currentTask.targetDatabase}}</el-descriptions-item>
        <el-descriptions-item label="表名">{{currentTask.sourceTable || '.*'}}</el-descriptions-item>
        <el-descriptions-item label="并行度">{{currentTask.parallelism}}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentTask.status==='RUNNING'?'success':'warning'">{{currentTask.status}}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{currentTask.createTime}}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{currentTask.lastUpdate}}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentTask.errorMessage">
          <el-alert type="error" :title="currentTask.errorMessage" show-icon />
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="showDetail=false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑任务对话框 - 编辑完整 YAML -->
    <el-dialog v-model="showEdit" title="编辑任务配置" width="1000px" :fullscreen="isMobile" :close-on-click-modal="false">
      <el-alert type="info" show-icon style="margin-bottom:16px">
        <template #title>直接编辑 YAML 配置，修改后将重新生成任务</template>
      </el-alert>
      <div style="border:1px solid #e2e8f0;border-radius:8px;overflow:hidden;background:#1e1e1e">
        <textarea 
          id="yaml-editor"
          v-model="editYaml"
          style="width:100%;height:500px;font-family:'Monaco','Menlo','Consolas',monospace;font-size:13px;background:#1e1e1e;color:#d4d4d4;border:none;padding:16px;resize:none;outline:none"
          spellcheck="false"
        ></textarea>
      </div>
      <template #footer>
        <el-button @click="showEdit=false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存并更新</el-button>
      </template>
    </el-dialog>

    <!-- YAML 预览对话框 -->
    <el-dialog v-model="showYaml" title="YAML 配置预览" width="960px" :fullscreen="isMobile">
      <div style="margin-bottom:16px;display:flex;justify-content:space-between;align-items:center">
        <div style="display:flex;align-items:center;gap:12px">
          <el-tag type="info" size="small">Flink CDC 3.x</el-tag>
          <span style="color:#64748b;font-size:13px">config.yaml</span>
        </div>
        <div style="display:flex;gap:8px">
          <el-button size="small" @click="copy"><el-icon><DocumentCopy /></el-icon> 复制</el-button>
        </div>
      </div>
      <div style="border:1px solid #e2e8f0;border-radius:8px;overflow:hidden;background:#1e1e1e">
        <pre style="margin:0;padding:20px;overflow:auto;max-height:600px;text-align:left"><code v-html="highlightedYaml"></code></pre>
      </div>
    </el-dialog>

    <!-- 启动确认对话框 -->
    <el-dialog v-model="showStartConfirm" title="启动任务" width="700px" :close-on-click-modal="false">
      <el-alert type="info" show-icon style="margin-bottom:20px">
        <template #title>选择 Flink 集群和启动参数</template>
      </el-alert>
      <el-form :model="startOptions" label-width="140px">
        <el-form-item label="Flink 集群" required>
          <el-select v-model="startOptions.clusterId" placeholder="选择要提交的 Flink 集群" style="width:100%" @change="onClusterChange">
            <el-option v-for="c in clusters" :key="c.id" :label="c.name" :value="c.id">
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span>{{c.name}}</span>
                <el-tag size="small" :type="c.status==='CONNECTED'?'success':'warning'" effect="flat">{{c.status}}</el-tag>
              </div>
            </el-option>
          </el-select>
          <div style="font-size:12px;color:#8492a6;margin-top:4px">
            <span v-if="selectedCluster">部署模式：{{selectedCluster.deploy_mode}} | 版本：{{selectedCluster.version || '未知'}}</span>
          </div>
        </el-form-item>
        
        <el-form-item label="启动模式">
          <el-radio-group v-model="startOptions.restoreMode">
            <el-radio label="initial">从头开始</el-radio>
            <el-radio label="checkpoint">从 Checkpoint 恢复</el-radio>
            <el-radio label="savepoint">从 Savepoint 恢复</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="Checkpoint ID" v-if="startOptions.restoreMode === 'checkpoint'">
          <el-select v-model="startOptions.checkpointId" placeholder="选择 Checkpoint" style="width:100%">
            <el-option v-for="c in checkpointList" :key="c.id" :label="`Checkpoint ${c.id}`" :value="c.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="Savepoint 路径" v-if="startOptions.restoreMode === 'savepoint'">
          <el-input v-model="startOptions.savepointPath" placeholder="/savepoints/task-name-timestamp" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showStartConfirm=false">取消</el-button>
        <el-button type="primary" @click="confirmStart" :loading="starting">启动任务</el-button>
      </template>
    </el-dialog>

    <!-- 日志查看对话框 -->
    <el-dialog v-model="showLogs" title="任务运行日志" width="1000px" :fullscreen="isMobile">
      <el-tabs v-model="activeLogTab">
        <el-tab-pane label="运行日志 (启停记录)" name="run">
          <el-table :data="logList.runLogs" style="width:100%" max-height="500" empty-text="暂无运行日志">
            <el-table-column prop="timestamp" label="时间" width="180" />
            <el-table-column label="类型" width="100">
              <template #default="{row}">
                <el-tag :type="row.event_type === 'START' ? 'success' : row.event_type === 'STOP' ? 'warning' : row.event_type === 'CHECKPOINT' ? 'primary' : 'info'" size="small">
                  {{row.event_type}}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="logLevel" label="级别" width="80" />
            <el-table-column prop="message" label="日志内容" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="错误日志" name="error">
          <el-table :data="logList.errorLogs" style="width:100%" max-height="500" empty-text="暂无错误日志">
            <el-table-column prop="timestamp" label="时间" width="180" />
            <el-table-column label="错误类型" width="120">
              <template #default="{row}">
                <el-tag type="danger" size="small">ERROR</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="错误信息" min-width="200" />
            <el-table-column label="操作" width="100">
              <template #default="{row}">
                <el-button size="small" @click="viewStackTrace(row)">查看堆栈</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button type="primary" @click="showLogs=false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 堆栈跟踪对话框 -->
    <el-dialog v-model="showStackTrace" title="错误堆栈跟踪" width="900px" :fullscreen="isMobile">
      <pre style="background:#1e1e1e;color:#d4d4d4;padding:20px;border-radius:8px;overflow:auto;max-height:500px;font-family:'Monaco','Menlo','Consolas',monospace;font-size:12px">{{currentStackTrace}}</pre>
      <template #footer>
        <el-button type="primary" @click="showStackTrace=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const connections = ref([])
const showAdd = ref(false)
const showEdit = ref(false)
const showDetail = ref(false)
const showYaml = ref(false)
const showStartConfirm = ref(false)
const showLogs = ref(false)
const showStackTrace = ref(false)
const currentYaml = ref('')
const editYaml = ref('')
const currentTask = ref(null)
const currentActionRow = ref(null)
const currentStackTrace = ref('')
const selectedIds = ref([])
const checkpointList = ref([])
const logList = ref({ runLogs: [], errorLogs: [] })
const activeLogTab = ref('run')
const isMobile = ref(window.innerWidth <= 768)
const clusters = ref([])
const selectedCluster = ref(null)
const starting = ref(false)

const startOptions = ref({
  clusterId: null,
  restoreMode: 'initial',
  checkpointId: null,
  savepointPath: null
})

// 步骤
const steps = ['source', 'database', 'table', 'target', 'config']
const currentStep = ref(0)

// 表单
const form = ref({
  taskName: '',
  sourceId: null,
  sourceDatabase: '',
  sourceTable: '.*',
  targetId: null,
  targetDatabase: '',
  parallelism: 2,
  checkpointEnabled: true,
  checkpointInterval: 300000,
  tableMode: 'ALL' // ALL 或 CUSTOM
})

// 数据库和表列表
const sourceDatabases = ref({ list: [], loading: false })
const targetDatabases = ref({ list: [], loading: false })
const tables = ref({ list: [], loading: false })

const sources = computed(() => connections.value.filter(c => c.role === 'SOURCE' || c.role === 'BOTH'))
const targets = computed(() => connections.value.filter(c => c.role === 'SINK' || c.role === 'BOTH'))

const canNext = computed(() => {
  if (currentStep.value === 0) return form.value.sourceId && form.value.sourceDatabase
  if (currentStep.value === 1) return form.value.tableMode === 'ALL' || form.value.sourceTable
  if (currentStep.value === 2) return form.value.targetId && form.value.targetDatabase
  if (currentStep.value === 3) return form.value.taskName
  return true
})

const getSourceName = (id) => connections.value.find(c => c.id === id)?.name || '未知'
const getTargetName = (id) => connections.value.find(c => c.id === id)?.name || '未知'
const getClusterName = (id) => clusters.value.find(c => c.id === id)?.name || '未知'

// 获取数据库列表（模拟，实际需要后端 API 支持）
const fetchDatabases = async (connId) => {
  // TODO: 需要后端实现获取数据库列表的 API
  // const res = await request.get(`/connections/${connId}/databases`)
  // return res.databases
  
  // 临时模拟数据
  return ['shop_db', 'order_db', 'user_db', 'product_db']
}

// 获取表列表（模拟，实际需要后端 API 支持）
const fetchTables = async (connId, database) => {
  // TODO: 需要后端实现获取表列表的 API
  // const res = await request.get(`/connections/${connId}/tables?database=${database}`)
  // return res.tables
  
  // 临时模拟数据
  return ['users', 'orders', 'products', 'categories', 'inventory']
}

// 数据源变化
const onSourceChange = async () => {
  sourceDatabases.value.loading = true
  try {
    const dbs = await fetchDatabases(form.value.sourceId)
    sourceDatabases.value.list = dbs
    form.value.sourceDatabase = ''
  } catch (e) {
    ElMessage.error('获取数据库列表失败')
  } finally {
    sourceDatabases.value.loading = false
  }
}

// 源数据库变化
const onSourceDbChange = async () => {
  tables.value.loading = true
  try {
    const tablesList = await fetchTables(form.value.sourceId, form.value.sourceDatabase)
    tables.value.list = tablesList
  } catch (e) {
    ElMessage.error('获取表列表失败')
  } finally {
    tables.value.loading = false
  }
}

// 目标源变化
const onTargetChange = async () => {
  targetDatabases.value.loading = true
  try {
    const dbs = await fetchDatabases(form.value.targetId)
    targetDatabases.value.list = dbs
    form.value.targetDatabase = ''
  } catch (e) {
    targetDatabases.value.list = []
  } finally {
    targetDatabases.value.loading = false
  }
}

const nextStep = async () => {
  if (currentStep.value < steps.length - 1) {
    currentStep.value++
  } else {
    await submit()
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const submit = async () => {
  const tableValue = form.value.tableMode === 'ALL' ? '.*' : 
    (Array.isArray(form.value.sourceTable) ? form.value.sourceTable.join(',') : form.value.sourceTable)
  
  const payload = {
    taskName: form.value.taskName,
    sourceId: form.value.sourceId,
    sourceDatabase: form.value.sourceDatabase,
    sourceTable: tableValue,
    targetId: form.value.targetId,
    targetDatabase: form.value.targetDatabase,
    parallelism: form.value.parallelism,
    checkpointEnabled: form.value.checkpointEnabled,
    checkpointInterval: form.value.checkpointInterval
  }
  
  const res = await request.post('/tasks', payload)
  if (res.success) {
    ElMessage.success('创建成功')
    showAdd.value = false
    resetForm()
    load()
  } else {
    ElMessage.error(res.message)
  }
}

const resetForm = () => {
  form.value = {
    taskName: '',
    sourceId: null,
    sourceDatabase: '',
    sourceTable: '.*',
    targetId: null,
    targetDatabase: '',
    parallelism: 2,
    checkpointEnabled: true,
    checkpointInterval: 300000,
    tableMode: 'ALL'
  }
  currentStep.value = 0
  sourceDatabases.value.list = []
  targetDatabases.value.list = []
  tables.value.list = []
}

const load = async () => { 
  list.value = await request.get('/tasks')
  connections.value = await request.get('/connections')
  clusters.value = await request.get('/clusters')
}

const handleSelectionChange = (val) => { selectedIds.value = val.map(x => x.id) }

const toggleTask = async (row) => {
  if (row.status === 'RUNNING') {
    // 停止任务
    await ElMessageBox.confirm('确定要停止此任务吗？', '提示', {
      type: 'warning',
      cancelButtonText: '取消',
      confirmButtonText: '停止',
      distinguishCancelAndClose: true
    }).then(async () => {
      const res = await request.post(`/tasks/${row.id}/stop`, { createSavepoint: true })
      if (res.success) {
        ElMessage.success('任务已停止' + (res.savepointPath ? `，Savepoint: ${res.savepointPath}` : ''))
        load()
      } else {
        ElMessage.error(res.message)
      }
    }).catch(() => {})
  } else if (row.status === 'STOPPED' || row.status === 'CREATED') {
    // 启动任务 - 加载集群列表并显示确认对话框
    currentActionRow.value = row
    startOptions.value = {
      clusterId: null,
      restoreMode: 'initial',
      checkpointId: null,
      savepointPath: null
    }
    selectedCluster.value = null
    showStartConfirm.value = true
  } else if (row.status === 'ERROR') {
    ElMessage.warning('任务处于错误状态，请先查看错误日志')
  }
}

const onClusterChange = (clusterId) => {
  selectedCluster.value = clusters.value.find(c => c.id === clusterId)
}

const confirmStart = async () => {
  if (!startOptions.value.clusterId) {
    ElMessage.warning('请选择 Flink 集群')
    return
  }
  
  const payload = {
    clusterId: startOptions.value.clusterId
  }
  if (startOptions.value.restoreMode !== 'initial') {
    payload.restoreMode = startOptions.value.restoreMode
    if (startOptions.value.checkpointId) payload.checkpointId = startOptions.value.checkpointId
    if (startOptions.value.savepointPath) payload.savepointPath = startOptions.value.savepointPath
  }
  
  starting.value = true
  try {
    const res = await request.post(`/tasks/${currentActionRow.value.id}/start`, payload)
    if (res.success) {
      ElMessage.success('任务已启动' + (startOptions.value.restoreMode !== 'initial' ? '（断点续传）' : ''))
      showStartConfirm.value = false
      load()
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('启动失败：' + e.message)
  } finally {
    starting.value = false
  }
}

const viewLogs = async (id) => {
  const res = await request.get(`/tasks/${id}/logs`)
  if (res.success) {
    logList.value = {
      runLogs: res.runLogs || [],
      errorLogs: res.errorLogs || []
    }
    activeLogTab.value = 'run'
    showLogs.value = true
  } else {
    ElMessage.error(res.message)
  }
}

const viewStackTrace = (row) => {
  currentStackTrace.value = row.stackTrace || '无堆栈信息'
  showStackTrace.value = true
}

const viewDetail = async (id) => {
  try {
    const res = await request.get(`/tasks/${id}/detail`)
    if (res.success) {
      currentTask.value = res.task
      currentTask.value.checkpointEnabled = !!res.task.checkpointEnabled
      showDetail.value = true
      // 加载 Checkpoint 历史（模拟数据，需要后端 API 支持）
      checkpointList.value = []
    }
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

const editTask = async (id) => {
  try {
    const res = await request.get(`/tasks/${id}/yaml`)
    if (res.success) {
      editYaml.value = res.yaml
      currentTask.value = { id }
      showEdit.value = true
    }
  } catch (e) {
    ElMessage.error('获取任务配置失败')
  }
}

const saveEdit = async () => {
  try {
    const res = await request.put(`/tasks/${currentTask.value.id}/yaml`, { yamlConfig: editYaml.value })
    if (res.success) {
      ElMessage.success('配置已更新')
      showEdit.value = false
      load()
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const viewYaml = async (id) => {
  const res = await request.get(`/tasks/${id}/yaml`)
  if (res.success) { currentYaml.value = res.yaml; showYaml.value = true }
}

const del = async (id) => {
  await ElMessageBox.confirm('确定删除此任务？', '提示', { type: 'warning' })
  await request.delete(`/tasks/${id}`)
  ElMessage.success('删除成功')
  load()
}

const batchDel = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 项？`, '警告', { type: 'warning' })
  await request.post('/tasks/batch-delete', selectedIds.value)
  ElMessage.success('删除成功')
  load()
}

const handleCheckpoint = async (command, row) => {
  if (command === 'checkpoint') {
    const res = await request.post(`/tasks/${row.id}/checkpoint`)
    if (res.success) ElMessage.success(`Checkpoint 已触发，ID: ${res.checkpointId}`)
    else ElMessage.error(res.message)
  } else if (command === 'savepoint') {
    const res = await request.post(`/tasks/${row.id}/savepoint`, { path: `/savepoints/${row.taskName}-${Date.now()}` })
    if (res.success) ElMessage.success(`Savepoint 已触发：${res.savepointPath}`)
    else ElMessage.error(res.message)
  }
}

const copy = () => { navigator.clipboard.writeText(currentYaml.value); ElMessage.success('已复制') }

const highlightedYaml = computed(() => {
  if (!currentYaml.value) return ''
  return currentYaml.value
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/^(source|sink|route|pipeline):/gm, '<span style="color:#569cd6;font-weight:bold">$1:</span>')
    .replace(/^(  type|  hostname|  port|  username|  password|  tables|  server-id|  server-time-zone|  fenodes|  default-database|  table\\.create\\.properties\\.[^:]+|  name|  parallelism|  source-table|  sink-table):/gm, '<span style="color:#9cdcfe">$1:</span>')
    .replace(/: (.*)$/gm, ': <span style="color:#ce9178">$1</span>')
})

onMounted(() => {
  window.addEventListener('resize', () => isMobile.value = window.innerWidth <= 768)
  load()
})
</script>

<style scoped>
/* 复用之前的样式 */
.page-container { max-width: 1400px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; flex-wrap: wrap; gap: 16px; }
.page-title { font-size: 28px; font-weight: 700; color: #1e293b; margin: 0 0 4px 0; }
.page-desc { color: #64748b; font-size: 14px; margin: 0; }
.add-btn { background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); border: none; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3); }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 20px; margin-bottom: 24px; }
.stat-card { background: #fff; border-radius: 16px; padding: 24px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); transition: all 0.3s ease; border: 1px solid #f1f5f9; }
.stat-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08); }
.stat-icon { width: 56px; height: 56px; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 26px; }
.stat-card.total .stat-icon { background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff; }
.stat-card.running .stat-icon { background: linear-gradient(135deg, #10b981, #059669); color: #fff; }
.stat-card.created .stat-icon { background: linear-gradient(135deg, #64748b, #475569); color: #fff; }
.stat-card.error .stat-icon { background: linear-gradient(135deg, #ef4444, #dc2626); color: #fff; }
.stat-content { flex: 1; }
.stat-label { color: #64748b; font-size: 13px; margin-bottom: 8px; }
.stat-value { font-size: 32px; font-weight: 700; color: #1e293b; }
.table-card { background: #fff; border-radius: 16px; padding: 24px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); border: 1px solid #f1f5f9; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.card-header h3 { margin: 0; font-size: 18px; color: #1e293b; }
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
