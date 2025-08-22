#!/bin/bash

# SSL Certificate Monitoring Script
# SSL证书监控脚本 - 检查证书状态、过期时间和配置

set -e

# 脚本配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/ssl-config.conf"
LOG_FILE="/var/log/ssl-monitor.log"
REPORT_FILE="/tmp/ssl-monitor-report.txt"

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
        log "INFO" "已加载配置文件: $CONFIG_FILE"
    else
        # 默认配置
        SSL_CERTS_DIR="${SSL_CERTS_DIR:-/opt/ssl/certs}"
        RENEWAL_DAYS_BEFORE="${RENEWAL_DAYS_BEFORE:-30}"
        NOTIFICATION_ENABLED="${NOTIFICATION_ENABLED:-true}"
        NOTIFICATION_EMAIL="${NOTIFICATION_EMAIL:-admin@example.com}"
    fi
}

# 获取证书信息
get_certificate_info() {
    local cert_file="$1"
    local domain_name="$2"
    
    if [ ! -f "$cert_file" ]; then
        echo "FILE_NOT_FOUND|$domain_name|证书文件不存在"
        return 1
    fi
    
    # 获取证书基本信息
    local subject issuer not_before not_after serial_number
    
    subject=$(openssl x509 -noout -subject -in "$cert_file" 2>/dev/null | sed 's/subject=//')
    issuer=$(openssl x509 -noout -issuer -in "$cert_file" 2>/dev/null | sed 's/issuer=//')
    not_before=$(openssl x509 -noout -startdate -in "$cert_file" 2>/dev/null | cut -d= -f2)
    not_after=$(openssl x509 -noout -enddate -in "$cert_file" 2>/dev/null | cut -d= -f2)
    serial_number=$(openssl x509 -noout -serial -in "$cert_file" 2>/dev/null | cut -d= -f2)
    
    # 计算剩余天数
    local expiry_epoch current_epoch days_until_expiry
    expiry_epoch=$(date -d "$not_after" +%s 2>/dev/null || echo "0")
    current_epoch=$(date +%s)
    days_until_expiry=$(( (expiry_epoch - current_epoch) / 86400 ))
    
    # 获取SAN域名列表
    local san_domains
    san_domains=$(openssl x509 -noout -text -in "$cert_file" 2>/dev/null | \
                  grep -A1 "Subject Alternative Name" | tail -1 | \
                  tr ',' '\n' | grep "DNS:" | cut -d':' -f2 | tr -d ' ' | tr '\n' ',' | sed 's/,$//')
    
    # 检查证书状态
    local status="VALID"
    if [ "$days_until_expiry" -lt 0 ]; then
        status="EXPIRED"
    elif [ "$days_until_expiry" -le "$RENEWAL_DAYS_BEFORE" ]; then
        status="EXPIRING_SOON"
    fi
    
    # 验证证书链
    local chain_valid="UNKNOWN"
    if openssl verify -CApath /etc/ssl/certs "$cert_file" &>/dev/null; then
        chain_valid="VALID"
    else
        chain_valid="INVALID"
    fi
    
    echo "$status|$domain_name|$days_until_expiry|$not_after|$subject|$issuer|$serial_number|$san_domains|$chain_valid"
}

# 测试HTTPS连接
test_https_connection() {
    local domain="$1"
    local port="${2:-443}"
    
    log "INFO" "测试HTTPS连接: $domain:$port"
    
    # 测试TCP连接
    if ! timeout 10 nc -z "$domain" "$port" 2>/dev/null; then
        echo "CONNECTION_FAILED|$domain|无法建立TCP连接"
        return 1
    fi
    
    # 测试SSL握手
    local ssl_info connection_status
    ssl_info=$(timeout 10 echo | openssl s_client -connect "$domain:$port" -servername "$domain" 2>/dev/null)
    
    if echo "$ssl_info" | grep -q "Verify return code: 0 (ok)"; then
        connection_status="SSL_OK"
    else
        local verify_result
        verify_result=$(echo "$ssl_info" | grep "Verify return code:" | cut -d':' -f2- | tr -d ' ')
        connection_status="SSL_ERROR: $verify_result"
    fi
    
    # 获取证书有效期信息
    local cert_dates
    cert_dates=$(echo "$ssl_info" | openssl x509 -noout -dates 2>/dev/null | tr '\n' '|')
    
    # 获取协议和加密套件信息
    local protocol cipher
    protocol=$(echo "$ssl_info" | grep "Protocol" | cut -d':' -f2 | tr -d ' ')
    cipher=$(echo "$ssl_info" | grep "Cipher" | cut -d':' -f2 | tr -d ' ')
    
    echo "$connection_status|$domain|$protocol|$cipher|$cert_dates"
}

# 检查Nginx配置
check_nginx_config() {
    local domain="$1"
    local config_file="/etc/nginx/conf.d/$domain-ssl.conf"
    
    if [ ! -f "$config_file" ]; then
        echo "CONFIG_NOT_FOUND|$domain|Nginx配置文件不存在"
        return 1
    fi
    
    # 检查配置语法
    if nginx -t 2>/dev/null; then
        echo "CONFIG_VALID|$domain|Nginx配置语法正确"
    else
        echo "CONFIG_INVALID|$domain|Nginx配置语法错误"
    fi
}

