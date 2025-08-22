#!/bin/bash

# SSL Certificate Deployment and Validation Script
# 用于部署和验证SSL证书的自动化脚本

set -e  # 遇到错误立即退出

# 脚本配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="/var/log/ssl-deployment.log"
CONFIG_FILE="${SCRIPT_DIR}/ssl-config.conf"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# 检查依赖
check_dependencies() {
    log "INFO" "检查系统依赖..."
    
    local deps=("openssl" "curl" "nginx" "certbot")
    local missing_deps=()
    
    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            missing_deps+=("$dep")
        fi
    done
    
    if [ ${#missing_deps[@]} -gt 0 ]; then
        log "ERROR" "缺少依赖: ${missing_deps[*]}"
        log "INFO" "请安装缺少的依赖后重新运行脚本"
        exit 1
    fi
    
    log "SUCCESS" "所有依赖检查通过"
}

# 加载配置文件
load_config() {
    if [ -f "$CONFIG_FILE" ]; then
        source "$CONFIG_FILE"
        log "INFO" "已加载配置文件: $CONFIG_FILE"
    else
        log "WARNING" "配置文件不存在，使用默认配置"
        # 默认配置
        SSL_CERTS_DIR="${SSL_CERTS_DIR:-/opt/ssl/certs}"
        NGINX_CONFIG_DIR="${NGINX_CONFIG_DIR:-/etc/nginx/conf.d}"
        BACKUP_DIR="${BACKUP_DIR:-/opt/ssl/backups}"
        ACME_DIR="${ACME_DIR:-/opt/ssl/acme}"
        LETS_ENCRYPT_EMAIL="${LETS_ENCRYPT_EMAIL:-admin@example.com}"
    fi
}

# 创建必要目录
create_directories() {
    log "INFO" "创建必要目录..."
    
    local dirs=("$SSL_CERTS_DIR" "$NGINX_CONFIG_DIR" "$BACKUP_DIR" "$ACME_DIR")
    
    for dir in "${dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            chmod 755 "$dir"
            log "SUCCESS" "创建目录: $dir"
        fi
    done
}

# 验证域名控制权
validate_domain_control() {
    local domain="$1"
    
    log "INFO" "验证域名控制权: $domain"
    
    # DNS解析检查
    if ! nslookup "$domain" &> /dev/null; then
        log "ERROR" "域名DNS解析失败: $domain"
        return 1
    fi
    
    # HTTP连通性检查
    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "http://$domain/.well-known/acme-challenge/test" || echo "000")
    
    if [[ "$http_code" =~ ^[45] ]]; then
        log "WARNING" "HTTP验证路径可能不可访问 (HTTP $http_code)"
    fi
    
    log "SUCCESS" "域名验证通过: $domain"
    return 0
}

# 获取Let's Encrypt证书
obtain_lets_encrypt_cert() {
    local domain="$1"
    local email="${2:-$LETS_ENCRYPT_EMAIL}"
    local challenge_type="${3:-HTTP01}"
    
    log "INFO" "开始获取Let's Encrypt证书: $domain"
    
    # 验证域名控制权
    if ! validate_domain_control "$domain"; then
        return 1
    fi
    
    # 创建域名专用目录
    local domain_dir="$SSL_CERTS_DIR/$domain"
    mkdir -p "$domain_dir"
    
    # 构建certbot命令
    local certbot_cmd="certbot certonly --non-interactive --agree-tos"
    certbot_cmd+=" --email $email"
    
    # 选择验证方式
    case "$challenge_type" in
        "HTTP01")
            certbot_cmd+=" --webroot --webroot-path=/var/www/html"
            ;;
        "DNS01")
            certbot_cmd+=" --manual --preferred-challenges dns"
            ;;
        *)
            log "ERROR" "不支持的验证方式: $challenge_type"
            return 1
            ;;
    esac
    
    certbot_cmd+=" --cert-path $domain_dir/cert.pem"
    certbot_cmd+=" --key-path $domain_dir/key.pem"
    certbot_cmd+=" --fullchain-path $domain_dir/fullchain.pem"
    certbot_cmd+=" --chain-path $domain_dir/chain.pem"
    certbot_cmd+=" -d $domain"
    
    # 执行certbot命令
    log "INFO" "执行certbot命令..."
    if eval "$certbot_cmd"; then
        log "SUCCESS" "Let's Encrypt证书获取成功: $domain"
        
        # 设置文件权限
        chmod 600 "$domain_dir"/*.pem
        chown root:root "$domain_dir"/*.pem
        
        return 0
    else
        log "ERROR" "Let's Encrypt证书获取失败: $domain"
        return 1
    fi
}

# 验证证书文件
validate_certificate() {
    local cert_file="$1"
    local key_file="$2"
    local domain="$3"
    
    log "INFO" "验证证书文件: $cert_file"
    
    # 检查文件是否存在
    if [ ! -f "$cert_file" ] || [ ! -f "$key_file" ]; then
        log "ERROR" "证书文件或密钥文件不存在"
        return 1
    fi
    
    # 验证证书格式
    if ! openssl x509 -in "$cert_file" -text -noout &> /dev/null; then
        log "ERROR" "证书文件格式无效: $cert_file"
        return 1
    fi
    
    # 验证私钥格式
    if ! openssl rsa -in "$key_file" -check &> /dev/null && ! openssl ec -in "$key_file" -check &> /dev/null; then
        log "ERROR" "私钥文件格式无效: $key_file"
        return 1
    fi
    
    # 验证证书和私钥匹配
    local cert_modulus key_modulus
    cert_modulus=$(openssl x509 -noout -modulus -in "$cert_file" 2>/dev/null | openssl md5)
    key_modulus=$(openssl rsa -noout -modulus -in "$key_file" 2>/dev/null | openssl md5)
    
    if [ "$cert_modulus" != "$key_modulus" ]; then
        log "ERROR" "证书和私钥不匹配"
        return 1
    fi
    
    # 检查证书是否过期
    if ! openssl x509 -checkend 86400 -noout -in "$cert_file" &> /dev/null; then
        log "WARNING" "证书将在24小时内过期"
    fi
    
    # 验证域名匹配
    if [ -n "$domain" ]; then
        local cert_subject
        cert_subject=$(openssl x509 -noout -subject -in "$cert_file" | grep -o "CN=.*" | cut -d'=' -f2 | cut -d',' -f1 | tr -d ' ')
        
        if [ "$cert_subject" != "$domain" ]; then
            # 检查SAN扩展
            local san_domains
            san_domains=$(openssl x509 -noout -text -in "$cert_file" | grep -A1 "Subject Alternative Name" | tail -1 | tr ',' '\n' | grep "DNS:" | cut -d':' -f2 | tr -d ' ')
            
            local domain_found=false
            for san_domain in $san_domains; do
                if [ "$san_domain" = "$domain" ]; then
                    domain_found=true
                    break
                fi
            done
            
            if [ "$domain_found" = false ]; then
                log "WARNING" "证书中未找到域名: $domain"
            fi
        fi
    fi
    
    log "SUCCESS" "证书验证通过: $cert_file"
    return 0
}

# 生成Nginx SSL配置
generate_nginx_config() {
    local domain="$1"
    local cert_file="$2"
    local key_file="$3"
    local chain_file="$4"
    
    local config_file="$NGINX_CONFIG_DIR/$domain-ssl.conf"
    
    log "INFO" "生成Nginx SSL配置: $config_file"
    
    cat > "$config_file" << EOF
# SSL configuration for $domain
server {
    listen 443 ssl http2;
    server_name $domain;

    ssl_certificate $cert_file;
    ssl_certificate_key $key_file;
EOF

    if [ -n "$chain_file" ] && [ -f "$chain_file" ]; then
        echo "    ssl_trusted_certificate $chain_file;" >> "$config_file"
    fi

    cat >> "$config_file" << EOF

    # SSL Security Settings
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-SHA256:ECDHE-RSA-AES256-SHA384;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
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

    # Application proxy
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}

# HTTP to HTTPS redirect
server {
    listen 80;
    server_name $domain;
    return 301 https://\$server_name\$request_uri;
}
EOF

    log "SUCCESS" "Nginx SSL配置生成完成: $config_file"
}

# 测试并重载Nginx配置
reload_nginx() {
    log "INFO" "测试Nginx配置..."
    
    if nginx -t; then
        log "SUCCESS" "Nginx配置测试通过"
        
        log "INFO" "重载Nginx配置..."
        if systemctl reload nginx; then
            log "SUCCESS" "Nginx配置重载完成"
            return 0
        else
            log "ERROR" "Nginx配置重载失败"
            return 1
        fi
    else
        log "ERROR" "Nginx配置测试失败"
        return 1
    fi
}

# 测试SSL证书配置
test_ssl_configuration() {
    local domain="$1"
    
    log "INFO" "测试SSL证书配置: $domain"
    
    # 等待Nginx重载完成
    sleep 2
    
    # 测试HTTPS连接
    local response_code
    response_code=$(curl -s -o /dev/null -w "%{http_code}" "https://$domain" --max-time 10 || echo "000")
    
    if [[ "$response_code" =~ ^[23] ]]; then
        log "SUCCESS" "HTTPS连接测试通过 (HTTP $response_code)"
    else
        log "ERROR" "HTTPS连接测试失败 (HTTP $response_code)"
        return 1
    fi
    
    # 测试SSL证书详细信息
    local ssl_info
    ssl_info=$(echo | openssl s_client -connect "$domain:443" -servername "$domain" 2>/dev/null | openssl x509 -noout -dates 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        log "SUCCESS" "SSL证书信息获取成功"
        echo "$ssl_info" | while IFS= read -r line; do
            log "INFO" "  $line"
        done
    else
        log "WARNING" "无法获取SSL证书详细信息"
    fi
    
    # 测试HTTP到HTTPS重定向
    local redirect_location
    redirect_location=$(curl -s -o /dev/null -w "%{redirect_url}" "http://$domain" --max-time 10)
    
    if [[ "$redirect_location" == https://* ]]; then
        log "SUCCESS" "HTTP到HTTPS重定向配置正确"
    else
        log "WARNING" "HTTP到HTTPS重定向可能未正确配置"
    fi
    
    return 0
}

# 备份证书
backup_certificates() {
    local backup_name="ssl_backup_$(date +%Y%m%d_%H%M%S)"
    local backup_path="$BACKUP_DIR/$backup_name"
    
    log "INFO" "开始备份SSL证书到: $backup_path"
    
    mkdir -p "$backup_path"
    
    # 备份证书文件
    if [ -d "$SSL_CERTS_DIR" ]; then
        cp -r "$SSL_CERTS_DIR" "$backup_path/"
        log "SUCCESS" "证书文件备份完成"
    fi
    
    # 备份Nginx配置
    if [ -d "$NGINX_CONFIG_DIR" ]; then
        mkdir -p "$backup_path/nginx"
        cp "$NGINX_CONFIG_DIR"/*-ssl.conf "$backup_path/nginx/" 2>/dev/null || true
        log "SUCCESS" "Nginx配置备份完成"
    fi
    
    # 创建备份信息文件
    cat > "$backup_path/backup_info.txt" << EOF
Backup Information
==================
Date: $(date)
Certificates Directory: $SSL_CERTS_DIR
Nginx Config Directory: $NGINX_CONFIG_DIR
Backup Path: $backup_path
EOF
    
    log "SUCCESS" "SSL证书备份完成: $backup_path"
}

# 主要部署函数
deploy_certificate() {
    local action="$1"
    local domain="$2"
    
    case "$action" in
        "obtain")
            local email="$3"
            local challenge_type="$4"
            
            if obtain_lets_encrypt_cert "$domain" "$email" "$challenge_type"; then
                local domain_dir="$SSL_CERTS_DIR/$domain"
                generate_nginx_config "$domain" "$domain_dir/fullchain.pem" "$domain_dir/key.pem" "$domain_dir/chain.pem"
                
                if reload_nginx; then
                    test_ssl_configuration "$domain"
                fi
            fi
            ;;
        "deploy")
            local cert_file="$3"
            local key_file="$4"
            local chain_file="$5"
            
            if validate_certificate "$cert_file" "$key_file" "$domain"; then
                generate_nginx_config "$domain" "$cert_file" "$key_file" "$chain_file"
                
                if reload_nginx; then
                    test_ssl_configuration "$domain"
                fi
            fi
            ;;
        "test")
            test_ssl_configuration "$domain"
            ;;
        "backup")
            backup_certificates
            ;;
        *)
            echo "用法: $0 <action> [options]"
            echo ""
            echo "Actions:"
            echo "  obtain <domain> [email] [challenge_type]  - 获取Let's Encrypt证书"
            echo "  deploy <domain> <cert_file> <key_file> [chain_file]  - 部署自定义证书"
            echo "  test <domain>                           - 测试SSL配置"
            echo "  backup                                  - 备份证书"
            echo ""
            echo "Examples:"
            echo "  $0 obtain example.com admin@example.com HTTP01"
            echo "  $0 deploy example.com /path/to/cert.pem /path/to/key.pem"
            echo "  $0 test example.com"
            echo "  $0 backup"
            exit 1
            ;;
    esac
}

# 主函数
main() {
    log "INFO" "SSL证书部署脚本启动"
    
    # 检查是否以root身份运行
    if [ "$EUID" -ne 0 ]; then
        log "ERROR" "请以root身份运行此脚本"
        exit 1
    fi
    
    # 初始化
    check_dependencies
    load_config
    create_directories
    
    # 执行部署操作
    deploy_certificate "$@"
    
    log "INFO" "SSL证书部署脚本完成"
}

# 运行主函数
main "$@"