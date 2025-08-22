# 📦 数据迁移操作指南

## 📋 迁移准备清单

### 迁移前准备工作

#### 1. 数据评估
```bash
# 评估当前邮件数据量
du -sh /var/mail/*
du -sh /mail/vhosts/*

# 统计用户数量和邮件数量
mysql -e "SELECT COUNT(*) as user_count FROM users;" email_system
mysql -e "SELECT COUNT(*) as email_count FROM emails;" email_system

# 检查数据库大小
mysql -e "SELECT table_schema AS 'Database', 
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
    FROM information_schema.tables 
    WHERE table_schema='email_system';"
```

#### 2. 迁移环境准备
```bash
# 在新服务器上安装必要软件
sudo apt update
sudo apt install -y mysql-server redis-server nginx rsync

# 创建迁移工作目录
mkdir -p /tmp/migration/{database,emails,config}
chmod 755 /tmp/migration
```

## 🔄 完整迁移流程

### 方案一：停机迁移 (推荐生产环境)

#### 步骤1：源服务器数据导出
```bash
#!/bin/bash
# 完整数据导出脚本 - export_data.sh

BACKUP_DIR="/tmp/migration"
MYSQL_USER="root"
MYSQL_PASS="your_password"
DATE=$(date +%Y%m%d_%H%M%S)

echo "开始数据导出..."

# 1. 导出数据库
echo "导出数据库..."
mysqldump -u$MYSQL_USER -p$MYSQL_PASS --single-transaction \
    --routines --triggers --events email_system > \
    "$BACKUP_DIR/database/email_system_$DATE.sql"

# 2. 导出邮件文件
echo "导出邮件文件..."
tar -czf "$BACKUP_DIR/emails/mailboxes_$DATE.tar.gz" /mail/vhosts/
tar -czf "$BACKUP_DIR/emails/postfix_$DATE.tar.gz" /etc/postfix/
tar -czf "$BACKUP_DIR/emails/dovecot_$DATE.tar.gz" /etc/dovecot/

# 3. 导出应用程序
echo "导出应用程序..."
tar -czf "$BACKUP_DIR/application_$DATE.tar.gz" /opt/email-system/

# 4. 导出配置文件
echo "导出配置文件..."
tar -czf "$BACKUP_DIR/config/system_config_$DATE.tar.gz" \
    /etc/nginx/ /etc/ssl/ /etc/letsencrypt/

# 5. 导出用户数据
echo "导出用户数据..."
cp /etc/passwd "$BACKUP_DIR/config/passwd_$DATE"
cp /etc/group "$BACKUP_DIR/config/group_$DATE"

# 6. 创建迁移清单
echo "创建迁移清单..."
cat > "$BACKUP_DIR/migration_manifest.txt" << EOF
数据迁移清单 - $DATE
==============================

数据库备份:
- email_system_$DATE.sql ($(du -h $BACKUP_DIR/database/email_system_$DATE.sql | cut -f1))

邮件数据:
- mailboxes_$DATE.tar.gz ($(du -h $BACKUP_DIR/emails/mailboxes_$DATE.tar.gz | cut -f1))
- postfix_$DATE.tar.gz ($(du -h $BACKUP_DIR/emails/postfix_$DATE.tar.gz | cut -f1))
- dovecot_$DATE.tar.gz ($(du -h $BACKUP_DIR/emails/dovecot_$DATE.tar.gz | cut -f1))

应用程序:
- application_$DATE.tar.gz ($(du -h $BACKUP_DIR/application_$DATE.tar.gz | cut -f1))

配置文件:
- system_config_$DATE.tar.gz ($(du -h $BACKUP_DIR/config/system_config_$DATE.tar.gz | cut -f1))
- passwd_$DATE, group_$DATE

总大小: $(du -sh $BACKUP_DIR | cut -f1)
EOF

echo "数据导出完成！"
cat "$BACKUP_DIR/migration_manifest.txt"
```

