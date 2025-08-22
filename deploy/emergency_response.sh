#!/bin/bash

# 黑客攻击应急响应脚本
# 作者: 企业邮件系统安全团队
# 版本: 1.0
# 用途: 在检测到攻击时自动执行应急响应措施

set -e  # 出错时退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 配置变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="/var/log/email-system-security"
BACKUP_DIR="/backup/emergency"
APP_DIR="/opt/email-system"
CONFIG_DIR="/etc/email-system"

# 创建必要目录
mkdir -p "$LOG_DIR" "$BACKUP_DIR"

# 安全响应函数

# 1. 立即阻断攻击者IP
block_attacker_ip() {
    local ip=$1
    local reason=${2:-"EMERGENCY_BLOCK"}
    
    log_warn "正在阻断攻击者IP: $ip"
    
    # 使用iptables阻断IP
    if command -v iptables >/dev/null 2>&1; then
        iptables -I INPUT -s "$ip" -j DROP
        iptables -I OUTPUT -d "$ip" -j DROP
        log_info "已使用iptables阻断IP: $ip"
    fi
    
    # 使用firewalld阻断IP
    if command -v firewall-cmd >/dev/null 2>&1; then
        firewall-cmd --permanent --add-rich-rule="rule family='ipv4' source address='$ip' reject"
        firewall-cmd --reload
        log_info "已使用firewalld阻断IP: $ip"
    fi
    
    # 使用UFW阻断IP
    if command -v ufw >/dev/null 2>&1; then
        ufw deny from "$ip"
        log_info "已使用UFW阻断IP: $ip"
    fi
    
    # 记录阻断信息
    echo "$(date '+%Y-%m-%d %H:%M:%S') - BLOCKED IP: $ip - REASON: $reason" >> "$LOG_DIR/blocked_ips.log"
}

# 2. 启用DDoS防护
enable_ddos_protection() {
    log_warn "启用DDoS防护..."
    
    if command -v iptables >/dev/null 2>&1; then
        # 限制连接数
        iptables -A INPUT -p tcp --dport 80 -m connlimit --connlimit-above 20 -j REJECT --reject-with tcp-reset
        iptables -A INPUT -p tcp --dport 443 -m connlimit --connlimit-above 20 -j REJECT --reject-with tcp-reset
        
        # 限制新连接频率
        iptables -A INPUT -p tcp --dport 80 -m state --state NEW -m recent --set
        iptables -A INPUT -p tcp --dport 80 -m state --state NEW -m recent --update --seconds 60 --hitcount 10 -j DROP
        
        # 防止SYN flood攻击
        iptables -A INPUT -p tcp --syn -m limit --limit 1/s --limit-burst 3 -j RETURN
        iptables -A INPUT -p tcp --syn -j DROP
        
        log_info "DDoS防护规则已启用"
    fi
}

# 3. 紧急系统备份
emergency_backup() {
    local reason=${1:-"EMERGENCY_BACKUP"}
    local timestamp=$(date '+%Y%m%d_%H%M%S')
    local backup_path="$BACKUP_DIR/emergency_$timestamp"
    
    log_warn "开始紧急系统备份..."
    mkdir -p "$backup_path"
    
    # 备份数据库
    if command -v mysqldump >/dev/null 2>&1; then
        log_info "备份MySQL数据库..."
        mysqldump --single-transaction --routines --triggers email_system > "$backup_path/database_$timestamp.sql"
        gzip "$backup_path/database_$timestamp.sql"
        log_info "数据库备份完成: $backup_path/database_$timestamp.sql.gz"
    fi
    
    # 备份应用程序
    if [ -d "$APP_DIR" ]; then
        log_info "备份应用程序..."
        tar -czf "$backup_path/application_$timestamp.tar.gz" -C "$APP_DIR" .
        log_info "应用程序备份完成: $backup_path/application_$timestamp.tar.gz"
    fi
    
    # 备份配置文件
    if [ -d "$CONFIG_DIR" ]; then
        log_info "备份配置文件..."
        tar -czf "$backup_path/config_$timestamp.tar.gz" -C "$CONFIG_DIR" .
        log_info "配置文件备份完成: $backup_path/config_$timestamp.tar.gz"
    fi
    
    # 备份日志文件
    if [ -d "$LOG_DIR" ]; then
        log_info "备份日志文件..."
        tar -czf "$backup_path/logs_$timestamp.tar.gz" -C "$LOG_DIR" .
        log_info "日志文件备份完成: $backup_path/logs_$timestamp.tar.gz"
    fi
    
    # 生成备份清单
    cat > "$backup_path/backup_manifest.txt" << EOF
紧急备份清单
===================
备份时间: $(date)
备份原因: $reason
备份位置: $backup_path

包含文件:
$(ls -la "$backup_path")
EOF
    
    log_info "紧急备份完成: $backup_path"
    echo "$backup_path"
}

