# 🔐 SSL Certificate Management System - Complete Guide

## 📋 目录

1. [系统概述](#系统概述)
2. [功能特性](#功能特性)
3. [技术架构](#技术架构)
4. [安装部署](#安装部署)
5. [配置说明](#配置说明)
6. [使用指南](#使用指南)
7. [API 参考](#api-参考)
8. [监控运维](#监控运维)
9. [故障排除](#故障排除)
10. [高级特性](#高级特性)

## 🌟 系统概述

企业级SSL证书管理系统是一个集成化的证书生命周期管理平台，支持：

- 🔄 **自动化证书管理**: Let's Encrypt免费证书自动获取和续期
- 📤 **自定义证书支持**: 上传和管理自有证书
- 🔍 **智能监控**: 实时监控证书状态和过期提醒
- ⚡ **性能优化**: 批量操作和智能负载均衡
- 📊 **数据可视化**: 丰富的图表和报告
- 🛠️ **CLI工具**: 强大的命令行管理工具

## 🚀 功能特性

### 证书管理
- ✅ Let's Encrypt自动获取 (HTTP01/DNS01验证)
- ✅ 自定义证书上传和管理
- ✅ 证书自动续期 (30天前)
- ✅ 证书验证和健康检查
- ✅ Nginx自动配置集成
- ✅ 证书备份和恢复

### 性能优化
- ✅ 批量证书验证
- ✅ 并行证书续期
- ✅ 智能负载均衡续期
- ✅ 预测性分析和建议
- ✅ 异步任务处理
- ✅ 信号量并发控制

### 监控告警
- ✅ Prometheus指标集成
- ✅ 实时性能监控
- ✅ 证书过期提醒
- ✅ 操作成功率统计
- ✅ 系统健康检查
- ✅ 自定义指标记录

### 用户界面
- ✅ Vue3 + Element Plus管理界面
- ✅ 实时状态仪表板
- ✅ 交互式图表展示
- ✅ 批量操作界面
- ✅ 详细的操作日志
- ✅ 响应式设计

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                     前端层 (Frontend)                        │
├─────────────────────────────────────────────────────────────┤
│  Vue 3 + Element Plus + Pinia + Vite + TypeScript          │
│  • SSL证书管理界面                                            │
│  • 性能监控仪表板                                            │
│  • 批量操作控制台                                            │
│  • 图表可视化 (ECharts)                                      │
└─────────────────────────────────────────────────────────────┘
                                │ HTTPS/API
┌─────────────────────────────────────────────────────────────┐
│                    API网关层 (Gateway)                      │
├─────────────────────────────────────────────────────────────┤
│  Nginx + SSL Termination                                   │
│  • 负载均衡                                                  │
│  • SSL证书自动应用                                           │
│  • 静态资源服务                                              │
│  • 反向代理                                                  │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                   应用服务层 (Backend)                       │
├─────────────────────────────────────────────────────────────┤
│  Spring Boot 3.2 + Spring Security 6 + Java 17            │
│  ┌─────────────────┬─────────────────┬─────────────────┐     │
│  │  证书管理服务     │  性能优化服务     │  监控指标服务     │     │
│  │  • ACME集成     │  • 批量操作      │  • Prometheus   │     │
│  │  • 证书验证     │  • 智能续期      │  • 健康检查     │     │
│  │  • Nginx集成    │  • 预测分析      │  • 自定义指标   │     │
│  └─────────────────┴─────────────────┴─────────────────┘     │
│  ┌─────────────────┬─────────────────┬─────────────────┐     │
│  │  异步任务处理     │  定时任务调度     │  事件通知系统     │     │
│  │  • 线程池管理     │  • Cron表达式    │  • 邮件通知     │     │
│  │  • 并发控制      │  • 任务监控      │  • Webhook     │     │
│  │  • 失败重试      │  • 历史记录      │  • 日志记录     │     │
│  └─────────────────┴─────────────────┴─────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                    数据存储层 (Storage)                      │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┬─────────────────┬─────────────────┐     │
│  │   MySQL 8.0     │   Redis 6.0     │   文件系统       │     │
│  │  • 证书元数据     │  • 会话缓存      │  • 证书文件     │     │
│  │  • 操作日志      │  • 任务队列      │  • 私钥文件     │     │
│  │  • 配置信息      │  • 指标缓存      │  • 备份文件     │     │
│  │  • 用户数据      │  • 分布式锁      │  • 日志文件     │     │
│  └─────────────────┴─────────────────┴─────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                   外部集成层 (External)                      │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┬─────────────────┬─────────────────┐     │
│  │  Let's Encrypt  │  DNS 提供商      │  监控系统       │     │
│  │  • ACME协议     │  • Cloudflare   │  • Prometheus   │     │
│  │  • 证书签发     │  • 阿里云DNS    │  • Grafana      │     │
│  │  • 吊销管理     │  • DNS验证      │  • AlertManager │     │
│  └─────────────────┴─────────────────┴─────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## 📦 安装部署

### 环境要求

**系统要求:**
- Ubuntu 20.04+ / CentOS 8+ / RHEL 8+
- CPU: 2核心以上
- 内存: 4GB以上
- 存储: 20GB以上可用空间
- 网络: 80/443端口可访问

**软件要求:**
- Docker 20.10+ & Docker Compose 2.0+
- 或 Java 17+ & Node.js 16+ & MySQL 8.0+ & Redis 6.0+

### 🐳 Docker 部署 (推荐)

#### 1. 克隆项目
```bash
git clone <repository-url>
cd java-vue-email-system
```

#### 2. 环境配置
```bash
# 复制环境配置
cp .env.example .env

# 编辑配置文件
nano .env
```

**.env 配置示例:**
```env
# 数据库配置
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_DATABASE=enterprise_email
MYSQL_USER=ssl_admin
MYSQL_PASSWORD=ssl_admin_password

# Redis配置
REDIS_PASSWORD=redis_secure_password

# SSL配置
LETS_ENCRYPT_EMAIL=admin@yourdomain.com
MANAGED_DOMAINS=yourdomain.com,mail.yourdomain.com
SSL_CERTS_DIR=/opt/ssl/certs
SSL_BACKUPS_DIR=/opt/ssl/backups

# 应用配置
API_BASE_URL=https://yourdomain.com/api
ADMIN_EMAIL=admin@yourdomain.com
JWT_SECRET=your-jwt-secret-key-2024

# Nginx配置
NGINX_CONFIG_DIR=/etc/nginx/conf.d
```

#### 3. 创建目录结构
```bash
# 创建SSL相关目录
sudo mkdir -p /opt/ssl/{certs,backups,temp}
sudo mkdir -p /var/www/html/.well-known/acme-challenge

# 设置权限
sudo chown -R $USER:$USER /opt/ssl
sudo chmod -R 755 /opt/ssl
```

#### 4. 启动服务
```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f ssl-manager
```

#### 5. 初始化系统
```bash
# 等待服务启动 (约30秒)
sleep 30

# 获取第一个SSL证书
docker exec ssl-manager ssl-deploy obtain yourdomain.com admin@yourdomain.com HTTP01

# 验证证书
docker exec ssl-manager ssl-deploy test yourdomain.com
```

### 🖥️ 原生部署

#### 1. 安装依赖
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-17-jdk nodejs npm mysql-server redis-server nginx certbot

# CentOS/RHEL
sudo dnf install -y java-17-openjdk nodejs npm mysql-server redis nginx certbot

# 启动服务
sudo systemctl enable --now mysql redis nginx
```

#### 2. 数据库初始化
```bash
# 连接MySQL
sudo mysql -u root -p

# 创建数据库和用户
CREATE DATABASE enterprise_email DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ssl_admin'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON enterprise_email.* TO 'ssl_admin'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 导入数据结构
mysql -u ssl_admin -p enterprise_email < database/schema.sql
```

#### 3. 后端部署
```bash
# 进入后端目录
cd backend

# 配置application.yml
cp src/main/resources/application.yml.example src/main/resources/application.yml
nano src/main/resources/application.yml

# 构建项目
./mvnw clean package -DskipTests

# 启动应用
java -jar target/enterprise-email-system-1.0.0.jar
```

#### 4. 前端部署
```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 构建项目
npm run build

# 部署到nginx
sudo cp -r dist/* /var/www/html/
```

#### 5. SSL脚本安装
```bash
# 复制SSL管理脚本
sudo cp scripts/ssl-* /usr/local/bin/
sudo chmod +x /usr/local/bin/ssl-*

# 安装配置文件
sudo mkdir -p /opt/ssl/scripts
sudo cp scripts/ssl-config.conf /opt/ssl/scripts/

# 安装systemd服务
sudo cp scripts/ssl-monitor.service /etc/systemd/system/
sudo cp scripts/ssl-monitor.timer /etc/systemd/system/

# 启用定时任务
sudo systemctl daemon-reload
sudo systemctl enable --now ssl-monitor.timer
```

## ⚙️ 配置说明

### 应用配置 (application.yml)

```yaml
# SSL证书管理配置
ssl:
  # Let's Encrypt配置
  lets-encrypt:
    enabled: true
    email: admin@example.com
    staging: false              # 生产环境设为false
    agree-tos: true
    key-size: 2048
    
  # 证书存储配置
  storage:
    certs-dir: /opt/ssl/certs
    backups-dir: /opt/ssl/backups
    temp-dir: /tmp/ssl
    
  # 自动续期配置
  auto-renewal:
    enabled: true
    days-before-expiry: 30      # 过期前30天开始续期
    max-attempts: 3             # 最大重试次数
    retry-delay-hours: 24       # 重试间隔
    
  # Nginx集成配置
  nginx:
    enabled: true
    config-dir: /etc/nginx/conf.d
    ssl-config-template: ssl-template.conf
    restart-command: systemctl reload nginx
    
  # 监控配置
  monitoring:
    enabled: true
    scan-interval-hours: 6      # 扫描间隔
    alert-days-before-expiry: 7 # 告警阈值
    
  # 异步处理配置
  async:
    core-pool-size: 5
    max-pool-size: 20
    queue-capacity: 100
    keep-alive-seconds: 300
    thread-name-prefix: ssl-task-

# Prometheus指标配置
management:
  metrics:
    export:
      prometheus:
        enabled: true
        step: 10s
    distribution:
      percentiles-histogram:
        ssl.certificates.obtain.duration: true
        ssl.certificates.renewal.duration: true
        ssl.certificates.validation.duration: true
```

### Nginx SSL模板配置

**文件: `/opt/ssl/templates/ssl-template.conf`**
```nginx
server {
    listen 80;
    server_name {DOMAIN};
    
    # ACME challenge
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }
    
    # 重定向到HTTPS
    location / {
        return 301 https://$server_name$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name {DOMAIN};
    
    # SSL证书配置
    ssl_certificate {CERT_PATH};
    ssl_certificate_key {KEY_PATH};
    
    # SSL安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-SHA256:ECDHE-RSA-AES256-SHA384:ECDHE-RSA-AES128-SHA:ECDHE-RSA-AES256-SHA:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA256:AES256-SHA256:AES128-SHA:AES256-SHA:!aNULL:!eNULL:!EXPORT:!DES:!MD5:!PSK:!RC4;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # 安全头
    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    
    # 应用代理
    location / {
        proxy_pass http://localhost:9000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # 静态资源
    location /static/ {
        root /var/www/html;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

### Docker Compose配置

**完整的 docker-compose.yml:**
```yaml
version: '3.8'

services:
  # MySQL数据库
  mysql:
    image: mysql:8.0
    container_name: ssl-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./database/schema.sql:/docker-entrypoint-initdb.d/schema.sql
    ports:
      - "3306:3306"
    networks:
      - ssl_network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10

  # Redis缓存
  redis:
    image: redis:6.2-alpine
    container_name: ssl-redis
    restart: unless-stopped
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    networks:
      - ssl_network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      timeout: 10s
      retries: 5

  # 后端应用
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: ssl-backend
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: mysql
      DB_USERNAME: ${MYSQL_USER}
      DB_PASSWORD: ${MYSQL_PASSWORD}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      LETS_ENCRYPT_EMAIL: ${LETS_ENCRYPT_EMAIL}
      SSL_CERTS_DIR: /opt/ssl/certs
      SSL_BACKUPS_DIR: /opt/ssl/backups
    volumes:
      - ssl_certs:/opt/ssl/certs
      - ssl_backups:/opt/ssl/backups
      - ssl_temp:/tmp/ssl
      - nginx_config:/etc/nginx/conf.d
      - acme_challenge:/var/www/html/.well-known/acme-challenge
    ports:
      - "9000:9000"
    networks:
      - ssl_network
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/api/actuator/health"]
      timeout: 10s
      retries: 5

  # Nginx代理
  nginx:
    image: nginx:alpine
    container_name: ssl-nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - nginx_config:/etc/nginx/conf.d
      - ssl_certs:/opt/ssl/certs
      - acme_challenge:/var/www/html/.well-known/acme-challenge
      - ./frontend/dist:/var/www/html
    networks:
      - ssl_network
    depends_on:
      - backend
    healthcheck:
      test: ["CMD", "nginx", "-t"]
      timeout: 10s
      retries: 3

  # SSL管理器
  ssl-manager:
    build:
      context: ./scripts
      dockerfile: Dockerfile.ssl-manager
    container_name: ssl-manager
    restart: unless-stopped
    environment:
      DOCKER_ENABLED: true
      DOCKER_NGINX_CONTAINER: ssl-nginx
      LETS_ENCRYPT_EMAIL: ${LETS_ENCRYPT_EMAIL}
      MANAGED_DOMAINS: ${MANAGED_DOMAINS}
      API_BASE: http://backend:9000/api
    volumes:
      - ssl_certs:/opt/ssl/certs
      - ssl_backups:/opt/ssl/backups
      - ssl_temp:/tmp/ssl
      - nginx_config:/etc/nginx/conf.d
      - acme_challenge:/var/www/html/.well-known/acme-challenge
      - /var/run/docker.sock:/var/run/docker.sock
    networks:
      - ssl_network
    depends_on:
      - backend
      - nginx

volumes:
  mysql_data:
  redis_data:
  ssl_certs:
  ssl_backups:
  ssl_temp:
  nginx_config:
  acme_challenge:

networks:
  ssl_network:
    driver: bridge
```

## 📖 使用指南

### Web界面操作

#### 1. 访问系统
- 主界面: `https://yourdomain.com`
- SSL管理: `https://yourdomain.com/admin/ssl`
- 性能监控: `https://yourdomain.com/admin/ssl/performance`

#### 2. 获取Let's Encrypt证书
1. 登录管理界面
2. 进入"SSL证书管理"
3. 点击"获取证书"
4. 填写域名和邮箱
5. 选择验证方式 (HTTP01/DNS01)
6. 点击"获取"

#### 3. 上传自定义证书
1. 进入"SSL证书管理"
2. 点击"上传证书"
3. 选择证书文件 (.pem)
4. 选择私钥文件 (.key)
5. 可选: 上传证书链 (.pem)
6. 点击"上传"

#### 4. 证书续期
- **手动续期**: 在证书列表中点击"续期"
- **批量续期**: 选择多个证书，点击"批量续期"
- **自动续期**: 系统会在过期前30天自动续期

#### 5. 性能监控
1. 进入"SSL性能监控"
2. 查看实时指标:
   - 证书状态统计
   - 性能指标
   - 预测性分析
   - 使用统计
3. 执行批量操作
4. 查看监控报告

### 命令行工具

#### 基本操作
```bash
# 列出所有证书
ssl-cli list

# 显示证书详情
ssl-cli show example.com

# 获取Let's Encrypt证书
ssl-cli obtain example.com admin@example.com

# 上传自定义证书
ssl-cli upload example.com cert.pem key.pem

# 续期证书
ssl-cli renew example.com

# 删除证书
ssl-cli delete example.com
```

#### 批量操作
```bash
# 批量验证证书 (ID列表)
ssl-cli batch-validate 1,2,3,4,5

# 批量续期证书
ssl-cli batch-renew 1,2,3,4,5

# 自动续期即将过期的证书
ssl-cli auto-renew
```

#### 监控和报告
```bash
# 查看系统状态
ssl-cli status

# 查看性能指标
ssl-cli metrics

# 生成监控报告
ssl-cli report

# 预测性分析
ssl-cli predictive

# 启动智能性能优化
ssl-cli performance
```

#### 备份和恢复
```bash
# 备份所有证书
ssl-cli backup

# 恢复证书 (从备份文件)
ssl-cli restore /opt/ssl/backups/ssl_backup_20241201_120000.tar.gz

# 清理过期备份
ssl-cli cleanup
```

### API调用示例

#### 获取证书列表
```bash
curl -X GET "https://yourdomain.com/api/ssl/certificates" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

#### 获取Let's Encrypt证书
```bash
curl -X POST "https://yourdomain.com/api/ssl/certificates/lets-encrypt" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "domain": "example.com",
    "email": "admin@example.com",
    "challengeType": "HTTP01"
  }'
```

#### 批量验证证书
```bash
curl -X POST "https://yourdomain.com/api/ssl/performance/batch-validate" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[1, 2, 3, 4, 5]'
```

#### 获取监控指标
```bash
curl -X GET "https://yourdomain.com/api/ssl/performance/monitoring/summary" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

## 📊 API 参考

### 证书管理 API

| 方法 | 端点 | 描述 |
|-----|------|------|
| GET | `/ssl/certificates` | 获取证书列表 |
| GET | `/ssl/certificates/{id}` | 获取证书详情 |
| GET | `/ssl/certificates/domain/{domain}` | 获取域名证书 |
| POST | `/ssl/certificates/lets-encrypt` | 获取Let's Encrypt证书 |
| POST | `/ssl/certificates/upload` | 上传自定义证书 |
| POST | `/ssl/certificates/{id}/renew` | 续期证书 |
| POST | `/ssl/certificates/{id}/apply` | 应用证书配置 |
| DELETE | `/ssl/certificates/{id}` | 删除证书 |

### 性能管理 API

| 方法 | 端点 | 描述 |
|-----|------|------|
| POST | `/ssl/performance/batch-validate` | 批量验证证书 |
| POST | `/ssl/performance/parallel-renewal` | 并行续期证书 |
| GET | `/ssl/performance/predictive-analysis` | 预测性分析 |
| GET | `/ssl/performance/usage-analytics` | 使用统计 |
| GET | `/ssl/performance/metrics` | 性能指标 |
| POST | `/ssl/performance/intelligent-renewal` | 智能续期 |

### 监控指标 API

| 方法 | 端点 | 描述 |
|-----|------|------|
| GET | `/ssl/performance/monitoring/summary` | 监控摘要 |
| GET | `/ssl/performance/monitoring/domains` | 域名指标 |
| GET | `/ssl/performance/monitoring/errors` | 错误统计 |
| GET | `/ssl/performance/monitoring/report` | 监控报告 |
| POST | `/ssl/performance/monitoring/reset` | 重置指标 |
| GET | `/ssl/metrics/prometheus` | Prometheus指标 |

## 📈 监控运维

### Prometheus集成

#### 指标端点
- **应用指标**: `https://yourdomain.com/api/actuator/prometheus`
- **SSL指标**: `https://yourdomain.com/api/ssl/metrics/prometheus`
- **健康检查**: `https://yourdomain.com/api/actuator/health`

#### 关键指标

**证书操作指标:**
```
# 证书获取总数
ssl_certificates_obtained_total{domain="example.com",type="LETS_ENCRYPT"}

# 证书续期总数
ssl_certificates_renewed_total{domain="example.com",status="success"}

# 证书验证总数
ssl_certificates_validated_total{domain="example.com",result="valid"}

# 证书操作错误总数
ssl_certificates_errors_total{operation="obtain",error_type="network"}
```

**性能指标:**
```
# 证书获取耗时
ssl_certificates_obtain_duration_seconds{domain="example.com",status="success"}

# 证书续期耗时
ssl_certificates_renewal_duration_seconds{domain="example.com",status="success"}

# 证书验证延迟
ssl_certificates_validation_duration_seconds{domain="example.com"}
```

**状态指标:**
```
# 活跃证书数量
ssl_certificates_active{application="ssl-certificate-system"}

# 即将过期证书数量
ssl_certificates_expiring{application="ssl-certificate-system"}

# 已过期证书数量
ssl_certificates_expired{application="ssl-certificate-system"}
```

### Grafana仪表板

#### 仪表板配置
```json
{
  "dashboard": {
    "id": null,
    "title": "SSL Certificate Management",
    "tags": ["ssl", "certificates"],
    "timezone": "browser",
    "panels": [
      {
        "title": "Certificate Status Overview",
        "type": "stat",
        "targets": [
          {
            "expr": "ssl_certificates_active",
            "legendFormat": "Active Certificates"
          },
          {
            "expr": "ssl_certificates_expiring",
            "legendFormat": "Expiring Soon"
          },
          {
            "expr": "ssl_certificates_expired",
            "legendFormat": "Expired"
          }
        ]
      },
      {
        "title": "Certificate Operations Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(ssl_certificates_obtained_total[5m])",
            "legendFormat": "Obtain Rate"
          },
          {
            "expr": "rate(ssl_certificates_renewed_total[5m])",
            "legendFormat": "Renewal Rate"
          }
        ]
      },
      {
        "title": "Operation Success Rate",
        "type": "singlestat",
        "targets": [
          {
            "expr": "(rate(ssl_certificates_obtained_total[1h]) + rate(ssl_certificates_renewed_total[1h])) / (rate(ssl_certificates_obtained_total[1h]) + rate(ssl_certificates_renewed_total[1h]) + rate(ssl_certificates_errors_total[1h])) * 100",
            "legendFormat": "Success Rate %"
          }
        ]
      }
    ]
  }
}
```

### 告警规则

#### Prometheus告警规则
```yaml
groups:
  - name: ssl_certificates
    rules:
      # 证书即将过期告警
      - alert: CertificateExpiringSoon
        expr: ssl_certificates_expiring > 0
        for: 0m
        labels:
          severity: warning
        annotations:
          summary: "SSL certificates expiring soon"
          description: "{{ $value }} SSL certificates will expire within 7 days"

      # 证书已过期告警
      - alert: CertificateExpired
        expr: ssl_certificates_expired > 0
        for: 0m
        labels:
          severity: critical
        annotations:
          summary: "SSL certificates expired"
          description: "{{ $value }} SSL certificates have already expired"

      # 证书操作失败率过高
      - alert: HighCertificateFailureRate
        expr: (rate(ssl_certificates_errors_total[5m]) / (rate(ssl_certificates_obtained_total[5m]) + rate(ssl_certificates_renewed_total[5m]) + rate(ssl_certificates_errors_total[5m]))) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High SSL certificate operation failure rate"
          description: "SSL certificate operation failure rate is {{ $value | humanizePercentage }}"

      # 系统响应延迟过高
      - alert: HighCertificateOperationLatency
        expr: histogram_quantile(0.95, rate(ssl_certificates_obtain_duration_seconds_bucket[5m])) > 30
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High SSL certificate operation latency"
          description: "95th percentile latency is {{ $value }}s"
```

### 日志管理

#### 应用日志配置
```yaml
# logback-spring.xml
<configuration>
    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/ssl-certificate-system/application.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/var/log/ssl-certificate-system/application.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
                <totalSizeCap>1GB</totalSizeCap>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%logger{50}] - %msg%n</pattern>
            </encoder>
        </appender>
        
        <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/ssl-certificate-system/application-json.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/var/log/ssl-certificate-system/application-json.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp />
                    <logLevel />
                    <loggerName />
                    <message />
                    <mdc />
                    <stackTrace />
                </providers>
            </encoder>
        </appender>
    </springProfile>
    
    <logger name="com.enterprise.email.service.impl.SslCertificateServiceImpl" level="INFO" />
    <logger name="com.enterprise.email.service.impl.SslCertificateMetricsService" level="INFO" />
    <logger name="com.enterprise.email.controller.SslCertificateController" level="INFO" />
    
    <root level="INFO">
        <appender-ref ref="FILE" />
        <appender-ref ref="JSON_FILE" />
    </root>
</configuration>
```

#### 日志查看命令
```bash
# 查看应用日志
tail -f /var/log/ssl-certificate-system/application.log

# 查看SSL操作日志
grep "SSL" /var/log/ssl-certificate-system/application.log

# 查看错误日志
grep "ERROR" /var/log/ssl-certificate-system/application.log

# 查看证书获取日志
grep "obtainLetsEncryptCertificate" /var/log/ssl-certificate-system/application.log

# 查看证书续期日志
grep "renewCertificate" /var/log/ssl-certificate-system/application.log
```

## 🔧 故障排除

### 常见问题

#### 1. 证书获取失败

**问题**: Let's Encrypt证书获取失败
```
ERROR: Failed to obtain certificate for domain.com: Challenge failed
```

**解决方案**:
```bash
# 检查域名DNS解析
nslookup domain.com

# 检查80端口是否可访问
curl -I http://domain.com/.well-known/acme-challenge/test

# 检查ACME challenge目录权限
ls -la /var/www/html/.well-known/acme-challenge/

# 手动测试certbot
sudo certbot certonly --webroot -w /var/www/html -d domain.com --dry-run

# 查看详细错误日志
docker logs ssl-manager
tail -f /var/log/letsencrypt/letsencrypt.log
```

#### 2. 证书续期失败

**问题**: 自动续期任务失败
```
ERROR: Certificate renewal failed for domain.com: Rate limit exceeded
```

**解决方案**:
```bash
# 检查Let's Encrypt速率限制
ssl-cli show domain.com

# 检查证书有效期
openssl x509 -in /opt/ssl/certs/domain.com/cert.pem -noout -dates

# 手动续期测试
ssl-cli renew domain.com --dry-run

# 重置失败计数
mysql -u ssl_admin -p -e "UPDATE ssl_certificates SET renewal_failures = 0 WHERE domain = 'domain.com';"

# 延迟续期 (等待速率限制重置)
ssl-cli config set auto-renewal.retry-delay-hours 48
```

#### 3. Nginx配置错误

**问题**: SSL证书应用到Nginx失败
```
ERROR: Failed to reload nginx: configuration test failed
```

**解决方案**:
```bash
# 测试Nginx配置
sudo nginx -t

# 查看具体错误
sudo nginx -t 2>&1 | grep error

# 检查证书文件权限
ls -la /opt/ssl/certs/domain.com/

# 验证证书和私钥匹配
ssl-deploy test domain.com

# 手动重载Nginx配置
sudo systemctl reload nginx

# 检查Nginx错误日志
sudo tail -f /var/log/nginx/error.log
```

#### 4. 数据库连接问题

**问题**: 应用无法连接数据库
```
ERROR: Could not get JDBC Connection; Connection refused
```

**解决方案**:
```bash
# 检查MySQL服务状态
sudo systemctl status mysql

# 检查端口监听
netstat -tlnp | grep 3306

# 测试数据库连接
mysql -u ssl_admin -p -h localhost enterprise_email

# 检查数据库用户权限
mysql -u root -p -e "SHOW GRANTS FOR 'ssl_admin'@'localhost';"

# Docker环境检查网络
docker network ls
docker network inspect ssl_network
```

#### 5. Redis连接问题

**问题**: Redis缓存连接失败
```
ERROR: Unable to connect to Redis; Connection refused
```

**解决方案**:
```bash
# 检查Redis服务状态
sudo systemctl status redis

# 测试Redis连接
redis-cli ping

# 检查Redis配置
redis-cli CONFIG GET "*"

# 检查密码认证
redis-cli -a your_password ping

# Docker环境检查
docker logs ssl-redis
```

### 性能优化

#### 1. 数据库优化

**优化配置 (my.cnf)**:
```ini
[mysqld]
# 连接池优化
max_connections = 200
max_user_connections = 180

# 缓存优化
innodb_buffer_pool_size = 2G
query_cache_size = 64M
query_cache_limit = 2M

# SSL证书表索引优化
# 在SSL证书表上创建复合索引
```

**索引优化SQL**:
```sql
-- 创建域名索引
CREATE INDEX idx_ssl_certs_domain ON ssl_certificates(domain);

-- 创建过期时间索引
CREATE INDEX idx_ssl_certs_expires ON ssl_certificates(expires_at);

-- 创建状态索引
CREATE INDEX idx_ssl_certs_status ON ssl_certificates(status);

-- 创建复合索引
CREATE INDEX idx_ssl_certs_domain_status ON ssl_certificates(domain, status);
CREATE INDEX idx_ssl_certs_auto_renew_expires ON ssl_certificates(auto_renew, expires_at);
```

#### 2. Redis优化

**优化配置**:
```conf
# 内存优化
maxmemory 1gb
maxmemory-policy allkeys-lru

# 持久化优化
save 900 1
save 300 10
save 60 10000

# 网络优化
tcp-keepalive 300
timeout 300
```

#### 3. 应用优化

**JVM参数优化**:
```bash
java -Xms2g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/ssl-certificate-system/ \
     -Dspring.profiles.active=prod \
     -jar target/enterprise-email-system-1.0.0.jar
```

**连接池优化**:
```yaml
spring:
  datasource:
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50
      max-wait: 60000
      validation-query: SELECT 1
      test-while-idle: true
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
```

### 安全配置

#### 1. SSL/TLS优化

**Nginx SSL配置**:
```nginx
# 现代化SSL配置
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384;
ssl_prefer_server_ciphers off;

# OCSP装订
ssl_stapling on;
ssl_stapling_verify on;
ssl_trusted_certificate /opt/ssl/certs/domain.com/chain.pem;

# 安全头
add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
add_header X-Frame-Options DENY always;
add_header X-Content-Type-Options nosniff always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:;" always;
```

#### 2. 访问控制

**API访问限制**:
```nginx
# 管理API访问限制
location /api/ssl/ {
    allow 10.0.0.0/8;
    allow 172.16.0.0/12;
    allow 192.168.0.0/16;
    deny all;
    
    proxy_pass http://backend;
}

# 速率限制
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
location /api/ {
    limit_req zone=api burst=20 nodelay;
    proxy_pass http://backend;
}
```

#### 3. 文件权限

**SSL文件权限设置**:
```bash
# 设置SSL证书目录权限
sudo chown -R root:ssl-cert /opt/ssl/certs
sudo chmod -R 750 /opt/ssl/certs

# 设置私钥文件权限 (只有owner可读)
sudo find /opt/ssl/certs -name "*.key" -exec chmod 600 {} \;

# 设置证书文件权限 (组可读)
sudo find /opt/ssl/certs -name "*.pem" -exec chmod 644 {} \;

# 设置备份目录权限
sudo chown -R ssl-admin:ssl-admin /opt/ssl/backups
sudo chmod -R 700 /opt/ssl/backups
```

## 🚀 高级特性

### 多CA支持

系统支持集成多个证书颁发机构:

#### Let's Encrypt (默认)
```yaml
ssl:
  providers:
    lets-encrypt:
      enabled: true
      directory-url: https://acme-v02.api.letsencrypt.org/directory
      staging-url: https://acme-staging-v02.api.letsencrypt.org/directory
      contact-email: admin@example.com
```

#### ZeroSSL集成
```yaml
ssl:
  providers:
    zerossl:
      enabled: true
      directory-url: https://acme.zerossl.com/v2/DV90
      api-key: your-zerossl-api-key
      contact-email: admin@example.com
```

#### Buypass集成
```yaml
ssl:
  providers:
    buypass:
      enabled: true
      directory-url: https://api.buypass.com/acme/directory
      staging-url: https://api.test4.buypass.no/acme/directory
      contact-email: admin@example.com
```

### 证书透明度日志 (CT Log)

监控证书透明度日志以检测未授权证书:

```java
@Service
public class CertificateTransparencyService {
    
    public void monitorCTLogs(String domain) {
        // 查询CT日志
        List<CTLogEntry> entries = queryCtLogs(domain);
        
        // 检测未授权证书
        for (CTLogEntry entry : entries) {
            if (isUnauthorizedCertificate(entry)) {
                sendSecurityAlert(domain, entry);
            }
        }
    }
}
```

### 证书固定 (Certificate Pinning)

生成HPKP头部和移动端固定配置:

```java
@Service
public class CertificatePinningService {
    
    public String generateHPKPHeader(String domain) {
        SslCertificate cert = getCertificate(domain);
        String primaryPin = calculateSPKIPin(cert.getCertPath());
        String backupPin = calculateSPKIPin(cert.getBackupKeyPath());
        
        return String.format(
            "Public-Key-Pins: pin-sha256=\"%s\"; pin-sha256=\"%s\"; max-age=5184000; includeSubDomains",
            primaryPin, backupPin
        );
    }
}
```

### 自动化DNS验证

集成多个DNS提供商API自动完成DNS-01验证:

```java
@Component
public class DnsProviderManager {
    
    private final Map<String, DnsProvider> providers = Map.of(
        "cloudflare", new CloudflareDnsProvider(),
        "route53", new Route53DnsProvider(),
        "aliyun", new AliyunDnsProvider()
    );
    
    public void addTxtRecord(String domain, String name, String value) {
        String provider = getProviderForDomain(domain);
        providers.get(provider).addTxtRecord(domain, name, value);
    }
}
```

### 证书生命周期自动化

完整的证书生命周期自动化管理:

```java
@Component
public class CertificateLifecycleManager {
    
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    public void manageCertificateLifecycle() {
        // 1. 扫描即将过期的证书
        List<SslCertificate> expiring = findExpiringCertificates(30);
        
        // 2. 智能续期决策
        for (SslCertificate cert : expiring) {
            if (shouldRenew(cert)) {
                scheduleRenewal(cert);
            }
        }
        
        // 3. 清理过期证书
        cleanupExpiredCertificates();
        
        // 4. 备份管理
        performIncrementalBackup();
        
        // 5. 合规性检查
        performComplianceCheck();
    }
}
```

### 高可用部署

支持多节点高可用部署:

```yaml
# docker-compose.ha.yml
version: '3.8'

services:
  # 负载均衡器
  haproxy:
    image: haproxy:2.4
    ports:
      - "80:80"
      - "443:443"
      - "8404:8404"
    volumes:
      - ./haproxy/haproxy.cfg:/usr/local/etc/haproxy/haproxy.cfg
    depends_on:
      - backend1
      - backend2

  # 后端节点1
  backend1:
    extends:
      file: docker-compose.yml
      service: backend
    environment:
      - NODE_ID=node1
      - CLUSTER_ENABLED=true

  # 后端节点2
  backend2:
    extends:
      file: docker-compose.yml
      service: backend
    environment:
      - NODE_ID=node2
      - CLUSTER_ENABLED=true

  # MySQL主从复制
  mysql-master:
    image: mysql:8.0
    environment:
      - MYSQL_REPLICATION_MODE=master
      
  mysql-slave:
    image: mysql:8.0
    environment:
      - MYSQL_REPLICATION_MODE=slave
      - MYSQL_MASTER_HOST=mysql-master

  # Redis Sentinel集群
  redis-sentinel:
    image: redis:6.2-alpine
    command: redis-sentinel /etc/redis/sentinel.conf
    volumes:
      - ./redis/sentinel.conf:/etc/redis/sentinel.conf
```

---

## 📞 支持与社区

### 技术支持
- 📧 **邮箱**: support@ssl-management.com
- 💬 **讨论区**: [GitHub Discussions](https://github.com/your-org/ssl-management/discussions)
- 🐛 **问题报告**: [GitHub Issues](https://github.com/your-org/ssl-management/issues)
- 📚 **文档**: [在线文档](https://docs.ssl-management.com)

### 贡献指南
欢迎贡献代码、文档或报告问题！请参考 [CONTRIBUTING.md](CONTRIBUTING.md)

### 许可证
本项目采用 [MIT License](LICENSE) 许可证。

---

**🎉 恭喜！您现在拥有了一个功能完整的企业级SSL证书管理系统！**

该系统提供了从证书获取、管理、监控到自动化运维的全套解决方案，助您轻松管理SSL证书生命周期。