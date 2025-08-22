#!/bin/bash

# 系统安全加固脚本
# 用于在攻击后或定期执行系统安全加固
# 版本: 2.0

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

# 配置变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="/var/log/security-hardening"
BACKUP_DIR="/backup/hardening"
CONFIG_BACKUP_DIR="$BACKUP_DIR/config_backup_$(date +%Y%m%d_%H%M%S)"

# 创建必要目录
mkdir -p "$LOG_DIR" "$BACKUP_DIR" "$CONFIG_BACKUP_DIR"

# 系统信息检测
detect_system() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$NAME
        VER=$VERSION_ID
    elif type lsb_release >/dev/null 2>&1; then
        OS=$(lsb_release -si)
        VER=$(lsb_release -sr)
    else
        OS=$(uname -s)
        VER=$(uname -r)
    fi
    
    log_info "检测到系统: $OS $VER"
}

# 备份关键配置文件
backup_configurations() {
    log_info "备份系统关键配置..."
    
    local configs=(
        "/etc/ssh/sshd_config"
        "/etc/passwd"
        "/etc/shadow"
        "/etc/sudoers"
        "/etc/hosts"
        "/etc/fstab"
        "/etc/crontab"
        "/etc/nginx/nginx.conf"
        "/etc/mysql/my.cnf"
        "/etc/iptables/rules.v4"
    )
    
    for config in "${configs[@]}"; do
        if [ -f "$config" ]; then
            cp "$config" "$CONFIG_BACKUP_DIR/" 2>/dev/null || true
            log_info "已备份: $config"
        fi
    done
    
    log_info "配置文件备份完成: $CONFIG_BACKUP_DIR"
}

# SSH安全加固
harden_ssh() {
    log_info "开始SSH安全加固..."
    
    local ssh_config="/etc/ssh/sshd_config"
    if [ ! -f "$ssh_config" ]; then
        log_error "SSH配置文件不存在: $ssh_config"
        return 1
    fi
    
    # 备份原配置
    cp "$ssh_config" "$ssh_config.backup.$(date +%s)"
    
    # SSH安全配置
    cat > /tmp/ssh_hardening.conf << 'EOF'
# SSH安全加固配置
Protocol 2
Port 2222
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
AuthorizedKeysFile %h/.ssh/authorized_keys
PermitEmptyPasswords no
ChallengeResponseAuthentication no
UsePAM yes
X11Forwarding no
PrintMotd no
PrintLastLog yes
TCPKeepAlive yes
ClientAliveInterval 300
ClientAliveCountMax 2
MaxAuthTries 3
MaxSessions 10
MaxStartups 10:30:100
LoginGraceTime 60
StrictModes yes
IgnoreRhosts yes
HostbasedAuthentication no
AllowGroups wheel ssh-users
DenyGroups games
DenyUsers guest nobody
Banner /etc/ssh/banner
Compression delayed
LogLevel VERBOSE
SyslogFacility AUTH
EOF

    # 合并配置
    grep -v -E "^(Protocol|Port|PermitRootLogin|PasswordAuthentication|PubkeyAuthentication|X11Forwarding)" "$ssh_config" > /tmp/ssh_config_clean
    cat /tmp/ssh_hardening.conf >> /tmp/ssh_config_clean
    mv /tmp/ssh_config_clean "$ssh_config"
    
    # 创建SSH警告横幅
    cat > /etc/ssh/banner << 'EOF'
**************************************************************************
                        AUTHORIZED ACCESS ONLY
                        
This system is for authorized users only. All activities are monitored
and logged. Unauthorized access is strictly prohibited and may result
in legal action.

**************************************************************************
EOF

    # 验证SSH配置
    if sshd -t; then
        log_info "SSH配置验证成功"
        systemctl reload sshd
        log_info "SSH服务已重新加载"
    else
        log_error "SSH配置验证失败，恢复备份"
        cp "$ssh_config.backup.*" "$ssh_config" 2>/dev/null || true
        return 1
    fi
}

