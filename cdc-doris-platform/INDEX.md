# CDC 数据同步平台 - 项目文件索引

## 项目结构总览

```
cdc-doris-platform/
│
├── 📄 README.md                    # 使用文档（主文档）
├── 📄 DEPLOYMENT.md                # 部署教程（详细版）
├── 📄 PROJECT_SUMMARY.md           # 项目总结
├── 📄 QUICKGUIDE.md                # 快速指南
├── 📄 INDEX.md                     # 本文件 - 文件索引
├── 📄 .gitignore                   # Git 忽略文件
│
├── 🔧 deploy.sh                    # 一键部署脚本（生产环境）
├── 🔧 quickstart.sh                # 快速启动脚本（开发环境）
│
├── 📁 backend/                     # SpringBoot 后端应用
│   ├── pom.xml                     # Maven 配置
│   ├── src/
│   │   └── main/java/com/cdc/
│   │       ├── CdcDorisPlatformApplication.java    # 主启动类
│   │       ├── config/
│   │       │   └── FlinkKubernetesConfig.java      # Flink K8s 配置
│   │       ├── controller/
│   │       │   ├── MysqlDataSourceController.java  # MySQL 数据源 API
│   │       │   ├── DorisTargetController.java      # Doris 目标库 API
│   │       │   └── SyncTaskController.java         # 同步任务 API
│   │       ├── entity/
│   │       │   ├── MysqlDataSource.java            # MySQL 数据源实体
│   │       │   ├── DorisTarget.java                # Doris 目标库实体
│   │       │   └── SyncTask.java                   # 同步任务实体
│   │       ├── mapper/
│   │       │   ├── MysqlDataSourceMapper.java      # MySQL Mapper
│   │       │   ├── DorisTargetMapper.java          # Doris Mapper
│   │       │   └── SyncTaskMapper.java             # 任务 Mapper
│   │       └── service/
│   │           ├── DatabaseTestService.java        # 数据库测试服务
│   │           └── FlinkTaskService.java           # Flink 任务服务
│   └── src/main/resources/
│       └── application.yml                         # 后端配置文件
│
├── 📁 flink-job/                   # Flink CDC 同步作业
│   ├── pom.xml                     # Maven 配置
│   └── src/main/java/com/cdc/
│       └── CdcDorisJob.java        # 核心同步代码
│
├── 📁 frontend/                    # Vue3 前端应用
│   ├── package.json                # Node.js 依赖
│   ├── vite.config.js              # Vite 构建配置
│   ├── index.html                  # 入口 HTML
│   └── src/
│       ├── main.js                 # Vue 入口
│       ├── App.vue                 # 根组件
│       ├── api/
│       │   └── index.js            # API 封装
│       ├── layout/
│       │   └── Layout.vue          # 主布局
│       ├── router/
│       │   └── index.js            # 路由配置
│       └── views/
│           ├── dashboard/
│           │   └── Index.vue       # 首页大盘
│           ├── mysql/
│           │   └── Index.vue       # MySQL 数据源
│           ├── doris/
│           │   └── Index.vue       # Doris 目标库
│           ├── task/
│           │   └── Index.vue       # 同步任务管理
│           ├── monitor/
│           │   └── Index.vue       # 运行监控
│           └── system/
│               └── Index.vue       # 系统配置
│
└── 📁 database/                    # 数据库脚本
    └── schema.sql                  # 建表语句
```

---

## 文件说明速查

### 文档类文件

| 文件名 | 类型 | 说明 |
|--------|------|------|
| README.md | 文档 | 主文档，包含完整使用指南 |
| DEPLOYMENT.md | 文档 | 部署教程，包含详细部署步骤 |
| PROJECT_SUMMARY.md | 文档 | 项目总结，技术亮点和架构说明 |
| QUICKGUIDE.md | 文档 | 快速指南，5 分钟上手 |
| INDEX.md | 文档 | 本文件，项目文件索引 |

### 脚本类文件

| 文件名 | 类型 | 说明 |
|--------|------|------|
| deploy.sh | 脚本 | 生产环境一键部署 |
| quickstart.sh | 脚本 | 开发环境快速启动 |

