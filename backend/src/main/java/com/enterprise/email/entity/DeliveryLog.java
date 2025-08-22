package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 投递日志实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("delivery_logs")
public class DeliveryLog {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 邮件ID
     */
    @TableField("message_id")
    private String messageId;

    /**
     * 队列ID
     */
    @TableField("queue_id")
    private String queueId;

    /**
     * 发件人
     */
    @TableField("sender")
    private String sender;

    /**
     * 收件人
     */
    @TableField("recipient")
    private String recipient;

    /**
     * 原始收件人
     */
    @TableField("original_recipient")
    private String originalRecipient;

    /**
     * 主题
     */
    @TableField("subject")
    private String subject;

    /**
     * 邮件大小（字节）
     */
    @TableField("message_size")
    private Long messageSize;

    /**
     * 投递状态
     */
    @TableField("status")
    private String status; // SENT, DELIVERED, BOUNCED, DEFERRED, REJECTED, FAILED

    /**
     * 投递结果
     */
    @TableField("delivery_result")
    private String deliveryResult; // SUCCESS, TEMP_FAILURE, PERM_FAILURE

    /**
     * SMTP状态码
     */
    @TableField("smtp_status")
    private String smtpStatus;

    /**
     * 响应消息
     */
    @TableField("response_message")
    private String responseMessage;

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
     * 下次重试时间
     */
    @TableField("next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * 投递延迟（毫秒）
     */
    @TableField("delivery_delay")
    private Long deliveryDelay;

    /**
     * 接收服务器
     */
    @TableField("receiving_server")
    private String receivingServer;

    /**
     * 接收服务器IP
     */
    @TableField("receiving_ip")
    private String receivingIp;

    /**
     * 发送服务器
     */
    @TableField("sending_server")
    private String sendingServer;

    /**
     * 发送服务器IP
     */
    @TableField("sending_ip")
    private String sendingIp;

    /**
     * TLS版本
     */
    @TableField("tls_version")
    private String tlsVersion;

    /**
     * 加密套件
     */
    @TableField("cipher_suite")
    private String cipherSuite;

    /**
     * SPF结果
     */
    @TableField("spf_result")
    private String spfResult;

    /**
     * DKIM结果
     */
    @TableField("dkim_result")
    private String dkimResult;

    /**
     * DMARC结果
     */
    @TableField("dmarc_result")
    private String dmarcResult;

    /**
     * 反垃圾邮件得分
     */
    @TableField("spam_score")
    private Double spamScore;

    /**
     * 病毒扫描结果
     */
    @TableField("virus_scan_result")
    private String virusScanResult;

    /**
     * 投递路径
     */
    @TableField("delivery_path")
    private String deliveryPath;

    /**
     * 邮件头部
     */
    @TableField("headers")
    private String headers;

    /**
     * 错误代码
     */
    @TableField("error_code")
    private String errorCode;

    /**
     * 错误详情
     */
    @TableField("error_details")
    private String errorDetails;

    /**
     * 处理时间（毫秒）
     */
    @TableField("processing_time")
    private Long processingTime;

    /**
     * 队列时间（毫秒）
     */
    @TableField("queue_time")
    private Long queueTime;

    /**
     * 用户代理
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 协议
     */
    @TableField("protocol")
    private String protocol; // SMTP, ESMTP, LMTP

    /**
     * 认证方法
     */
    @TableField("auth_method")
    private String authMethod;

    /**
     * 认证用户
     */
    @TableField("auth_user")
    private String authUser;

    /**
     * 客户端IP
     */
    @TableField("client_ip")
    private String clientIp;

    /**
     * 客户端主机名
     */
    @TableField("client_hostname")
    private String clientHostname;

    /**
     * 投递开始时间
     */
    @TableField("delivery_started_at")
    private LocalDateTime deliveryStartedAt;

    /**
     * 投递完成时间
     */
    @TableField("delivery_completed_at")
    private LocalDateTime deliveryCompletedAt;

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
}