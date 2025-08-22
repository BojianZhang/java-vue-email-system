#!/bin/bash

# SSL Certificate Management System Installation Script
# SSL证书管理系统安装脚本

set -e

# 脚本配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="/opt/ssl"
LOG_FILE="/var/log/ssl-install.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 日志函数
log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    echo "[$timestamp] [$level] $message" | tee -a "$LOG_FILE"
    
    case $level in
        "ERROR") echo -e "${RED}[ERROR]${NC} $message" ;;
        "SUCCESS") echo -e "${GREEN}[SUCCESS]${NC} $message" ;;
        "WARNING") echo -e "${YELLOW}[WARNING]${NC} $message" ;;
        "INFO") echo -e "${BLUE}[INFO]${NC} $message" ;;
    esac
}

# 检查是否以root身份运行
check_root() {
    if [ "$EUID" -ne 0 ]; then
        log "ERROR" "请以root身份运行此脚本"
        exit 1
    fi
}

# 检测操作系统
detect_os() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$NAME
        OS_VERSION=$VERSION_ID
        log "INFO" "检测到操作系统: $OS $OS_VERSION"
    else
        log "ERROR" "无法检测操作系统类型"
        exit 1
    fi
}

# 安装系统依赖
install_dependencies() {
    log "INFO" "安装系统依赖..."
    
    case "$OS" in
        *"Ubuntu"*|*"Debian"*)
            apt-get update
            apt-get install -y \
                nginx \
                certbot \
                python3-certbot-nginx \
                openssl \
                curl \
                wget \
                jq \
                cron \
                logrotate \
                mailutils \
                netcat-openbsd \
                dnsutils
            ;;
        *"CentOS"*|*"Red Hat"*|*"Rocky"*|*"AlmaLinux"*)
            yum update -y
            yum install -y epel-release
            yum install -y \
                nginx \
                certbot \
                python3-certbot-nginx \
                openssl \
                curl \
                wget \
                jq \
                cronie \
                logrotate \
                mailx \
                nc \
                bind-utils
            ;;
        *)
            log "WARNING" "未知的操作系统，请手动安装依赖"
            ;;
    esac
    
    log "SUCCESS" "系统依赖安装完成"
}

# 创建系统用户和目录
setup_directories() {
    log "INFO" "创建系统目录结构..."
    
    # 创建主要目录
    local dirs=(
        "$INSTALL_DIR"
        "$INSTALL_DIR/scripts"
        "$INSTALL_DIR/certs"
        "$INSTALL_DIR/backups"
        "$INSTALL_DIR/acme"
        "$INSTALL_DIR/logs"
        "$INSTALL_DIR/templates"
        "/var/log/ssl"
        "/etc/nginx/ssl-snippets"
    )
    
    for dir in "${dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            log "SUCCESS" "创建目录: $dir"
        fi
    done
    
    # 设置目录权限
    chmod 755 "$INSTALL_DIR"
    chmod 700 "$INSTALL_DIR/certs"
    chmod 755 "$INSTALL_DIR/backups"
    chmod 755 "$INSTALL_DIR/logs"
    
    log "SUCCESS" "目录结构创建完成"
}

# 复制脚本文件
install_scripts() {
    log "INFO" "安装SSL管理脚本..."
    
    # 复制脚本文件
    local scripts=(
        "ssl-deploy.sh"
        "ssl-monitor.sh"
        "ssl-config.conf"
    )
    
    for script in "${scripts[@]}"; do
        if [ -f "$SCRIPT_DIR/$script" ]; then
            cp "$SCRIPT_DIR/$script" "$INSTALL_DIR/scripts/"
            if [[ "$script" == *.sh ]]; then
                chmod +x "$INSTALL_DIR/scripts/$script"
            fi
            log "SUCCESS" "安装脚本: $script"
        else
            log "WARNING" "脚本文件不存在: $script"
        fi
    done
    
    # 创建符号链接到系统PATH
    ln -sf "$INSTALL_DIR/scripts/ssl-deploy.sh" "/usr/local/bin/ssl-deploy"
    ln -sf "$INSTALL_DIR/scripts/ssl-monitor.sh" "/usr/local/bin/ssl-monitor"
    
    log "SUCCESS" "脚本安装完成"
}

