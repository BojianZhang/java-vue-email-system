package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * SMTP服务配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("smtp_configs")
public class SmtpConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * SMTP服务器地址
     */
    @TableField("smtp_host")
    private String smtpHost;

    /**
     * SMTP端口 (25, 465, 587)
     */
    @TableField("smtp_port")
    private Integer smtpPort;

    /**
     * 是否启用SSL/TLS
     */
    @TableField("use_ssl")
    private Boolean useSsl;

    /**
     * 是否启用STARTTLS
     */
    @TableField("use_starttls")
    private Boolean useStarttls;

    /**
     * 最大连接数
     */
    @TableField("max_connections")
    private Integer maxConnections;

    /**
     * 连接超时时间(秒)
     */
    @TableField("connection_timeout")
    private Integer connectionTimeout;

    /**
     * 认证类型 (PLAIN, LOGIN, CRAM-MD5)
     */
    @TableField("auth_type")
    private String authType;

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
     * 配置信息JSON
     */
    @TableField("config_json")
    private String configJson;

    /**
     * 最后测试时间
     */
    @TableField("last_test_at")
    private LocalDateTime lastTestAt;

    /**
     * 最后测试结果
     */
    @TableField("last_test_result")
    private String lastTestResult;

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