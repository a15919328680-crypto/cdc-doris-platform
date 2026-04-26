# CDC Doris Platform v3.0 - Flink CDC YAML 编排

## 🎉 重大更新

**v3.0 引入 Flink CDC 3.x 的 YAML 配置管理能力**，采用声明式的 YAML 配置来编排数据同步任务，更加灵活强大。

## 📋 核心特性

### 1. YAML 配置编排

使用 Flink CDC 3.x 的 YAML 语法定义同步任务:

```yaml
source:
  type: mysql
  hostname: 192.168.1.100
  port: 3306
  username: root
  password: xxx
  tables: db_1\.*
  server-id: 5400-5404

sink:
  type: doris
  fenodes: 192.168.1.200:8030
  username: root
  password: ""
  default-database: doris_db

route:
  - source-table: db_1\.*
    sink-table: doris_db.${source_table}

pipeline:
  name: mysql-to-doris-sync
  parallelism: 2
```

### 2. 自动化 YAML 生成

通过 Web 界面创建任务时，系统会自动生成符合 Flink CDC 3.x 规范的 YAML 配置。

### 3. 支持的同步场景

- ✅ **整库同步**：自动映射源库所有表到目标库
- ✅ **选表同步**：精确指定需要同步的表
- ✅ **多表合一**：多个源表合并到一个目标表
- ✅ **分库分表**：支持正则表达式匹配多个分库分表
- ✅ **CDC 模式**：全量 + 增量实时同步

## 🚀 快速开始

### 1. 添加数据库连接

访问 **"数据库连接"** 页面，添加 MySQL 和 Doris 连接：

```json
// MySQL 数据源
{
  "name": "业务 MySQL",
  "type": "MYSQL",
  "host": "192.168.1.100",
  "port": 3306,
  "username": "app_user",
  "password": "password123",
  "role": "SOURCE"
}

// Doris 目标库
{
  "name": "Doris 集群",
  "type": "DORIS",
  "host": "192.168.1.200",
  "port": 9030,
  "username": "root",
  "password": "",
  "role": "TARGET"
}
```

### 2. 创建同步任务

访问 **"同步任务"** 页面，创建任务并生成 YAML：

**填写表单**：
- 任务名称：`order-sync`
- 数据源：选择 "业务 MySQL"
- 目标库：选择 "Doris 集群"
- 源数据库：`order_db`
- 源表：`.*` (同步所有表)
- 目标数据库：`doris_order`
- 同步模式：`CDC`
- 并行度：`2`
- Checkpoint 间隔：`60` 秒

**生成的 YAML**：
```yaml
source:
  type: mysql
  hostname: 192.168.1.100
  port: 3306
  username: app_user
  password: password123
  tables: order_db\..*
  server-id: 5400-5404
  server-time-zone: Asia/Shanghai

sink:
  type: doris
  fenodes: 192.168.1.200:8030
  username: root
  password: ""
  default-database: doris_order
  table.create.properties.light_schema_change: true

route:
  - source-table: order_db\.*
    sink-table: doris_order.${source_table}

pipeline:
  name: order-sync
  parallelism: 2
```

### 3. 提交 Flink 任务

点击 **YAML** 按钮复制配置，然后使用 flink-cdc.sh 提交：

```bash
# 保存 YAML 文件
cat > order-sync.yaml <<EOF
# 粘贴 YAML 内容
EOF

# 提交任务
cd $FLINK_CDC_HOME
./bin/flink-cdc.sh pipeline order-sync.yaml
```

## 📁 文件变更清单

### 新增文件

**后端**：
```
backend/src/main/java/com/cdc/service/FlinkCDCYamlService.java
```

**前端**：
```
frontend/src/views/SyncTasks.vue
```

**模板**：
```
templates/flink-cdc-pipeline.template.yaml
```

**数据库**：
```
database/schema-v3.yaml-support.sql
```

### 修改文件

**后端**：
- `SyncTask.java` - 新增 YAML 相关字段
- `SyncTaskMapper.java` - 更新 SQL 适配新字段
- `SyncTaskController.java` - 重写，支持 YAML 生成

**前端**：
- `router/index.js` - 更新路由
- `Dashboard.vue` - 简化展示

## 🔧 YAML 配置说明

### 源配置（Source）

| 参数 | 必填 | 说明 |
|------|------|------|
| type | 是 | 数据源类型，固定为 `mysql` |
| hostname | 是 | MySQL 主机地址 |
| port | 是 | MySQL 端口 |
| username | 是 | 用户名 |
| password | 是 | 密码 |
| tables | 是 | 表匹配模式（支持正则） |
| server-id | 是 | MySQL server-id 范围 |
| server-time-zone | 是 | 时区 |

### 汇配置（Sink）

| 参数 | 必填 | 说明 |
|------|------|------|
| type | 是 | 目标类型，固定为 `doris` |
| fenodes | 是 | Doris FE 节点地址 |
| username | 是 | Doris 用户名 |
| password | 是 | Doris 密码 |
| default-database | 是 | 默认目标数据库 |
| light_schema_change | 否 | 是否允许轻量级 schema 变更 |

