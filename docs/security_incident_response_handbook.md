# 黑客攻击应对手册

## 📋 目录
1. [威胁检测与分类](#威胁检测与分类)
2. [立即响应措施](#立即响应措施)
3. [系统加固方案](#系统加固方案)
4. [数据备份与恢复](#数据备份与恢复)
5. [网络安全防护](#网络安全防护)
6. [应急响应流程](#应急响应流程)
7. [取证与报告](#取证与报告)
8. [预防措施](#预防措施)

## 🚨 威胁检测与分类

### 1. SQL注入攻击
**检测特征:**
- URL参数包含: `union select`, `insert into`, `drop table`
- 数据库错误信息泄露
- 异常的数据库查询模式

**响应等级:** 🔴 HIGH
**自动响应:** 立即阻断攻击者IP，启用SQL防护模式

### 2. XSS攻击
**检测特征:**
- 输入包含: `<script>`, `javascript:`, `eval()`
- Cookie窃取尝试
- DOM操作异常

**响应等级:** 🟡 MEDIUM  
**自动响应:** 启用内容过滤，临时阻断IP

### 3. DDoS攻击
**检测特征:**
- 异常高的请求频率
- 来源IP高度集中
- 系统资源消耗激增

**响应等级:** 🔴 CRITICAL
**自动响应:** 启用DDoS防护，流量限制，CDN激活

### 4. 暴力破解
**检测特征:**
- 短时间内大量登录失败
- 字典攻击模式
- 异常登录时间和地点

**响应等级:** 🟡 MEDIUM
**自动响应:** 账户锁定，强制MFA，IP限制

### 5. 恶意软件
**检测特征:**
- 异常进程活动
- 网络通信异常
- 文件完整性破坏

**响应等级:** 🔴 CRITICAL
**自动响应:** 进程隔离，网络断开，系统扫描

## ⚡ 立即响应措施

### 第一时间响应（0-5分钟）

```bash
# 1. 立即阻断攻击者IP
./emergency_response.sh SQL_INJECTION 192.168.1.100

# 2. 启用DDoS防护
iptables -A INPUT -p tcp --dport 80 -m connlimit --connlimit-above 20 -j REJECT

# 3. 检查系统状态
ps auxf | grep -E "(nc|netcat|socat|nmap)"
netstat -tulpn | grep -E "(LISTEN|ESTABLISHED)"
```

### 紧急隔离措施（5-15分钟）

```bash
# 1. 网络隔离
./emergency_response.sh --isolate

# 2. 服务保护
systemctl stop nginx  # 停止Web服务
systemctl stop mysql  # 保护数据库
systemctl restart ssh # 重启SSH确保安全

# 3. 进程清理
pkill -f "suspicious_process"
```

### 数据保护（并行执行）

```bash
# 1. 紧急备份
./emergency_response.sh --backup-only

# 2. 文件完整性检查
find /etc -name "*.conf" -exec sha256sum {} \; > /tmp/config_hashes.txt

# 3. 权限检查
find /var/www -type f -perm /o+w -exec ls -l {} \;
```

## 🛡️ 系统加固方案

### 1. 防火墙配置强化

```bash
#!/bin/bash
# 高级防火墙配置

# 清空现有规则
iptables -F
iptables -X
iptables -t nat -F
iptables -t nat -X

# 设置默认策略
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT ACCEPT

# 允许本地回环
iptables -A INPUT -i lo -j ACCEPT

# 允许已建立的连接
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# SSH访问限制（仅允许特定IP）
iptables -A INPUT -p tcp --dport 22 -s 管理员IP -j ACCEPT

# Web服务限制
iptables -A INPUT -p tcp --dport 80 -m connlimit --connlimit-above 25 -j REJECT
iptables -A INPUT -p tcp --dport 443 -m connlimit --connlimit-above 25 -j REJECT

# 防止端口扫描
iptables -A INPUT -m state --state NEW -m recent --set
iptables -A INPUT -m state --state NEW -m recent --update --seconds 60 --hitcount 10 -j DROP

# 防止ping洪水
iptables -A INPUT -p icmp --icmp-type echo-request -m limit --limit 1/s -j ACCEPT
iptables -A INPUT -p icmp --icmp-type echo-request -j DROP

# 保存规则
iptables-save > /etc/iptables/rules.v4
```

### 2. SSH安全加固

```bash
# SSH配置强化
cat >> /etc/ssh/sshd_config << EOF
# 安全配置
Port 2222                    # 修改默认端口
PermitRootLogin no          # 禁止root登录
PasswordAuthentication no   # 禁用密码认证
PubkeyAuthentication yes    # 启用密钥认证
MaxAuthTries 3              # 限制认证尝试
ClientAliveInterval 300     # 会话超时
ClientAliveCountMax 2       # 超时重试次数
MaxStartups 10:30:100       # 限制并发连接

# 允许的用户和组
AllowUsers admin
AllowGroups wheel

# 禁用危险功能
X11Forwarding no
AllowTcpForwarding no
GatewayPorts no
PermitTunnel no
EOF

systemctl restart sshd
```

### 3. Web应用安全配置

```nginx
# Nginx安全配置
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    # SSL配置
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    
    # 安全头
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload";
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'";
    
    # 限制请求大小
    client_max_body_size 10M;
    
    # 限制请求频率
    limit_req_zone $binary_remote_addr zone=login:10m rate=5r/m;
    limit_req_zone $binary_remote_addr zone=api:10m rate=30r/m;
    
    location /api/auth/login {
        limit_req zone=login burst=3 nodelay;
        proxy_pass http://backend;
    }
    
    location /api/ {
        limit_req zone=api burst=10 nodelay;
        proxy_pass http://backend;
    }
    
    # 隐藏版本信息
    server_tokens off;
    
    # 阻止恶意请求
    location ~* \.(git|svn|env|ini|log|bak)$ {
        deny all;
        return 404;
    }
}
```

## 💾 数据备份与恢复

### 自动备份系统

```bash
#!/bin/bash
# 自动备份脚本

BACKUP_DIR="/backup/$(date +%Y%m%d)"
mkdir -p "$BACKUP_DIR"

# 数据库备份
mysqldump --single-transaction --routines --triggers email_system | gzip > "$BACKUP_DIR/database.sql.gz"

# 应用程序备份
tar -czf "$BACKUP_DIR/application.tar.gz" /opt/email-system

# 配置文件备份
tar -czf "$BACKUP_DIR/config.tar.gz" /etc/email-system

# 加密备份
gpg --cipher-algo AES256 --compress-algo 1 --symmetric --output "$BACKUP_DIR/backup.gpg" "$BACKUP_DIR/"

# 上传到安全位置
rsync -avz "$BACKUP_DIR/" backup-server:/secure-backups/
```

### 快速恢复流程

```bash
#!/bin/bash
# 系统快速恢复

# 1. 停止所有服务
systemctl stop nginx mysql email-system

# 2. 恢复数据库
mysql email_system < backup/database.sql

# 3. 恢复应用程序
tar -xzf backup/application.tar.gz -C /

# 4. 恢复配置
tar -xzf backup/config.tar.gz -C /

# 5. 修复权限
chown -R email:email /opt/email-system
chmod -R 755 /opt/email-system

# 6. 重启服务
systemctl start mysql nginx email-system

# 7. 验证恢复
./emergency_response.sh --check
```

## 🌐 网络安全防护

### 1. 入侵检测系统配置

```yaml
# Suricata配置示例
%YAML 1.1
---

vars:
  address-groups:
    HOME_NET: "[192.168.1.0/24]"
    EXTERNAL_NET: "!$HOME_NET"
    
  port-groups:
    HTTP_PORTS: "80"
    HTTPS_PORTS: "443"

rule-files:
  - sql-injection.rules
  - xss-attack.rules
  - ddos-protection.rules

outputs:
  - fast:
      enabled: yes
      filename: fast.log
  - unified2-alert:
      enabled: yes
      filename: unified2.alert

# 自定义规则示例
# sql-injection.rules
alert http any any -> $HOME_NET $HTTP_PORTS (msg:"SQL Injection Attack"; content:"union select"; http_uri; sid:1001; rev:1;)
alert http any any -> $HOME_NET $HTTP_PORTS (msg:"SQL Injection Attack"; content:"drop table"; http_uri; sid:1002; rev:1;)

# xss-attack.rules
alert http any any -> $HOME_NET $HTTP_PORTS (msg:"XSS Attack"; content:"<script"; http_client_body; sid:2001; rev:1;)
alert http any any -> $HOME_NET $HTTP_PORTS (msg:"XSS Attack"; content:"javascript:"; http_uri; sid:2002; rev:1;)
```

### 2. 流量监控与分析

```bash
#!/bin/bash
# 网络流量监控脚本

# 实时监控网络连接
watch -n 1 'netstat -tulpn | grep -E "(LISTEN|ESTABLISHED)" | head -20'

# 监控异常流量
tcpdump -i any -c 100 -w /tmp/suspicious_traffic.pcap

# 分析流量模式
tshark -r /tmp/suspicious_traffic.pcap -q -z conv,ip
tshark -r /tmp/suspicious_traffic.pcap -q -z endpoints,ip

# 检测DDoS攻击
netstat -ntu | awk '{print $5}' | cut -d: -f1 | sort | uniq -c | sort -n | tail -20
```

## 🚑 应急响应流程

### 响应等级分类

#### 🟢 LOW (低危)
- **响应时间:** 4小时内
- **措施:** 记录日志，增强监控
- **人员:** 运维值班人员

#### 🟡 MEDIUM (中危)  
- **响应时间:** 1小时内
- **措施:** 临时阻断，加强防护
- **人员:** 安全团队 + 运维团队

#### 🔴 HIGH (高危)
- **响应时间:** 15分钟内  
- **措施:** 立即阻断，紧急备份
- **人员:** 全体安全团队 + 管理层

#### ⚫ CRITICAL (严重)
- **响应时间:** 5分钟内
- **措施:** 系统隔离，应急预案
- **人员:** 应急响应小组 + 外部专家

### 应急响应checklist

```
□ 1. 威胁确认与分类 (2分钟内)
  □ 确认攻击类型
  □ 评估影响范围  
  □ 确定响应等级

□ 2. 立即响应措施 (5分钟内)
  □ 阻断攻击来源
  □ 启用防护机制
  □ 通知相关人员

□ 3. 损害控制 (15分钟内)
  □ 隔离受影响系统
  □ 停止敏感服务
  □ 启动备份流程

□ 4. 调查取证 (30分钟内)
  □ 保全现场证据
  □ 收集日志信息
  □ 分析攻击路径

□ 5. 恢复准备 (1小时内)
  □ 评估系统状态
  □ 制定恢复计划
  □ 准备恢复资源

□ 6. 系统恢复 (根据情况)
  □ 执行恢复计划
  □ 验证系统功能
  □ 监控异常活动

□ 7. 后续改进 (24小时内)
  □ 编写事件报告
  □ 分析根本原因
  □ 制定改进措施
```

## 🔍 取证与报告

### 取证数据收集

```bash
#!/bin/bash
# 完整取证数据收集脚本

INCIDENT_ID="INC_$(date +%Y%m%d_%H%M%S)"
EVIDENCE_DIR="/evidence/$INCIDENT_ID"
mkdir -p "$EVIDENCE_DIR"

# 1. 系统快照
{
    echo "=== 系统信息 ==="
    uname -a
    date
    uptime
    
    echo "=== 内存信息 ==="
    free -h
    cat /proc/meminfo
    
    echo "=== CPU信息 ==="
    cat /proc/cpuinfo | grep "model name" | head -1
    cat /proc/loadavg
    
    echo "=== 磁盘信息 ==="
    df -h
    mount | grep -v tmpfs
    
    echo "=== 网络信息 ==="
    ip addr show
    ip route show
    netstat -tulpn
    ss -tulpn
    
    echo "=== 进程信息 ==="
    ps auxf
    
    echo "=== 用户会话 ==="
    who
    w
    last -n 50
    
} > "$EVIDENCE_DIR/system_snapshot.txt"

# 2. 安全日志
cp /var/log/auth.log "$EVIDENCE_DIR/" 2>/dev/null
cp /var/log/syslog "$EVIDENCE_DIR/" 2>/dev/null
cp /var/log/nginx/access.log "$EVIDENCE_DIR/" 2>/dev/null
cp /var/log/nginx/error.log "$EVIDENCE_DIR/" 2>/dev/null

# 3. 应用程序日志  
cp -r /var/log/email-system "$EVIDENCE_DIR/" 2>/dev/null

# 4. 网络捕获
timeout 60 tcpdump -i any -w "$EVIDENCE_DIR/network_capture.pcap" 2>/dev/null &

# 5. 文件系统时间线
find /var/log -type f -newermt "$(date -d '1 hour ago')" -exec ls -la {} \; > "$EVIDENCE_DIR/recent_files.txt"

# 6. 配置文件状态
tar -czf "$EVIDENCE_DIR/config_backup.tar.gz" /etc 2>/dev/null

# 7. 创建取证报告
cat > "$EVIDENCE_DIR/forensic_report.md" << EOF
# 安全事件取证报告

## 基本信息
- **事件ID:** $INCIDENT_ID
- **取证时间:** $(date)
- **取证人员:** $(whoami)
- **系统信息:** $(uname -a)

## 证据清单
$(ls -la "$EVIDENCE_DIR")

## 初步分析
### 系统状态
- **系统负载:** $(uptime | cut -d',' -f3-)
- **内存使用:** $(free -h | grep Mem | awk '{print $3"/"$2}')
- **磁盘使用:** $(df -h / | tail -1 | awk '{print $5}')

### 网络状态
- **活动连接:** $(netstat -an | grep ESTABLISHED | wc -l)
- **监听端口:** $(netstat -tln | grep LISTEN | wc -l)

### 安全事件
- **最近登录:** $(last -n 5 | head -5)
- **失败登录:** $(grep "Failed password" /var/log/auth.log | tail -5)

## 建议后续行动
1. 深入分析网络流量
2. 检查文件完整性
3. 审计用户活动
4. 验证系统配置

---
*此报告由自动取证系统生成*
EOF

echo "取证数据收集完成: $EVIDENCE_DIR"
```

## 🛡️ 预防措施

### 1. 系统安全基线

```bash
#!/bin/bash
# 系统安全基线检查

# 密码策略
echo "检查密码策略..."
grep -E "(PASS_MIN_LEN|PASS_MAX_DAYS)" /etc/login.defs

# 账户安全
echo "检查账户安全..."
awk -F: '$3 == 0 {print $1}' /etc/passwd  # 检查root权限账户
awk -F: '$2 == "" {print $1}' /etc/shadow  # 检查空密码账户

# 文件权限
echo "检查关键文件权限..."
ls -l /etc/passwd /etc/shadow /etc/hosts /etc/ssh/sshd_config

# 服务安全
echo "检查运行的服务..."
systemctl list-units --type=service --state=running

# 网络安全
echo "检查网络配置..."
ss -tulpn | grep LISTEN
```

### 2. 定期安全检查

```bash
#!/bin/bash
# 每日安全检查脚本

LOG_FILE="/var/log/daily_security_check.log"

{
    echo "=== 每日安全检查 $(date) ==="
    
    # 检查失败登录
    echo "最近24小时失败登录:"
    grep "Failed password" /var/log/auth.log | grep "$(date +%b\ %d)"
    
    # 检查sudo使用
    echo "最近24小时sudo使用:"
    grep "sudo:" /var/log/auth.log | grep "$(date +%b\ %d)"
    
    # 检查新文件
    echo "系统关键目录新文件:"
    find /etc /usr/bin /usr/sbin -type f -newermt "$(date -d '1 day ago')" 2>/dev/null
    
    # 检查网络连接
    echo "异常网络连接:"
    netstat -tulpn | grep -v "127.0.0.1\|::1"
    
    # 检查系统负载
    echo "系统资源使用:"
    uptime
    df -h
    free -h
    
    echo "=== 检查完成 ==="
    
} >> "$LOG_FILE"

# 如果发现异常，发送告警
if grep -q "Failed password" "$LOG_FILE"; then
    echo "发现安全异常，请检查日志: $LOG_FILE" | mail -s "安全告警" admin@company.com
fi
```

### 3. 安全监控配置

```python
#!/usr/bin/env python3
# 实时安全监控脚本

import time
import psutil
import logging
import smtplib
from email.mime.text import MIMEText
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

class SecurityMonitor:
    def __init__(self):
        self.logger = logging.getLogger('SecurityMonitor')
        self.setup_logging()
        
    def setup_logging(self):
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler('/var/log/security_monitor.log'),
                logging.StreamHandler()
            ]
        )
    
    def check_system_resources(self):
        """检查系统资源异常"""
        cpu_percent = psutil.cpu_percent(interval=1)
        memory_percent = psutil.virtual_memory().percent
        disk_percent = psutil.disk_usage('/').percent
        
        if cpu_percent > 90:
            self.send_alert(f"CPU使用率过高: {cpu_percent}%")
        
        if memory_percent > 90:
            self.send_alert(f"内存使用率过高: {memory_percent}%")
            
        if disk_percent > 90:
            self.send_alert(f"磁盘使用率过高: {disk_percent}%")
    
    def check_network_connections(self):
        """检查异常网络连接"""
        connections = psutil.net_connections()
        
        suspicious_ports = [1234, 4444, 5555, 6666]
        for conn in connections:
            if conn.laddr.port in suspicious_ports:
                self.send_alert(f"发现可疑端口连接: {conn.laddr.port}")
    
    def send_alert(self, message):
        """发送告警邮件"""
        try:
            msg = MIMEText(message)
            msg['Subject'] = '安全监控告警'
            msg['From'] = 'security@company.com'
            msg['To'] = 'admin@company.com'
            
            server = smtplib.SMTP('localhost')
            server.send_message(msg)
            server.quit()
            
            self.logger.warning(f"安全告警: {message}")
        except Exception as e:
            self.logger.error(f"发送告警失败: {e}")

class FileSystemWatcher(FileSystemEventHandler):
    def on_modified(self, event):
        if '/etc/' in event.src_path:
            logging.warning(f"关键配置文件被修改: {event.src_path}")

if __name__ == "__main__":
    monitor = SecurityMonitor()
    
    # 启动文件系统监控
    observer = Observer()
    observer.schedule(FileSystemWatcher(), '/etc', recursive=True)
    observer.start()
    
    try:
        while True:
            monitor.check_system_resources()
            monitor.check_network_connections()
            time.sleep(60)  # 每分钟检查一次
    except KeyboardInterrupt:
        observer.stop()
    observer.join()
```

## 📞 应急联系信息

### 内部联系人
- **安全团队负责人:** security-lead@company.com / +86-xxx-xxxx-xxx1
- **系统管理员:** sysadmin@company.com / +86-xxx-xxxx-xxx2  
- **网络管理员:** netadmin@company.com / +86-xxx-xxxx-xxx3
- **数据库管理员:** dba@company.com / +86-xxx-xxxx-xxx4

### 外部联系人
- **ISP技术支持:** isp-support@provider.com / 400-xxx-xxxx
- **安全厂商:** vendor-support@security.com / 400-xxx-xxxx
- **执法部门:** cyberpolice@local.gov / 110

### 应急响应矩阵

| 事件类型 | 严重级别 | 响应时间 | 负责人 | 联系方式 |
|---------|---------|---------|--------|----------|
| DDoS攻击 | CRITICAL | 5分钟 | 网络管理员 | +86-xxx-xxxx-xxx3 |
| 数据泄露 | CRITICAL | 5分钟 | 安全负责人 | +86-xxx-xxxx-xxx1 |
| 系统入侵 | HIGH | 15分钟 | 系统管理员 | +86-xxx-xxxx-xxx2 |
| 恶意软件 | HIGH | 15分钟 | 安全团队 | security@company.com |

---

**最后更新:** 2024年1月
**版本:** 1.0
**维护者:** 企业邮件系统安全团队