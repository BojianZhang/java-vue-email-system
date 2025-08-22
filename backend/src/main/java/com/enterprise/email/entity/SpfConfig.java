package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * SPF配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("spf_configs")
public class SpfConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * SPF记录内容
     */
    @TableField("spf_record")
    private String spfRecord;

    /**
     * 包含的域名
     */
    @TableField("include_domains")
    private String includeDomains;

    /**
     * 授权的IP地址
     */
    @TableField("ip4_addresses")
    private String ip4Addresses;

    /**
     * 授权的IPv6地址
     */
    @TableField("ip6_addresses")
    private String ip6Addresses;

    /**
     * 授权的A记录
     */
    @TableField("a_records")
    private String aRecords;

    /**
     * 授权的MX记录
     */
    @TableField("mx_records")
    private String mxRecords;

    /**
     * 失败策略
     */
    @TableField("fail_policy")
    private String failPolicy; // ~all, -all, +all, ?all

    /**
     * 重定向域名
     */
    @TableField("redirect_domain")
    private String redirectDomain;

    /**
     * 解释记录
     */
    @TableField("explanation")
    private String explanation;

    /**
     * 严格模式
     */
    @TableField("strict_mode")
    private Boolean strictMode;

    /**
     * DNS查询限制
     */
    @TableField("dns_lookup_limit")
    private Integer dnsLookupLimit;

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