### 后端文件

| 文件名 | 路径 | 说明 |
|--------|------|------|
| pom.xml | backend/ | Maven 依赖配置 |
| application.yml | backend/resources/ | 应用配置 |
| CdcDorisPlatformApplication.java | backend/java/com/cdc/ | 启动类 |
| FlinkKubernetesConfig.java | backend/config/ | Flink K8s 配置 |
| MysqlDataSourceController.java | backend/controller/ | MySQL API |
| DorisTargetController.java | backend/controller/ | Doris API |
| SyncTaskController.java | backend/controller/ | 任务 API |
| DatabaseTestService.java | backend/service/ | 测试服务 |
| FlinkTaskService.java | backend/service/ | Flink 运维服务 |

### Flink 作业文件

| 文件名 | 路径 | 说明 |
|--------|------|------|
| pom.xml | flink-job/ | Maven 依赖 |
| CdcDorisJob.java | flink-job/java/com/cdc/ | CDC 同步核心代码 |

### 前端文件

| 文件名 | 路径 | 说明 |
|--------|------|------|
| package.json | frontend/ | Node 包配置 |
| vite.config.js | frontend/ | Vite 配置 |
| main.js | frontend/src/ | Vue 入口 |
| App.vue | frontend/src/ | 根组件 |
| index.js | frontend/src/api/ | API 封装 |
| Layout.vue | frontend/src/layout/ | 布局组件 |
| index.js | frontend/src/router/ | 路由配置 |
| Index.vue | frontend/src/views/dashboard/ | 首页大盘 |
| Index.vue | frontend/src/views/mysql/ | MySQL 数据源 |
| Index.vue | frontend/src/views/doris/ | Doris 目标库 |
| Index.vue | frontend/src/views/task/ | 任务管理 |
| Index.vue | frontend/src/views/monitor/ | 监控页面 |
| Index.vue | frontend/src/views/system/ | 系统配置 |

### 数据库文件

| 文件名 | 路径 | 说明 |
|--------|------|------|
| schema.sql | database/ | 建表 SQL |

---

## 快速查找

### 我想...

#### 部署项目
👉 查看 [DEPLOYMENT.md](./DEPLOYMENT.md)  
👉 运行 `./deploy.sh` (生产环境)

#### 开发调试
👉 运行 `./quickstart.sh`  
👉 修改 `backend/src/main/resources/application.yml` 配置数据库  
👉 修改 `frontend/vite.config.js` 配置 API 代理

#### 添加 API 接口
👉 编辑 `backend/src/main/java/com/cdc/controller/` 下的控制器  
👉 添加方法  
👉 更新 `frontend/src/api/index.js`

#### 修改前端页面
👉 编辑 `frontend/src/views/` 下对应的 `.vue` 文件

#### 修改同步逻辑
👉 编辑 `flink-job/src/main/java/com/cdc/CdcDorisJob.java`

---

## 关键代码入口

### 后端入口
```
backend/src/main/java/com/cdc/CdcDorisPlatformApplication.java
```

### Flink 作业入口
```
flink-job/src/main/java/com/cdc/CdcDorisJob.java
```

### 前端入口
```
frontend/src/main.js
```

---

## API 接口列表

### MySQL 数据源 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/mysql/list | 获取所有 MySQL 数据源 |
| GET | /api/mysql/{id} | 获取单个数据源 |
| POST | /api/mysql/add | 新增数据源 |
| PUT | /api/mysql/update | 更新数据源 |
| DELETE | /api/mysql/delete/{id} | 删除数据源 |
| POST | /api/mysql/test | 测试连接 |
| GET | /api/mysql/tables/{id} | 获取表列表 |

### Doris 目标库 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/doris/list | 获取所有 Doris 目标库 |
| GET | /api/doris/{id} | 获取单个目标库 |
| POST | /api/doris/add | 新增目标库 |
| PUT | /api/doris/update | 更新目标库 |
| DELETE | /api/doris/delete/{id} | 删除目标库 |
| POST | /api/doris/test | 测试连接 |
| GET | /api/doris/databases/{id} | 获取数据库列表 |
| GET | /api/doris/tables/{id} | 获取表列表 |

