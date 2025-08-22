package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Sieve邮件过滤规则实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sieve_rules")
public class SieveRule {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户别名ID
     */
    @TableField("user_alias_id")
    private Long userAliasId;

    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 规则描述
     */
    @TableField("description")
    private String description;

    /**
     * Sieve脚本内容
     */
    @TableField("sieve_script")
    private String sieveScript;

    /**
     * 规则优先级
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 规则类型
     */
    @TableField("rule_type")
    private String ruleType; // FILTER, FORWARD, VACATION, REJECT, DISCARD

    /**
     * 条件类型
     */
    @TableField("condition_type")
    private String conditionType; // ALL, ANY, HEADER, BODY, SIZE, ENVELOPE

    /**
     * 条件配置（JSON格式）
     */
    @TableField("conditions")
    private String conditions;

    /**
     * 动作类型
     */
    @TableField("action_type")
    private String actionType; // KEEP, DISCARD, REDIRECT, FILEINTO, REJECT, STOP

    /**
     * 动作配置（JSON格式）
     */
    @TableField("actions")
    private String actions;

    /**
     * 目标文件夹
     */
    @TableField("target_folder")
    private String targetFolder;

    /**
     * 转发地址
     */
    @TableField("forward_address")
    private String forwardAddress;

    /**
     * 拒绝消息
     */
    @TableField("reject_message")
    private String rejectMessage;

    /**
     * 是否继续处理后续规则
     */
    @TableField("continue_processing")
    private Boolean continueProcessing;

    /**
     * 是否启用日志
     */
    @TableField("enable_logging")
    private Boolean enableLogging;

    /**
     * 测试模式
     */
    @TableField("test_mode")
    private Boolean testMode;

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
     * 应用统计
     */
    @TableField("applied_count")
    private Long appliedCount;

    /**
     * 最后应用时间
     */
    @TableField("last_applied_at")
    private LocalDateTime lastAppliedAt;

    /**
     * 错误统计
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
     * 语法验证状态
     */
    @TableField("syntax_valid")
    private Boolean syntaxValid;

    /**
     * 语法验证错误
     */
    @TableField("syntax_error")
    private String syntaxError;

    /**
     * 最后验证时间
     */
    @TableField("last_validated_at")
    private LocalDateTime lastValidatedAt;

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