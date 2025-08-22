# 🚀 SSL/TLS Certificate Management System Deployment Guide

This guide provides step-by-step instructions for deploying the SSL/TLS certificate management system in production environments.

## 📋 Prerequisites

### System Requirements
- **Operating System**: Ubuntu 20.04+ / CentOS 8+ / Debian 11+
- **RAM**: Minimum 4GB, Recommended 8GB+
- **Storage**: Minimum 50GB free space
- **CPU**: 2+ cores recommended
- **Network**: Public IP address with ports 80/443 accessible

### Required Software
- Docker & Docker Compose (for containerized deployment)
- OR Native installation: Nginx, Certbot, OpenSSL, MySQL, Redis

### Domain Requirements
- Valid domain name(s) with DNS pointing to your server
- Domain ownership verification capability
- Firewall ports 80 (HTTP) and 443 (HTTPS) open

## 🐳 Docker Deployment (Recommended)

### 1. Clone and Setup
```bash
git clone <repository-url>
cd java-vue-email-system

# Copy environment configuration
cp .env.example .env
```

### 2. Configure Environment
Edit `.env` file with your settings:
```bash
# Essential Settings
LETS_ENCRYPT_EMAIL=your-email@example.com
NOTIFICATION_EMAIL=admin@example.com
MANAGED_DOMAINS=yourdomain.com,mail.yourdomain.com

# Database Settings
MYSQL_ROOT_PASSWORD=your_secure_password_here
MYSQL_PASSWORD=your_app_password_here
REDIS_PASSWORD=your_redis_password_here

# SSL Settings
SSL_AUTO_RENEWAL_ENABLED=true
LETS_ENCRYPT_STAGING=false  # Set to true for testing
```

### 3. Prepare SSL Directories
```bash
sudo mkdir -p /opt/ssl/{certs,backups,logs}
sudo mkdir -p /var/www/html/.well-known
sudo chown -R $USER:$USER /opt/ssl
```

### 4. Build and Start Services
```bash
# Build containers
docker-compose build

# Start all services
docker-compose up -d

# Check service status
docker-compose ps
docker-compose logs ssl-manager
```

### 5. Obtain First Certificate
```bash
# Using Docker
docker exec ssl-manager ssl-deploy obtain yourdomain.com your-email@example.com HTTP01

# Or using host commands (after installation)
ssl-deploy obtain yourdomain.com your-email@example.com HTTP01
```

## 🖥️ Native Installation

### 1. Run Installation Script
```bash
# Download and run installer
curl -sSL https://raw.githubusercontent.com/your-repo/main/scripts/install-ssl-system.sh | sudo bash

# Or clone and install
git clone <repository-url>
cd java-vue-email-system
sudo bash scripts/install-ssl-system.sh
```

### 2. Configure System
```bash
# Edit configuration
sudo nano /opt/ssl/scripts/ssl-config.conf

# Key settings to modify:
SSL_CERTS_DIR="/opt/ssl/certs"
NGINX_CONFIG_DIR="/etc/nginx/conf.d"
LETS_ENCRYPT_EMAIL="your-email@example.com"
NOTIFICATION_EMAIL="admin@example.com"
```

### 3. Database Setup
```bash
# Install MySQL and create database
mysql -u root -p < database/ssl_certificates.sql

# Configure application database connection
sudo nano backend/src/main/resources/application-production.yml
```

### 4. Start Services
```bash
# Enable and start SSL monitoring
sudo systemctl enable ssl-monitor.timer
sudo systemctl start ssl-monitor.timer

# Start application services
sudo systemctl start nginx
sudo systemctl enable nginx
```

## ⚙️ Configuration

### Nginx SSL Configuration
The system automatically generates Nginx configurations. You can customize the template:

```bash
# Edit SSL snippet template
sudo nano /etc/nginx/ssl-snippets/ssl-params.conf

# Example custom configuration
ssl_protocols TLSv1.2 TLSv1.3;
ssl_prefer_server_ciphers on;
ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;

# Security Headers
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
add_header X-Frame-Options DENY always;
add_header X-Content-Type-Options nosniff always;
```

### Let's Encrypt Configuration
```bash
# Configure certbot
sudo nano /etc/letsencrypt/renewal-hooks/deploy/ssl-manager.sh

#!/bin/bash
# Restart services after certificate renewal
systemctl reload nginx
/opt/ssl/scripts/ssl-renewal-hooks.sh post $RENEWED_DOMAINS
```

### Monitoring Configuration
```bash
# Configure monitoring alerts
sudo nano /opt/ssl/scripts/ssl-config.conf

# Email notifications
NOTIFICATION_ENABLED=true
NOTIFICATION_EMAIL="admin@example.com"

# Webhook notifications (optional)
NOTIFICATION_WEBHOOK="https://hooks.slack.com/your-webhook-url"
```

## 🔐 Security Best Practices

### 1. File Permissions
```bash
# Set secure permissions
sudo chmod 600 /opt/ssl/certs/*/key.pem
sudo chmod 644 /opt/ssl/certs/*/cert.pem
sudo chown -R root:root /opt/ssl/certs
```

### 2. Firewall Configuration
```bash
# Ubuntu/Debian (UFW)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable

# CentOS/RHEL (firewalld)
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --reload
```

### 3. SSL Security Headers
The system automatically configures security headers:
- HSTS with preload
- Content Security Policy
- X-Frame-Options
- X-Content-Type-Options
- Referrer-Policy

