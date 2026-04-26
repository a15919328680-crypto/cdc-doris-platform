# CDC 数据同步平台 - 打包和部署教程

## 一、环境准备

### 1.1 基础环境

```bash
# 安装 Java 11
yum install -y java-11-openjdk-devel

# 安装 Maven
yum install -y maven

# 安装 Node.js 18+
curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
yum install -y nodejs

# 安装 MySQL
yum install -y mysql-server
systemctl start mysqld
```

### 1.2 中间件环境

```bash
# 安装 Docker
yum install -y yum-utils
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io
systemctl start docker
systemctl enable docker

# 安装 K3s
curl -sfL https://get.k3s.io | sh -
systemctl start k3s
systemctl enable k3s

# 配置 kubectl
mkdir -p $HOME/.kube
cp /etc/rancher/k3s/k3s.yaml $HOME/.kube/config
chmod 600 $HOME/.kube/config

# 安装 Flink
cd /opt
wget https://archive.apache.org/dist/flink/flink-1.17.0/flink-1.17.0-bin-scala_2.12.tgz
tar -xzf flink-1.17.0-bin-scala_2.12.tgz
ln -s flink-1.17.0 flink
```

---

## 二、后端打包

### 2.1 修改配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cdc_platform?useSSL=false&serverTimezone=UTC
    username: root
    password: 你的数据库密码

cdc:
  flink:
    home: /opt/flink
    jar-path: /opt/cdc-doris-job/cdc-doris-job-1.0.0-jar-with-dependencies.jar
    kubernetes:
      namespace: cdc-platform
      service-account: flink
```

### 2.2 执行打包

```bash
cd backend
mvn clean package -DskipTests
```

### 2.3 产物位置

```
target/cdc-doris-platform-1.0.0.jar
```

---

## 三、Flink Job 打包

### 3.1 修改 pom.xml（可选）

编辑 `flink-job/pom.xml`，确认 Flink 版本和依赖。

### 3.2 执行打包

```bash
cd flink-job
mvn clean package
```

### 3.3 产物位置

```
target/cdc-doris-job-1.0.0-jar-with-dependencies.jar
```

### 3.4 部署 JAR 文件

```bash
# 创建目录
mkdir -p /opt/cdc-doris-job

# 复制 JAR 文件
cp target/cdc-doris-job-1.0.0-jar-with-dependencies.jar /opt/cdc-doris-job/
```

---

## 四、前端打包

### 4.1 修改 API 地址（生产环境）

编辑 `frontend/vite.config.js`：

```javascript
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://你的服务器 IP:8080',  // 修改为后端地址
        changeOrigin: true
      }
    }
  }
})
```

### 4.2 执行打包

```bash
cd frontend
npm install
npm run build
```

### 4.3 产物位置

```
dist/
  ├── index.html
  ├── assets/
  │   ├── index.xxxx.js
  │   ├── index.xxxx.css
  │   └── ...
```

---

## 五、数据库初始化

### 5.1 创建数据库

```bash
mysql -u root -p
```

```sql
CREATE DATABASE cdc_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 5.2 导入表结构

```bash
mysql -u root -p cdc_platform < database/schema.sql
```

### 5.3 验证表结构

```bash
mysql -u root -p cdc_platform -e "SHOW TABLES;"
```

应显示：
- mysql_data_source
- doris_target
- sync_task
- task_log
- system_config

---

## 六、部署方式

### 方式一：单机部署（推荐）

#### 6.1.1 上传文件

```bash
# 创建部署目录
mkdir -p /opt/cdc-platform
cd /opt/cdc-platform

# 上传后端 JAR
mkdir -p backend
cp /path/to/cdc-doris-platform-1.0.0.jar backend/

# 上传 Flink JAR
mkdir -p flink-job
cp /opt/cdc-doris-job/cdc-doris-job-1.0.0-jar-with-dependencies.jar flink-job/

# 上传前端
mkdir -p frontend
cp -r /path/to/frontend/dist/* frontend/
```

#### 6.1.2 启动后端

```bash
cd /opt/cdc-platform/backend
nohup java -jar cdc-doris-platform-1.0.0.jar > ../logs/backend.log 2>&1 &
echo $! > ../backend.pid
```

#### 6.1.3 配置 Nginx

```bash
cat > /etc/nginx/conf.d/cdc-platform.conf <<EOF
server {
    listen 80;
    server_name localhost;

    location / {
        root /opt/cdc-platform/frontend;
        try_files \$uri \$uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }
}
EOF

systemctl restart nginx
```

---

### 方式二：Docker 部署

#### 6.2.1 创建 Dockerfile

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

COPY backend/target/cdc-doris-platform-1.0.0.jar app.jar
COPY database/schema.sql /docker-entrypoint-initdb.d/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 6.2.2 构建镜像

```bash
docker build -t cdc-platform:1.0.0 .
```

#### 6.2.3 启动容器

```bash
docker run -d \
  --name cdc-platform \
  -p 8080:8080 \
  -v /opt/cdc-doris-job:/opt/cdc-doris-job \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/cdc_platform \
  cdc-platform:1.0.0
```