#### 步骤2：数据传输到新服务器
```bash
#!/bin/bash
# 数据传输脚本 - transfer_data.sh

SOURCE_SERVER="old.server.com"
DEST_SERVER="new.server.com"
BACKUP_DIR="/tmp/migration"

echo "开始数据传输..."

# 方式1：使用rsync (推荐)
rsync -avz --progress --compress \
    root@$SOURCE_SERVER:$BACKUP_DIR/ \
    $BACKUP_DIR/

# 方式2：使用scp (适用于小文件)
# scp -r root@$SOURCE_SERVER:$BACKUP_DIR/* $BACKUP_DIR/

# 验证传输完整性
echo "验证传输完整性..."
ssh root@$SOURCE_SERVER "cd $BACKUP_DIR && find . -type f -exec md5sum {} \;" > /tmp/source_checksums.txt
cd $BACKUP_DIR && find . -type f -exec md5sum {} \; > /tmp/dest_checksums.txt

if diff /tmp/source_checksums.txt /tmp/dest_checksums.txt; then
    echo "✓ 数据传输完整性验证通过"
else
    echo "✗ 数据传输完整性验证失败，请重新传输"
    exit 1
fi
```

#### 步骤3：新服务器环境配置
```bash
#!/bin/bash
# 新服务器环境配置 - setup_environment.sh

echo "配置新服务器环境..."

# 1. 创建用户和组
echo "创建系统用户..."
useradd -m -s /bin/bash email
useradd -r -s /bin/false postfix
useradd -r -s /bin/false dovecot

# 2. 创建目录结构
echo "创建目录结构..."
mkdir -p /mail/vhosts
mkdir -p /opt/email-system
mkdir -p /var/log/email-system
chown email:email /opt/email-system /var/log/email-system
chown email:email /mail/vhosts

# 3. 安装SSL证书
echo "配置SSL证书..."
mkdir -p /etc/ssl/email-system
# 如果使用Let's Encrypt
# certbot certonly --standalone -d mail.yourdomain.com

# 4. 配置防火墙
echo "配置防火墙..."
ufw allow 22/tcp
ufw allow 25/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 587/tcp
ufw allow 993/tcp
ufw allow 995/tcp
ufw --force enable

echo "环境配置完成"
```

#### 步骤4：数据导入和恢复
```bash
#!/bin/bash
# 数据导入脚本 - import_data.sh

BACKUP_DIR="/tmp/migration"
MYSQL_USER="root"
MYSQL_PASS="your_new_password"
DATE=$(ls $BACKUP_DIR/database/ | grep email_system | head -1 | sed 's/email_system_\(.*\)\.sql/\1/')

echo "开始数据导入 - $DATE..."

# 1. 恢复数据库
echo "恢复数据库..."
mysql -u$MYSQL_USER -p$MYSQL_PASS -e "CREATE DATABASE IF NOT EXISTS email_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u$MYSQL_USER -p$MYSQL_PASS email_system < "$BACKUP_DIR/database/email_system_$DATE.sql"

# 验证数据库导入
USER_COUNT=$(mysql -u$MYSQL_USER -p$MYSQL_PASS -se "SELECT COUNT(*) FROM email_system.users;")
EMAIL_COUNT=$(mysql -u$MYSQL_USER -p$MYSQL_PASS -se "SELECT COUNT(*) FROM email_system.emails;")
echo "✓ 数据库导入完成 - 用户数: $USER_COUNT, 邮件数: $EMAIL_COUNT"

# 2. 恢复邮件文件
echo "恢复邮件文件..."
tar -xzf "$BACKUP_DIR/emails/mailboxes_$DATE.tar.gz" -C /
tar -xzf "$BACKUP_DIR/emails/postfix_$DATE.tar.gz" -C /
tar -xzf "$BACKUP_DIR/emails/dovecot_$DATE.tar.gz" -C /

# 修复邮件文件权限
chown -R email:email /mail/vhosts/
find /mail/vhosts/ -type d -exec chmod 755 {} \;
find /mail/vhosts/ -type f -exec chmod 644 {} \;

# 3. 恢复应用程序
echo "恢复应用程序..."
tar -xzf "$BACKUP_DIR/application_$DATE.tar.gz" -C /
chown -R email:email /opt/email-system/
chmod +x /opt/email-system/bin/*

# 4. 恢复配置文件
echo "恢复配置文件..."
tar -xzf "$BACKUP_DIR/config/system_config_$DATE.tar.gz" -C /

# 修改配置文件中的服务器相关信息
sed -i "s/old.server.com/new.server.com/g" /etc/nginx/sites-available/*
sed -i "s/old.server.com/new.server.com/g" /etc/postfix/main.cf

echo "数据导入完成！"
```

