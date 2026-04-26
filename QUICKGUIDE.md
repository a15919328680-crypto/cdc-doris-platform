# CDC 数据同步平台 - 快速指南

## 五分钟快速开始

### 1. 环境检查

```bash
# 确保以下工具已安装
java -version      # Java 11+
mvn -version       # Maven 3+
node -v            # Node.js 18+
docker --version   # Docker 20.10+
```

### 2. 一键启动

```bash
# 进入项目目录
cd cdc-doris-platform

# 执行快速启动脚本
chmod +x quickstart.sh
./quickstart.sh
```

### 3. 访问平台

```
前端地址：http://localhost:3000
后端地址：http://localhost:8080
```

---

## 十分钟配置同步任务

### 步骤 1: 配置 MySQL 数据源（2 分钟）

1. 访问 http://localhost:3000
2. 点击左侧菜单 **MySQL 数据源**
3. 点击 **新增数据源**
4. 填写信息：
   - 名称：`test-mysql`
   - 主机：`192.168.1.100`
   - 端口：`3306`
   - 用户名/密码
5. 点击 **测试连接** 验证
6. 点击 **确定**

### 步骤 2: 配置 Doris 目标库（2 分钟）

1. 点击左侧菜单 **Doris 目标库**
2. 点击 **新增目标库**
3. 填写信息：
   - 名称：`test-doris`
   - FE 节点：`192.168.1.101:8030`
   - 用户名/密码
4. 点击 **测试连接** 验证
5. 点击 **确定**

### 步骤 3: 创建同步任务（3 分钟）

1. 点击左侧菜单 **同步任务管理**
2. 点击 **新建任务**
3. 填写：
   - 任务名称：`sync-users`
   - 源数据源：选择 `test-mysql`
   - 源数据库：`test_db`
   - 源表名：`users`
   - 目标库：选择 `test-doris`
   - 目标数据库：`warehouse`
   - 目标表名：`users_dwd`
4. 点击 **确定**

### 步骤 4: 启动任务（3 分钟）

1. 在任务列表找到 `sync-users`
2. 点击 **启动** 按钮
3. 等待 3 秒，状态变为 **运行中**
4. 点击 **日志** 查看同步详情
5. 完成！

---

## 常用运维操作

### 查看任务状态

```bash
# 方法 1: 平台查看
首页 -> 任务状态卡片

# 方法 2: API 查看
curl http://localhost:8080/api/task/list
```

### 停止任务

```
同步任务管理 -> 运行中的任务 -> 点击"运行中" -> 选择操作
- 优雅停止：保留位点，下次继续
- 强制停止：清空位点
```

### 重启任务

```
同步任务管理 -> 选择任务 -> 点击"重启"
```

### 重置位点（重新全量同步）

```
同步任务管理 -> 选择任务 -> 点击"重置位点" -> 确认
```

### 查看日志

```bash
# 方法 1: 平台查看
同步任务管理 -> 选择任务 -> 点击"日志"

# 方法 2: 命令行查看
tail -f /opt/cdc-platform/logs/backend.log

# 方法 3: Flink 原生日志
flink logs <job-id>
```

---

## 故障排查速查

### 问题 1: 任务启动失败

```bash
# 1. 检查 K3s
kubectl get pods -n cdc-platform

# 2. 查看错误日志
curl http://localhost:8080/api/task/logs/<task-id>

# 3. 检查数据库连接
mysql -h <mysql-host> -u <user> -p

# 4. 检查 Doris 连接
mysql -h <doris-fe> -P 9030 -u <user> -p
```

### 问题 2: 同步延迟高

```bash
# 1. 查看任务状态
curl http://localhost:8080/api/task/stats/overview

# 2. 检查 Flink 资源
kubectl describe pod -n cdc-platform -l app=flink

# 3. 检查 MySQL Binlog
mysql -e "SHOW BINARY LOGS;"

# 4. 检查网络延迟
ping <doris-fe>
```

### 问题 3: 数据不一致

```bash
# 1. 检查源表数据量
mysql -e "SELECT COUNT(*) FROM db.table;"

# 2. 检查目标表数据量
mysql -h <doris> -e "SELECT COUNT(*) FROM db.table;"

# 3. 重置位点重新同步
同步任务管理 -> 重置位点
```

### 问题 4: 平台无法访问

```bash
# 1. 检查后端进程
ps aux | grep cdc-doris-platform

# 2. 检查端口
netstat -tlnp | grep 8080

# 3. 重启后端
./stop-backend.sh
./start-backend.sh

# 4. 查看日志
tail -100 logs/backend.log
```

---

## 常用命令速查

### 后端管理

```bash
# 启动
./start-backend.sh

# 停止
./stop-backend.sh

# 查看进程
ps aux | grep cdc-doris-platform

# 查看日志
tail -f logs/backend.log
```

### 前端管理

```bash
# 开发模式
cd frontend
npm run dev

# 构建生产版本
cd frontend
npm run build
```