# 防火墙加固
harden_firewall() {
    log_info "开始防火墙加固..."
    
    # 安装iptables-persistent (Ubuntu/Debian)
    if command -v apt-get >/dev/null 2>&1; then
        apt-get update && apt-get install -y iptables-persistent
    fi
    
    # 保存当前规则
    if command -v iptables-save >/dev/null 2>&1; then
        iptables-save > "$BACKUP_DIR/iptables_backup_$(date +%s).rules"
    fi
    
    # 清空现有规则
    iptables -F
    iptables -X
    iptables -t nat -F
    iptables -t nat -X
    iptables -t mangle -F
    iptables -t mangle -X
    
    # 设置默认策略
    iptables -P INPUT DROP
    iptables -P FORWARD DROP
    iptables -P OUTPUT ACCEPT
    
    # 允许本地回环
    iptables -A INPUT -i lo -j ACCEPT
    iptables -A OUTPUT -o lo -j ACCEPT
    
    # 允许已建立的连接
    iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT
    
    # SSH访问 (修改后的端口)
    iptables -A INPUT -p tcp --dport 2222 -m conntrack --ctstate NEW -m recent --set
    iptables -A INPUT -p tcp --dport 2222 -m conntrack --ctstate NEW -m recent --update --seconds 60 --hitcount 4 -j DROP
    iptables -A INPUT -p tcp --dport 2222 -j ACCEPT
    
    # HTTP/HTTPS服务
    iptables -A INPUT -p tcp --dport 80 -m connlimit --connlimit-above 25 --connlimit-mask 32 -j REJECT --reject-with tcp-reset
    iptables -A INPUT -p tcp --dport 443 -m connlimit --connlimit-above 25 --connlimit-mask 32 -j REJECT --reject-with tcp-reset
    iptables -A INPUT -p tcp --dport 80 -j ACCEPT
    iptables -A INPUT -p tcp --dport 443 -j ACCEPT
    
    # 邮件服务端口
    iptables -A INPUT -p tcp --dport 25 -j ACCEPT    # SMTP
    iptables -A INPUT -p tcp --dport 587 -j ACCEPT   # SMTP提交
    iptables -A INPUT -p tcp --dport 993 -j ACCEPT   # IMAPS
    iptables -A INPUT -p tcp --dport 995 -j ACCEPT   # POP3S
    
    # 防止端口扫描
    iptables -A INPUT -m conntrack --ctstate NEW -m recent --set
    iptables -A INPUT -m conntrack --ctstate NEW -m recent --update --seconds 60 --hitcount 10 -j DROP
    
    # 防止SYN flood攻击
    iptables -A INPUT -p tcp --syn -m limit --limit 1/s --limit-burst 3 -j RETURN
    iptables -A INPUT -p tcp --syn -j DROP
    
    # 防止ping洪水
    iptables -A INPUT -p icmp --icmp-type echo-request -m limit --limit 1/s -j ACCEPT
    iptables -A INPUT -p icmp --icmp-type echo-request -j DROP
    
    # 阻止常见攻击端口
    iptables -A INPUT -p tcp --dport 135:139 -j DROP  # Windows网络
    iptables -A INPUT -p tcp --dport 445 -j DROP      # SMB
    iptables -A INPUT -p udp --dport 137:139 -j DROP  # NetBIOS
    
    # 记录被丢弃的包
    iptables -A INPUT -m limit --limit 2/min -j LOG --log-prefix "IPTables-Dropped: " --log-level 4
    
    # 保存规则
    if command -v iptables-save >/dev/null 2>&1; then
        iptables-save > /etc/iptables/rules.v4
        log_info "防火墙规则已保存"
    fi
    
    log_info "防火墙加固完成"
}

