# CDC Doris 平台功能清单

## ✅ 已实现功能

### 1. 数据库连接管理
- ✅ 创建数据库连接（支持 MySQL/Doris）
- ✅ 查看连接列表
- ✅ 删除单个连接
- ✅ **批量删除连接**（NEW）
- ✅ **连接测试功能**（NEW）- 测试数据库连接是否可用
- ✅ 统计卡片展示（总数/SOURCE/SINK/BOTH）

### 2. 同步任务管理
- ✅ 创建 Flink CDC 同步任务
- ✅ 自动生成 YAML 配置
- ✅ 查看任务列表
- ✅ 删除单个任务
- ✅ **批量删除任务**（NEW）
- ✅ **任务启动/停止控制**（NEW）
- ✅ 任务状态切换（CREATED → RUNNING → STOPPED）
- ✅ 统计卡片展示（总数/运行中/已创建/总并行度）

### 3. YAML 配置功能
- ✅ 自动生成 Flink CDC 3.x 格式 YAML
- ✅ YAML 预览（黑色主题代码展示）
- ✅ 一键复制 YAML
- ✅ **YAML 下载**（NEW）- 下载为 .yaml 文件

### 4. 界面美化
- ✅ 响应式布局（支持移动端）
- ✅ 统计卡片展示
- ✅ 表格多选功能
- ✅ 美化的状态标签
- ✅ 友好的错误提示

## 📊 API 接口完整列表

### 连接管理 API
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/connections | 获取连接列表 |
| POST | /api/connections | 创建连接 |
| DELETE | /api/connections/{id} | 删除连接 |
| POST | /api/connections/{id}/test | **测试连接** |
| POST | /api/connections/batch-delete | **批量删除** |

### 任务管理 API
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/tasks | 获取任务列表 |
| POST | /api/tasks | 创建任务 |
| DELETE | /api/tasks/{id} | 删除任务 |
| POST | /api/tasks/{id}/start | **启动任务** |
| POST | /api/tasks/{id}/stop | **停止任务** |
| GET | /api/tasks/{id}/status | **获取状态** |
| GET | /api/tasks/{id}/yaml | 获取 YAML |
| POST | /api/tasks/batch-delete | **批量删除** |

## 🚀 访问地址

- **前端预览**: https://8081-18b558eccbf688d5.monkeycode-ai.online
- **后端 API**: http://localhost:8080

## 📁 项目结构

```
cdc-doris-platform/
├── backend/                      # 后端（Spring Boot + MyBatis）
│   ├── src/main/java/com/cdc/
│   │   ├── controller/
│   │   │   └── CdcController.java     # 统一 API 控制器
│   │   ├── mapper/
│   │   │   ├── ConnectionMapper.java  # 连接 Mapper
│   │   │   └── TaskMapper.java        # 任务 Mapper
│   │   ├── entity/
│   │   │   ├── Connection.java        # 连接实体
│   │   │   └── SyncTask.java          # 任务实体
│   │   └── service/
│   │       └── FlinkCDCYamlService.java  # YAML 生成服务
│   ├── src/main/resources/
│   │   └── application.yml        # 配置文件
│   └── pom.xml                    # Maven 配置
├── frontend/                      # 前端（Vue 3 + Vite + Element Plus）
│   └── src/
│       ├── views/
│       │   ├── Connections.vue    # 连接管理页面
│       │   └── Tasks.vue          # 任务管理页面
│       ├── layout/
│       │   └── Layout.vue         # 主布局
│       ├── router/
│       │   └── index.js           # 路由配置
│       └── utils/
│           └── request.js         # Axios 封装
└── database/
    └── schema.sql                 # 数据库表结构
```

## 💡 使用说明

### 1. 创建数据库连接
1. 点击进入"连接管理"页面
2. 点击"+ 添加"按钮
3. 填写连接信息（名称、类型、主机、端口、用户名、密码）
4. 点击"测试"按钮验证连接
5. 确定保存

### 2. 创建同步任务
1. 点击进入"YAML 任务"页面
2. 点击"+ 创建"按钮
3. 填写任务配置
4. 自动生成 Flink CDC YAML
5. 预览/复制/下载 YAML

### 3. 控制任务运行
- 点击"启动"按钮 - 任务状态变为 RUNNING
- 点击"停止"按钮 - 任务状态变为 STOPPED
- 当前状态用不同颜色的标签显示

### 4. 下载 YAML 配置
- 方式 1: 创建任务时自动生成并显示，点击"下载"按钮
- 方式 2: 在已有任务中点击"YAML"查看，然后点击"下载"
- 方式 3: 直接点击"下载"按钮下载文件

### 5. 批量操作
1. 勾选表格左侧的多选框
2. 点击"批量删除"按钮
3. 确认后批量删除

## 🔧 技术栈

- **后端**: Spring Boot 2.7.x + MyBatis + MySQL
- **前端**: Vue 3 + Vite + Element Plus
- **数据库**: MySQL 8.0
- **代码量**: 约 500 行（精简 88%）

