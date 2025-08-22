#!/bin/bash

# 企业邮件系统自动化部署脚本
# 支持一键安装和配置整个系统

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# 检查端口是否被占用
check_port() {
    if lsof -i:$1 &> /dev/null; then
        log_error "端口 $1 已被占用，请先释放该端口"
        exit 1
    fi
}

# 获取系统信息
get_system_info() {
    OS=$(uname -s)
    log_info "检测到操作系统: $OS"
    
    if [[ "$OS" == "Linux" ]]; then
        if command -v apt-get &> /dev/null; then
            PACKAGE_MANAGER="apt"
        elif command -v yum &> /dev/null; then
            PACKAGE_MANAGER="yum"
        else
            log_error "不支持的Linux发行版"
            exit 1
        fi
    elif [[ "$OS" == "Darwin" ]]; then
        PACKAGE_MANAGER="brew"
    else
        log_error "不支持的操作系统: $OS"
        exit 1
    fi
    
    log_info "包管理器: $PACKAGE_MANAGER"
}

# 安装依赖
install_dependencies() {
    log_step "安装系统依赖..."
    
    # 检查并安装Java 17
    if ! java -version 2>&1 | grep -q "17"; then
        log_info "安装Java 17..."
        case $PACKAGE_MANAGER in
            apt)
                sudo apt update
                sudo apt install -y openjdk-17-jdk
                ;;
            yum)
                sudo yum install -y java-17-openjdk-devel
                ;;
            brew)
                brew install openjdk@17
                ;;
        esac
    else
        log_info "Java 17 已安装"
    fi
    
    # 检查并安装Maven
    if ! command -v mvn &> /dev/null; then
        log_info "安装Maven..."
        case $PACKAGE_MANAGER in
            apt)
                sudo apt install -y maven
                ;;
            yum)
                sudo yum install -y maven
                ;;
            brew)
                brew install maven
                ;;
        esac
    else
        log_info "Maven 已安装"
    fi
    
    # 检查并安装Node.js
    if ! command -v node &> /dev/null; then
        log_info "安装Node.js..."
        case $PACKAGE_MANAGER in
            apt)
                curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
                sudo apt install -y nodejs
                ;;
            yum)
                curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
                sudo yum install -y nodejs
                ;;
            brew)
                brew install node
                ;;
        esac
    else
        log_info "Node.js 已安装"
    fi
    
    # 检查并安装MySQL
    if ! command -v mysql &> /dev/null; then
        log_info "安装MySQL..."
        case $PACKAGE_MANAGER in
            apt)
                sudo apt install -y mysql-server mysql-client
                ;;
            yum)
                sudo yum install -y mysql-server mysql
                ;;
            brew)
                brew install mysql
                ;;
        esac
    else
        log_info "MySQL 已安装"
    fi
    
    # 检查并安装Redis
    if ! command -v redis-server &> /dev/null; then
        log_info "安装Redis..."
        case $PACKAGE_MANAGER in
            apt)
                sudo apt install -y redis-server
                ;;
            yum)
                sudo yum install -y redis
                ;;
            brew)
                brew install redis
                ;;
        esac
    else
        log_info "Redis 已安装"
    fi
}

# 配置数据库
setup_database() {
    log_step "配置数据库..."
    
    # 启动MySQL服务
    case $PACKAGE_MANAGER in
        apt|yum)
            sudo systemctl start mysql
            sudo systemctl enable mysql
            ;;
        brew)
            brew services start mysql
            ;;
    esac
    
    # 配置数据库
    read -p "请输入MySQL root密码 (如果是首次安装，可直接回车): " mysql_password
    
    if [ -z "$mysql_password" ]; then
        mysql_command="mysql -u root"
    else
        mysql_command="mysql -u root -p$mysql_password"
    fi
    
    # 创建数据库和用户
    log_info "创建数据库..."
    $mysql_command << EOF
CREATE DATABASE IF NOT EXISTS enterprise_email DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'email_user'@'localhost' IDENTIFIED BY 'email_password_123';
GRANT ALL PRIVILEGES ON enterprise_email.* TO 'email_user'@'localhost';
FLUSH PRIVILEGES;
EOF
    
    # 导入数据库结构
    log_info "导入数据库结构..."
    if [ -f "database/init.sql" ]; then
        $mysql_command enterprise_email < database/init.sql
        log_info "数据库初始化完成"
    else
        log_error "数据库初始化文件不存在: database/init.sql"
        exit 1
    fi
}

# 配置Redis
setup_redis() {
    log_step "配置Redis..."
    
    # 启动Redis服务
    case $PACKAGE_MANAGER in
        apt|yum)
            sudo systemctl start redis
            sudo systemctl enable redis
            ;;
        brew)
            brew services start redis
            ;;
    esac
    
    log_info "Redis配置完成"
}

# 构建后端
build_backend() {
    log_step "构建后端应用..."
    
    cd backend
    
    # 创建配置文件
    if [ ! -f "src/main/resources/application-local.yml" ]; then
        log_info "创建本地配置文件..."
        cat > src/main/resources/application-local.yml << EOF
spring:
  datasource:
    druid:
      username: email_user
      password: email_password_123
  redis:
    password: ""
  mail:
    username: your-email@qq.com
    password: your-auth-code

app:
  jwt:
    secret: enterprise-email-system-jwt-secret-key-$(date +%s)
EOF
    fi
    
    # 编译项目
    log_info "编译Spring Boot项目..."
    mvn clean compile -DskipTests
    
    # 打包项目
    log_info "打包Spring Boot项目..."
    mvn package -DskipTests
    
    cd ..
    log_info "后端构建完成"
}

