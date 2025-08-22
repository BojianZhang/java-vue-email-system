package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * DNS记录实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dns_records")
public class DnsRecord {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * 记录名称
     */
    @TableField("name")
    private String name;

    /**
     * 记录类型
     */
    @TableField("type")
    private String type; // A, AAAA, CNAME, MX, TXT, SRV, PTR

    /**
     * 记录值
     */
    @TableField("value")
    private String value;

    /**
     * TTL
     */
    @TableField("ttl")
    private Integer ttl;

    /**
     * 优先级（用于MX记录）
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 权重（用于SRV记录）
     */
    @TableField("weight")
    private Integer weight;

    /**
     * 端口（用于SRV记录）
     */
    @TableField("port")
    private Integer port;

    /**
     * 记录状态
     */
    @TableField("status")
    private String status; // ACTIVE, INACTIVE, PENDING, FAILED

    /**
     * 记录来源
     */
    @TableField("source")
    private String source; // MANUAL, AUTO_GENERATED, IMPORTED

    /**
     * 关联的服务类型
     */
    @TableField("service_type")
    private String serviceType; // DKIM, SPF, DMARC, MX, AUTOCONFIG

    /**
     * 关联的服务ID
     */
    @TableField("service_id")
    private Long serviceId;

    /**
     * 是否自动管理
     */
    @TableField("auto_managed")
    private Boolean autoManaged;

    /**
     * 最后验证时间
     */
    @TableField("last_verified_at")
    private LocalDateTime lastVerifiedAt;

    /**
     * DNS验证状态
     */
    @TableField("dns_verified")
    private Boolean dnsVerified;

    /**
     * DNS错误信息
     */
    @TableField("dns_error")
    private String dnsError;

    /**
     * 解析的IP地址
     */
    @TableField("resolved_ips")
    private String resolvedIps;

    /**
     * DNS服务器
     */
    @TableField("dns_server")
    private String dnsServer;

    /**
     * 备注
     */
    @TableField("notes")
    private String notes;

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