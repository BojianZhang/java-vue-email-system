# 📋 系统部署配置要求

## 🖥️ 硬件配置要求

### 最低配置 (测试环境)
- **CPU:** 2核心 2.0GHz
- **内存:** 4GB RAM
- **存储:** 50GB SSD
- **网络:** 10Mbps带宽
- **用户规模:** 10-50用户

### 推荐配置 (生产环境)
- **CPU:** 4核心 2.5GHz
- **内存:** 8GB RAM  
- **存储:** 200GB SSD
- **网络:** 100Mbps带宽
- **用户规模:** 100-500用户

### 高性能配置 (企业环境)
- **CPU:** 8核心 3.0GHz
- **内存:** 16GB RAM
- **存储:** 500GB NVMe SSD
- **网络:** 1Gbps带宽
- **用户规模:** 1000+用户

## 🐧 操作系统要求

### 支持的操作系统
- **Ubuntu:** 20.04 LTS / 22.04 LTS (推荐)
- **CentOS:** 7.x / 8.x / Rocky Linux 8
- **Debian:** 10 / 11
- **RHEL:** 8.x / 9.x

### 系统用户权限
- **Root权限** - 安装软件和系统配置
- **应用用户** - email:email (非特权用户运行应用)
- **数据库用户** - mysql:mysql
- **Web服务用户** - www-data:www-data

## 📦 软件依赖要求

### Java环境
- **Java版本:** OpenJDK 17+ (必需)
- **Maven:** 3.8.0+ (构建工具)

### 数据库
- **MySQL:** 8.0+ (主数据库，必需)
- **Redis:** 6.0+ (缓存和会话，必需)

### Web服务器
- **Nginx:** 1.18+ (反向代理，必需)

### Node.js环境 (前端构建)
- **Node.js:** 18+ 
- **NPM:** 8+

### 邮件服务
- **Postfix:** 主邮件服务器 (SMTP)
- **Dovecot:** IMAP/POP3服务器
- 或使用外部邮件服务

### 监控和安全工具
- **Fail2ban:** IP封禁 (推荐)
- **UFW/iptables:** 防火墙
- **ClamAV:** 病毒扫描 (可选)
- **Lynis:** 安全审计 (可选)

## 🌐 网络配置要求

### 端口配置
```bash
# Web服务
80/tcp   - HTTP (重定向到HTTPS)
443/tcp  - HTTPS (主要访问端口)

# 邮件服务
25/tcp   - SMTP (邮件发送)
587/tcp  - SMTP提交 (推荐)
993/tcp  - IMAPS (安全IMAP)
995/tcp  - POP3S (安全POP3)

# 管理服务
22/tcp   - SSH (建议修改端口)
3306/tcp - MySQL (仅内网访问)
6379/tcp - Redis (仅内网访问)

# 应用服务
8080/tcp - 后端API (内网)
3000/tcp - 前端开发服务 (开发时)
```

### DNS配置要求
```dns
# 基础DNS记录
mail.yourdomain.com.    A     服务器IP
webmail.yourdomain.com. A     服务器IP
yourdomain.com.         MX    10 mail.yourdomain.com.

# 安全DNS记录 (推荐)
yourdomain.com.         TXT   "v=spf1 mx ~all"
_dmarc.yourdomain.com.  TXT   "v=DMARC1; p=quarantine; rua=mailto:dmarc@yourdomain.com"
default._domainkey.yourdomain.com. TXT "v=DKIM1; k=rsa; p=公钥内容"
```

### SSL证书要求
- **Let's Encrypt** - 免费SSL证书 (推荐)
- **商业证书** - 企业级部署可选
- **通配符证书** - 多子域名支持

## 🔧 系统配置清单

### 1. 系统初始化
```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装基础工具
sudo apt install -y wget curl git vim htop tree

# 设置时区
sudo timedatectl set-timezone Asia/Shanghai

# 创建应用用户
sudo useradd -m -s /bin/bash email
sudo usermod -aG sudo email
```

