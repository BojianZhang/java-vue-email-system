-- SSL证书管理表
CREATE TABLE ssl_certificates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    domain VARCHAR(255) NOT NULL COMMENT '域名',
    cert_type VARCHAR(50) NOT NULL COMMENT '证书类型: LETS_ENCRYPT, UPLOADED, SELF_SIGNED',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '证书状态: ACTIVE, EXPIRED, PENDING, FAILED, REVOKED',
    cert_path VARCHAR(500) COMMENT '证书文件路径',
    key_path VARCHAR(500) COMMENT '私钥文件路径',
    chain_path VARCHAR(500) COMMENT '证书链文件路径',
    issued_at DATETIME COMMENT '证书颁发时间',
    expires_at DATETIME COMMENT '证书过期时间',
    auto_renew BOOLEAN DEFAULT FALSE COMMENT '是否启用自动续期',
    last_renewal DATETIME COMMENT '最后续期时间',
    renewal_failures INT DEFAULT 0 COMMENT '续期失败次数',
    challenge_type VARCHAR(50) COMMENT 'Let\'s Encrypt 挑战类型: HTTP01, DNS01',
    fingerprint VARCHAR(128) COMMENT '证书指纹',
    cert_info JSON COMMENT '证书详细信息(JSON格式)',
    email VARCHAR(255) COMMENT '邮箱地址(用于Let\'s Encrypt注册)',
    applied BOOLEAN DEFAULT FALSE COMMENT '是否应用到服务',
    applied_at DATETIME COMMENT '应用时间',
    error_message TEXT COMMENT '错误信息',
    notes TEXT COMMENT '备注',
    created_by BIGINT COMMENT '创建者ID',
    updated_by BIGINT COMMENT '更新者ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    
    -- 索引
    INDEX idx_domain (domain),
    INDEX idx_cert_type (cert_type),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at),
    INDEX idx_auto_renew (auto_renew),
    INDEX idx_applied (applied),
    INDEX idx_created_by (created_by),
    INDEX idx_create_time (create_time),
    
    -- 唯一约束
    UNIQUE KEY uk_domain_active (domain, status) COMMENT '同一域名只能有一个ACTIVE状态的证书'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSL证书管理表';

