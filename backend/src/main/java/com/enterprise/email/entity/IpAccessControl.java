package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * IP访问控制实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ip_access_controls")
public class IpAccessControl {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 规则名称
     */
    @TableField("name")
    private String name;

    /**
     * 规则类型
     */
    @TableField("rule_type")
    private String ruleType; // ALLOW, DENY, RATE_LIMIT, MONITOR

    /**
     * IP地址或CIDR
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * CIDR掩码
     */
    @TableField("cidr_mask")
    private Integer cidrMask;

    /**
     * 国家代码
     */
    @TableField("country_code")
    private String countryCode;

    /**
     * 地区
     */
    @TableField("region")
    private String region;

    /**
     * 城市
     */
    @TableField("city")
    private String city;

    /**
     * ISP提供商
     */
    @TableField("isp")
    private String isp;

    /**
     * 应用的服务
     */
    @TableField("service")
    private String service; // SMTP, IMAP, POP3, WEBMAIL, API, ALL

    /**
     * 端口号
     */
    @TableField("port")
    private Integer port;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 优先级
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 动作
     */
    @TableField("action")
    private String action; // ACCEPT, REJECT, DROP, LOG

    /**
     * 速率限制（每分钟连接数）
     */
    @TableField("rate_limit")
    private Integer rateLimit;

    /**
     * 时间窗口（秒）
     */
    @TableField("time_window")
    private Integer timeWindow;

    /**
     * 当前计数
     */
    @TableField("current_count")
    private Integer currentCount;

    /**
     * 计数重置时间
     */
    @TableField("count_reset_at")
    private LocalDateTime countResetAt;

    /**
     * 匹配次数
     */
    @TableField("match_count")
    private Long matchCount;

    /**
     * 阻止次数
     */
    @TableField("block_count")
    private Long blockCount;

    /**
     * 最后匹配时间
     */
    @TableField("last_matched_at")
    private LocalDateTime lastMatchedAt;

    /**
     * 最后阻止时间
     */
    @TableField("last_blocked_at")
    private LocalDateTime lastBlockedAt;

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
     * 是否临时规则
     */
    @TableField("temporary")
    private Boolean temporary;

    /**
     * 临时规则持续时间（分钟）
     */
    @TableField("temporary_duration")
    private Integer temporaryDuration;

    /**
     * 白名单ID（如果是例外）
     */
    @TableField("whitelist_id")
    private Long whitelistId;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 备注
     */
    @TableField("notes")
    private String notes;

    /**
     * 创建者
     */
    @TableField("created_by")
    private Long createdBy;

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