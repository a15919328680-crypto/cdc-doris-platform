# CDC Doris Platform v3.0

极简 Flink CDC 3.x YAML 任务生成工具

## 功能

- ✅ 数据库连接管理（MySQL/Doris）
- ✅ Flink CDC YAML 配置自动生成
- ✅ YAML 一键复制
- ✅ 移动端适配

## 快速开始

### 1. 数据库初始化

```bash
mysql -u root -p < database/schema.sql
```

### 2. 启动后端

```bash
cd backend && mvn clean package -DskipTests
java -jar target/cdc-doris-platform-3.0.0.jar
```

### 3. 启动前端

```bash
cd frontend && npm run dev
```

## 使用方式

1. 访问 http://localhost:8081
2. 添加 MySQL 和 Doris 连接
3. 创建同步任务，自动生成 YAML
4. 复制 YAML，使用 `flink-cdc.sh pipeline xxx.yaml` 提交

## YAML 示例

```yaml
source:
  type: mysql
  hostname: localhost
  port: 3306
  username: root
  password: xxx
  tables: db_name\.*
  server-id: 5400-5404
  server-time-zone: Asia/Shanghai

sink:
  type: doris
  fenodes: localhost:8030
  username: root
  password: ""
  default-database: doris_db
  table.create.properties.light_schema_change: true

route:
  - source-table: db_name\.*
    sink-table: doris_db.${source_table}

pipeline:
  name: sync-task
  parallelism: 2
```

## API

```
GET    /api/connections     - 获取连接列表
POST   /api/connections     - 添加连接
DELETE /api/connections/:id - 删除连接

GET    /api/tasks           - 获取任务列表
POST   /api/tasks           - 创建任务
GET    /api/tasks/:id/yaml  - 获取任务 YAML
DELETE /api/tasks/:id       - 删除任务
```

## License

MIT
