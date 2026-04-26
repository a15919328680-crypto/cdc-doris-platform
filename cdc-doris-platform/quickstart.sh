#!/bin/bash

echo "========================================"
echo " CDC 数据同步平台 - 快速启动脚本 (开发环境)"
echo "========================================"

# 后端启动脚本
cat > start-backend.sh <<'EOF'
#!/bin/bash
echo "启动 CDC 数据同步平台后端服务..."

# 检查 Java 是否安装
if ! command -v java &> /dev/null; then
    echo "错误：未检测到 Java，请先安装 Java 11+"
    exit 1
fi

# 检查 Maven 是否安装
if ! command -v mvn &> /dev/null; then
    echo "错误：未检测到 Maven，请先安装 Maven"
    exit 1
fi

# 检查 MySQL 是否运行
if ! command -v mysql &> /dev/null; then
    echo "警告：未检测到 MySQL 客户端，请确保数据库已配置"
fi

cd backend

# 构建项目
echo "构建后端项目..."
mvn clean package -DskipTests -q

# 初始化数据库
echo "初始化数据库..."
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="root"
DB_PASS="root123"
DB_NAME="cdc_platform"

mysql -u$DB_USER -p$DB_PASS -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;" 2>/dev/null || {
    echo "警告：无法连接 MySQL，请稍后手动执行 database/schema.sql"
}

mysql -u$DB_USER -p$DB_PASS $DB_NAME < ../database/schema.sql 2>/dev/null || {
    echo "警告：数据库初始化失败，请手动执行"
}

# 启动服务
echo "启动 SpringBoot 应用..."
nohup java -jar target/cdc-doris-platform-1.0.0.jar > ../logs/backend.log 2>&1 &
PID=$!
echo $PID > ../backend.pid

echo ""
echo "========================================"
echo "后端服务已启动！"
echo "========================================"
echo "进程 ID: $PID"
echo "访问地址：http://localhost:8080"
echo "日志文件：logs/backend.log"
echo ""
echo "停止服务：./stop-backend.sh"
echo "查看日志：tail -f logs/backend.log"
echo ""
EOF

chmod +x start-backend.sh

# 停止脚本
cat > stop-backend.sh <<'EOF'
#!/bin/bash
if [ -f backend.pid ]; then
    PID=$(cat backend.pid)
    echo "停止后端服务 (PID: $PID)..."
    kill -9 $PID 2>/dev/null
    rm backend.pid
    echo "服务已停止"
else
    echo "未找到运行中的服务"
fi
EOF

chmod +x stop-backend.sh

# 前端启动脚本
cat > start-frontend.sh <<'EOF'
#!/bin/bash
echo "启动前端开发服务器..."

cd frontend

# 检查 Node.js
if ! command -v node &> /dev/null; then
    echo "错误：未检测到 Node.js，请先安装 Node.js 18+"
    exit 1
fi

# 安装依赖
echo "安装依赖..."
npm install --quiet

# 启动开发服务器
echo "启动开发服务器..."
npm run dev
EOF

chmod +x start-frontend.sh

# 创建日志目录
mkdir -p logs

# 完成
echo ""
echo "========================================"
echo "部署脚本已创建完成！"
echo "========================================"
echo ""
echo "使用指南:"
echo ""
echo "1. 启动后端服务:"
echo "   ./start-backend.sh"
echo ""
echo "2. 启动前端开发服务器:"
echo "   ./start-frontend.sh"
echo ""
echo "3. 访问前端页面:"
echo "   http://localhost:3000"
echo ""
echo "4. 访问后端 API:"
echo "   http://localhost:8080/api"
echo ""
echo "========================================"
echo ""

# 自动启动后端
./start-backend.sh