# 内核参数优化
harden_kernel() {
    log_info "开始内核参数优化..."
    
    # 备份原配置
    cp /etc/sysctl.conf /etc/sysctl.conf.backup.$(date +%s) 2>/dev/null || true
    
    # 内核安全参数
    cat >> /etc/sysctl.conf << 'EOF'

# 安全加固参数
# 禁用IP转发
net.ipv4.ip_forward = 0
net.ipv6.conf.all.forwarding = 0

# 禁用源路由
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.default.accept_source_route = 0
net.ipv6.conf.all.accept_source_route = 0
net.ipv6.conf.default.accept_source_route = 0

# 禁用ICMP重定向
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv6.conf.all.accept_redirects = 0
net.ipv6.conf.default.accept_redirects = 0

# 禁用发送ICMP重定向
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.default.send_redirects = 0

# 忽略ICMP ping请求
net.ipv4.icmp_echo_ignore_all = 1

# 忽略广播ping
net.ipv4.icmp_echo_ignore_broadcasts = 1

# 启用SYN cookies
net.ipv4.tcp_syncookies = 1

# 记录欺骗包、源路由包、重定向包
net.ipv4.conf.all.log_martians = 1
net.ipv4.conf.default.log_martians = 1

# 防止SYN flood攻击
net.ipv4.tcp_max_syn_backlog = 2048
net.ipv4.tcp_synack_retries = 2
net.ipv4.tcp_syn_retries = 5

# 减少TCP keepalive时间
net.ipv4.tcp_keepalive_time = 1800
net.ipv4.tcp_keepalive_intvl = 15
net.ipv4.tcp_keepalive_probes = 5

# 增加安全随机性
kernel.randomize_va_space = 2

# 禁用magic sysrq键
kernel.sysrq = 0

# 控制core dumps
fs.suid_dumpable = 0

# 限制进程数
kernel.pid_max = 65536

# 共享内存限制
kernel.shmmax = 68719476736
kernel.shmall = 4294967296

# 文件描述符限制
fs.file-max = 2097152

# 网络性能优化
net.core.rmem_default = 262144
net.core.rmem_max = 16777216
net.core.wmem_default = 262144
net.core.wmem_max = 16777216
net.core.netdev_max_backlog = 5000

# TCP缓冲区优化
net.ipv4.tcp_rmem = 4096 65536 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216

# 禁用IPv6 (如果不需要)
net.ipv6.conf.all.disable_ipv6 = 1
net.ipv6.conf.default.disable_ipv6 = 1

EOF

    # 应用参数
    sysctl -p
    log_info "内核参数优化完成"
}

# 用户和权限加固
harden_users() {
    log_info "开始用户和权限加固..."
    
    # 锁定不必要的系统账户
    local system_users=("games" "news" "uucp" "proxy" "www-data" "backup" "list" "irc" "gnats" "nobody" "systemd-network" "systemd-resolve")
    
    for user in "${system_users[@]}"; do
        if id "$user" >/dev/null 2>&1; then
            usermod -L "$user" 2>/dev/null || true
            usermod -s /usr/sbin/nologin "$user" 2>/dev/null || true
            log_info "已锁定用户: $user"
        fi
    done
    
    # 设置密码策略
    if [ -f /etc/login.defs ]; then
        sed -i 's/^PASS_MAX_DAYS.*/PASS_MAX_DAYS 90/' /etc/login.defs
        sed -i 's/^PASS_MIN_DAYS.*/PASS_MIN_DAYS 7/' /etc/login.defs
        sed -i 's/^PASS_MIN_LEN.*/PASS_MIN_LEN 8/' /etc/login.defs
        sed -i 's/^PASS_WARN_AGE.*/PASS_WARN_AGE 14/' /etc/login.defs
        log_info "密码策略已更新"
    fi
    
    # 配置PAM密码复杂度
    if [ -f /etc/pam.d/common-password ]; then
        if ! grep -q "pam_pwquality.so" /etc/pam.d/common-password; then
            sed -i '/pam_unix.so/i password requisite pam_pwquality.so retry=3 minlen=8 difok=3 ucredit=-1 lcredit=-1 dcredit=-1 ocredit=-1' /etc/pam.d/common-password
            log_info "PAM密码复杂度已配置"
        fi
    fi
    
    # 配置账户锁定策略
    if [ -f /etc/pam.d/common-auth ]; then
        if ! grep -q "pam_tally2.so" /etc/pam.d/common-auth; then
            sed -i '/pam_unix.so/i auth required pam_tally2.so onerr=fail audit silent deny=5 unlock_time=900' /etc/pam.d/common-auth
            log_info "账户锁定策略已配置"
        fi
    fi
    
    # 设置umask
    echo "umask 027" >> /etc/bash.bashrc
    echo "umask 027" >> /etc/profile
    
    log_info "用户和权限加固完成"
}

