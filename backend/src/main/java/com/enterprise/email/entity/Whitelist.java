package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 白名单实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("whitelists")
public class Whitelist {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 白名单类型
     */
    @TableField("type")
    private String type; // EMAIL, DOMAIN, IP, CIDR, SENDER, RECIPIENT

    /**
     * 白名单值
     */
    @TableField("value")
    private String value;

    /**
     * 白名单名称
     */
    @TableField("name")
    private String name;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 适用域名
     */
    @TableField("domain")
    private String domain;

    /**
     * 用户别名ID
     */
    @TableField("user_alias_id")
    private Long userAliasId;

    /**
     * 是否全局生效
     */
    @TableField("global")
    private Boolean global;

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
    private String status; // ACTIVE, INACTIVE, EXPIRED

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
     * 匹配次数
     */
    @TableField("match_count")
    private Long matchCount;

    /**
     * 最后匹配时间
     */
    @TableField("last_matched_at")
    private LocalDateTime lastMatchedAt;

    /**
     * 匹配的IP地址
     */
    @TableField("matched_ips")
    private String matchedIps;

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