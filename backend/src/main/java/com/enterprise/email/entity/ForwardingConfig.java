package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邮件转发配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("forwarding_configs")
public class ForwardingConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户别名ID
     */
    @TableField("user_alias_id")
    private Long userAliasId;

    /**
     * 转发规则名称
     */
    @TableField("name")
    private String name;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 转发类型
     */
    @TableField("forwarding_type")
    private String forwardingType; // SIMPLE, CONDITIONAL, COPY, REDIRECT

    /**
     * 转发目标地址（JSON格式支持多个地址）
     */
    @TableField("target_addresses")
    private String targetAddresses;

    /**
     * 是否保留本地副本
     */
    @TableField("keep_local_copy")
    private Boolean keepLocalCopy;

    /**
     * 转发条件类型
     */
    @TableField("condition_type")
    private String conditionType; // ALL, SENDER, SUBJECT, HEADER, BODY, SIZE

    /**
     * 转发条件配置（JSON格式）
     */
    @TableField("conditions")
    private String conditions;

    /**
     * 是否转发附件
     */
    @TableField("forward_attachments")
    private Boolean forwardAttachments;

    /**
     * 最大转发大小（MB）
     */
    @TableField("max_forward_size")
    private Integer maxForwardSize;

    /**
     * 转发头部处理方式
     */
    @TableField("header_handling")
    private String headerHandling; // PRESERVE, MODIFY, REWRITE

    /**
     * 自定义转发头部
     */
    @TableField("custom_headers")
    private String customHeaders;

    /**
     * 转发主题前缀
     */
    @TableField("subject_prefix")
    private String subjectPrefix;

    /**
     * 转发内容前缀
     */
    @TableField("content_prefix")
    private String contentPrefix;

    /**
     * 转发内容后缀
     */
    @TableField("content_suffix")
    private String contentSuffix;

    /**
     * 生效时间
     */
    @TableField("effective_from")
    private LocalDateTime effectiveFrom;

    /**
     * 失效时间
     */
    @TableField("effective_until")
    private LocalDateTime effectiveUntil;

    /**
     * 生效时间段（JSON格式）
     */
    @TableField("active_hours")
    private String activeHours;

    /**
     * 生效星期（JSON格式）
     */
    @TableField("active_days")
    private String activeDays;

    /**
     * 转发频率限制
     */
    @TableField("rate_limit")
    private Integer rateLimit; // 每小时最大转发次数

    /**
     * 当前小时转发次数
     */
    @TableField("current_hour_count")
    private Integer currentHourCount;

    /**
     * 当前小时开始时间
     */
    @TableField("current_hour_start")
    private LocalDateTime currentHourStart;

    /**
     * 白名单发件人
     */
    @TableField("whitelist_senders")
    private String whitelistSenders;

    /**
     * 黑名单发件人
     */
    @TableField("blacklist_senders")
    private String blacklistSenders;

    /**
     * 白名单域名
     */
    @TableField("whitelist_domains")
    private String whitelistDomains;

    /**
     * 黑名单域名
     */
    @TableField("blacklist_domains")
    private String blacklistDomains;

    /**
     * 是否转发垃圾邮件
     */
    @TableField("forward_spam")
    private Boolean forwardSpam;

    /**
     * 是否转发病毒邮件
     */
    @TableField("forward_virus")
    private Boolean forwardVirus;

    /**
     * 循环检测
     */
    @TableField("loop_detection")
    private Boolean loopDetection;

    /**
     * 最大跳数
     */
    @TableField("max_hops")
    private Integer maxHops;

    /**
     * 转发统计
     */
    @TableField("total_forwarded")
    private Long totalForwarded;

    /**
     * 转发成功次数
     */
    @TableField("successful_forwards")
    private Long successfulForwards;

    /**
     * 转发失败次数
     */
    @TableField("failed_forwards")
    private Long failedForwards;

    /**
     * 跳过次数
     */
    @TableField("skipped_count")
    private Long skippedCount;

    /**
     * 最后转发时间
     */
    @TableField("last_forwarded_at")
    private LocalDateTime lastForwardedAt;

    /**
     * 最后错误信息
     */
    @TableField("last_error")
    private String lastError;

    /**
     * 最后错误时间
     */
    @TableField("last_error_at")
    private LocalDateTime lastErrorAt;

    /**
     * 优先级
     */
    @TableField("priority")
    private Integer priority;

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