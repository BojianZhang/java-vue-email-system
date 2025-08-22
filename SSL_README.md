# 🔐 SSL/TLS Certificate Management System

A comprehensive SSL/TLS certificate management system for enterprise email services with automated Let's Encrypt integration, custom certificate support, and advanced monitoring capabilities.

## ✨ Features

### 🎯 **Core Capabilities**
- **Let's Encrypt Integration** - Automated free SSL certificate acquisition
- **Custom Certificates** - Upload and manage your own SSL certificates  
- **Self-Signed Certificates** - Generate certificates for development/testing
- **Automatic Renewal** - Smart auto-renewal 30 days before expiry
- **Multi-Domain Support** - Manage certificates for multiple domains
- **Certificate Validation** - Comprehensive certificate and chain validation

### 🛡️ **Security & Compliance**
- **HTTPS Enforcement** - Automatic HTTP to HTTPS redirects
- **Security Headers** - HSTS, CSP, X-Frame-Options, and more
- **TLS 1.2/1.3 Support** - Modern encryption protocols
- **OCSP Stapling** - Enhanced certificate validation
- **Perfect Forward Secrecy** - Secure cipher suite configuration

### 📊 **Monitoring & Management**
- **Real-time Monitoring** - Certificate status and expiry tracking
- **Health Checks** - Automated system health validation
- **Email Notifications** - Alerts for expiring/failed certificates
- **Backup & Restore** - Automated certificate backup system
- **Audit Logging** - Comprehensive certificate operation logs

### 🎨 **User Interface**
- **Web Dashboard** - Intuitive certificate management interface
- **Statistics Overview** - Certificate status and metrics
- **One-Click Operations** - Easy certificate deployment and testing
- **Configuration Preview** - Nginx SSL configuration generation

## 🏗️ **Architecture**

```
SSL Certificate Management System
├── Backend (Spring Boot)
│   ├── Certificate Service Layer
│   ├── ACME Protocol Integration
│   ├── Monitoring & Scheduling
│   └── Security Configuration
├── Frontend (Vue.js + Element Plus)
│   ├── Management Dashboard
│   ├── Certificate Statistics
│   └── Configuration Tools
├── Database Layer
│   ├── Certificate Storage
│   ├── Event Logging
│   └── Monitoring Data
└── Operations Scripts
    ├── Deployment Automation
    ├── Monitoring Tools
    └── System Integration
```

## 📦 **Installation**

### Quick Install
```bash
# Clone the repository
git clone <repository-url>
cd java-vue-email-system

# Run the installation script (as root)
sudo bash scripts/install-ssl-system.sh
```

### Manual Installation

#### 1. **Prerequisites**
```bash
# Ubuntu/Debian
apt-get update
apt-get install -y nginx certbot python3-certbot-nginx openssl curl wget jq

# CentOS/RHEL
yum install -y epel-release
yum install -y nginx certbot python3-certbot-nginx openssl curl wget jq
```

#### 2. **Database Setup**
```sql
-- Run the SSL certificate database schema
mysql -u root -p < database/ssl_certificates.sql
```

#### 3. **Backend Configuration**
```yaml
# application.yml
ssl:
  certs:
    directory: /opt/ssl/certs
  nginx:
    config: /etc/nginx/conf.d
  lets-encrypt:
    email: admin@example.com
    staging: false
  auto-renewal:
    enabled: true
    days-before: 30
```

#### 4. **Script Installation**
```bash
# Copy scripts to system directory
mkdir -p /opt/ssl/scripts
cp scripts/* /opt/ssl/scripts/
chmod +x /opt/ssl/scripts/*.sh

# Create symbolic links
ln -s /opt/ssl/scripts/ssl-deploy.sh /usr/local/bin/ssl-deploy
ln -s /opt/ssl/scripts/ssl-monitor.sh /usr/local/bin/ssl-monitor

# Install systemd services
cp scripts/ssl-monitor.service /etc/systemd/system/
cp scripts/ssl-monitor.timer /etc/systemd/system/
systemctl daemon-reload
systemctl enable ssl-monitor.timer
systemctl start ssl-monitor.timer
```

## 🚀 **Usage**

### Command Line Interface

#### **Deploy Let's Encrypt Certificate**
```bash
# HTTP-01 validation (recommended)
ssl-deploy obtain example.com admin@example.com HTTP01

# DNS-01 validation (for wildcards)
ssl-deploy obtain *.example.com admin@example.com DNS01
```

#### **Deploy Custom Certificate**
```bash
ssl-deploy deploy example.com /path/to/cert.pem /path/to/key.pem /path/to/chain.pem
```

#### **Monitor Certificates**
```bash
# Scan all certificates
ssl-monitor scan

# Generate monitoring report
ssl-monitor report

# Renew expiring certificates
ssl-monitor renew

# Backup certificates
ssl-deploy backup
```

#### **Test SSL Configuration**
```bash
ssl-deploy test example.com
```

### Web Interface

Access the SSL management dashboard at: `https://your-domain/ssl/certificates`

#### **Dashboard Features:**
- 📊 **Certificate Statistics** - Overview of all certificates
- 🔍 **Certificate Search** - Filter by domain, type, status
- ➕ **Add Certificate** - Get Let's Encrypt or upload custom certificates
- 🔄 **Renewal Management** - Manual and automatic renewal controls
- 📋 **Configuration Preview** - View generated Nginx configurations
- 🧪 **Testing Tools** - Validate certificate deployments

### REST API