### 4. Certificate Validation
```bash
# Test certificate configuration
ssl-deploy test yourdomain.com

# Check certificate details online
openssl s_client -connect yourdomain.com:443 -servername yourdomain.com
```

## 📊 Monitoring and Maintenance

### Health Checks
```bash
# System health check
ssl-monitor scan

# Generate detailed report
ssl-monitor report

# Check specific certificate
openssl x509 -in /opt/ssl/certs/yourdomain.com/cert.pem -text -noout
```

### Log Monitoring
```bash
# SSL management logs
tail -f /var/log/ssl-monitor.log
tail -f /var/log/ssl-deployment.log

# Nginx SSL logs
tail -f /var/log/nginx/error.log

# Let's Encrypt logs
tail -f /var/log/letsencrypt/letsencrypt.log

# System service logs
journalctl -u ssl-monitor.service -f
```

### Automated Monitoring
```bash
# View cron jobs
crontab -l

# Manual renewal test
certbot renew --dry-run

# Check systemd timers
systemctl list-timers ssl-monitor.timer
```

## 🔄 Backup and Recovery

### Certificate Backup
```bash
# Create backup
ssl-deploy backup

# List backups
ls -la /opt/ssl/backups/

# Restore from backup
ssl-deploy restore /opt/ssl/backups/ssl_backup_YYYYMMDD_HHMMSS
```

### Database Backup
```bash
# Backup SSL certificate database
mysqldump -u root -p email_system ssl_certificates ssl_certificate_logs > ssl_certs_backup.sql

# Restore database
mysql -u root -p email_system < ssl_certs_backup.sql
```

## 🚨 Troubleshooting

### Common Issues

#### Certificate Acquisition Fails
```bash
# Check domain DNS resolution
nslookup yourdomain.com

# Verify HTTP challenge path
curl -I http://yourdomain.com/.well-known/acme-challenge/test

# Check certbot logs
journalctl -u certbot.service
cat /var/log/letsencrypt/letsencrypt.log
```

#### Certificate Not Applied
```bash
# Test Nginx configuration
nginx -t

# Reload Nginx
systemctl reload nginx

# Check certificate files exist
ls -la /opt/ssl/certs/yourdomain.com/
```

#### Automatic Renewal Issues
```bash
# Check renewal configuration
certbot certificates

# Test renewal
certbot renew --dry-run --cert-name yourdomain.com

# Check SSL monitor status
systemctl status ssl-monitor.timer
systemctl status ssl-monitor.service
```

#### Docker Issues
```bash
# Check container status
docker-compose ps

# View container logs
docker-compose logs ssl-manager
docker-compose logs nginx

# Restart services
docker-compose restart ssl-manager
docker-compose restart nginx
```

### Performance Issues
```bash
# Check system resources
htop
df -h
free -m

# Monitor SSL connections
ss -tlnp | grep :443

# Check certificate validation time
time openssl s_client -connect yourdomain.com:443 -servername yourdomain.com < /dev/null
```

## 📈 Scaling and Optimization

### Multi-Domain Management
```bash
# Obtain certificates for multiple domains
ssl-deploy obtain domain1.com admin@domain1.com HTTP01
ssl-deploy obtain domain2.com admin@domain2.com HTTP01
ssl-deploy obtain domain3.com admin@domain3.com HTTP01

# Batch operations
for domain in domain1.com domain2.com domain3.com; do
    ssl-deploy test "$domain"
done
```

### Load Balancer Integration
For load balancer setups, configure SSL termination at the load balancer:

```nginx
# Load balancer SSL configuration
upstream backend_servers {
    server backend1.internal:8080;
    server backend2.internal:8080;
    server backend3.internal:8080;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;
    
    ssl_certificate /opt/ssl/certs/yourdomain.com/fullchain.pem;
    ssl_certificate_key /opt/ssl/certs/yourdomain.com/key.pem;
    
    location / {
        proxy_pass http://backend_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### High Availability
```bash
# Configure certificate sync between servers
rsync -av /opt/ssl/certs/ backup-server:/opt/ssl/certs/

# Setup database replication for certificate metadata
# Configure load balancer with SSL health checks
```

## 🔧 Customization

### Custom Certificate Validation
```bash
# Add custom validation scripts
sudo nano /opt/ssl/hooks/pre-renewal-custom.sh
sudo nano /opt/ssl/hooks/post-renewal-custom.sh
sudo chmod +x /opt/ssl/hooks/*.sh
```

### Custom Nginx Templates
```bash
# Create custom SSL configuration templates
sudo nano /opt/ssl/templates/custom-ssl.conf

# Use custom template in deployment
ssl-deploy deploy yourdomain.com /path/to/cert.pem /path/to/key.pem --template custom-ssl.conf
```

### Integration with External Systems
```bash
# Configure webhook notifications
export NOTIFICATION_WEBHOOK="https://your-monitoring-system.com/webhook"

# Custom API integration
curl -X POST "https://your-api.com/ssl-status" \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{"domain": "yourdomain.com", "status": "renewed"}'
```

## 📞 Support and Maintenance

### Regular Maintenance Tasks
- [ ] Weekly certificate status review
- [ ] Monthly backup verification
- [ ] Quarterly security updates
- [ ] Semi-annual configuration review

### Getting Help
- 📧 Email: support@example.com
- 🐛 Issues: GitHub Issues
- 📖 Documentation: Project Wiki
- 💬 Community: Discord/Slack

---

**Your SSL/TLS certificate management system is now ready for production! 🎉**