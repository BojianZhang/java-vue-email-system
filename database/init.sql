-- MySQL数据库初始化脚本
-- 企业邮件系统数据库结构

-- 创建数据库
CREATE DATABASE IF NOT EXISTS enterprise_email DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE enterprise_email;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱地址',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    display_name VARCHAR(100) NULL COMMENT '显示名称',
    role VARCHAR(20) DEFAULT 'user' COMMENT '用户角色',
    is_active BOOLEAN DEFAULT TRUE COMMENT '账户状态',
    email_verified BOOLEAN DEFAULT FALSE COMMENT '邮箱验证状态',
    last_login TIMESTAMP NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(45) NULL COMMENT '最后登录IP',
    login_count INT DEFAULT 0 COMMENT '登录次数',
    storage_quota BIGINT DEFAULT 1024 COMMENT '邮箱存储配额（MB）',
    storage_used BIGINT DEFAULT 0 COMMENT '已使用存储空间（MB）',
    avatar_url VARCHAR(500) NULL COMMENT '用户头像URL',
    timezone VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '时区',
    language VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言',
    notes TEXT NULL COMMENT '备注信息',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除标志',
    version INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 域名表
CREATE TABLE IF NOT EXISTS domains (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    domain_name VARCHAR(255) NOT NULL UNIQUE COMMENT '域名',
    description VARCHAR(500) NULL COMMENT '域名描述',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否为默认域名',
    mx_record VARCHAR(255) NULL COMMENT 'MX记录',
    smtp_host VARCHAR(255) NULL COMMENT 'SMTP服务器',
    smtp_port INT DEFAULT 587 COMMENT 'SMTP端口',
    smtp_ssl BOOLEAN DEFAULT TRUE COMMENT 'SMTP是否启用SSL',
    imap_host VARCHAR(255) NULL COMMENT 'IMAP服务器',
    imap_port INT DEFAULT 993 COMMENT 'IMAP端口',
    imap_ssl BOOLEAN DEFAULT TRUE COMMENT 'IMAP是否启用SSL',
    created_by BIGINT NULL COMMENT '创建者ID',
    updated_by BIGINT NULL COMMENT '更新者ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除标志',
    version INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    
    INDEX idx_domain_name (domain_name),
    INDEX idx_is_active (is_active),
    INDEX idx_is_default (is_default),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='域名表';

-- 用户别名表
CREATE TABLE IF NOT EXISTS user_aliases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    domain_id BIGINT NOT NULL COMMENT '域名ID',
    alias_address VARCHAR(255) NOT NULL UNIQUE COMMENT '别名地址',
    alias_name VARCHAR(100) NULL COMMENT '别名名称/描述',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否为默认别名',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_by BIGINT NULL COMMENT '创建者ID',
    updated_by BIGINT NULL COMMENT '更新者ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除标志',
    version INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    
    INDEX idx_user_id (user_id),
    INDEX idx_domain_id (domain_id),
    INDEX idx_alias_address (alias_address),
    INDEX idx_is_active (is_active),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (domain_id) REFERENCES domains(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户别名表';

-- 邮件表
CREATE TABLE IF NOT EXISTS emails (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    alias_id BIGINT NULL COMMENT '别名ID（如果通过别名收发）',
    message_uid VARCHAR(255) NULL COMMENT '邮件UID（IMAP服务器中的唯一标识）',
    message_id VARCHAR(255) NULL COMMENT '邮件ID（Message-ID头）',
    sender VARCHAR(255) NOT NULL COMMENT '发件人',
    recipient VARCHAR(255) NOT NULL COMMENT '收件人',
    cc TEXT NULL COMMENT '抄送',
    bcc TEXT NULL COMMENT '密送',
    subject VARCHAR(500) NULL COMMENT '邮件主题',
    content_text LONGTEXT NULL COMMENT '邮件内容（文本）',
    content_html LONGTEXT NULL COMMENT '邮件内容（HTML）',
    size_bytes BIGINT DEFAULT 0 COMMENT '邮件大小（字节）',
    email_type VARCHAR(20) DEFAULT 'inbox' COMMENT '邮件类型（inbox/sent/draft/trash）',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    is_important BOOLEAN DEFAULT FALSE COMMENT '是否重要',
    has_attachment BOOLEAN DEFAULT FALSE COMMENT '是否有附件',
    attachment_count INT DEFAULT 0 COMMENT '附件数量',
    sent_time TIMESTAMP NULL COMMENT '邮件发送时间',
    received_time TIMESTAMP NULL COMMENT '邮件接收时间',
    headers LONGTEXT NULL COMMENT '原始邮件头信息',
    file_path VARCHAR(1000) NULL COMMENT '邮件文件路径',
    status VARCHAR(20) DEFAULT 'new' COMMENT '邮件状态',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除标志',
    version INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    
    INDEX idx_user_id (user_id),
    INDEX idx_alias_id (alias_id),
    INDEX idx_sender (sender),
    INDEX idx_recipient (recipient),
    INDEX idx_email_type (email_type),
    INDEX idx_sent_time (sent_time),
    INDEX idx_received_time (received_time),
    INDEX idx_is_read (is_read),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (alias_id) REFERENCES user_aliases(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件表';

-- 用户登录日志表
CREATE TABLE IF NOT EXISTS user_login_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_token_hash VARCHAR(255) NULL COMMENT '会话令牌哈希',
    ip_address VARCHAR(45) NOT NULL COMMENT 'IP地址',
    user_agent TEXT NULL COMMENT 'User-Agent',
    device_type VARCHAR(50) NULL COMMENT '设备类型',
    os VARCHAR(100) NULL COMMENT '操作系统',
    browser VARCHAR(100) NULL COMMENT '浏览器',
    device_fingerprint VARCHAR(255) NULL COMMENT '设备指纹',
    country VARCHAR(100) NULL COMMENT '国家',
    region VARCHAR(100) NULL COMMENT '地区',
    city VARCHAR(100) NULL COMMENT '城市',
    latitude DOUBLE NULL COMMENT '纬度',
    longitude DOUBLE NULL COMMENT '经度',
    isp VARCHAR(255) NULL COMMENT 'ISP',
    risk_score INT DEFAULT 0 COMMENT '风险分数',
    is_suspicious BOOLEAN DEFAULT FALSE COMMENT '是否可疑',
    suspicious_reasons TEXT NULL COMMENT '可疑原因',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否活跃',
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    logout_time TIMESTAMP NULL COMMENT '登出时间',
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除标志',
    version INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    
    INDEX idx_user_id (user_id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_login_time (login_time),
    INDEX idx_is_suspicious (is_suspicious),
    INDEX idx_is_active (is_active),
    INDEX idx_device_fingerprint (device_fingerprint),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';

-- 安全警报表
CREATE TABLE IF NOT EXISTS security_alerts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    alert_type VARCHAR(50) NOT NULL COMMENT '警报类型',
    severity VARCHAR(20) NOT NULL COMMENT '严重程度',
    title VARCHAR(255) NOT NULL COMMENT '警报标题',
    description TEXT NULL COMMENT '警报描述',
    alert_data JSON NULL COMMENT '警报详细数据（JSON格式）',
    ip_address VARCHAR(45) NULL COMMENT 'IP地址',
    user_agent TEXT NULL COMMENT 'User-Agent',
    location VARCHAR(255) NULL COMMENT '地理位置',
    is_resolved BOOLEAN DEFAULT FALSE COMMENT '是否已解决',
    resolved_at TIMESTAMP NULL COMMENT '解决时间',
    resolved_by BIGINT NULL COMMENT '解决人ID',
    resolution_notes TEXT NULL COMMENT '解决备注',
    is_notified BOOLEAN DEFAULT FALSE COMMENT '是否已通知',
    notified_at TIMESTAMP NULL COMMENT '通知时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除标志',
    version INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    
    INDEX idx_user_id (user_id),
    INDEX idx_alert_type (alert_type),
    INDEX idx_severity (severity),
    INDEX idx_is_resolved (is_resolved),
    INDEX idx_create_time (create_time),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全警报表';

-- 用户可信设备表
CREATE TABLE IF NOT EXISTS user_trusted_devices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_fingerprint VARCHAR(255) NOT NULL COMMENT '设备指纹',
    device_name VARCHAR(100) NULL COMMENT '设备名称',
    device_type VARCHAR(50) NULL COMMENT '设备类型',
    os VARCHAR(100) NULL COMMENT '操作系统',
    browser VARCHAR(100) NULL COMMENT '浏览器',
    ip_address VARCHAR(45) NULL COMMENT 'IP地址',
    location VARCHAR(255) NULL COMMENT '地理位置',
    is_trusted BOOLEAN DEFAULT TRUE COMMENT '是否信任',
    trusted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '信任时间',
    last_used TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最后使用时间',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除标志',
    version INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    
    INDEX idx_user_id (user_id),
    INDEX idx_device_fingerprint (device_fingerprint),
    INDEX idx_is_trusted (is_trusted),
    INDEX idx_expires_at (expires_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户可信设备表';

-- 安全配置表
CREATE TABLE IF NOT EXISTS security_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    setting_value TEXT NOT NULL COMMENT '配置值',
    setting_type VARCHAR(20) DEFAULT 'string' COMMENT '配置类型',
    description TEXT NULL COMMENT '配置描述',
    category VARCHAR(50) DEFAULT 'general' COMMENT '配置分类',
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否为系统配置',
    updated_by BIGINT NULL COMMENT '更新者ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除标志',
    version INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    
    INDEX idx_setting_key (setting_key),
    INDEX idx_category (category),
    INDEX idx_is_system (is_system),
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全配置表';

-- 插入默认数据

-- 插入默认管理员用户
INSERT INTO users (username, email, password, display_name, role, is_active, email_verified, storage_quota) VALUES 
('admin', 'admin@system.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLIIpLbabmu6', '系统管理员', 'admin', TRUE, TRUE, 10240),
('demo', 'demo@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLIIpLbabmu6', '演示用户', 'user', TRUE, TRUE, 1024);
-- 密码: admin123

-- 插入默认域名
INSERT INTO domains (domain_name, description, is_active, is_default, smtp_host, smtp_port, smtp_ssl, imap_host, imap_port, imap_ssl) VALUES 
('system.com', '系统默认域名', TRUE, TRUE, 'smtp.system.com', 587, TRUE, 'imap.system.com', 993, TRUE),
('example.com', '演示域名', TRUE, FALSE, 'smtp.example.com', 587, TRUE, 'imap.example.com', 993, TRUE);

-- 插入默认别名
INSERT INTO user_aliases (user_id, domain_id, alias_address, alias_name, is_default, is_active) VALUES 
(1, 1, 'admin@system.com', '管理员邮箱', TRUE, TRUE),
(2, 2, 'demo@example.com', '演示邮箱', TRUE, TRUE);

-- 插入默认安全配置
INSERT INTO security_settings (setting_key, setting_value, setting_type, description, category, is_system) VALUES
-- 登录监控配置
('login_monitoring_enabled', 'true', 'boolean', '启用登录监控功能', 'login_monitoring', FALSE),
('login_logs_retention_days', '90', 'integer', '登录日志保留天数', 'login_monitoring', FALSE),
('geo_cache_hours', '24', 'integer', 'IP地理位置缓存时间（小时）', 'login_monitoring', FALSE),

-- 异常检测配置
('geo_anomaly_detection_enabled', 'true', 'boolean', '启用地理位置异常检测', 'anomaly_detection', FALSE),
('geo_anomaly_distance_km', '500', 'integer', '地理异常距离阈值（公里）', 'anomaly_detection', FALSE),
('time_anomaly_window_hours', '6', 'integer', '时间异常检测窗口（小时）', 'anomaly_detection', FALSE),
('login_frequency_limit', '10', 'integer', '登录频率限制（次数）', 'anomaly_detection', FALSE),
('login_frequency_window_minutes', '30', 'integer', '登录频率检测窗口（分钟）', 'anomaly_detection', FALSE),
('max_concurrent_sessions', '5', 'integer', '最大并发会话数', 'anomaly_detection', FALSE),

-- 风险评分配置
('base_risk_score', '10', 'integer', '基础风险分数', 'risk_scoring', FALSE),
('geo_anomaly_risk_score', '25', 'integer', '地理异常风险分数', 'risk_scoring', FALSE),
('new_device_risk_score', '15', 'integer', '新设备风险分数', 'risk_scoring', FALSE),
('suspicious_ip_risk_score', '30', 'integer', '可疑IP风险分数', 'risk_scoring', FALSE),
('high_risk_threshold', '70', 'integer', '高风险阈值', 'risk_scoring', FALSE),

-- 通知配置
('email_notifications_enabled', 'true', 'boolean', '启用邮件通知', 'notifications', FALSE),
('admin_emails', 'admin@system.com', 'string', '管理员邮箱列表（一行一个）', 'notifications', FALSE),
('notification_severity_level', 'medium', 'string', '通知严重程度级别', 'notifications', FALSE),
('notification_rate_limit', '5', 'integer', '通知频率限制（封数）', 'notifications', FALSE),
('notification_rate_window_minutes', '60', 'integer', '通知频率窗口（分钟）', 'notifications', FALSE),

-- 会话管理配置
('session_timeout_hours', '24', 'integer', '会话超时时间（小时）', 'session_management', FALSE),
('remember_me_days', '30', 'integer', '记住登录时长（天）', 'session_management', FALSE),
('force_single_session', 'false', 'boolean', '强制单点登录', 'session_management', FALSE),
('trusted_device_days', '90', 'integer', '可信设备有效期（天）', 'session_management', FALSE);

-- 外部别名同步配置表
CREATE TABLE IF NOT EXISTS external_alias_sync (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alias_id BIGINT NOT NULL COMMENT '用户别名ID',
    platform_type VARCHAR(50) NOT NULL COMMENT '外部平台类型 (POSTE_IO, MAIL_COW, ZIMBRA, EXCHANGE, CUSTOM)',
    platform_url VARCHAR(500) NOT NULL COMMENT '外部平台服务器地址',
    api_key TEXT NULL COMMENT '外部平台API密钥或访问令牌（加密存储）',
    external_username VARCHAR(255) NULL COMMENT '外部平台用户名',
    external_password TEXT NULL COMMENT '外部平台密码（加密存储）',
    external_alias_id VARCHAR(255) NULL COMMENT '外部平台上的别名ID或标识',
    external_alias_address VARCHAR(255) NULL COMMENT '外部平台上的别名地址',
    external_alias_name VARCHAR(255) NULL COMMENT '外部平台上的别名名称（同步目标）',
    external_alias_description VARCHAR(500) NULL COMMENT '外部平台上的别名描述',
    auto_sync_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用自动同步',
    sync_frequency_minutes INT DEFAULT 60 COMMENT '同步频率（分钟）',
    last_sync_time TIMESTAMP NULL COMMENT '最后同步时间',
    last_sync_status ENUM('SUCCESS', 'FAILED', 'PENDING') DEFAULT 'PENDING' COMMENT '最后同步状态',
    last_sync_error TEXT NULL COMMENT '最后同步错误信息',
    retry_count INT DEFAULT 0 COMMENT '同步重试次数',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL COMMENT '创建者ID',
    updated_by BIGINT NULL COMMENT '更新者ID',
    
    UNIQUE KEY uk_alias_platform (alias_id, platform_type),
    FOREIGN KEY (alias_id) REFERENCES user_aliases(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_platform_type (platform_type),
    INDEX idx_sync_status (last_sync_status),
    INDEX idx_sync_time (last_sync_time),
    INDEX idx_auto_sync (auto_sync_enabled),
    INDEX idx_active_sync (is_active, auto_sync_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部别名同步配置表';

-- 插入示例外部别名同步配置
INSERT INTO external_alias_sync (
    alias_id, 
    platform_type, 
    platform_url, 
    external_username, 
    external_alias_address,
    external_alias_name,
    external_alias_description,
    auto_sync_enabled, 
    sync_frequency_minutes,
    last_sync_status,
    is_active, 
    created_by
) VALUES
(1, 'POSTE_IO', 'https://mail.external-platform.com', 'admin', 'admin@system.com', 
 '外部平台管理员账户', '从Poste.io平台同步的管理员别名', TRUE, 30, 'SUCCESS', TRUE, 1),
(2, 'MAIL_COW', 'https://mailcow.company.com', NULL, 'demo@example.com', 
 '演示用户邮箱', '从Mailcow平台同步的演示用户别名', TRUE, 60, 'SUCCESS', TRUE, 2),
(1, 'HACKERONE', 'https://api.hackerone.com', 'admin_user', 'admin_user@wearehackerone.com',
 'Admin User', '从HackerOne平台同步的用户别名', TRUE, 120, 'PENDING', TRUE, 1);

-- 别名转发规则表
CREATE TABLE IF NOT EXISTS alias_forward_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alias_id BIGINT NOT NULL COMMENT '用户别名ID',
    rule_name VARCHAR(100) NULL COMMENT '转发规则名称',
    forward_to VARCHAR(255) NOT NULL COMMENT '转发目标地址',
    condition_type ENUM('ALL', 'SUBJECT', 'FROM', 'TO') DEFAULT 'ALL' COMMENT '转发条件类型',
    condition_value VARCHAR(500) NULL COMMENT '转发条件值',
    keep_original BOOLEAN DEFAULT TRUE COMMENT '是否保留原邮件',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    priority INT DEFAULT 1 COMMENT '转发优先级',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL COMMENT '创建者ID',
    updated_by BIGINT NULL COMMENT '更新者ID',
    
    FOREIGN KEY (alias_id) REFERENCES user_aliases(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_alias_id (alias_id),
    INDEX idx_forward_to (forward_to),
    INDEX idx_is_active (is_active),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='别名转发规则表';

-- 自动回复设置表
CREATE TABLE IF NOT EXISTS auto_reply_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alias_id BIGINT NOT NULL COMMENT '用户别名ID',
    reply_subject VARCHAR(255) NULL COMMENT '自动回复主题',
    reply_content TEXT NOT NULL COMMENT '自动回复内容',
    content_type ENUM('TEXT', 'HTML') DEFAULT 'TEXT' COMMENT '回复内容类型',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    start_time TIMESTAMP NULL COMMENT '生效开始时间',
    end_time TIMESTAMP NULL COMMENT '生效结束时间',
    reply_frequency INT DEFAULT 1 COMMENT '回复频率限制（0-无限制，1-每天一次，2-每周一次）',
    external_only BOOLEAN DEFAULT TRUE COMMENT '只对外部邮件回复',
    exclude_senders TEXT NULL COMMENT '排除发件人列表（逗号分隔）',
    include_keywords TEXT NULL COMMENT '包含主题关键词（逗号分隔）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL COMMENT '创建者ID',
    updated_by BIGINT NULL COMMENT '更新者ID',
    
    UNIQUE KEY uk_alias_id (alias_id),
    FOREIGN KEY (alias_id) REFERENCES user_aliases(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_is_active (is_active),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动回复设置表';

-- 自动回复发送历史表
CREATE TABLE IF NOT EXISTS auto_reply_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alias_id BIGINT NOT NULL COMMENT '别名ID',
    sender_email VARCHAR(255) NOT NULL COMMENT '原发件人邮箱',
    original_subject VARCHAR(255) NULL COMMENT '原邮件主题',
    reply_subject VARCHAR(255) NULL COMMENT '回复主题',
    reply_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '回复时间',
    
    FOREIGN KEY (alias_id) REFERENCES user_aliases(id) ON DELETE CASCADE,
    
    INDEX idx_alias_sender (alias_id, sender_email),
    INDEX idx_reply_time (reply_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动回复发送历史表';

-- 插入示例转发规则
INSERT INTO alias_forward_rules (alias_id, rule_name, forward_to, condition_type, condition_value, keep_original, is_active, priority, created_by) VALUES
(1, '管理员邮件转发', 'backup-admin@system.com', 'ALL', NULL, TRUE, TRUE, 1, 1),
(2, '重要邮件转发', 'important@example.com', 'SUBJECT', '重要,紧急', TRUE, TRUE, 1, 2);

-- 插入示例自动回复设置
INSERT INTO auto_reply_settings (alias_id, reply_subject, reply_content, content_type, is_active, reply_frequency, external_only, created_by) VALUES
(1, '自动回复：您的邮件已收到', '感谢您的邮件。我们已收到您的信息，将尽快回复。如有紧急事务，请致电：400-xxx-xxxx。', 'TEXT', FALSE, 1, TRUE, 1),
(2, 'Re: 自动回复', '您好！\n\n感谢您的邮件。我们已收到您的消息，会在24小时内回复。\n\n如有急事，请联系：demo@example.com\n\n此为自动回复，请勿回复。', 'TEXT', FALSE, 1, TRUE, 2);

-- 创建索引以优化查询性能
CREATE INDEX idx_emails_user_type_time ON emails(user_id, email_type, received_time DESC);
CREATE INDEX idx_login_logs_user_time ON user_login_logs(user_id, login_time DESC);
CREATE INDEX idx_security_alerts_severity_time ON security_alerts(severity, create_time DESC);

COMMIT;