# 检查证书文件权限
check_file_permissions() {
    local cert_file="$1"
    local key_file="$2"
    local domain="$3"
    
    local issues=()
    
    # 检查证书文件权限
    if [ -f "$cert_file" ]; then
        local cert_perms
        cert_perms=$(stat -c "%a" "$cert_file")
        if [ "$cert_perms" != "644" ] && [ "$cert_perms" != "600" ]; then
            issues+=("证书文件权限不当: $cert_perms")
        fi
    fi
    
    # 检查私钥文件权限
    if [ -f "$key_file" ]; then
        local key_perms
        key_perms=$(stat -c "%a" "$key_file")
        if [ "$key_perms" != "600" ]; then
            issues+=("私钥文件权限不当: $key_perms (应为600)")
        fi
    fi
    
    if [ ${#issues[@]} -eq 0 ]; then
        echo "PERMISSIONS_OK|$domain|文件权限正常"
    else
        local issue_list
        issue_list=$(IFS='; '; echo "${issues[*]}")
        echo "PERMISSIONS_ISSUE|$domain|$issue_list"
    fi
}

# 扫描所有证书
scan_certificates() {
    log "INFO" "开始扫描SSL证书..."
    
    local total_certs=0
    local valid_certs=0
    local expiring_certs=0
    local expired_certs=0
    local invalid_certs=0
    
    # 创建报告文件
    cat > "$REPORT_FILE" << EOF
SSL Certificate Monitoring Report
=================================
Generated: $(date)
Server: $(hostname)

Certificate Status Summary:
EOF
    
    # 扫描证书目录
    if [ -d "$SSL_CERTS_DIR" ]; then
        for domain_dir in "$SSL_CERTS_DIR"/*/; do
            if [ -d "$domain_dir" ]; then
                local domain
                domain=$(basename "$domain_dir")
                
                log "INFO" "检查域名: $domain"
                
                local cert_file="$domain_dir/cert.pem"
                local fullchain_file="$domain_dir/fullchain.pem"
                local key_file="$domain_dir/key.pem"
                
                # 优先使用fullchain文件
                if [ -f "$fullchain_file" ]; then
                    cert_file="$fullchain_file"
                fi
                
                total_certs=$((total_certs + 1))
                
                # 获取证书信息
                local cert_info
                cert_info=$(get_certificate_info "$cert_file" "$domain")
                local status
                status=$(echo "$cert_info" | cut -d'|' -f1)
                
                case "$status" in
                    "VALID")
                        valid_certs=$((valid_certs + 1))
                        log "SUCCESS" "$domain: 证书有效"
                        ;;
                    "EXPIRING_SOON")
                        expiring_certs=$((expiring_certs + 1))
                        local days
                        days=$(echo "$cert_info" | cut -d'|' -f3)
                        log "WARNING" "$domain: 证书将在 $days 天后过期"
                        ;;
                    "EXPIRED")
                        expired_certs=$((expired_certs + 1))
                        log "ERROR" "$domain: 证书已过期"
                        ;;
                    *)
                        invalid_certs=$((invalid_certs + 1))
                        log "ERROR" "$domain: 证书状态异常"
                        ;;
                esac
                
                # 添加到报告
                echo "" >> "$REPORT_FILE"
                echo "Domain: $domain" >> "$REPORT_FILE"
                echo "Status: $status" >> "$REPORT_FILE"
                echo "Details: $cert_info" >> "$REPORT_FILE"
                
                # 测试HTTPS连接
                local https_test
                https_test=$(test_https_connection "$domain")
                echo "HTTPS Test: $https_test" >> "$REPORT_FILE"
                
                # 检查Nginx配置
                local nginx_check
                nginx_check=$(check_nginx_config "$domain")
                echo "Nginx Config: $nginx_check" >> "$REPORT_FILE"
                
                # 检查文件权限
                local perms_check
                perms_check=$(check_file_permissions "$cert_file" "$key_file" "$domain")
                echo "Permissions: $perms_check" >> "$REPORT_FILE"
            fi
        done
    else
        log "WARNING" "证书目录不存在: $SSL_CERTS_DIR"
    fi
    
    # 添加总结到报告
    cat >> "$REPORT_FILE" << EOF

Summary:
--------
Total Certificates: $total_certs
Valid Certificates: $valid_certs
Expiring Soon: $expiring_certs
Expired: $expired_certs
Invalid/Error: $invalid_certs

EOF
    
    log "INFO" "证书扫描完成"
    log "INFO" "总计: $total_certs, 有效: $valid_certs, 即将过期: $expiring_certs, 已过期: $expired_certs"
    
    # 如果有问题证书，发送通知
    if [ $expiring_certs -gt 0 ] || [ $expired_certs -gt 0 ] || [ $invalid_certs -gt 0 ]; then
        send_notification
    fi
}

# 发送通知
send_notification() {
    if [ "$NOTIFICATION_ENABLED" != "true" ]; then
        return 0
    fi
    
    log "INFO" "发送证书状态通知"
    
    local subject="SSL证书监控报告 - $(hostname) - $(date +'%Y-%m-%d')"
    
    # 发送邮件通知
    if command -v mail &> /dev/null && [ -n "$NOTIFICATION_EMAIL" ]; then
        mail -s "$subject" "$NOTIFICATION_EMAIL" < "$REPORT_FILE"
        log "SUCCESS" "邮件通知已发送到: $NOTIFICATION_EMAIL"
    fi
    
    # 发送Webhook通知（如果配置了）
    if [ -n "$NOTIFICATION_WEBHOOK" ]; then
        local webhook_payload
        webhook_payload=$(cat << EOF
{
    "hostname": "$(hostname)",
    "timestamp": "$(date -Iseconds)",
    "subject": "$subject",
    "report": $(cat "$REPORT_FILE" | jq -Rs .)
}
EOF
        )
        
        if curl -s -X POST -H "Content-Type: application/json" -d "$webhook_payload" "$NOTIFICATION_WEBHOOK" &>/dev/null; then
            log "SUCCESS" "Webhook通知已发送"
        else
            log "ERROR" "Webhook通知发送失败"
        fi
    fi
}

# 续期即将过期的证书
renew_expiring_certificates() {
    log "INFO" "检查需要续期的证书..."
    
    if [ -d "$SSL_CERTS_DIR" ]; then
        for domain_dir in "$SSL_CERTS_DIR"/*/; do
            if [ -d "$domain_dir" ]; then
                local domain
                domain=$(basename "$domain_dir")
                
                local cert_file="$domain_dir/fullchain.pem"
                if [ ! -f "$cert_file" ]; then
                    cert_file="$domain_dir/cert.pem"
                fi
                
                if [ -f "$cert_file" ]; then
                    local cert_info
                    cert_info=$(get_certificate_info "$cert_file" "$domain")
                    local status days_until_expiry
                    status=$(echo "$cert_info" | cut -d'|' -f1)
                    days_until_expiry=$(echo "$cert_info" | cut -d'|' -f3)
                    
                    if [ "$status" = "EXPIRING_SOON" ] || [ "$status" = "EXPIRED" ]; then
                        log "INFO" "尝试续期证书: $domain (剩余 $days_until_expiry 天)"
                        
                        # 检查是否为Let's Encrypt证书
                        local issuer
                        issuer=$(openssl x509 -noout -issuer -in "$cert_file" 2>/dev/null | grep -o "Let's Encrypt")
                        
                        if [ -n "$issuer" ]; then
                            # 使用certbot续期
                            if certbot renew --cert-name "$domain" --non-interactive; then
                                log "SUCCESS" "证书续期成功: $domain"
                                
                                # 重载Nginx配置
                                if systemctl reload nginx; then
                                    log "SUCCESS" "Nginx配置已重载"
                                fi
                            else
                                log "ERROR" "证书续期失败: $domain"
                            fi
                        else
                            log "WARNING" "非Let's Encrypt证书，需要手动续期: $domain"
                        fi
                    fi
                fi
            fi
        done
    fi
}

# 清理过期的备份和日志
cleanup_old_files() {
    log "INFO" "清理过期的备份和日志文件..."
    
    # 清理旧备份（超过90天）
    if [ -d "$BACKUP_DIR" ]; then
        find "$BACKUP_DIR" -type d -name "ssl_backup_*" -mtime +90 -exec rm -rf {} + 2>/dev/null || true
        log "INFO" "已清理90天前的备份文件"
    fi
    
    # 清理旧日志（超过30天）
    if [ -f "$LOG_FILE" ]; then
        find "$(dirname "$LOG_FILE")" -name "$(basename "$LOG_FILE")*" -mtime +30 -delete 2>/dev/null || true
        log "INFO" "已清理30天前的日志文件"
    fi
}

# 主函数
main() {
    local action="${1:-scan}"
    
    log "INFO" "SSL证书监控脚本启动 - 操作: $action"
    
    # 加载配置
    load_config
    
    case "$action" in
        "scan")
            scan_certificates
            ;;
        "renew")
            renew_expiring_certificates
            ;;
        "cleanup")
            cleanup_old_files
            ;;
        "notify")
            scan_certificates
            send_notification
            ;;
        "report")
            scan_certificates
            cat "$REPORT_FILE"
            ;;
        *)
            echo "用法: $0 [action]"
            echo ""
            echo "Actions:"
            echo "  scan    - 扫描所有证书状态 (默认)"
            echo "  renew   - 续期即将过期的证书"
            echo "  cleanup - 清理旧文件"
            echo "  notify  - 扫描并发送通知"
            echo "  report  - 生成并显示报告"
            echo ""
            echo "Examples:"
            echo "  $0 scan"
            echo "  $0 renew"
            echo "  $0 notify"
            exit 1
            ;;
    esac
    
    log "INFO" "SSL证书监控脚本完成"
}

# 运行主函数
main "$@"