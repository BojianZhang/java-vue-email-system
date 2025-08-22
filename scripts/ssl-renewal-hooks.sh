#!/bin/bash

# SSL Certificate Renewal Hooks
# 证书续期钩子脚本 - 在证书续期前后执行自定义操作

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/ssl-config.conf"
LOG_FILE="/var/log/ssl-renewal-hooks.log"

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

# 加载配置
load_config() {
    if [ -f "$CONFIG_FILE" ]; then
        source "$CONFIG_FILE"
    fi
}

# 续期前钩子
pre_renewal_hook() {
    local domain="$1"
    local cert_path="$2"
    
    log "INFO" "执行续期前钩子: $domain"
    
    # 备份当前证书
    if [ -f "$cert_path" ]; then
        local backup_dir="/tmp/ssl-renewal-backup-$(date +%s)"
        mkdir -p "$backup_dir"
        cp -r "$(dirname "$cert_path")" "$backup_dir/"
        log "SUCCESS" "当前证书已备份到: $backup_dir"
    fi
    
    # 记录当前服务状态
    local services=("nginx" "apache2" "httpd")
    for service in "${services[@]}"; do
        if systemctl is-active --quiet "$service" 2>/dev/null; then
            echo "$service:active" > "/tmp/ssl-services-status-$domain"
            log "INFO" "记录服务状态: $service is active"
        fi
    done
    
    # 执行自定义预处理脚本
    if [ -f "/opt/ssl/hooks/pre-renewal-custom.sh" ]; then
        log "INFO" "执行自定义续期前脚本"
        bash "/opt/ssl/hooks/pre-renewal-custom.sh" "$domain" "$cert_path"
    fi
    
    # 发送通知
    if [ "$NOTIFICATION_ENABLED" = "true" ]; then
        send_notification "SSL证书续期开始" "域名 $domain 的证书续期过程已开始"
    fi
}

# 续期后钩子
post_renewal_hook() {
    local domain="$1"
    local cert_path="$2"
    local success="$3"
    
    log "INFO" "执行续期后钩子: $domain, 成功: $success"
    
    if [ "$success" = "true" ]; then
        # 续期成功
        log "SUCCESS" "证书续期成功: $domain"
        
        # 验证新证书
        if openssl x509 -in "$cert_path" -text -noout &> /dev/null; then
            log "SUCCESS" "新证书验证通过"
            
            # 重启相关服务
            restart_services "$domain"
            
            # 更新证书权限
            update_certificate_permissions "$cert_path"
            
            # 测试HTTPS连接
            test_https_after_renewal "$domain"
            
            # 清理备份
            cleanup_old_backups
            
        else
            log "ERROR" "新证书验证失败"
            success="false"
        fi
    else
        # 续期失败
        log "ERROR" "证书续期失败: $domain"
        
        # 恢复备份（如果需要）
        restore_certificate_backup "$domain"
    fi
    
    # 执行自定义后处理脚本
    if [ -f "/opt/ssl/hooks/post-renewal-custom.sh" ]; then
        log "INFO" "执行自定义续期后脚本"
        bash "/opt/ssl/hooks/post-renewal-custom.sh" "$domain" "$cert_path" "$success"
    fi
    
    # 发送通知
    if [ "$NOTIFICATION_ENABLED" = "true" ]; then
        if [ "$success" = "true" ]; then
            send_notification "SSL证书续期成功" "域名 $domain 的证书续期已成功完成"
        else
            send_notification "SSL证书续期失败" "域名 $domain 的证书续期失败，请检查日志"
        fi
    fi
    
    # 清理临时文件
    rm -f "/tmp/ssl-services-status-$domain"
}

# 重启相关服务
restart_services() {
    local domain="$1"
    
    log "INFO" "重启相关服务"
    
    # 检查服务状态文件
    if [ -f "/tmp/ssl-services-status-$domain" ]; then
        while IFS=':' read -r service status; do
            if [ "$status" = "active" ]; then
                log "INFO" "重启服务: $service"
                if systemctl restart "$service"; then
                    log "SUCCESS" "服务重启成功: $service"
                else
                    log "ERROR" "服务重启失败: $service"
                fi
            fi
        done < "/tmp/ssl-services-status-$domain"
    else
        # 默认重启Nginx
        if systemctl is-active --quiet nginx; then
            log "INFO" "重载Nginx配置"
            if systemctl reload nginx; then
                log "SUCCESS" "Nginx重载成功"
            else
                log "ERROR" "Nginx重载失败"
            fi
        fi
    fi
}

# 更新证书权限
update_certificate_permissions() {
    local cert_path="$1"
    local cert_dir="$(dirname "$cert_path")"
    
    log "INFO" "更新证书权限"
    
    # 设置证书文件权限
    find "$cert_dir" -name "*.pem" -type f -exec chmod 644 {} \;
    find "$cert_dir" -name "*key*.pem" -type f -exec chmod 600 {} \;
    
    # 设置所有者
    chown -R root:root "$cert_dir"
    
    log "SUCCESS" "证书权限更新完成"
}

