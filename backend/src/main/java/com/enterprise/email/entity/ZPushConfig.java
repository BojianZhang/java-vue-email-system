package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Z-Push ActiveSync配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("zpush_configs")
public class ZPushConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * Z-Push安装路径
     */
    @TableField("install_path")
    private String installPath;

    /**
     * ActiveSync服务URL
     */
    @TableField("activesync_url")
    private String activesyncUrl;

    /**
     * 后端类型 (IMAP, Zimbra, etc.)
     */
    @TableField("backend_type")
    private String backendType;

    /**
     * IMAP服务器配置
     */
    @TableField("imap_server")
    private String imapServer;

    @TableField("imap_port")
    private Integer imapPort;

    @TableField("imap_ssl")
    private Boolean imapSsl;

    /**
     * SMTP服务器配置
     */
    @TableField("smtp_server")
    private String smtpServer;

    @TableField("smtp_port")
    private Integer smtpPort;

    @TableField("smtp_ssl")
    private Boolean smtpSsl;

    /**
     * 同步设置
     */
    @TableField("sync_interval")
    private Integer syncInterval;

    @TableField("max_sync_items")
    private Integer maxSyncItems;

    @TableField("sync_folders")
    private String syncFolders;

    /**
     * 推送设置
     */
    @TableField("push_enabled")
    private Boolean pushEnabled;

    @TableField("push_lifetime")
    private Integer pushLifetime;

    @TableField("heartbeat_interval")
    private Integer heartbeatInterval;

    /**
     * 日历和联系人同步
     */
    @TableField("calendar_enabled")
    private Boolean calendarEnabled;

    @TableField("contacts_enabled")
    private Boolean contactsEnabled;

    @TableField("tasks_enabled")
    private Boolean tasksEnabled;

    /**
     * 安全设置
     */
    @TableField("device_password_enabled")
    private Boolean devicePasswordEnabled;

    @TableField("password_min_length")
    private Integer passwordMinLength;

    @TableField("auto_provision")
    private Boolean autoProvision;

    /**
     * 日志设置
     */
    @TableField("log_level")
    private String logLevel;

    @TableField("log_file")
    private String logFile;

    /**
     * 自定义配置JSON
     */
    @TableField("custom_config")
    private String customConfig;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 状态 (ACTIVE, INACTIVE, ERROR)
     */
    @TableField("status")
    private String status;

    /**
     * 版本信息
     */
    @TableField("version")
    private String version;

    /**
     * 最后同步时间
     */
    @TableField("last_sync_at")
    private LocalDateTime lastSyncAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}