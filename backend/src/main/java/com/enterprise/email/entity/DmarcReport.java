package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * DMARC报告实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dmarc_reports")
public class DmarcReport {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 报告ID
     */
    @TableField("report_id")
    private String reportId;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * 报告组织
     */
    @TableField("org_name")
    private String orgName;

    /**
     * 报告邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 报告类型
     */
    @TableField("report_type")
    private String reportType; // AGGREGATE, FORENSIC

    /**
     * 报告格式
     */
    @TableField("report_format")
    private String reportFormat; // XML, JSON

    /**
     * 报告开始时间
     */
    @TableField("date_range_begin")
    private LocalDateTime dateRangeBegin;

    /**
     * 报告结束时间
     */
    @TableField("date_range_end")
    private LocalDateTime dateRangeEnd;

    /**
     * 域名策略
     */
    @TableField("domain_policy")
    private String domainPolicy; // NONE, QUARANTINE, REJECT

    /**
     * 子域名策略
     */
    @TableField("subdomain_policy")
    private String subdomainPolicy;

    /**
     * 策略覆盖百分比
     */
    @TableField("policy_percentage")
    private Integer policyPercentage;

    /**
     * DKIM对齐模式
     */
    @TableField("dkim_alignment")
    private String dkimAlignment; // RELAXED, STRICT

    /**
     * SPF对齐模式
     */
    @TableField("spf_alignment")
    private String spfAlignment; // RELAXED, STRICT

    /**
     * 源IP地址
     */
    @TableField("source_ip")
    private String sourceIp;

    /**
     * 邮件数量
     */
    @TableField("count")
    private Long count;

    /**
     * DMARC处置
     */
    @TableField("disposition")
    private String disposition; // NONE, QUARANTINE, REJECT

    /**
     * DKIM结果
     */
    @TableField("dkim")
    private String dkim; // PASS, FAIL

    /**
     * SPF结果
     */
    @TableField("spf")
    private String spf; // PASS, FAIL

    /**
     * 原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 头部发件人
     */
    @TableField("header_from")
    private String headerFrom;

    /**
     * 信封发件人
     */
    @TableField("envelope_from")
    private String envelopeFrom;

    /**
     * DKIM域名
     */
    @TableField("dkim_domain")
    private String dkimDomain;

    /**
     * DKIM选择器
     */
    @TableField("dkim_selector")
    private String dkimSelector;

    /**
     * DKIM结果详情
     */
    @TableField("dkim_result")
    private String dkimResult;

    /**
     * SPF域名
     */
    @TableField("spf_domain")
    private String spfDomain;

    /**
     * SPF结果详情
     */
    @TableField("spf_result")
    private String spfResult;

    /**
     * 认证结果
     */
    @TableField("auth_results")
    private String authResults;

    /**
     * 标识符对齐
     */
    @TableField("identifier_alignment")
    private String identifierAlignment;

    /**
     * 原始报告内容
     */
    @TableField("raw_report")
    private String rawReport;

    /**
     * 报告摘要
     */
    @TableField("report_summary")
    private String reportSummary;

    /**
     * 处理状态
     */
    @TableField("processing_status")
    private String processingStatus; // PENDING, PROCESSED, FAILED

    /**
     * 处理错误
     */
    @TableField("processing_error")
    private String processingError;

    /**
     * 是否合规
     */
    @TableField("compliant")
    private Boolean compliant;

    /**
     * 风险等级
     */
    @TableField("risk_level")
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

    /**
     * 建议动作
     */
    @TableField("recommended_action")
    private String recommendedAction;

    /**
     * 报告来源
     */
    @TableField("report_source")
    private String reportSource;

    /**
     * 额外信息
     */
    @TableField("extra_info")
    private String extraInfo;

    /**
     * 接收时间
     */
    @TableField("received_at")
    private LocalDateTime receivedAt;

    /**
     * 处理时间
     */
    @TableField("processed_at")
    private LocalDateTime processedAt;

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