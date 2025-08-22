package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Roundcube配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("roundcube_configs")
public class RoundcubeConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * Roundcube安装路径
     */
    @TableField("install_path")
    private String installPath;

    /**
     * Roundcube访问URL
     */
    @TableField("access_url")
    private String accessUrl;

    /**
     * 数据库配置
     */
    @TableField("db_host")
    private String dbHost;

    @TableField("db_port")
    private Integer dbPort;

    @TableField("db_name")
    private String dbName;

    @TableField("db_username")
    private String dbUsername;

    @TableField("db_password")
    private String dbPassword;

    /**
     * IMAP/SMTP配置
     */
    @TableField("default_host")
    private String defaultHost;

    @TableField("default_port")
    private String defaultPort;

    @TableField("smtp_server")
    private String smtpServer;

    @TableField("smtp_port")
    private Integer smtpPort;

    /**
     * 安全配置
     */
    @TableField("des_key")
    private String desKey;

    @TableField("session_lifetime")
    private Integer sessionLifetime;

    /**
     * 语言和主题
     */
    @TableField("language")
    private String language;

    @TableField("skin")
    private String skin;

    /**
     * 启用的插件列表
     */
    @TableField("enabled_plugins")
    private String enabledPlugins;

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
     * 最后更新时间
     */
    @TableField("last_update_at")
    private LocalDateTime lastUpdateAt;

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