# 续期后测试HTTPS连接
test_https_after_renewal() {
    local domain="$1"
    
    log "INFO" "测试HTTPS连接: $domain"
    
    # 等待服务重启完成
    sleep 5
    
    # 测试HTTPS连接
    local response_code
    response_code=$(curl -s -o /dev/null -w "%{http_code}" "https://$domain" --max-time 30 --connect-timeout 10 || echo "000")
    
    if [[ "$response_code" =~ ^[23] ]]; then
        log "SUCCESS" "HTTPS连接测试通过 (HTTP $response_code)"
        
        # 获取证书信息
        local cert_info
        cert_info=$(echo | openssl s_client -connect "$domain:443" -servername "$domain" 2>/dev/null | openssl x509 -noout -dates 2>/dev/null)
        if [ $? -eq 0 ]; then
            log "INFO" "新证书信息:"
            echo "$cert_info" | while IFS= read -r line; do
                log "INFO" "  $line"
            done
        fi
        
    else
        log "ERROR" "HTTPS连接测试失败 (HTTP $response_code)"
    fi
}

# 恢复证书备份
restore_certificate_backup() {
    local domain="$1"
    
    log "WARNING" "尝试恢复证书备份: $domain"
    
    # 查找最新的备份
    local backup_dir
    backup_dir=$(find /tmp -name "ssl-renewal-backup-*" -type d | sort -r | head -1)
    
    if [ -n "$backup_dir" ] && [ -d "$backup_dir" ]; then
        log "INFO" "找到备份目录: $backup_dir"
        
        # 恢复证书文件
        local cert_domain_dir
        cert_domain_dir=$(find "$backup_dir" -name "$domain" -type d | head -1)
        
        if [ -n "$cert_domain_dir" ] && [ -d "$cert_domain_dir" ]; then
            local target_dir="/opt/ssl/certs/$domain"
            cp -r "$cert_domain_dir"/* "$target_dir/"
            log "SUCCESS" "证书文件已恢复"
            
            # 重启服务
            restart_services "$domain"
        else
            log "ERROR" "备份目录中未找到域名文件夹: $domain"
        fi
    else
        log "ERROR" "未找到证书备份"
    fi
}

# 清理旧备份
cleanup_old_backups() {
    log "INFO" "清理旧的临时备份"
    
    # 删除1小时前的临时备份
    find /tmp -name "ssl-renewal-backup-*" -type d -mmin +60 -exec rm -rf {} + 2>/dev/null || true
    
    log "SUCCESS" "旧备份清理完成"
}

# 发送通知
send_notification() {
    local subject="$1"
    local message="$2"
    
    if [ "$NOTIFICATION_ENABLED" != "true" ]; then
        return 0
    fi
    
    # 邮件通知
    if command -v mail &> /dev/null && [ -n "$NOTIFICATION_EMAIL" ]; then
        echo "$message" | mail -s "$subject - $(hostname)" "$NOTIFICATION_EMAIL"
        log "INFO" "通知邮件已发送"
    fi
    
    # Webhook通知
    if [ -n "$NOTIFICATION_WEBHOOK" ]; then
        local webhook_payload
        webhook_payload=$(cat << EOF
{
    "hostname": "$(hostname)",
    "timestamp": "$(date -Iseconds)",
    "subject": "$subject",
    "message": "$message",
    "domain": "$domain"
}
EOF
        )
        
        curl -s -X POST -H "Content-Type: application/json" -d "$webhook_payload" "$NOTIFICATION_WEBHOOK" &>/dev/null || true
        log "INFO" "Webhook通知已发送"
    fi
}

# Docker容器重启支持
restart_docker_containers() {
    local domain="$1"
    
    if [ "$DOCKER_ENABLED" = "true" ]; then
        log "INFO" "重启Docker容器"
        
        # 重启Nginx容器
        if [ -n "$DOCKER_NGINX_CONTAINER" ]; then
            if docker restart "$DOCKER_NGINX_CONTAINER"; then
                log "SUCCESS" "Nginx容器重启成功"
            else
                log "ERROR" "Nginx容器重启失败"
            fi
        fi
        
        # 重启应用容器
        if [ -n "$DOCKER_APP_CONTAINER" ]; then
            if docker restart "$DOCKER_APP_CONTAINER"; then
                log "SUCCESS" "应用容器重启成功"
            else
                log "ERROR" "应用容器重启失败"
            fi
        fi
    fi
}

# 主函数
main() {
    local action="$1"
    local domain="$2"
    local cert_path="$3"
    local success="$4"
    
    load_config
    
    case "$action" in
        "pre")
            pre_renewal_hook "$domain" "$cert_path"
            ;;
        "post")
            post_renewal_hook "$domain" "$cert_path" "$success"
            ;;
        "test")
            test_https_after_renewal "$domain"
            ;;
        *)
            echo "用法: $0 <action> <domain> [cert_path] [success]"
            echo ""
            echo "Actions:"
            echo "  pre <domain> <cert_path>           - 续期前钩子"
            echo "  post <domain> <cert_path> <success> - 续期后钩子"
            echo "  test <domain>                      - 测试HTTPS连接"
            echo ""
            exit 1
            ;;
    esac
}

# 运行主函数
main "$@"