# 文件系统加固
harden_filesystem() {
    log_info "开始文件系统加固..."
    
    # 查找并修复危险权限文件
    log_info "检查危险权限文件..."
    
    # 查找world-writable文件
    find / -type f -perm -002 2>/dev/null | head -20 | while read file; do
        if [ -f "$file" ]; then
            chmod o-w "$file"
            log_warn "已修复world-writable文件: $file"
        fi
    done
    
    # 查找SUID文件
    find / -type f -perm -4000 2>/dev/null > /tmp/suid_files.txt
    log_info "SUID文件列表已保存到: /tmp/suid_files.txt"
    
    # 查找SGID文件
    find / -type f -perm -2000 2>/dev/null > /tmp/sgid_files.txt
    log_info "SGID文件列表已保存到: /tmp/sgid_files.txt"
    
    # 设置关键文件权限
    chmod 600 /etc/shadow 2>/dev/null || true
    chmod 600 /etc/gshadow 2>/dev/null || true
    chmod 644 /etc/passwd 2>/dev/null || true
    chmod 644 /etc/group 2>/dev/null || true
    chmod 600 /boot/grub/grub.cfg 2>/dev/null || true
    chmod 700 /root 2>/dev/null || true
    
    # 删除危险文件
    rm -f /etc/security/console.perms 2>/dev/null || true
    rm -f /etc/hosts.equiv 2>/dev/null || true
    rm -f ~/.rhosts 2>/dev/null || true
    
    log_info "文件系统加固完成"
}

# 服务加固
harden_services() {
    log_info "开始服务加固..."
    
    # 禁用不必要的服务
    local unnecessary_services=(
        "telnet"
        "rsh"
        "rlogin"
        "ftp"
        "tftp"
        "finger"
        "chargen"
        "daytime"
        "echo"
        "discard"
        "time"
        "avahi-daemon"
        "cups"
        "bluetooth"
    )
    
    for service in "${unnecessary_services[@]}"; do
        if systemctl is-enabled "$service" >/dev/null 2>&1; then
            systemctl disable "$service"
            systemctl stop "$service" 2>/dev/null || true
            log_info "已禁用服务: $service"
        fi
    done
    
    # 配置关键服务
    if systemctl is-active nginx >/dev/null 2>&1; then
        log_info "配置Nginx安全..."
        configure_nginx_security
    fi
    
    if systemctl is-active mysql >/dev/null 2>&1; then
        log_info "配置MySQL安全..."
        configure_mysql_security
    fi
    
    log_info "服务加固完成"
}

