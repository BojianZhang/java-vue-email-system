# 🚀 Quick Start Guide - SSL Certificate Management

Get your SSL certificate management system up and running in minutes!

## ⚡ One-Line Installation

```bash
curl -sSL https://raw.githubusercontent.com/your-repo/main/scripts/install-ssl-system.sh | sudo bash
```

## 🐳 Docker Quick Start (Recommended)

### 1. Download and Setup
```bash
git clone <your-repository>
cd java-vue-email-system
cp .env.example .env
```

### 2. Configure Your Domain
```bash
# Edit .env file - ONLY change these essential settings:
LETS_ENCRYPT_EMAIL=your-email@example.com          # Your email
MANAGED_DOMAINS=yourdomain.com                     # Your domain
MYSQL_ROOT_PASSWORD=your_secure_password           # Database password
```

### 3. Start Everything
```bash
# Create SSL directories
sudo mkdir -p /opt/ssl/{certs,backups} /var/www/html/.well-known

# Start services
docker-compose up -d

# Wait 30 seconds, then get your first certificate
sleep 30
docker exec ssl-manager ssl-deploy obtain yourdomain.com your-email@example.com HTTP01
```

### 4. Access Your System
- 🌐 **Website**: `https://yourdomain.com`
- 📱 **SSL Dashboard**: `https://yourdomain.com/ssl/certificates`  
- 📊 **API**: `https://yourdomain.com/api/ssl/certificates`

## 🖥️ Native Installation Quick Start

### 1. Install System
```bash
# Download project
git clone <your-repository>
cd java-vue-email-system

# Run installer (as root)
sudo bash scripts/install-ssl-system.sh
```

### 2. Get Certificate
```bash
# Get your first Let's Encrypt certificate
ssl-deploy obtain yourdomain.com your-email@example.com HTTP01

# Test it works
ssl-deploy test yourdomain.com
```

### 3. Enable Monitoring
```bash
# Enable automatic monitoring and renewal
sudo systemctl enable ssl-monitor.timer
sudo systemctl start ssl-monitor.timer

# Check status
systemctl status ssl-monitor.timer
```

## 🎯 Quick Commands Reference

```bash
# Get Let's Encrypt certificate
ssl-deploy obtain example.com admin@example.com HTTP01

# Upload custom certificate  
ssl-deploy deploy example.com /path/to/cert.pem /path/to/key.pem

# Test certificate configuration
ssl-deploy test example.com

# Check all certificates
ssl-monitor scan

# Generate monitoring report
ssl-monitor report

# Backup certificates
ssl-deploy backup

# View logs
tail -f /var/log/ssl-monitor.log
```

## 🔧 Essential Configuration

### Domain Setup (REQUIRED)
1. **Point DNS**: Make sure your domain points to your server's IP
2. **Open Ports**: Ensure ports 80 and 443 are accessible
3. **Email Access**: Use a valid email for Let's Encrypt registration

### Basic Nginx Config (Auto-generated)
The system automatically creates:
```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;
    
    ssl_certificate /opt/ssl/certs/yourdomain.com/fullchain.pem;
    ssl_certificate_key /opt/ssl/certs/yourdomain.com/key.pem;
    
    # Your app
    location / {
        proxy_pass http://localhost:8080;
    }
}
```

## 🔍 Verify Installation

### Check Services
```bash
# Docker deployment
docker-compose ps
docker-compose logs ssl-manager

# Native installation  
systemctl status nginx
systemctl status ssl-monitor.timer
```

### Test Certificate
```bash
# Test HTTPS connection
curl -I https://yourdomain.com

# Check certificate details
openssl s_client -connect yourdomain.com:443 -servername yourdomain.com
```

### Web Interface
Visit `https://yourdomain.com/ssl/certificates` to:
- ✅ View certificate status
- ✅ Obtain new certificates
- ✅ Monitor expiry dates
- ✅ Manage renewals

## 🆘 Quick Troubleshooting

### Certificate Acquisition Fails
```bash
# Check DNS
nslookup yourdomain.com

# Check HTTP challenge
curl http://yourdomain.com/.well-known/acme-challenge/test

# View certbot logs
tail -f /var/log/letsencrypt/letsencrypt.log
```

### Services Not Starting
```bash
# Check Docker
docker-compose logs

# Check system resources
free -m && df -h

# Check ports
netstat -tlnp | grep :443
```

### Need Help?
- 📖 **Full Documentation**: See `DEPLOYMENT_GUIDE.md`
- 🔧 **Configuration**: Edit `/opt/ssl/scripts/ssl-config.conf`
- 📧 **Support**: your-support@example.com

## 🎉 You're Done!

Your SSL certificate management system is now:
- ✅ **Automatically renewing** certificates 30 days before expiry
- ✅ **Monitoring** all certificates daily
- ✅ **Securing** your website with HTTPS
- ✅ **Backing up** certificates automatically
- ✅ **Notifying** you of any issues

**Welcome to enterprise-grade SSL management! 🔐**

---

### Next Steps
1. 🌟 Star the repository if it helped you!
2. 📚 Read the full documentation for advanced features
3. 🔔 Set up monitoring alerts for your team
4. 🚀 Deploy to your production environment

**Questions? Check our [FAQ](FAQ.md) or [open an issue](https://github.com/your-repo/issues)!**