# 配置systemd服务
setup_systemd_services() {
    log "INFO" "配置systemd服务..."
    
    # 复制服务文件
    local services=(
        "ssl-monitor.service"
        "ssl-monitor.timer"
    )
    
    for service in "${services[@]}"; do
        if [ -f "$SCRIPT_DIR/$service" ]; then
            cp "$SCRIPT_DIR/$service" "/etc/systemd/system/"
            log "SUCCESS" "安装服务: $service"
        fi
    done
    
    # 重载systemd配置
    systemctl daemon-reload
    
    # 启用和启动timer
    systemctl enable ssl-monitor.timer
    systemctl start ssl-monitor.timer
    
    log "SUCCESS" "systemd服务配置完成"
}

# 配置Nginx SSL片段
setup_nginx_ssl_snippets() {
    log "INFO" "配置Nginx SSL片段..."
    
    # 创建通用SSL配置片段
    cat > /etc/nginx/ssl-snippets/ssl-params.conf << 'EOF'
# SSL Configuration
ssl_protocols TLSv1.2 TLSv1.3;
ssl_prefer_server_ciphers on;
ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-SHA256:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-ECDSA-AES256-SHA384;

# SSL Session Settings
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;
ssl_session_tickets off;

# OCSP Stapling
ssl_stapling on;
ssl_stapling_verify on;
resolver 8.8.8.8 8.8.4.4 valid=300s;
resolver_timeout 5s;

# Security Headers
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
add_header X-Frame-Options DENY always;
add_header X-Content-Type-Options nosniff always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header X-Robots-Tag noindex always;
EOF

    # 创建强SSL配置片段
    cat > /etc/nginx/ssl-snippets/ssl-strong.conf << 'EOF'
# Strong SSL Configuration
ssl_protocols TLSv1.3;
ssl_prefer_server_ciphers off;
ssl_ciphers TLS_AES_128_GCM_SHA256:TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;

# Enhanced Security Headers
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self'; media-src 'self'; object-src 'none'; child-src 'self'; frame-ancestors 'none'; form-action 'self'; base-uri 'self';" always;
add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
add_header Cross-Origin-Embedder-Policy "require-corp" always;
add_header Cross-Origin-Opener-Policy "same-origin" always;
add_header Cross-Origin-Resource-Policy "same-origin" always;
EOF

    # 测试Nginx配置
    if nginx -t; then
        log "SUCCESS" "Nginx SSL片段配置完成"
    else
        log "ERROR" "Nginx配置测试失败"
        return 1
    fi
}

# 配置防火墙
setup_firewall() {
    log "INFO" "配置防火墙规则..."
    
    # 检查防火墙类型并配置
    if command -v ufw &> /dev/null; then
        # Ubuntu/Debian UFW
        ufw allow 80/tcp
        ufw allow 443/tcp
        log "SUCCESS" "UFW防火墙规则已配置"
    elif command -v firewall-cmd &> /dev/null; then
        # CentOS/RHEL firewalld
        firewall-cmd --permanent --add-service=http
        firewall-cmd --permanent --add-service=https
        firewall-cmd --reload
        log "SUCCESS" "firewalld防火墙规则已配置"
    elif command -v iptables &> /dev/null; then
        # iptables
        iptables -A INPUT -p tcp --dport 80 -j ACCEPT
        iptables -A INPUT -p tcp --dport 443 -j ACCEPT
        log "SUCCESS" "iptables防火墙规则已配置"
    else
        log "WARNING" "未检测到防火墙，请手动配置HTTP(80)和HTTPS(443)端口"
    fi
}

# 配置日志轮转
setup_log_rotation() {
    log "INFO" "配置日志轮转..."
    
    cat > /etc/logrotate.d/ssl-management << 'EOF'
/var/log/ssl/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 root root
    postrotate
        systemctl reload rsyslog > /dev/null 2>&1 || true
    endscript
}

/opt/ssl/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 root root
}
EOF
    
    log "SUCCESS" "日志轮转配置完成"
}