# Nginx安全配置
configure_nginx_security() {
    local nginx_conf="/etc/nginx/nginx.conf"
    if [ -f "$nginx_conf" ]; then
        # 备份配置
        cp "$nginx_conf" "$nginx_conf.backup.$(date +%s)"
        
        # 添加安全配置
        cat > /tmp/nginx_security.conf << 'EOF'
# 安全配置
server_tokens off;
client_max_body_size 10M;
client_body_buffer_size 128k;
client_header_buffer_size 1k;
large_client_header_buffers 4 4k;

# 限制请求方法
if ($request_method !~ ^(GET|HEAD|POST)$ ) {
    return 405;
}

# 安全头
add_header X-Frame-Options DENY;
add_header X-Content-Type-Options nosniff;
add_header X-XSS-Protection "1; mode=block";
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload";
add_header Referrer-Policy "strict-origin-when-cross-origin";
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'";

# 隐藏敏感文件
location ~* \.(git|svn|env|ini|log|bak|sql)$ {
    deny all;
    return 404;
}

# 限制PHP执行
location ~* \.(php|php5|phtml)$ {
    deny all;
    return 404;
}
EOF
        
        # 注入安全配置到http块
        sed -i '/http {/r /tmp/nginx_security.conf' "$nginx_conf"
        
        # 验证配置
        if nginx -t; then
            systemctl reload nginx
            log_info "Nginx安全配置已应用"
        else
            log_error "Nginx配置验证失败"
            cp "$nginx_conf.backup.*" "$nginx_conf" 2>/dev/null || true
        fi
    fi
}

# MySQL安全配置
configure_mysql_security() {
    local mysql_conf="/etc/mysql/my.cnf"
    if [ -f "$mysql_conf" ]; then
        # 备份配置
        cp "$mysql_conf" "$mysql_conf.backup.$(date +%s)"
        
        # 添加安全配置
        cat >> "$mysql_conf" << 'EOF'

[mysqld]
# 安全配置
skip-networking=false
bind-address=127.0.0.1
local-infile=0
skip-show-database
safe-user-create=1

# 日志配置
log-error=/var/log/mysql/error.log
slow-query-log=1
slow-query-log-file=/var/log/mysql/slow.log
long_query_time=2

# 性能配置
max_connections=100
max_user_connections=50
connect_timeout=10
wait_timeout=600
interactive_timeout=600

EOF
        
        systemctl restart mysql
        log_info "MySQL安全配置已应用"
    fi
}

# 日志和审计配置
configure_logging() {
    log_info "配置日志和审计..."
    
    # 配置rsyslog
    if [ -f /etc/rsyslog.conf ]; then
        cat >> /etc/rsyslog.conf << 'EOF'

# 安全日志配置
auth,authpriv.*                 /var/log/auth.log
kern.*                          /var/log/kern.log
daemon.*                        /var/log/daemon.log
mail.*                          /var/log/mail.log
user.*                          /var/log/user.log
local0.*                        /var/log/security.log

EOF
        systemctl restart rsyslog
        log_info "rsyslog配置已更新"
    fi
    
    # 配置logrotate
    cat > /etc/logrotate.d/security << 'EOF'
/var/log/security.log
/var/log/auth.log
/var/log/kern.log
{
    daily
    missingok
    rotate 52
    compress
    delaycompress
    notifempty
    create 640 root adm
    postrotate
        /usr/lib/rsyslog/rsyslog-rotate
    endscript
}
EOF
    
    log_info "日志轮转配置已创建"
}

# 安装安全工具
install_security_tools() {
    log_info "安装安全工具..."
    
    if command -v apt-get >/dev/null 2>&1; then
        apt-get update
        apt-get install -y \
            fail2ban \
            rkhunter \
            chkrootkit \
            aide \
            clamav \
            clamav-daemon \
            lynis \
            psad \
            logwatch \
            unattended-upgrades
    elif command -v yum >/dev/null 2>&1; then
        yum install -y \
            fail2ban \
            rkhunter \
            chkrootkit \
            aide \
            clamav \
            lynis \
            psad \
            logwatch \
            yum-cron
    fi
    
    # 配置fail2ban
    if command -v fail2ban-server >/dev/null 2>&1; then
        configure_fail2ban
    fi
    
    # 配置rkhunter
    if command -v rkhunter >/dev/null 2>&1; then
        rkhunter --update
        rkhunter --propupd
        log_info "rkhunter已配置"
    fi
    
    # 配置AIDE
    if command -v aide >/dev/null 2>&1; then
        aide --init
        cp /var/lib/aide/aide.db.new /var/lib/aide/aide.db
        log_info "AIDE已初始化"
    fi
    
    log_info "安全工具安装完成"
}