# 4. 网络隔离
network_isolation() {
    log_warn "启动网络隔离..."
    
    # 断开外部网络连接
    if command -v iptables >/dev/null 2>&1; then
        # 保存当前规则
        iptables-save > "$LOG_DIR/iptables_backup_$(date +%s).rules"
        
        # 阻断所有外部连接，只保留内网和SSH
        iptables -P INPUT DROP
        iptables -P FORWARD DROP
        iptables -P OUTPUT DROP
        
        # 允许本地回环
        iptables -A INPUT -i lo -j ACCEPT
        iptables -A OUTPUT -o lo -j ACCEPT
        
        # 允许SSH（紧急访问）
        iptables -A INPUT -p tcp --dport 22 -j ACCEPT
        iptables -A OUTPUT -p tcp --sport 22 -j ACCEPT
        
        # 允许内网通信
        iptables -A INPUT -s 192.168.0.0/16 -j ACCEPT
        iptables -A OUTPUT -d 192.168.0.0/16 -j ACCEPT
        iptables -A INPUT -s 172.16.0.0/12 -j ACCEPT
        iptables -A OUTPUT -d 172.16.0.0/12 -j ACCEPT
        iptables -A INPUT -s 10.0.0.0/8 -j ACCEPT
        iptables -A OUTPUT -d 10.0.0.0/8 -j ACCEPT
        
        log_info "网络隔离规则已启用"
    fi
}

# 5. 进程检查和清理
check_and_clean_processes() {
    log_warn "检查可疑进程..."
    
    # 可疑进程模式
    local suspicious_patterns=(
        "nc.*-l"           # netcat监听
        "python.*http"     # Python HTTP服务器
        "perl.*socket"     # Perl socket
        "bash.*dev/tcp"    # Bash TCP连接
        "sh.*dev/tcp"      # Shell TCP连接
        "socat"            # socat工具
        "nmap"             # 端口扫描
        "masscan"          # 大规模扫描
        "sqlmap"           # SQL注入工具
        "metasploit"       # 渗透测试框架
    )
    
    for pattern in "${suspicious_patterns[@]}"; do
        local pids=$(pgrep -f "$pattern" 2>/dev/null || true)
        if [ -n "$pids" ]; then
            log_warn "发现可疑进程: $pattern (PID: $pids)"
            echo "$pids" | xargs -r kill -9
            log_info "已终止可疑进程: $pids"
        fi
    done
    
    # 检查网络连接
    log_info "检查异常网络连接..."
    netstat -tulpn | grep -E "(LISTEN|ESTABLISHED)" | while read line; do
        # 检查非标准端口的监听
        if echo "$line" | grep -E ":([2-9][0-9]{3,}|[1-9][0-9]{4,})" | grep -q LISTEN; then
            log_warn "发现异常端口监听: $line"
        fi
    done
}