### 同步任务 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/task/list | 获取所有任务 |
| GET | /api/task/{id} | 获取单个任务 |
| POST | /api/task/add | 新增任务 |
| PUT | /api/task/update | 更新任务 |
| DELETE | /api/task/delete/{id} | 删除任务 |
| POST | /api/task/start/{id} | 启动任务 |
| POST | /api/task/stop/{id} | 停止任务 |
| POST | /api/task/restart/{id} | 重启任务 |
| POST | /api/task/reset/{id} | 重置位点 |
| GET | /api/task/logs/{id} | 查看日志 |
| GET | /api/task/stats/overview | 获取统计概览 |

---

## 数据库表结构

| 表名 | 说明 | 记录内容 |
|------|------|---------|
| mysql_data_source | MySQL 数据源 | 数据源连接信息 |
| doris_target | Doris 目标库 | Doris 集群信息 |
| sync_task | 同步任务 | 任务配置和状态 |
| task_log | 任务日志 | 运行日志记录 |
| system_config | 系统配置 | 全局配置参数 |

---

## 依赖版本清单

### Java 依赖

```xml
<spring-boot.version>2.7.18</spring-boot.version>
<flink.version>1.17.0</flink.version>
<mysql-cdc.version>2.3.0</mysql-cdc.version>
<doris-connector.version>23.1.0</doris-connector.version>
<mysql-connector.version>8.0.33</mysql-connector.version>
<kubernetes-client.version>18.0.0</kubernetes-client.version>
```

### Node.js 依赖

```json
{
  "vue": "^3.3.4",
  "vue-router": "^4.2.4",
  "pinia": "^2.1.6",
  "element-plus": "^2.3.14",
  "axios": "^1.5.0",
  "echarts": "^5.4.3",
  "vite": "^4.4.9"
}
```

---

## 配置文件清单

| 配置文件 | 位置 | 用途 |
|---------|------|------|
| application.yml | backend/resources/ | SpringBoot 配置 |
| pom.xml | backend/ | 后端 Maven 依赖 |
| pom.xml | flink-job/ | Flink 作业 Maven 依赖 |
| package.json | frontend/ | 前端 Node 依赖 |
| vite.config.js | frontend/ | 前端 Vite 配置 |
| schema.sql | database/ | 数据库建表语句 |

---

## 页面路由清单

| 路径 | 页面 | 说明 |
|------|------|------|
| / | 重定向到 /dashboard | 默认首页 |
| /dashboard | 首页大盘 | 统计图表和概览 |
| /mysql-source | MySQL 数据源 | 数据源配置管理 |
| /doris-target | Doris 目标库 | 目标库配置管理 |
| /sync-task | 同步任务 | 任务管理核心页面 |
| /monitor | 运行监控 | 监控面板 |
| /system-config | 系统配置 | 全局参数配置 |

---

## 运维命令清单

### 启动服务
```bash
# 后端
cd backend && java -jar target/cdc-doris-platform-1.0.0.jar

# 前端（开发）
cd frontend && npm run dev

# 构建前端
cd frontend && npm run build
```

### Flink 命令
```bash
# 提交任务
flink run-application --target kubernetes-application ...

# 停止任务
flink stop <job-id>

# 取消任务
flink cancel <job-id>

# 查看任务
flink list -r
```

### Kubernetes 命令
```bash
# 查看 Pods
kubectl get pods -n cdc-platform

# 查看日志
kubectl logs -f <pod-name> -n cdc-platform

# 重启 Pod
kubectl delete pod -n cdc-platform -l app=flink
```

---

## 联系和支持

如有问题，请参考：
- 📖 完整文档：[README.md](./README.md)
- 🚀 部署教程：[DEPLOYMENT.md](./DEPLOYMENT.md)
- 📋 快速指南：[QUICKGUIDE.md](./QUICKGUIDE.md)
- 📊 项目总结：[PROJECT_SUMMARY.md](./PROJECT_SUMMARY.md)

---

**项目版本**: 1.0.0  
**索引更新**: 2024-01-15  
**总计文件数**: 30+  
**代码行数**: 约 5000+