### 路由配置（Route）

| 参数 | 必填 | 说明 |
|------|------|------|
| source-table | 是 | 源表匹配模式（支持正则） |
| sink-table | 是 | 目标表名，支持变量替换 |

### Pipeline 配置

| 参数 | 必填 | 说明 |
|------|------|------|
| name | 是 | 任务名称 |
| parallelism | 是 | 并行度 |

## 📊 数据库表结构

### sync_task 表（新增字段）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| task_name | VARCHAR(100) | 任务名称 |
| source_id | BIGINT | 源连接 ID |
| target_id | BIGINT | 目标连接 ID |
| **yaml_config** | TEXT | 完整的 YAML 配置 |
| **source_database_pattern** | VARCHAR(200) | 源数据库正则 |
| **source_table_pattern** | VARCHAR(200) | 源表正则 |
| **target_table_rule** | VARCHAR(200) | 目标表映射规则 |
| **parallelism** | INT | 并行度 |
| **checkpoint_interval** | INT | Checkpoint 间隔（秒） |

## 🌐 API 接口说明

### 1. 任务管理

```
GET    /api/task/list          # 获取任务列表
GET    /api/task/{id}          # 获取任务详情
POST   /api/task/add           # 创建任务
PUT    /api/task/update        # 更新任务
DELETE /api/task/delete/{id}   # 删除任务
GET    /api/task/{id}/yaml     # 获取任务的 YAML 配置
POST   /api/task/{id}/start    # 启动任务
POST   /api/task/{id}/stop     # 停止任务
```

### 2. 创建任务请求示例

```json
{
  "taskName": "order-sync",
  "sourceId": 1,
  "targetId": 2,
  "sourceDatabase": "order_db",
  "sourceTable": ".*",
  "targetDatabase": "doris_order",
  "syncMode": "CDC",
  "parallelism": 2,
  "checkpointInterval": 60,
  "description": "订单数据同步"
}
```

**响应**：
```json
{
  "success": true,
  "message": "任务创建成功",
  "data": { ... },
  "yaml": "source:\n  type: mysql\n..."
}
```

## 💡 使用示例

### 示例 1: 整库同步

**YAML**：
```yaml
source:
  type: mysql
  hostname: 192.168.1.100
  port: 3306
  username: root
  password: xxx
  tables: shop_db\.*
  server-id: 5400-5404

sink:
  type: doris
  fenodes: 192.168.1.200:8030
  username: root
  password: ""

route:
  - source-table: shop_db\.*
    sink-table: doris_shop.${source_table}

pipeline:
  name: shop-db-sync
  parallelism: 2
```

### 示例 2: 分库分表合并

**YAML**：
```yaml
source:
  type: mysql
  hostname: 192.168.1.100
  port: 3306
  username: root
  password: xxx
  tables: order_\d+\.order_\d+
  server-id: 5400-5404

sink:
  type: doris
  fenodes: 192.168.1.200:8030
  username: root
  password: ""

route:
  - source-table: order_\d+\.order_\d+
    sink-table: doris_order.ods_orders_all

pipeline:
  name: merge-orders
  parallelism: 4
```

### 示例 3: 转换和过滤

**YAML**：
```yaml
source:
  type: mysql
  hostname: 192.168.1.100
  port: 3306
  username: root
  password: xxx
  tables: shop_db\.users
  server-id: 5400-5404

sink:
  type: doris
  fenodes: 192.168.1.200:8030
  username: root
  password: ""

transformer:
  - source-table: shop_db\.users
    filters:
      - "status = 1"
    projections:
      - "id"
      - "user_name"
      - "CONCAT(first_name, ' ', last_name) AS full_name"
      - "FROM_UNIXTIME(created_at) AS create_time"

route:
  - source-table: shop_db\.users
    sink-table: doris_shop.dim_users

pipeline:
  name: user-transform
  parallelism: 2
```

## 🎯 版本对比

| 特性 | v2.0 | v3.0 |
|------|------|------|
| 配置方式 | 数据库字段 | YAML 配置 |
| 任务创建 | 表单填写 | 自动生成 YAML |
| 灵活性 | 基础 | 高（支持 transformer） |
| 分库分表 | 支持 | 原生支持 |
| Schema 变更 | 手动 | 自动（light_schema_change） |
| 可视化 | 基础 | YAML 预览 |

## ✅ 测试清单

- ✅ 数据库表结构创建
- ✅ 后端编译
- ✅ YAML 生成服务
- ✅ API 接口测试
- ✅ GitHub 推送

## 📦 在线演示

- **前端**: https://8081-18b558eccbf688d5.monkeycode-ai.online
- **后端 API**: https://8080-18b558eccbf688d5.monkeycode-ai.online
- **GitHub**: https://github.com/a15919328680-crypto/cdc-doris-platform

---

**更新时间**：2026-04-26  
**版本**：v3.0.0  
**Flink CDC 版本**：3.1.0
