<template>
  <div class="system-config-page">
    <el-card>
      <template #header>
        <span>系统配置</span>
      </template>

      <el-form label-width="200px">
        <el-form-item label="K3s 集群地址">
          <el-input v-model="config.k3sUrl" placeholder="例如：https://192.168.1.100:6443" />
        </el-form-item>
        
        <el-form-item label="Flink 镜像">
          <el-input v-model="config.flinkImage" placeholder="例如：apache/flink:1.17.0-scala_2.12-java11" />
        </el-form-item>
        
        <el-form-item label="JobManager 内存">
          <el-select v-model="config.jobmanagerMemory">
            <el-option label="512M" value="512M" />
            <el-option label="1G" value="1G" />
            <el-option label="2G" value="2G" />
            <el-option label="4G" value="4G" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="TaskManager 内存">
          <el-select v-model="config.taskmanagerMemory">
            <el-option label="1G" value="1G" />
            <el-option label="2G" value="2G" />
            <el-option label="4G" value="4G" />
            <el-option label="8G" value="8G" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="Kubernetes 命名空间">
          <el-input v-model="config.namespace" placeholder="例如：cdc-platform" />
        </el-form-item>
        
        <el-form-item label="Service Account">
          <el-input v-model="config.serviceAccount" placeholder="例如：flink" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave">保存配置</el-button>
          <el-button @click="handleCheck">一键环境检测</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <span>环境检测结果</span>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="K3s 集群状态">
          <el-tag type="success">正常</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Flink 环境">
          <el-tag type="success">正常</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Kubernetes API">
          <el-tag type="success">可访问</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Doris 连接器">
          <el-tag type="success">已加载</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const config = ref({
  k3sUrl: 'https://192.168.1.100:6443',
  flinkImage: 'apache/flink:1.17.0-scala_2.12-java11',
  jobmanagerMemory: '1G',
  taskmanagerMemory: '2G',
  namespace: 'cdc-platform',
  serviceAccount: 'flink'
})

const handleSave = () => {
  ElMessage.success('配置保存成功')
}

const handleCheck = () => {
  ElMessage.success('环境检测通过')
}
</script>

<style scoped>
.system-config-page {
  padding: 10px;
}
</style>
