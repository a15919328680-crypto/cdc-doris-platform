#!/bin/bash

set -e

echo "========================================"
echo " CDC 数据同步运维管理平台 - 一键部署脚本"
echo "========================================"

BASE_DIR="/opt/cdc-platform"
FLINK_VERSION="1.17.0"
KUBECONFIG="$HOME/.kube/config"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否以 root 运行
if [ "$EUID" -ne 0 ]; then 
    log_error "请使用 sudo 运行此脚本"
    exit 1
fi

# 1. 安装基础依赖
log_info "正在安装基础依赖..."
yum install -y wget curl java-11-openjdk java-11-openjdk-devel maven git

# 2. 配置 Java 环境
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# 3. 创建部署目录
log_info "创建部署目录..."
mkdir -p $BASE_DIR/{backend,frontend,flink-job,database,logs}

# 4. 安装 Docker
log_info "安装 Docker..."
if ! command -v docker &> /dev/null; then
    yum install -y yum-utils
    yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    yum install -y docker-ce docker-ce-cli containerd.io
    systemctl start docker
    systemctl enable docker
fi

# 5. 安装 K3s
log_info "安装 K3s..."
if ! command -v k3s &> /dev/null; then
    curl -sfL https://get.k3s.io | sh -
    systemctl start k3s
    systemctl enable k3s
    export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
fi

# 6. 配置 kubectl
log_info "配置 kubectl..."
mkdir -p $HOME/.kube
cp /etc/rancher/k3s/k3s.yaml $HOME/.kube/config
chmod 600 $HOME/.kube/config

# 7. 安装 Flink
log_info "安装 Apache Flink..."
if [ ! -d "/opt/flink" ]; then
    cd /tmp
    wget https://archive.apache.org/dist/flink/flink-${FLINK_VERSION}/flink-${FLINK_VERSION}-bin-scala_2.12.tgz
    tar -xzf flink-${FLINK_VERSION}-bin-scala_2.12.tgz
    mv flink-${FLINK_VERSION} /opt/flink
    rm flink-${FLINK_VERSION}-bin-scala_2.12.tgz
fi

# 8. 创建 Flink on K8s 配置
log_info "配置 Flink on Kubernetes..."
cat > /tmp/flink-service-account.yaml <<EOF
apiVersion: v1
kind: ServiceAccount
metadata:
  name: flink
  namespace: cdc-platform
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: flink-role
rules:
- apiGroups: [""]
  resources: ["pods", "services", "configmaps", "events"]
  verbs: ["get", "list", "watch", "create", "update", "delete"]
- apiGroups: ["apps"]
  resources: ["deployments"]
  verbs: ["get", "list", "watch", "create", "update", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: flink-role-binding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: flink-role
subjects:
- kind: ServiceAccount
  name: flink
  namespace: cdc-platform
EOF

kubectl create namespace cdc-platform --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f /tmp/flink-service-account.yaml

# 9. 构建后端项目
log_info "构建后端项目..."
cd $BASE_DIR/backend
# 拷贝 Maven 配置文件
cp $BASE_DIR/../workspace/cdc-doris-platform/backend/pom.xml .
cp -r $BASE_DIR/../workspace/cdc-doris-platform/backend/src .
mvn clean package -DskipTests

# 10. 构建 Flink Job
log_info "构建 Flink CDC 作业..."
cd $BASE_DIR/flink-job
cp $BASE_DIR/../workspace/cdc-doris-platform/flink-job/pom.xml .
mkdir -p src/main/java/com/cdc
cp $BASE_DIR/../workspace/cdc-doris-platform/flink-job/src/main/java/com/cdc/CdcDorisJob.java src/main/java/com/cdc/
mvn clean package

# 11. 初始化数据库
log_info "初始化数据库..."
mysql -u root -e "source $BASE_DIR/../workspace/cdc-doris-platform/database/schema.sql" || {
    log_warn "MySQL 未运行，请稍后手动导入数据库脚本：$BASE_DIR/database/schema.sql"
}

# 12. 配置后端启动脚本
log_info "创建启动脚本..."
cat > $BASE_DIR/start-backend.sh <<EOF
#!/bin/bash
cd $BASE_DIR/backend
nohup java -jar target/cdc-doris-platform-1.0.0.jar > $BASE_DIR/logs/backend.log 2>&1 &
echo \$! > $BASE_DIR/backend.pid
log_info "后端服务已启动，PID: \$(cat $BASE_DIR/backend.pid)"
EOF
chmod +x $BASE_DIR/start-backend.sh

# 13. 安装 Node.js 和前端构建
log_info "安装 Node.js..."
if ! command -v node &> /dev/null; then
    curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
    yum install -y nodejs
fi

# 14. 构建前端
log_info "构建前端..."
cd $BASE_DIR/frontend
cp -r $BASE_DIR/../workspace/cdc-doris-platform/frontend/* .
npm install
npm run build

# 15. 使用 Nginx部署前端（可选）
log_info "安装 Nginx..."
if ! command -v nginx &> /dev/null; then
    yum install -y nginx
fi

cat > /etc/nginx/conf.d/cdc-platform.conf <<EOF
server {
    listen 80;
    server_name localhost;

    location / {
        root $BASE_DIR/frontend/dist;
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
systemctl enable nginx

# 16. 启动后端
log_info "启动后端服务..."
$BASE_DIR/start-backend.sh

# 17. 验证部署
log_info "验证部署..."
sleep 10
if curl -s http://localhost:8080/api/task/stats/overview > /dev/null; then
    log_info "后端服务运行正常"
else
    log_warn "后端服务可能未正常启动，请检查日志：$BASE_DIR/logs/backend.log"
fi

# 完成
echo ""
echo "========================================"
echo -e "${GREEN}部署完成！${NC}"
echo "========================================"
echo ""
echo "访问地址：http://<服务器IP>"
echo "后端 API: http://<服务器IP>:8080/api"
echo "日志目录：$BASE_DIR/logs"
echo ""
echo "管理命令:"
echo "  启动后端：$BASE_DIR/start-backend.sh"
echo "  查看日志：tail -f $BASE_DIR/logs/backend.log"
echo "  重启服务：systemctl restart cdc-platform"
echo ""