-- SSL证书事件日志表
CREATE TABLE ssl_certificate_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    certificate_id BIGINT NOT NULL COMMENT '证书ID',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型: CREATE, RENEW, APPLY, REVOKE, DELETE, CHECK',
    event_status VARCHAR(50) NOT NULL COMMENT '事件状态: SUCCESS, FAILED, PENDING',
    event_message TEXT COMMENT '事件消息',
    event_details JSON COMMENT '事件详细信息',
    duration_ms INT COMMENT '事件耗时(毫秒)',
    user_id BIGINT COMMENT '操作用户ID',
    user_ip VARCHAR(45) COMMENT '操作用户IP',
    user_agent VARCHAR(500) COMMENT '用户代理',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 索引
    INDEX idx_certificate_id (certificate_id),
    INDEX idx_event_type (event_type),
    INDEX idx_event_status (event_status),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time),
    
    -- 外键约束
    FOREIGN KEY (certificate_id) REFERENCES ssl_certificates(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSL证书事件日志表';

-- SSL证书续期调度表
CREATE TABLE ssl_certificate_renewal_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    certificate_id BIGINT NOT NULL COMMENT '证书ID',
    scheduled_time DATETIME NOT NULL COMMENT '预定续期时间',
    actual_time DATETIME COMMENT '实际续期时间',
    renewal_status VARCHAR(50) DEFAULT 'SCHEDULED' COMMENT '续期状态: SCHEDULED, RUNNING, SUCCESS, FAILED, SKIPPED',
    failure_reason TEXT COMMENT '失败原因',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME COMMENT '下次重试时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_certificate_id (certificate_id),
    INDEX idx_scheduled_time (scheduled_time),
    INDEX idx_renewal_status (renewal_status),
    INDEX idx_next_retry_time (next_retry_time),
    
    -- 外键约束
    FOREIGN KEY (certificate_id) REFERENCES ssl_certificates(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSL证书续期调度表';

-- SSL配置模板表
CREATE TABLE ssl_certificate_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(50) NOT NULL COMMENT '模板类型: NGINX, APACHE, TOMCAT',
    template_content TEXT NOT NULL COMMENT '模板内容',
    template_version VARCHAR(20) DEFAULT '1.0' COMMENT '模板版本',
    description TEXT COMMENT '模板描述',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否默认模板',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_by BIGINT COMMENT '创建者ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_template_type (template_type),
    INDEX idx_template_name (template_name),
    INDEX idx_is_default (is_default),
    INDEX idx_is_active (is_active),
    
    -- 唯一约束
    UNIQUE KEY uk_template_name (template_name, template_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSL配置模板表';

-- 插入默认的Nginx SSL配置模板
INSERT INTO ssl_certificate_templates (template_name, template_type, template_content, description, is_default) VALUES
('Default Nginx SSL', 'NGINX', 
'# SSL configuration for {{domain}}
server {
    listen 443 ssl http2;
    server_name {{domain}};

    ssl_certificate {{cert_path}};
    ssl_certificate_key {{key_path}};
    {{#chain_path}}ssl_trusted_certificate {{chain_path}};{{/chain_path}}

    # SSL Security Settings
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-SHA256:ECDHE-RSA-AES256-SHA384;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;

    # Your application configuration here
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# HTTP to HTTPS redirect
server {
    listen 80;
    server_name {{domain}};
    return 301 https://$server_name$request_uri;
}', 
'默认的Nginx SSL配置模板', 
TRUE),

('Nginx SSL with OCSP', 'NGINX',
'# SSL configuration for {{domain}} with OCSP stapling
server {
    listen 443 ssl http2;
    server_name {{domain}};

    ssl_certificate {{cert_path}};
    ssl_certificate_key {{key_path}};
    {{#chain_path}}ssl_trusted_certificate {{chain_path}};{{/chain_path}}

    # SSL Security Settings
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 8.8.8.8 8.8.4.4 valid=300s;
    resolver_timeout 5s;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Your application configuration here
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# HTTP to HTTPS redirect
server {
    listen 80;
    server_name {{domain}};
    return 301 https://$server_name$request_uri;
}',
'带OCSP装订的高级Nginx SSL配置模板',
FALSE);

-- SSL证书域名验证记录表
CREATE TABLE ssl_certificate_domain_validations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    certificate_id BIGINT NOT NULL COMMENT '证书ID',
    domain VARCHAR(255) NOT NULL COMMENT '域名',
    challenge_type VARCHAR(50) NOT NULL COMMENT '验证类型: HTTP01, DNS01',
    challenge_token VARCHAR(255) COMMENT '验证令牌',
    challenge_content TEXT COMMENT '验证内容',
    challenge_url VARCHAR(500) COMMENT '验证URL',
    validation_status VARCHAR(50) DEFAULT 'PENDING' COMMENT '验证状态: PENDING, VALID, INVALID, EXPIRED',
    validation_time DATETIME COMMENT '验证时间',
    expiry_time DATETIME COMMENT '过期时间',
    error_detail TEXT COMMENT '错误详情',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_certificate_id (certificate_id),
    INDEX idx_domain (domain),
    INDEX idx_validation_status (validation_status),
    INDEX idx_expiry_time (expiry_time),
    
    -- 外键约束
    FOREIGN KEY (certificate_id) REFERENCES ssl_certificates(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSL证书域名验证记录表';

-- 系统配置表中添加SSL相关配置
INSERT INTO system_settings (setting_key, setting_value, description, category) VALUES
('ssl.certs.directory', '/opt/ssl/certs', 'SSL证书存储目录', 'SSL'),
('ssl.nginx.config', '/etc/nginx/conf.d', 'Nginx SSL配置目录', 'SSL'),
('ssl.acme.directory', '/opt/ssl/acme', 'ACME客户端工作目录', 'SSL'),
('ssl.backup.directory', '/opt/ssl/backups', 'SSL备份目录', 'SSL'),
('ssl.lets-encrypt.staging', 'false', '是否使用Let\'s Encrypt测试环境', 'SSL'),
('ssl.lets-encrypt.email', 'admin@example.com', 'Let\'s Encrypt注册邮箱', 'SSL'),
('ssl.auto-renewal.enabled', 'true', '是否启用自动续期', 'SSL'),
('ssl.auto-renewal.days-before', '30', '提前多少天开始续期', 'SSL'),
('ssl.max-renewal-failures', '5', '最大续期失败次数', 'SSL'),
('ssl.notification.enabled', 'true', '是否启用证书通知', 'SSL'),
('ssl.notification.email', 'admin@example.com', '证书通知邮箱', 'SSL');