# 配置fail2ban
configure_fail2ban() {
    cat > /etc/fail2ban/jail.local << 'EOF'
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5
backend = auto
usedns = warn
logencoding = auto
enabled = false
mode = normal
filter = %(__name__)s[mode=%(mode)s]

[sshd]
enabled = true
port = 2222
logpath = /var/log/auth.log
maxretry = 3
bantime = 1800

[nginx-http-auth]
enabled = true
port = http,https
logpath = /var/log/nginx/error.log

[nginx-noscript]
enabled = true
port = http,https
logpath = /var/log/nginx/access.log
maxretry = 6

[nginx-badbots]
enabled = true
port = http,https
logpath = /var/log/nginx/access.log
maxretry = 2

[nginx-noproxy]
enabled = true
port = http,https
logpath = /var/log/nginx/access.log
maxretry = 2

[postfix]
enabled = true
port = smtp,465,submission
logpath = /var/log/mail.log

[postfix-sasl]
enabled = true
port = smtp,465,submission,imap3,imaps,pop3,pop3s
logpath = /var/log/mail.log

EOF

    systemctl enable fail2ban
    systemctl restart fail2ban
    log_info "fail2ban已配置并启动"
}

# 创建安全监控脚本
create_monitoring_scripts() {
    log_info "创建安全监控脚本..."
    
    # 每日安全检查脚本
    cat > /usr/local/bin/daily-security-check.sh << 'EOF'
#!/bin/bash
# 每日安全检查脚本

LOG_FILE="/var/log/daily-security-check.log"
DATE=$(date '+%Y-%m-%d %H:%M:%S')

{
    echo "=== 每日安全检查 $DATE ==="
    
    # 检查失败登录
    echo "最近24小时失败登录:"
    grep "Failed password" /var/log/auth.log | grep "$(date +%b\ %d)" | wc -l
    
    # 检查新用户
    echo "新增用户:"
    find /home -maxdepth 1 -type d -newermt "$(date -d '1 day ago')" 2>/dev/null
    
    # 检查SUID文件变化
    echo "SUID文件检查:"
    find / -type f -perm -4000 2>/dev/null | sort > /tmp/suid_current
    if [ -f /tmp/suid_baseline ]; then
        diff /tmp/suid_baseline /tmp/suid_current || echo "SUID文件发生变化"
    else
        cp /tmp/suid_current /tmp/suid_baseline
    fi
    
    # 检查系统负载
    echo "系统负载:"
    uptime
    
    # 检查磁盘使用
    echo "磁盘使用:"
    df -h | grep -v tmpfs
    
    # 检查网络连接
    echo "异常网络连接:"
    netstat -tulpn | grep -v "127.0.0.1\|::1" | head -10
    
    echo "=== 检查完成 ==="
    echo
    
} >> "$LOG_FILE"

# 发送告警邮件（如果有配置）
if [ -n "${ADMIN_EMAIL:-}" ] && command -v mail >/dev/null 2>&1; then
    tail -50 "$LOG_FILE" | mail -s "每日安全检查报告 - $(hostname)" "$ADMIN_EMAIL"
fi
EOF

    chmod +x /usr/local/bin/daily-security-check.sh
    
    # 添加到crontab
    (crontab -l 2>/dev/null; echo "0 6 * * * /usr/local/bin/daily-security-check.sh") | crontab -
    
    log_info "安全监控脚本已创建"
}