#### **Get Certificate List**
```http
GET /api/ssl/certificates?current=1&size=20&domain=example.com
```

#### **Obtain Let's Encrypt Certificate**
```http
POST /api/ssl/certificates/lets-encrypt
Content-Type: application/json

{
  "domain": "example.com",
  "email": "admin@example.com",
  "challengeType": "HTTP01"
}
```

#### **Upload Custom Certificate**
```http
POST /api/ssl/certificates/upload
Content-Type: multipart/form-data

domain=example.com
certFile=@certificate.pem
keyFile=@private-key.pem
chainFile=@chain.pem
```

#### **Renew Certificate**
```http
POST /api/ssl/certificates/{id}/renew
```

#### **Apply Certificate to Nginx**
```http
POST /api/ssl/certificates/{id}/apply
```

## ⚙️ **Configuration**

### **Main Configuration** (`/opt/ssl/scripts/ssl-config.conf`)
```bash
# Certificate storage directory
SSL_CERTS_DIR="/opt/ssl/certs"

# Nginx configuration directory  
NGINX_CONFIG_DIR="/etc/nginx/conf.d"

# Let's Encrypt settings
LETS_ENCRYPT_EMAIL="admin@example.com"
AUTO_RENEWAL_ENABLED=true
RENEWAL_DAYS_BEFORE=30

# Security settings
SECURITY_HEADERS_ENABLED=true
SSL_PROTOCOLS="TLSv1.2 TLSv1.3"
HSTS_MAX_AGE=31536000

# Notification settings
NOTIFICATION_ENABLED=true
NOTIFICATION_EMAIL="admin@example.com"
```

### **SSL Security Headers**
The system automatically configures secure headers:
- **HSTS** - HTTP Strict Transport Security
- **CSP** - Content Security Policy  
- **X-Frame-Options** - Clickjacking protection
- **X-Content-Type-Options** - MIME sniffing protection
- **Referrer-Policy** - Referrer information control

### **Nginx SSL Configuration**
Generated configurations include:
- Modern TLS protocols (1.2, 1.3)
- Secure cipher suites
- OCSP stapling
- Perfect Forward Secrecy
- HTTP/2 support
- Security headers

## 📊 **Monitoring**

### **Automated Monitoring**
- ✅ Daily certificate status checks
- ⏰ Automatic renewal attempts
- 📧 Email notifications for expiring certificates
- 🧹 Automated cleanup of old backups and logs
- 📈 Health check monitoring

### **Manual Monitoring**
```bash
# View certificate status
ssl-monitor scan

# Check specific domain
curl -I https://example.com

# View systemd service status
systemctl status ssl-monitor.timer
systemctl status ssl-monitor.service

# View logs
tail -f /var/log/ssl-monitor.log
journalctl -u ssl-monitor.service -f
```

### **Notification Channels**
- **Email** - Certificate expiry and renewal notifications
- **Webhooks** - Custom webhook integration for alerts
- **System Logs** - Detailed logging for all operations
- **Web Dashboard** - Real-time status in the UI

## 🔧 **Troubleshooting**

### **Common Issues**

#### **Let's Encrypt Certificate Acquisition Fails**
```bash
# Check domain DNS resolution
nslookup example.com

# Verify HTTP challenge path accessibility
curl -I http://example.com/.well-known/acme-challenge/test

# Check certbot logs
journalctl -u certbot
```

#### **Certificate Not Applied to Nginx**
```bash
# Test Nginx configuration
nginx -t

# Check Nginx SSL configuration
cat /etc/nginx/conf.d/example.com-ssl.conf

# Reload Nginx
systemctl reload nginx
```

#### **Automatic Renewal Fails**
```bash
# Check renewal status
certbot certificates

# Test renewal
certbot renew --dry-run

# Check SSL monitor logs
tail -f /var/log/ssl-monitor.log
```

### **Log Locations**
- SSL Management: `/var/log/ssl-*.log`
- Nginx: `/var/log/nginx/error.log`  
- Certbot: `/var/log/letsencrypt/letsencrypt.log`
- System Service: `journalctl -u ssl-monitor.service`

### **Reset/Recovery**
```bash
# Reset SSL system
systemctl stop ssl-monitor.timer
rm -rf /opt/ssl/certs/*
systemctl start ssl-monitor.timer

# Restore from backup
ssl-deploy restore /opt/ssl/backups/ssl_backup_YYYYMMDD_HHMMSS
```

## 📚 **API Documentation**

Complete API documentation is available at: `https://your-domain/swagger-ui.html`

### **Key Endpoints**
- `GET /api/ssl/certificates` - List certificates
- `POST /api/ssl/certificates/lets-encrypt` - Obtain Let's Encrypt certificate
- `POST /api/ssl/certificates/upload` - Upload custom certificate
- `POST /api/ssl/certificates/{id}/renew` - Renew certificate
- `POST /api/ssl/certificates/{id}/apply` - Apply to Nginx
- `GET /api/ssl/certificates/stats` - Get statistics
- `GET /api/ssl/certificates/health` - Health check

## 🛠️ **Development**

### **Backend Development**
```bash
# Start Spring Boot application
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build JAR
./mvnw clean package
```

### **Frontend Development**  
```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```

### **Contributing**
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 **Support**

- 📧 **Email**: support@example.com
- 💬 **Issues**: [GitHub Issues](https://github.com/your-repo/issues)
- 📖 **Documentation**: [Wiki](https://github.com/your-repo/wiki)

---

**Built with ❤️ for enterprise email security**