### 方案二：在线迁移 (最小停机时间)

#### 步骤1：实时同步配置
```bash
#!/bin/bash
# 实时同步脚本 - sync_realtime.sh

SOURCE_SERVER="old.server.com"
DEST_SERVER="new.server.com"

# 持续同步邮件数据
while true; do
    rsync -avz --delete --exclude="*.lock" \
        root@$SOURCE_SERVER:/mail/vhosts/ \
        /mail/vhosts/
    
    sleep 300  # 每5分钟同步一次
done &

echo "实时同步已启动"
```

#### 步骤2：切换前最后同步
```bash
#!/bin/bash
# 最后同步脚本 - final_sync.sh

echo "执行最后同步..."

# 停止源服务器邮件服务
ssh root@$SOURCE_SERVER "systemctl stop postfix dovecot"

# 最后一次同步
rsync -avz --delete root@$SOURCE_SERVER:/mail/vhosts/ /mail/vhosts/

# 同步数据库
mysqldump -h $SOURCE_SERVER -u root -p email_system | mysql email_system

echo "最后同步完成，可以切换DNS"
```

## 🔧 特殊数据迁移

### Postfix配置迁移
```bash
# Postfix主配置文件适配
cp /etc/postfix/main.cf /etc/postfix/main.cf.backup

# 更新服务器相关配置
postconf -e "myhostname = mail.new-domain.com"
postconf -e "mydomain = new-domain.com"
postconf -e "myorigin = new-domain.com"

# 重新生成别名数据库
newaliases
postmap /etc/postfix/virtual
systemctl restart postfix
```

### Dovecot配置迁移
```bash
# 更新Dovecot配置
sed -i 's/old-domain\.com/new-domain.com/g' /etc/dovecot/conf.d/*.conf

# 重建用户数据库
doveadm auth cache flush
systemctl restart dovecot
```

### SSL证书迁移
```bash
# 如果使用Let's Encrypt
certbot certonly --standalone -d mail.new-domain.com -d webmail.new-domain.com

# 如果使用自有证书
cp /path/to/certificate.crt /etc/ssl/certs/
cp /path/to/private.key /etc/ssl/private/
chmod 644 /etc/ssl/certs/certificate.crt
chmod 600 /etc/ssl/private/private.key
```

## 🧪 迁移测试和验证

### 功能测试脚本
```bash
#!/bin/bash
# 迁移后功能测试 - test_migration.sh

echo "开始迁移后功能测试..."

# 1. 数据库连接测试
echo "测试数据库连接..."
mysql -u emailapp -p -e "SELECT COUNT(*) FROM users;" email_system
if [ $? -eq 0 ]; then
    echo "✓ 数据库连接正常"
else
    echo "✗ 数据库连接失败"
fi

# 2. 邮件服务测试
echo "测试邮件服务..."
systemctl is-active postfix dovecot nginx
if [ $? -eq 0 ]; then
    echo "✓ 邮件服务运行正常"
else
    echo "✗ 邮件服务异常"
fi

# 3. SMTP测试
echo "测试SMTP发送..."
echo "Test email" | mail -s "Migration Test" test@yourdomain.com
if [ $? -eq 0 ]; then
    echo "✓ SMTP发送测试通过"
else
    echo "✗ SMTP发送测试失败"
fi

# 4. IMAP测试
echo "测试IMAP连接..."
openssl s_client -connect localhost:993 -quiet <<EOF
a1 LOGIN testuser@yourdomain.com password
a2 LIST "" "*"
a3 LOGOUT
EOF

# 5. Web界面测试
echo "测试Web界面..."
curl -s -o /dev/null -w "%{http_code}" https://webmail.yourdomain.com/
if [ $? -eq 200 ]; then
    echo "✓ Web界面访问正常"
else
    echo "✗ Web界面访问异常"
fi

echo "功能测试完成！"
```