# 系统完整性检查
verify_hardening() {
    log_info "验证安全加固结果..."
    
    local checks_passed=0
    local total_checks=0
    
    # SSH配置检查
    ((total_checks++))
    if grep -q "PermitRootLogin no" /etc/ssh/sshd_config; then
        log_info "✓ SSH root登录已禁用"
        ((checks_passed++))
    else
        log_error "✗ SSH root登录未禁用"
    fi
    
    # 防火墙检查
    ((total_checks++))
    if iptables -L | grep -q "policy DROP"; then
        log_info "✓ 防火墙默认拒绝策略已启用"
        ((checks_passed++))
    else
        log_error "✗ 防火墙默认拒绝策略未启用"
    fi
    
    # 内核参数检查
    ((total_checks++))
    if sysctl net.ipv4.ip_forward | grep -q "= 0"; then
        log_info "✓ IP转发已禁用"
        ((checks_passed++))
    else
        log_error "✗ IP转发未禁用"
    fi
    
    # fail2ban检查
    ((total_checks++))
    if systemctl is-active fail2ban >/dev/null 2>&1; then
        log_info "✓ fail2ban服务正在运行"
        ((checks_passed++))
    else
        log_error "✗ fail2ban服务未运行"
    fi
    
    # 文件权限检查
    ((total_checks++))
    if [ "$(stat -c %a /etc/shadow)" = "600" ]; then
        log_info "✓ /etc/shadow文件权限正确"
        ((checks_passed++))
    else
        log_error "✗ /etc/shadow文件权限不正确"
    fi
    
    # 生成验证报告
    local success_rate=$((checks_passed * 100 / total_checks))
    
    {
        echo "安全加固验证报告"
        echo "=================="
        echo "验证时间: $(date)"
        echo "通过检查: $checks_passed/$total_checks"
        echo "成功率: $success_rate%"
        echo ""
        if [ "$success_rate" -ge 80 ]; then
            echo "状态: 加固成功"
        elif [ "$success_rate" -ge 60 ]; then
            echo "状态: 加固部分成功"
        else
            echo "状态: 加固失败"
        fi
    } > "$LOG_DIR/hardening_verification_$(date +%s).txt"
    
    log_info "安全加固验证完成，成功率: $success_rate%"
    return $((100 - success_rate))
}

# 主函数
main() {
    # 检查权限
    if [ "$EUID" -ne 0 ]; then
        log_error "此脚本需要root权限运行"
        exit 1
    fi
    
    log_info "🔒 开始系统安全加固..."
    
    # 记录开始时间
    start_time=$(date +%s)
    
    # 检测系统
    detect_system
    
    # 执行加固步骤
    backup_configurations
    harden_ssh
    harden_firewall
    harden_kernel
    harden_users
    harden_filesystem
    harden_services
    configure_logging
    install_security_tools
    create_monitoring_scripts
    
    # 验证加固结果
    verify_hardening
    
    # 计算耗时
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    
    log_info "🔒 系统安全加固完成，耗时: ${duration}秒"
    log_info "备份位置: $CONFIG_BACKUP_DIR"
    log_info "日志位置: $LOG_DIR"
    
    # 重启提醒
    log_warn "建议重启系统以确保所有配置生效"
    log_warn "重启命令: sudo reboot"
}

# 使用说明
usage() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help          显示此帮助信息"
    echo "  --ssh-only          仅加固SSH"
    echo "  --firewall-only     仅加固防火墙"
    echo "  --verify            验证加固状态"
    echo "  --restore           恢复配置"
    echo ""
    echo "示例:"
    echo "  $0                  执行完整加固"
    echo "  $0 --ssh-only       仅加固SSH"
    echo "  $0 --verify         验证加固状态"
}

# 命令行参数处理
case "${1:-}" in
    -h|--help)
        usage
        exit 0
        ;;
    --ssh-only)
        detect_system
        backup_configurations
        harden_ssh
        ;;
    --firewall-only)
        detect_system
        backup_configurations
        harden_firewall
        ;;
    --verify)
        verify_hardening
        exit $?
        ;;
    --restore)
        log_info "配置恢复功能待实现"
        exit 1
        ;;
    "")
        main
        ;;
    *)
        log_error "未知选项: $1"
        usage
        exit 1
        ;;
esac