---

### 方式三：Kubernetes 部署

#### 6.3.1 创建 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cdc-platform
  namespace: cdc-platform
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cdc-platform
  template:
    metadata:
      labels:
        app: cdc-platform
    spec:
      containers:
      - name: cdc-platform
        image: cdc-platform:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:mysql://mysql-service:3306/cdc_platform"
        volumeMounts:
        - name: flink-jar
          mountPath: /opt/cdc-doris-job
      volumes:
      - name: flink-jar
        hostPath:
          path: /opt/cdc-doris-job
---
apiVersion: v1
kind: Service
metadata:
  name: cdc-platform-service
  namespace: cdc-platform
spec:
  selector:
    app: cdc-platform
  ports:
  - port: 80
    targetPort: 8080
  type: NodePort
```

#### 6.3.2 部署应用

```bash
kubectl apply -f deployment.yaml
```

---

## 七、验证部署

### 7.1 检查进程

```bash
# 后端进程
ps aux | grep cdc-doris-platform

# Flink 进程
ps aux | grep flink

# K3s 进程
systemctl status k3s
```

### 7.2 检查端口

```bash
# 后端端口
netstat -tlnp | grep 8080

# Nginx 端口
netstat -tlnp | grep 80
```

### 7.3 访问测试

```bash
# 测试后端 API
curl http://localhost:8080/api/task/stats/overview

# 测试前端
curl http://localhost
```

### 7.4 查看日志

```bash
# 后端日志
tail -f /opt/cdc-platform/logs/backend.log

# Nginx 日志
tail -f /var/log/nginx/access.log
```

---

## 八、常见问题排查

### Q1: 后端启动失败

**错误**: `java.net.ConnectException: Connection refused`

**原因**: MySQL 未启动或配置错误

**解决**:
```bash
systemctl status mysqld
# 检查 application.yml 中的数据库配置
```

### Q2: Flink 任务无法提交

**错误**: `Could not build the client for Kubernetes`

**原因**: K3s 未配置正确

**解决**:
```bash
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
kubectl get pods -n cdc-platform
```

### Q3: 前端页面空白

**原因**: API 地址配置错误

**解决**:
```bash
# 检查 vite.config.js 中的 proxy 配置
# 或检查 Nginx 配置
cat /etc/nginx/conf.d/cdc-platform.conf
```

### Q4: 404 Not Found

**原因**: 前端路由配置问题

**解决**:
```bash
# 确保 Nginx 配置了 try_files
location / {
    root /opt/cdc-platform/frontend;
    try_files $uri $uri/ /index.html;
}
```

---

## 九、性能优化建议

### 9.1 后端优化

```yaml
# application.yml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10

spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

### 9.2 Flink 优化

```yaml
# 提交任务时的参数
-Dtaskmanager.numberOfTaskSlots=2
-Dparallelism.default=1
-Dexecution.checkpointing.interval=10000ms
```

### 9.3 前端优化

```javascript
// vite.config.js
build: {
  rollupOptions: {
    output: {
      manualChunks: {
        'element-plus': ['element-plus'],
        'echarts': ['echarts']
      }
    }
  },
  chunkSizeWarningLimit: 2000
}
```

---

## 十、监控和维护

### 10.1 日志轮转

创建 `/etc/logrotate.d/cdc-platform`:

```
/opt/cdc-platform/logs/*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    create 0644 root root
}
```

### 10.2 健康检查

```bash
# 创建健康检查脚本
cat > /opt/cdc-platform/healthcheck.sh <<EOF
#!/bin/bash
curl -s http://localhost:8080/api/task/stats/overview > /dev/null
if [ $? -eq 0 ]; then
    echo "OK"
    exit 0
else
    echo "FAIL"
    exit 1
fi
EOF

chmod +x /opt/cdc-platform/healthcheck.sh

# 添加到 crontab
*/5 * * * * /opt/cdc-platform/healthcheck.sh
```

### 10.3 自动重启

创建 systemd 服务：

```bash
cat > /etc/systemd/system/cdc-platform.service <<EOF
[Unit]
Description=CDC Data Sync Platform
After=syslog.target network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/cdc-platform/backend
ExecStart=/usr/bin/java -jar cdc-doris-platform-1.0.0.jar
ExecStop=/bin/kill -15 $MAINPID
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable cdc-platform
systemctl start cdc-platform
```

---

## 十一、升级指南

### 11.1 备份

```bash
# 备份数据库
mysqldump -u root -p cdc_platform > cdc_platform_backup.sql

# 备份配置文件
cp -r /opt/cdc-platform /opt/cdc-platform.backup
```

### 11.2 升级步骤

```bash
# 停止服务
systemctl stop cdc-platform

# 替换 JAR 文件
cp new-version.jar /opt/cdc-platform/backend/

# 执行数据库迁移（如有）
mysql -u root -p cdc_platform < migration.sql

# 启动服务
systemctl start cdc-platform
```

---

## 联系方式

如有问题，请联系技术支持团队。

**版本**: 1.0.0  
**更新日期**: 2024-01-15
