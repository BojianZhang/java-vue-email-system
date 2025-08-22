package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邮件队列实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("email_queue")
public class EmailQueue {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 发件人邮箱
     */
    @TableField("from_email")
    private String fromEmail;

    /**
     * 收件人邮箱
     */
    @TableField("to_email")
    private String toEmail;

    /**
     * 抄送邮箱
     */
    @TableField("cc_email")
    private String ccEmail;

    /**
     * 密送邮箱
     */
    @TableField("bcc_email")
    private String bccEmail;

    /**
     * 邮件主题
     */
    @TableField("subject")
    private String subject;

    /**
     * 邮件内容
     */
    @TableField("content")
    private String content;

    /**
     * 内容类型 (HTML, TEXT)
     */
    @TableField("content_type")
    private String contentType;

    /**
     * 邮件头信息JSON
     */
    @TableField("headers_json")
    private String headersJson;

    /**
     * 附件信息JSON
     */
    @TableField("attachments_json")
    private String attachmentsJson;

    /**
     * 优先级 (HIGH, NORMAL, LOW)
     */
    @TableField("priority")
    private String priority;

    /**
     * 状态 (PENDING, SENDING, SENT, FAILED, RETRY)
     */
    @TableField("status")
    private String status;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    @TableField("max_retries")
    private Integer maxRetries;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 发送时间
     */
    @TableField("sent_at")
    private LocalDateTime sentAt;

    /**
     * 下次重试时间
     */
    @TableField("retry_at")
    private LocalDateTime retryAt;

    /**
     * SMTP服务器配置ID
     */
    @TableField("smtp_config_id")
    private Long smtpConfigId;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

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