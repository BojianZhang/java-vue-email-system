package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 自动回复配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("autoresponder_configs")
public class AutoresponderConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户别名ID
     */
    @TableField("user_alias_id")
    private Long userAliasId;

    /**
     * 自动回复名称
     */
    @TableField("name")
    private String name;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 回复主题
     */
    @TableField("subject")
    private String subject;

    /**
     * 回复内容
     */
    @TableField("message")
    private String message;

    /**
     * 回复内容类型
     */
    @TableField("content_type")
    private String contentType; // TEXT, HTML

    /**
     * 发件人地址
     */
    @TableField("from_address")
    private String fromAddress;

    /**
     * 发件人姓名
     */
    @TableField("from_name")
    private String fromName;

    /**
     * 触发条件类型
     */
    @TableField("trigger_type")
    private String triggerType; // ALL, SENDER, SUBJECT, HEADER, TIME

    /**
     * 触发条件配置（JSON格式）
     */
    @TableField("trigger_conditions")
    private String triggerConditions;

    /**
     * 回复频率限制
     */
    @TableField("frequency_limit")
    private String frequencyLimit; // ONCE, DAILY, WEEKLY, ALWAYS

    /**
     * 回复间隔（小时）
     */
    @TableField("reply_interval")
    private Integer replyInterval;

    /**
     * 开始时间
     */
    @TableField("start_date")
    private LocalDateTime startDate;

    /**
     * 结束时间
     */
    @TableField("end_date")
    private LocalDateTime endDate;

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
     * 是否包含原邮件
     */
    @TableField("include_original")
    private Boolean includeOriginal;

    /**
     * 是否仅回复内部邮件
     */
    @TableField("internal_only")
    private Boolean internalOnly;

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
     * 白名单邮件地址
     */
    @TableField("whitelist_emails")
    private String whitelistEmails;

    /**
     * 黑名单邮件地址
     */
    @TableField("blacklist_emails")
    private String blacklistEmails;

    /**
     * 最大回复次数
     */
    @TableField("max_replies")
    private Integer maxReplies;

    /**
     * 当前回复次数
     */
    @TableField("current_replies")
    private Integer currentReplies;

    /**
     * 最后回复时间
     */
    @TableField("last_reply_at")
    private LocalDateTime lastReplyAt;

    /**
     * 回复统计
     */
    @TableField("total_replies")
    private Long totalReplies;

    /**
     * 总触发次数
     */
    @TableField("total_triggers")
    private Long totalTriggers;

    /**
     * 跳过次数
     */
    @TableField("skipped_count")
    private Long skippedCount;

    /**
     * 错误次数
     */
    @TableField("error_count")
    private Long errorCount;

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
     * 优先级
     */
    @TableField("priority")
    private Integer priority;

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