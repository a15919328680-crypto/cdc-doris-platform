# CDC 数据同步运维管理平台 - 使用文档

## 目录

1. [平台简介](#平台简介)
2. [技术架构](#技术架构)
3. [快速开始](#快速开始)
4. [功能说明](#功能说明)
5. [操作指南](#操作指南)
6. [常见问题](#常见问题)

---

## 平台简介

CDC 数据同步运维管理平台是一款轻量级的 MySQL 到 Doris 实时数据同步工具，基于 Flink CDC 技术实现零延迟的数据同步。

### 核心特性

- **零中间件**: 无需 Kafka、Paimon 等中间件，单机全混部
- **精确一次**: 基于 Checkpoint 实现 Exactly-Once 语义
- **断点续传**: 自动保存同步位点，故障自动恢复
- **可视化**: 简洁商务风格的 Web 管理界面
- **K3s 集成**: 基于 Kubernetes 部署 Flink 任务

---

## 技术架构

### 架构图

```
┌──────────────┐     ┌─────────────┐     ┌──────────────┐
│   MySQL      │ ──> │ Flink CDC   │ ──> │ Apache Doris │
│  (数据源)    │     │ (同步引擎)  │     │  (目标库)    │
└──────────────┘     └─────────────┘     └──────────────┘
                            │
                      ┌─────▼────────┐
                      │   K3s        │
                      │  (容器底座)  │
                      └──────────────┘
       ┌─────────────────────────────────┐
       │   SpringBoot + Vue3 管理平台     │
       │   - 任务配置                     │
       │   - 运维监控                     │
       │   - 日志查看                     │
       └─────────────────────────────────┘
```

### 技术栈

- **后端**: SpringBoot 2.7.18 + MyBatis
- **前端**: Vue 3 + Element Plus + ECharts
- **同步引擎**: Flink 1.17.0 + Flink CDC 2.3.0
- **容器**: K3s (Kubernetes)
- **数据库**: MySQL 8.0

---

## 快速开始

### 环境要求

- CentOS 7.9+ / Ubuntu 20.04+
- CPU: 4 核以上
- 内存：16GB 以上
- 硬盘：100GB 以上
- Java 11+
- Docker 20.10+

### 在线体验

**前端预览**: https://8081-18b558eccbf688d5.monkeycode-ai.online

**后端 API**: https://8080-18b558eccbf688d5.monkeycode-ai.online

**API 列表**:
- `GET /api/mysql/list` - 获取 MySQL 数据源列表
- `POST /api/mysql/add` - 添加 MySQL 数据源
- `GET /api/doris/list` - 获取 Doris 集群列表
- `POST /api/doris/add` - 添加 Doris 集群
- `GET /api/task/list` - 获取同步任务列表
- `POST /api/task/add` - 创建同步任务
- `POST /api/task/start/{id}` - 启动任务
- `POST /api/task/stop/{id}` - 停止任务
- `POST /api/task/restart/{id}` - 重启任务
- `POST /api/task/reset/{id}` - 重置 Checkpoint
- `GET /api/task/log/{id}` - 获取任务日志

### 一键部署

```bash
# 下载部署脚本
cd /tmp
wget https://your-repo/cdc-doris-platform/deploy.sh

# 执行部署
sudo bash deploy.sh

# 部署完成后访问
# http://<服务器 IP>
```

### 手动部署

#### 1. 安装 K3s

```bash
curl -sfL https://get.k3s.io | sh -
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
```

#### 2. 安装 Flink

```bash
cd /opt
wget https://archive.apache.org/dist/flink/flink-1.17.0/flink-1.17.0-bin-scala_2.12.tgz
tar -xzf flink-1.17.0-bin-scala_2.12.tgz
ln -s flink-1.17.0 flink
```

#### 3. 构建项目

```bash
# 构建后端
cd backend
mvn clean package -DskipTests

# 构建 Flink Job
cd ../flink-job
mvn clean package
```

#### 4. 初始化数据库

```bash
mysql -u root -p < database/schema.sql
```

#### 5. 启动服务

```bash
# 启动后端
cd backend
java -jar target/cdc-doris-platform-1.0.0.jar

# 访问
http://localhost:8080
```

---

## 功能说明

### 1. 首页大盘

- 任务统计卡片：总任务数、运行中、已停止、异常任务
- 同步数据总量趋势图
- 最近运行日志
- 任务状态列表

### 2. MySQL 数据源管理

支持配置多个 MySQL 数据源：
- 新增/编辑/删除数据源
- 连接测试
- 查看库表结构

### 3. Doris 目标库管理

支持配置多个 Doris 集群：
- FE 节点配置
- 账号密码管理
- 数据库/表浏览

### 4. 同步任务管理（核心）

支持单表一对一同步任务的全生命周期管理：

#### 创建任务
1. 选择源 MySQL 数据源
2. 选择源数据库和表
3. 选择目标 Doris 库和表
4. 填写任务名称和描述

#### 任务操作

| 操作 | 说明 |
|------|------|
| 启动 | 提交任务至 K3s 运行 |
| 优雅停止 | 保存 Checkpoint 位点后停止 |
| 强制停止 | 立即停止，清空位点 |
| 重启 | 从上次位点继续同步 |
| 重置位点 | 重新全量同步 |
| 查看日志 | 实时查看 Flink 任务日志 |
| 编辑 | 修改任务配置 |
| 删除 | 删除任务配置 |

### 5. 运行监控

- 任务延迟监控
- Checkpoint 位点查看
- Pod 状态监控
- 告警提示

### 6. 系统配置

- K3s 集群地址
- Flink 镜像配置
- 内存资源配置
- 一键环境检测

---

## 操作指南

### 创建第一个同步任务

#### 步骤 1: 配置 MySQL 数据源

1. 进入 **MySQL 数据源** 页面
2. 点击 **新增数据源**
3. 填写信息：
   - 名称: `mysql-source-1`
   - 主机地址：`192.168.1.100`
   - 端口：`3306`
   - 用户名/密码
4. 点击 **测试连接** 验证
5. 点击 **确定** 保存

#### 步骤 2: 配置 Doris 目标库

1. 进入 **Doris 目标库** 页面
2. 点击 **新增目标库**
3. 填写信息：
   - 名称：`doris-target-1`
   - FE 节点：`192.168.1.101:8030`
   - 用户名/密码
4. 点击 **测试连接** 验证
5. 点击 **确定** 保存

#### 步骤 3: 创建同步任务

1. 进入 **同步任务管理** 页面
2. 点击 **新建任务**
3. 填写信息：
   - 任务名称：`sync_user_table`
   - 源数据源：选择 `mysql-source-1`
   - 源数据库：`test_db`
   - 源表名：`user_info`
   - 目标库：选择 `doris-target-1`
   - 目标数据库：`warehouse_db`
   - 目标表名：`user_info_dwd`
4. 点击 **确定** 保存

#### 步骤 4: 启动任务

1. 在任务列表中找到刚创建的任务
2. 点击 **启动** 按钮
3. 观察任务状态变为 **运行中**
4. 点击 **日志** 查看同步详情

### 任务运维操作

#### 优雅停止（推荐）

保留 Checkpoint 位点，下次启动从断点继续：

```
任务列表 -> 运行中任务 -> 点击"运行中"下拉菜单 -> 选择"优雅停止"
```

#### 强制停止

立即停止，清空位点：

```
任务列表 -> 运行中任务 -> 点击"运行中"下拉菜单 -> 选择"强制停止"
```

#### 重置位点

清空位点后重新全量同步：

```
任务列表 -> 选择任务 -> 点击"重置位点" -> 确认
```

#### 查看日志

```
任务列表 -> 选择任务 -> 点击"日志"按钮
```

---

## 常见问题

### Q1: 任务启动失败

**现象**: 点击启动后任务状态变为 FAILED

**排查步骤**:
1. 检查 K3s 集群状态：`kubectl get pods -n cdc-platform`
2. 查看 Flink 日志：在任务页面点击"日志"
3. 检查 MySQL/ Doris 连接是否正常
4. 验证数据源配置是否正确

**常见原因**:
- K3s 未正常运行
- Flink 镜像拉取失败
- 网络不通导致无法连接数据库

### Q2: 同步延迟较高

**现象**: 监控显示同步延迟>5 秒

**解决方案**:
1. 增加 TaskManager 内存（系统配置）
2. 检查 MySQL Binlog 格式是否为 ROW
3. 检查 Doris 写入压力
4. 优化网络延迟

### Q3: 数据不一致

**现象**: 源表和目标表数据量不一致

**排查步骤**:
1. 检查任务是否发生过强制停止
2. 查看 Flink Checkpoint 是否正常
3. 验证 Doris 表结构是否匹配
4. 使用"重置位点"功能重新全量同步

### Q4: 平台启动失败

**现象**: 访问页面 404 或后端无法连接

**解决方案**:
1. 检查后端进程：`ps aux | grep cdc-doris-platform`
2. 查看后端日志：`tail -f /opt/cdc-platform/logs/backend.log`
3. 检查数据库连接配置
4. 确认端口未被占用

### Q5: K3s Flink Pod 无法创建

**现象**: Flink 任务提交后 Pod 一直 Pending

**排查步骤**:
1. 查看集群资源：`kubectl describe nodes`
2. 检查 ServiceAccount 权限：`kubectl get sa flink -n cdc-platform`
3. 查看事件日志：`kubectl get events -n cdc-platform`

---

## 附录

### 配置文件说明

#### backend/src/main/resources/application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cdc_platform
    username: root
    password: your_password

cdc:
  flink:
    home: /opt/flink
    jar-path: /opt/cdc-doris-job/cdc-doris-job-1.0.0-jar-with-dependencies.jar
```

### 数据库表结构

- `mysql_data_source`: MySQL 数据源配置
- `doris_target`: Doris 目标库配置
- `sync_task`: 同步任务配置
- `task_log`: 任务运行日志
- `system_config`: 系统配置

### 管理命令

```bash
# 启动后端
/opt/cdc-platform/start-backend.sh

# 查看后端进程
ps aux | grep cdc-doris-platform

# 查看后端日志
tail -f /opt/cdc-platform/logs/backend.log

# 重启 Flink 任务
kubectl delete pod -n cdc-platform -l app=flink

# 查看 K3s 集群状态
kubectl get pods -n cdc-platform
```

---

## 技术支持

如有问题，请联系技术支持团队或提交 Issue。

**版本**: 1.0.0  
**更新日期**: 2024-01-15