# 创建示例配置文件
create_example_configs() {
    log "INFO" "创建示例配置文件..."
    
    # 创建Let's Encrypt配置示例
    cat > "$INSTALL_DIR/templates/lets-encrypt-example.conf" << 'EOF'
# Let's Encrypt Certificate Example
# 使用方法: ssl-deploy obtain example.com admin@example.com HTTP01

DOMAIN="example.com"
EMAIL="admin@example.com"
CHALLENGE_TYPE="HTTP01"  # HTTP01 或 DNS01
WEBROOT_PATH="/var/www/html"
EOF
    
    # 创建自定义证书示例
    cat > "$INSTALL_DIR/templates/custom-cert-example.conf" << 'EOF'
# Custom Certificate Example  
# 使用方法: ssl-deploy deploy example.com /path/to/cert.pem /path/to/key.pem

DOMAIN="example.com"
CERT_FILE="/path/to/certificate.pem"
KEY_FILE="/path/to/private-key.pem"
CHAIN_FILE="/path/to/chain.pem"  # 可选
EOF
    
    log "SUCCESS" "示例配置文件创建完成"
}

# 验证安装
verify_installation() {
    log "INFO" "验证安装..."
    
    local errors=0
    
    # 检查命令是否可用
    local commands=("ssl-deploy" "ssl-monitor" "nginx" "certbot" "openssl")
    for cmd in "${commands[@]}"; do
        if ! command -v "$cmd" &> /dev/null; then
            log "ERROR" "命令不可用: $cmd"
            errors=$((errors + 1))
        else
            log "SUCCESS" "命令可用: $cmd"
        fi
    done
    
    # 检查目录是否存在
    local dirs=("$INSTALL_DIR" "$INSTALL_DIR/scripts" "$INSTALL_DIR/certs")
    for dir in "${dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            log "ERROR" "目录不存在: $dir"
            errors=$((errors + 1))
        else
            log "SUCCESS" "目录存在: $dir"
        fi
    done
    
    # 检查服务状态
    if systemctl is-active --quiet ssl-monitor.timer; then
        log "SUCCESS" "SSL监控定时器正在运行"
    else
        log "WARNING" "SSL监控定时器未运行"
        errors=$((errors + 1))
    fi
    
    # 检查Nginx配置
    if nginx -t &> /dev/null; then
        log "SUCCESS" "Nginx配置语法正确"
    else
        log "ERROR" "Nginx配置语法错误"
        errors=$((errors + 1))
    fi
    
    if [ $errors -eq 0 ]; then
        log "SUCCESS" "安装验证通过"
        return 0
    else
        log "ERROR" "发现 $errors 个错误"
        return 1
    fi
}

# 显示安装后信息
show_post_install_info() {
    log "INFO" "显示安装后信息..."
    
    cat << 'EOF'

🎉 SSL证书管理系统安装完成！

📁 安装目录: /opt/ssl/
├── scripts/          # 管理脚本
├── certs/           # 证书存储
├── backups/         # 备份文件
├── logs/            # 日志文件
└── templates/       # 配置模板

🔧 可用命令:
• ssl-deploy obtain <domain> <email> [challenge_type]  # 获取Let's Encrypt证书
• ssl-deploy deploy <domain> <cert> <key> [chain]     # 部署自定义证书
• ssl-deploy test <domain>                            # 测试SSL配置
• ssl-deploy backup                                   # 备份证书
• ssl-monitor scan                                    # 扫描证书状态
• ssl-monitor renew                                   # 续期即将过期的证书
• ssl-monitor report                                  # 生成监控报告

📋 服务状态:
• SSL监控定时器: systemctl status ssl-monitor.timer
• 手动运行监控: systemctl start ssl-monitor.service

📖 配置文件:
• 主配置: /opt/ssl/scripts/ssl-config.conf
• 示例配置: /opt/ssl/templates/

🔗 使用示例:
# 获取Let's Encrypt免费证书
ssl-deploy obtain example.com admin@example.com HTTP01

# 测试证书配置
ssl-deploy test example.com

# 查看监控报告
ssl-monitor report

📚 更多信息请查看文档或日志文件 /var/log/ssl-install.log

EOF
}

# 主安装函数
main() {
    log "INFO" "开始安装SSL证书管理系统..."
    
    # 检查权限
    check_root
    
    # 检测系统
    detect_os
    
    # 执行安装步骤
    install_dependencies
    setup_directories
    install_scripts
    setup_systemd_services
    setup_nginx_ssl_snippets
    setup_firewall
    setup_log_rotation
    create_example_configs
    
    # 验证安装
    if verify_installation; then
        show_post_install_info
        log "SUCCESS" "SSL证书管理系统安装成功！"
    else
        log "ERROR" "安装过程中发现问题，请检查日志"
        exit 1
    fi
}

# 运行安装
main "$@"