### 2. 防火墙配置
```bash
# 启用UFW防火墙
sudo ufw enable
sudo ufw default deny incoming
sudo ufw default allow outgoing

# 允许必要端口
sudo ufw allow ssh
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 25/tcp
sudo ufw allow 587/tcp
sudo ufw allow 993/tcp
sudo ufw allow 995/tcp
```

### 3. 系统优化
```bash
# 内核参数优化
echo "net.core.somaxconn = 65535" >> /etc/sysctl.conf
echo "net.ipv4.tcp_max_syn_backlog = 65535" >> /etc/sysctl.conf
echo "vm.swappiness = 10" >> /etc/sysctl.conf
sysctl -p

# 文件描述符限制
echo "* soft nofile 65535" >> /etc/security/limits.conf
echo "* hard nofile 65535" >> /etc/security/limits.conf
```

## 📊 监控配置

### 系统监控
- **Prometheus + Grafana** - 系统监控 (可选)
- **Zabbix** - 企业监控 (可选)
- **Nagios** - 传统监控 (可选)

### 日志管理
```bash
# 日志轮转配置
/var/log/email-system/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 644 email email
}
```

### 备份配置
- **数据库备份:** 每日自动备份
- **应用备份:** 每周完整备份  
- **配置备份:** 每次修改后备份
- **异地备份:** 推荐云存储备份

## 🔐 安全配置

### SSL/TLS配置
```nginx
# Nginx SSL配置
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
ssl_prefer_server_ciphers off;
ssl_session_cache shared:SSL:10m;
```

### 应用安全
- **HTTPS强制** - 所有流量强制HTTPS
- **CSP策略** - 内容安全策略
- **安全头** - X-Frame-Options, X-XSS-Protection等
- **JWT配置** - Token过期时间和刷新策略

### 数据库安全
```sql
-- 创建应用专用数据库用户
CREATE USER 'emailapp'@'localhost' IDENTIFIED BY '强密码';
GRANT ALL PRIVILEGES ON email_system.* TO 'emailapp'@'localhost';
FLUSH PRIVILEGES;

-- 删除默认用户和数据库
DROP USER ''@'localhost';
DROP USER ''@'hostname';
DROP DATABASE test;
```

## 📈 性能优化配置

### Java应用优化
```bash
# JVM参数优化
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:+UseContainerSupport"
```

### MySQL优化
```ini
[mysqld]
innodb_buffer_pool_size = 2G
innodb_log_file_size = 256M
query_cache_size = 128M
max_connections = 500
```

### Redis优化
```conf
maxmemory 1gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

### Nginx优化
```nginx
worker_processes auto;
worker_connections 2048;
keepalive_timeout 65;
client_max_body_size 50M;
gzip on;
gzip_types text/plain text/css application/json application/javascript;
```

## 💾 存储配置

### 磁盘分区建议
```bash
/          - 20GB  (系统根分区)
/var       - 30GB  (日志和缓存)
/opt       - 50GB  (应用程序)
/backup    - 100GB (备份存储)
/mail      - 剩余空间 (邮件存储)
```

### 邮件存储
- **Maildir格式** - 推荐使用Maildir
- **存储位置** - /mail/vhosts/domain.com/user/
- **权限设置** - email:email 700

## ⚖️ 规模化部署

### 小规模部署 (单服务器)
- 所有服务运行在一台服务器
- 适用于100用户以内
- 成本最低，管理简单

### 中等规模部署 (应用分离)
- Web服务器 + 数据库服务器分离
- 适用于100-1000用户
- 提高性能和可靠性

### 大规模部署 (微服务)
- 负载均衡器 + 多个应用服务器
- 数据库集群 + Redis集群
- 适用于1000+用户
- 高可用和水平扩展

---

**💡 提示:** 
- 生产环境建议使用至少推荐配置
- 定期备份和监控非常重要
- 安全配置不可忽视
- 根据实际用户量调整配置