# 6. 文件完整性检查
file_integrity_check() {
    log_warn "执行文件完整性检查..."
    
    # 检查关键系统文件
    local critical_files=(
        "/etc/passwd"
        "/etc/shadow"
        "/etc/hosts"
        "/etc/ssh/sshd_config"
        "/etc/crontab"
        "/etc/sudoers"
    )
    
    for file in "${critical_files[@]}"; do
        if [ -f "$file" ]; then
            local current_hash=$(sha256sum "$file" | cut -d' ' -f1)
            local baseline_file="$LOG_DIR/baseline_$(basename "$file").sha256"
            
            if [ -f "$baseline_file" ]; then
                local baseline_hash=$(cat "$baseline_file")
                if [ "$current_hash" != "$baseline_hash" ]; then
                    log_error "文件完整性违规: $file"
                    log_error "基线哈希: $baseline_hash"
                    log_error "当前哈希: $current_hash"
                fi
            else
                # 创建基线
                echo "$current_hash" > "$baseline_file"
                log_info "创建文件基线: $file"
            fi
        fi
    done
}

# 7. 取证数据收集
collect_forensic_data() {
    local incident_id=${1:-"INCIDENT_$(date +%s)"}
    local forensic_dir="$LOG_DIR/forensics_$incident_id"
    
    log_warn "开始收集取证数据..."
    mkdir -p "$forensic_dir"
    
    # 系统信息
    log_info "收集系统信息..."
    {
        echo "=== 系统信息 ==="
        uname -a
        date
        uptime
        echo ""
        
        echo "=== 用户信息 ==="
        who
        w
        last -n 20
        echo ""
        
        echo "=== 进程信息 ==="
        ps auxf
        echo ""
        
        echo "=== 网络连接 ==="
        netstat -tulpn
        ss -tulpn
        echo ""
        
        echo "=== 文件系统 ==="
        df -h
        mount
        echo ""
        
        echo "=== 最近登录 ==="
        lastlog
        echo ""
        
    } > "$forensic_dir/system_info.txt"
    
    # 网络配置
    log_info "收集网络配置..."
    {
        ip addr show
        ip route show
        iptables -L -n -v
    } > "$forensic_dir/network_config.txt"
    
    # 系统日志
    log_info "收集系统日志..."
    if [ -f "/var/log/auth.log" ]; then
        tail -1000 /var/log/auth.log > "$forensic_dir/auth.log"
    fi
    if [ -f "/var/log/syslog" ]; then
        tail -1000 /var/log/syslog > "$forensic_dir/syslog"
    fi
    
    # 应用程序日志
    if [ -d "$LOG_DIR" ]; then
        cp -r "$LOG_DIR"/* "$forensic_dir/"
    fi
    
    # 创建取证报告
    cat > "$forensic_dir/forensic_report.txt" << EOF
取证数据收集报告
===================
事件ID: $incident_id
收集时间: $(date)
收集位置: $forensic_dir

收集内容:
$(ls -la "$forensic_dir")

系统快照:
- 主机名: $(hostname)
- 内核版本: $(uname -r)
- 系统负载: $(uptime | cut -d',' -f3-)
- 内存使用: $(free -h | grep Mem)
- 磁盘使用: $(df -h / | tail -1)
EOF
    
    log_info "取证数据收集完成: $forensic_dir"
    echo "$forensic_dir"
}

# 8. 发送告警通知
send_alert_notification() {
    local alert_type=$1
    local message=$2
    local severity=${3:-"HIGH"}
    
    log_warn "发送告警通知: $alert_type"
    
    # 创建告警文件
    local alert_file="$LOG_DIR/alert_$(date +%s).json"
    cat > "$alert_file" << EOF
{
    "timestamp": "$(date -Iseconds)",
    "alert_type": "$alert_type",
    "severity": "$severity",
    "hostname": "$(hostname)",
    "message": "$message",
    "source": "emergency_response_script"
}
EOF
    
    # 发送到syslog
    logger -t "SECURITY_ALERT" -p local0.crit "$alert_type: $message"
    
    # 如果配置了邮件，发送邮件告警
    if command -v mail >/dev/null 2>&1 && [ -n "${ADMIN_EMAIL:-}" ]; then
        {
            echo "紧急安全告警"
            echo "============="
            echo "告警类型: $alert_type"
            echo "严重级别: $severity"
            echo "主机名称: $(hostname)"
            echo "发生时间: $(date)"
            echo "详细信息: $message"
            echo ""
            echo "请立即检查系统状态并采取相应措施。"
        } | mail -s "🚨 紧急安全告警: $alert_type" "$ADMIN_EMAIL"
        log_info "告警邮件已发送到: $ADMIN_EMAIL"
    fi
    
    log_info "告警通知已发送: $alert_file"
}

# 9. 系统恢复检查
system_recovery_check() {
    log_warn "执行系统恢复检查..."
    
    local checks_passed=0
    local total_checks=0
    
    # 检查服务状态
    log_info "检查关键服务状态..."
    local critical_services=("ssh" "mysql" "nginx" "email-system")
    
    for service in "${critical_services[@]}"; do
        ((total_checks++))
        if systemctl is-active --quiet "$service" 2>/dev/null; then
            log_info "服务正常: $service"
            ((checks_passed++))
        else
            log_error "服务异常: $service"
        fi
    done
    
    # 检查网络连通性
    log_info "检查网络连通性..."
    ((total_checks++))
    if ping -c 1 8.8.8.8 >/dev/null 2>&1; then
        log_info "外网连通性正常"
        ((checks_passed++))
    else
        log_error "外网连通性异常"
    fi
    
    # 检查磁盘空间
    log_info "检查磁盘空间..."
    ((total_checks++))
    local disk_usage=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
    if [ "$disk_usage" -lt 90 ]; then
        log_info "磁盘空间充足: ${disk_usage}%"
        ((checks_passed++))
    else
        log_error "磁盘空间不足: ${disk_usage}%"
    fi
    
    # 检查负载
    log_info "检查系统负载..."
    ((total_checks++))
    local load_avg=$(uptime | awk -F'load average:' '{print $2}' | awk '{print $1}' | sed 's/,//')
    if (( $(echo "$load_avg < 5.0" | bc -l) )); then
        log_info "系统负载正常: $load_avg"
        ((checks_passed++))
    else
        log_error "系统负载过高: $load_avg"
    fi
    
    # 生成恢复报告
    local recovery_score=$(( checks_passed * 100 / total_checks ))
    
    {
        echo "系统恢复检查报告"
        echo "=================="
        echo "检查时间: $(date)"
        echo "通过检查: $checks_passed/$total_checks"
        echo "恢复评分: $recovery_score%"
        echo ""
        if [ "$recovery_score" -ge 80 ]; then
            echo "状态: 系统恢复良好"
        elif [ "$recovery_score" -ge 60 ]; then
            echo "状态: 系统部分恢复"
        else
            echo "状态: 系统恢复不完整"
        fi
    } > "$LOG_DIR/recovery_report_$(date +%s).txt"
    
    log_info "系统恢复检查完成，评分: $recovery_score%"
    return $((100 - recovery_score))
}

# 主要响应函数
emergency_response() {
    local attack_type=${1:-"UNKNOWN"}
    local attacker_ip=${2:-""}
    local additional_info=${3:-""}
    
    log_error "🚨 启动紧急响应流程 🚨"
    log_error "攻击类型: $attack_type"
    log_error "攻击者IP: $attacker_ip"
    log_error "额外信息: $additional_info"
    
    # 创建事件ID
    local incident_id="INCIDENT_$(date +%s)"
    local response_log="$LOG_DIR/emergency_response_$incident_id.log"
    
    # 记录响应开始
    {
        echo "=================================="
        echo "紧急安全响应日志"
        echo "=================================="
        echo "事件ID: $incident_id"
        echo "开始时间: $(date)"
        echo "攻击类型: $attack_type"
        echo "攻击者IP: $attacker_ip"
        echo "额外信息: $additional_info"
        echo "=================================="
        echo ""
    } > "$response_log"
    
    # 执行响应步骤
    local steps=(
        "立即阻断攻击者"
        "启用DDoS防护"
        "执行紧急备份"
        "网络隔离"
        "进程检查清理"
        "文件完整性检查"
        "收集取证数据"
        "发送告警通知"
    )
    
    for i in "${!steps[@]}"; do
        local step_num=$((i + 1))
        local step_name="${steps[$i]}"
        
        log_warn "步骤 $step_num: $step_name"
        echo "[$step_num] $(date): 开始 - $step_name" >> "$response_log"
        
        case $step_num in
            1)
                if [ -n "$attacker_ip" ]; then
                    block_attacker_ip "$attacker_ip" "$attack_type"
                fi
                ;;
            2)
                enable_ddos_protection
                ;;
            3)
                local backup_path
                backup_path=$(emergency_backup "$attack_type")
                echo "备份位置: $backup_path" >> "$response_log"
                ;;
            4)
                if [[ "$attack_type" =~ (DDOS|NETWORK|INTRUSION) ]]; then
                    network_isolation
                fi
                ;;
            5)
                check_and_clean_processes
                ;;
            6)
                file_integrity_check
                ;;
            7)
                local forensic_path
                forensic_path=$(collect_forensic_data "$incident_id")
                echo "取证数据: $forensic_path" >> "$response_log"
                ;;
            8)
                send_alert_notification "$attack_type" "事件ID: $incident_id, 攻击者IP: $attacker_ip" "CRITICAL"
                ;;
        esac
        
        echo "[$step_num] $(date): 完成 - $step_name" >> "$response_log"
        log_info "步骤 $step_num 完成: $step_name"
    done
    
    # 响应完成
    {
        echo ""
        echo "=================================="
        echo "紧急响应完成"
        echo "完成时间: $(date)"
        echo "事件状态: 已处理"
        echo "后续建议: 请检查系统状态并进行恢复验证"
        echo "=================================="
    } >> "$response_log"
    
    log_info "🚨 紧急响应流程完成，事件ID: $incident_id"
    log_info "响应日志: $response_log"
    
    # 可选: 自动进行恢复检查
    if [ "${AUTO_RECOVERY_CHECK:-false}" = "true" ]; then
        log_info "开始自动恢复检查..."
        system_recovery_check
    fi
}

# 脚本使用说明
usage() {
    echo "用法: $0 [选项] <攻击类型> [攻击者IP] [额外信息]"
    echo ""
    echo "选项:"
    echo "  -h, --help          显示此帮助信息"
    echo "  -b, --backup-only   仅执行紧急备份"
    echo "  -i, --isolate       网络隔离模式"
    echo "  -c, --check         系统恢复检查"
    echo "  -f, --forensic      仅收集取证数据"
    echo ""
    echo "攻击类型:"
    echo "  SQL_INJECTION       SQL注入攻击"
    echo "  XSS_ATTACK         XSS攻击"
    echo "  DDOS_ATTACK        DDoS攻击"
    echo "  BRUTE_FORCE        暴力破解"
    echo "  MALWARE            恶意软件"
    echo "  INTRUSION          入侵检测"
    echo "  DATA_BREACH        数据泄露"
    echo ""
    echo "示例:"
    echo "  $0 SQL_INJECTION 192.168.1.100"
    echo "  $0 DDOS_ATTACK 10.0.0.50 '大量请求'"
    echo "  $0 --backup-only"
    echo "  $0 --check"
}

# 主程序
main() {
    # 检查权限
    if [ "$EUID" -ne 0 ]; then
        log_error "此脚本需要root权限运行"
        exit 1
    fi
    
    # 解析命令行参数
    case "${1:-}" in
        -h|--help)
            usage
            exit 0
            ;;
        -b|--backup-only)
            emergency_backup "MANUAL_BACKUP"
            exit 0
            ;;
        -i|--isolate)
            network_isolation
            exit 0
            ;;
        -c|--check)
            system_recovery_check
            exit $?
            ;;
        -f|--forensic)
            collect_forensic_data "MANUAL_FORENSIC"
            exit 0
            ;;
        "")
            log_error "请指定攻击类型"
            usage
            exit 1
            ;;
        *)
            # 执行完整应急响应
            emergency_response "$@"
            ;;
    esac
}

# 信号处理
trap 'log_error "应急响应脚本被中断"; exit 130' INT TERM

# 执行主程序
main "$@"