### K3s 管理

```bash
# 查看 Pods
kubectl get pods -n cdc-platform

# 查看日志
kubectl logs -f <pod-name> -n cdc-platform

# 重启 Pod
kubectl delete pod -n cdc-platform -l app=flink

# 查看事件
kubectl get events -n cdc-platform
```

### Flink 管理

```bash
# 列出运行中的任务
flink list -r

# 列出所有任务
flink list -a

# 停止任务
flink stop <job-id>

# 取消任务
flink cancel <job-id>

# 查看日志
flink logs <job-id>
```

---

## 配置文件速查

### 后端配置 (application.yml)

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cdc_platform
    username: root
    password: your_password

# Flink 配置
cdc:
  flink:
    home: /opt/flink
    jar-path: /opt/cdc-doris-job/cdc-doris-job.jar
    kubernetes:
      image: apache/flink:1.17.0
      namespace: cdc-platform
      jobmanager-memory: 1G
      taskmanager-memory: 2G
```

### 前端代理 (vite.config.js)

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### Nginx 配置

```nginx
server {
    listen 80;
    
    location / {
        root /opt/cdc-platform/frontend;
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
    }
}
```

---

## 端口速查

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80 | Web 访问入口 |
| SpringBoot | 8080 | 后端 API |
| MySQL | 3306 | 数据源 |
| Doris FE | 8030 | FE HTTP 端口 |
| Doris BE | 9030 | BE MySQL 协议端口 |
| K3s | 6443 | Kubernetes API |
| Flink UI | 8081 | Flink Web UI（如独立部署） |

---

## 监控检查清单

### 每日检查

- [ ] 任务都处于 RUNNING 状态
- [ ] 同步延迟 < 1 秒
- [ ] Checkpoint 正常保存
- [ ] 无 ERROR 级别日志
- [ ] 服务器资源正常

### 每周检查

- [ ] 磁盘空间充足
- [ ] 数据库空间充足
- [ ] 日志文件大小正常
- [ ] 备份数据库

### 每月检查

- [ ] 更新系统补丁
- [ ] 清理历史日志
- [ ] 性能测试
- [ ] 灾难恢复演练

---

## 性能调优速查

### 增加 Flink 内存

```yaml
# application.yml
cdc:
  flink:
    kubernetes:
      jobmanager-memory: 2G   # 原来 1G
      taskmanager-memory: 4G  # 原来 2G
```

### 调整 Checkpoint 间隔

```java
// CdcDorisJob.java
env.enableCheckpointing(10000);  // 10 秒（默认 5 秒）
```

### 增加并行度

```java
// CdcDorisJob.java
env.setParallelism(2);  // 根据数据量调整
```

---

## 安全建议

### 密码管理

```bash
# 定期修改数据库密码
mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';"

# 修改 Doris 密码
mysql -h <fe> -P 9030 -e "SET PASSWORD FOR 'root' = PASSWORD('new_password');"
```

### 网络隔离

```bash
# 配置防火墙
firewall-cmd --add-port=80/tcp --permanent
firewall-cmd --add-port=8080/tcp --permanent
firewall-cmd --reload

# 限制访问来源
iptables -A INPUT -s 192.168.1.0/24 -p tcp --dport 8080 -j ACCEPT
```

---

## 备份恢复

### 备份数据库

```bash
mysqldump -u root -p cdc_platform > backup_$(date +%Y%m%d).sql
```

### 恢复数据库

```bash
mysql -u root -p cdc_platform < backup_20240115.sql
```

### 备份配置文件

```bash
tar -czf config_backup.tar.gz \
    /opt/cdc-platform/backend/target/*.yml \
    /etc/nginx/conf.d/cdc-platform.conf
```

---

## 快速参考卡片

```
┌─────────────────────────────────────────┐
│        CDC 数据同步平台快速参考          │
├─────────────────────────────────────────┤
│                                         │
│ 访问地址：http://<服务器IP>             │
│                                         │
│ 启动后端：./start-backend.sh            │
│ 停止后端：./stop-backend.sh             │
│ 查看日志：tail -f logs/backend.log      │
│                                         │
│ 常用 API:                                │
│ - GET  /api/task/list                   │
│ - POST /api/task/start/<id>             │
│ - POST /api/task/stop/<id>?graceful=true│
│ - GET  /api/task/logs/<id>              │
│                                         │
│ 常用命令：                               │
│ kubectl get pods -n cdc-platform        │
│ flink list -r                           │
│                                         │
│ 紧急联系：技术支持团队                  │
│                                         │
└─────────────────────────────────────────┘
```

---

## 更多资源

- 完整文档：README.md
- 部署教程：DEPLOYMENT.md
- 项目总结：PROJECT_SUMMARY.md
- 源码目录：backend/, frontend/, flink-job/

---

**版本**: 1.0.0  
**更新**: 2024-01-15  
**快速指南到此结束，祝使用愉快！**
