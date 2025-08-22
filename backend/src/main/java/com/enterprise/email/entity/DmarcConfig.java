package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * DMARC配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dmarc_configs")
public class DmarcConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * DMARC记录内容
     */
    @TableField("dmarc_record")
    private String dmarcRecord;

    /**
     * 策略
     */
    @TableField("policy")
    private String policy; // none, quarantine, reject

    /**
     * 子域名策略
     */
    @TableField("subdomain_policy")
    private String subdomainPolicy;

    /**
     * DKIM对齐模式
     */
    @TableField("dkim_alignment")
    private String dkimAlignment; // r(relaxed), s(strict)

    /**
     * SPF对齐模式
     */
    @TableField("spf_alignment")
    private String spfAlignment; // r(relaxed), s(strict)

    /**
     * 聚合报告URI
     */
    @TableField("aggregate_report_uri")
    private String aggregateReportUri;

    /**
     * 失败报告URI
     */
    @TableField("failure_report_uri")
    private String failureReportUri;

    /**
     * 报告格式
     */
    @TableField("report_format")
    private String reportFormat;

    /**
     * 报告间隔
     */
    @TableField("report_interval")
    private Integer reportInterval;

    /**
     * 失败报告选项
     */
    @TableField("failure_report_options")
    private String failureReportOptions;

    /**
     * 百分比
     */
    @TableField("percentage")
    private Integer percentage;

    /**
     * 版本
     */
    @TableField("version")
    private String version;

    /**
     * 是否生成报告
     */
    @TableField("generate_reports")
    private Boolean generateReports;

    /**
     * 报告发送邮箱
     */
    @TableField("report_email")
    private String reportEmail;

    /**
     * 最大报告大小
     */
    @TableField("max_report_size")
    private Long maxReportSize;

    /**
     * 验证状态
     */
    @TableField("validation_status")
    private String validationStatus;

    /**
     * 验证错误信息
     */
    @TableField("validation_error")
    private String validationError;

    /**
     * 最后验证时间
     */
    @TableField("last_validated_at")
    private LocalDateTime lastValidatedAt;

    /**
     * 统计信息
     */
    @TableField("total_messages")
    private Long totalMessages;

    @TableField("passed_messages")
    private Long passedMessages;

    @TableField("failed_messages")
    private Long failedMessages;

    /**
     * 最后报告时间
     */
    @TableField("last_report_at")
    private LocalDateTime lastReportAt;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 状态
     */
    @TableField("status")
    private String status;

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