package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * IMAP/POP3配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("imap_pop3_configs")
public class ImapPop3Config {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * IMAP服务器地址
     */
    @TableField("imap_host")
    private String imapHost;

    /**
     * IMAP端口 (143, 993)
     */
    @TableField("imap_port")
    private Integer imapPort;

    /**
     * IMAP是否启用SSL
     */
    @TableField("imap_ssl")
    private Boolean imapSsl;

    /**
     * POP3服务器地址
     */
    @TableField("pop3_host")
    private String pop3Host;

    /**
     * POP3端口 (110, 995)
     */
    @TableField("pop3_port")
    private Integer pop3Port;

    /**
     * POP3是否启用SSL
     */
    @TableField("pop3_ssl")
    private Boolean pop3Ssl;

    /**
     * 邮件存储路径
     */
    @TableField("mailbox_path")
    private String mailboxPath;

    /**
     * 邮件存储格式 (Maildir, mbox)
     */
    @TableField("mailbox_format")
    private String mailboxFormat;

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