## 🔍 故障排查

### 常见问题及解决方案

#### 数据库连接问题
```bash
# 检查MySQL服务状态
systemctl status mysql

# 检查用户权限
mysql -e "SHOW GRANTS FOR 'emailapp'@'localhost';"

# 重置用户密码
mysql -e "ALTER USER 'emailapp'@'localhost' IDENTIFIED BY 'new_password';"
```

#### 邮件服务问题
```bash
# 检查Postfix日志
tail -f /var/log/mail.log

# 测试Postfix配置
postfix check

# 检查Dovecot配置
doveconf -n
```

#### 权限问题
```bash
# 修复邮件目录权限
chown -R email:email /mail/vhosts/
find /mail/vhosts/ -type d -exec chmod 755 {} \;
find /mail/vhosts/ -type f -exec chmod 644 {} \;

# 修复应用程序权限
chown -R email:email /opt/email-system/
chmod +x /opt/email-system/bin/*
```

## 📊 迁移性能优化

### 大数据量迁移优化
```bash
# 使用并行传输
parallel -j4 rsync -avz {} new-server:/mail/vhosts/ ::: /mail/vhosts/*/

# 压缩传输
rsync -avz --compress-level=9 source/ destination/

# 增量备份
rsync -avz --link-dest=/previous/backup/ source/ new-backup/
```

### 网络传输优化
```bash
# 调整TCP窗口大小
echo 'net.core.wmem_max = 134217728' >> /etc/sysctl.conf
echo 'net.core.rmem_max = 134217728' >> /etc/sysctl.conf
sysctl -p
```

## 📋 迁移后检查清单

```
□ 数据库数据完整性验证
  □ 用户数据检查
  □ 邮件数据检查  
  □ 配置数据检查

□ 邮件服务功能验证
  □ SMTP发送测试
  □ IMAP接收测试
  □ Web界面访问测试
  □ 用户登录测试

□ 系统配置验证
  □ DNS解析检查
  □ SSL证书检查
  □ 防火墙规则检查
  □ 系统服务状态检查

□ 性能和监控
  □ 系统负载检查
  □ 内存使用检查
  □ 磁盘空间检查
  □ 日志轮转配置

□ 安全验证
  □ 访问控制测试
  □ 端口扫描检查
  □ 安全更新应用
  □ 备份计划验证

□ 用户通知和培训
  □ 迁移完成通知
  □ 新服务器信息更新
  □ 用户使用培训
  □ 问题反馈收集
```

## 🔄 回滚计划

### 紧急回滚步骤
```bash
#!/bin/bash
# 紧急回滚脚本 - emergency_rollback.sh

echo "执行紧急回滚..."

# 1. 停止新服务器服务
systemctl stop nginx postfix dovecot mysql

# 2. 恢复DNS指向
# 手动操作：将DNS A记录指向原服务器

# 3. 重启原服务器服务
ssh root@old-server "systemctl start nginx postfix dovecot mysql"

# 4. 验证原服务器状态
ssh root@old-server "systemctl status nginx postfix dovecot mysql"

echo "回滚完成，请验证服务状态"
```

---

**⚠️ 重要提醒:**
1. **测试环境验证** - 在生产迁移前先在测试环境完整演练
2. **备份策略** - 迁移前确保有完整的数据备份
3. **停机计划** - 制定详细的停机维护计划并提前通知用户
4. **回滚准备** - 准备完整的回滚方案以备不测
5. **分步验证** - 每个步骤完成后都要验证功能正常再进行下一步