# 构建前端
build_frontend() {
    log_step "构建前端应用..."
    
    cd frontend
    
    # 安装依赖
    log_info "安装前端依赖..."
    npm install
    
    # 构建生产版本
    log_info "构建前端生产版本..."
    npm run build
    
    cd ..
    log_info "前端构建完成"
}

# 部署应用
deploy_application() {
    log_step "部署应用..."
    
    # 创建部署目录
    DEPLOY_DIR="/opt/enterprise-email"
    sudo mkdir -p $DEPLOY_DIR
    sudo chown $USER:$USER $DEPLOY_DIR
    
    # 复制后端文件
    log_info "部署后端应用..."
    cp backend/target/*.jar $DEPLOY_DIR/
    
    # 复制前端文件
    log_info "部署前端应用..."
    sudo mkdir -p /var/www/enterprise-email
    sudo cp -r frontend/dist/* /var/www/enterprise-email/
    sudo chown -R www-data:www-data /var/www/enterprise-email/
    
    # 创建启动脚本
    log_info "创建启动脚本..."
    cat > $DEPLOY_DIR/start.sh << 'EOF'
#!/bin/bash
cd /opt/enterprise-email
java -jar -Dspring.profiles.active=local *.jar > app.log 2>&1 &
echo $! > app.pid
echo "应用已启动，PID: $(cat app.pid)"
EOF
    
    cat > $DEPLOY_DIR/stop.sh << 'EOF'
#!/bin/bash
cd /opt/enterprise-email
if [ -f app.pid ]; then
    PID=$(cat app.pid)
    kill $PID
    rm app.pid
    echo "应用已停止"
else
    echo "应用未运行"
fi
EOF
    
    chmod +x $DEPLOY_DIR/start.sh
    chmod +x $DEPLOY_DIR/stop.sh
    
    log_info "应用部署完成"
}

# 配置Nginx（可选）
setup_nginx() {
    log_step "配置Nginx..."
    
    if ! command -v nginx &> /dev/null; then
        log_info "安装Nginx..."
        case $PACKAGE_MANAGER in
            apt)
                sudo apt install -y nginx
                ;;
            yum)
                sudo yum install -y nginx
                ;;
            brew)
                brew install nginx
                ;;
        esac
    fi
    
    # 创建Nginx配置
    log_info "创建Nginx配置..."
    sudo tee /etc/nginx/sites-available/enterprise-email << 'EOF'
server {
    listen 80;
    server_name localhost;
    
    # 前端文件
    location / {
        root /var/www/enterprise-email;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    # API代理
    location /api/ {
        proxy_pass http://localhost:9000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        root /var/www/enterprise-email;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF
    
    # 启用站点
    if [ -d "/etc/nginx/sites-enabled" ]; then
        sudo ln -sf /etc/nginx/sites-available/enterprise-email /etc/nginx/sites-enabled/
        sudo rm -f /etc/nginx/sites-enabled/default
    fi
    
    # 测试配置
    sudo nginx -t
    
    # 启动Nginx
    case $PACKAGE_MANAGER in
        apt|yum)
            sudo systemctl start nginx
            sudo systemctl enable nginx
            ;;
        brew)
            brew services start nginx
            ;;
    esac
    
    log_info "Nginx配置完成"
}

# 启动应用
start_application() {
    log_step "启动应用..."
    
    # 检查端口
    check_port 9000
    
    # 启动后端应用
    cd /opt/enterprise-email
    ./start.sh
    
    # 等待应用启动
    log_info "等待应用启动..."
    sleep 10
    
    # 检查应用状态
    if curl -f http://localhost:9000/api/health &> /dev/null; then
        log_info "后端应用启动成功"
    else
        log_error "后端应用启动失败，请检查日志"
        exit 1
    fi
    
    log_info "应用启动完成"
}

# 显示部署信息
show_deployment_info() {
    log_step "部署完成！"
    
    echo ""
    echo "==================== 部署信息 ===================="
    echo "前端访问地址: http://localhost"
    echo "后端API地址: http://localhost:9000/api"
    echo "健康检查: http://localhost:9000/api/health"
    echo ""
    echo "默认管理员账户:"
    echo "邮箱: admin@system.com"
    echo "密码: admin123"
    echo ""
    echo "默认演示账户:"
    echo "邮箱: demo@example.com"  
    echo "密码: admin123"
    echo ""
    echo "应用目录: /opt/enterprise-email"
    echo "启动命令: /opt/enterprise-email/start.sh"
    echo "停止命令: /opt/enterprise-email/stop.sh"
    echo "日志文件: /opt/enterprise-email/app.log"
    echo ""
    echo "数据库信息:"
    echo "数据库名: enterprise_email"
    echo "用户名: email_user"
    echo "密码: email_password_123"
    echo "=================================================="
    echo ""
    
    log_info "企业邮件系统部署成功！"
}

# 主函数
main() {
    echo ""
    echo "========================================"
    echo "  企业邮件系统自动化部署脚本"
    echo "========================================"
    echo ""
    
    # 检查权限
    if [[ $EUID -eq 0 ]]; then
        log_error "请不要使用root用户运行此脚本"
        exit 1
    fi
    
    # 获取系统信息
    get_system_info
    
    # 检查基本命令
    log_step "检查系统环境..."
    check_command curl
    check_command wget
    check_command git
    
    # 安装依赖
    install_dependencies
    
    # 配置服务
    setup_database
    setup_redis
    
    # 构建应用
    build_backend
    build_frontend
    
    # 部署应用
    deploy_application
    
    # 配置Web服务器
    if command -v nginx &> /dev/null || [ "$1" = "--with-nginx" ]; then
        setup_nginx
    fi
    
    # 启动应用
    start_application
    
    # 显示部署信息
    show_deployment_info
}

# 执行主函数
main "$@"