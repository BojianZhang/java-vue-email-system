package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * DKIM密钥配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dkim_configs")
public class DkimConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * 选择器
     */
    @TableField("selector")
    private String selector;

    /**
     * 私钥
     */
    @TableField("private_key")
    private String privateKey;

    /**
     * 公钥
     */
    @TableField("public_key")
    private String publicKey;

    /**
     * 密钥长度
     */
    @TableField("key_size")
    private Integer keySize;

    /**
     * 签名算法
     */
    @TableField("algorithm")
    private String algorithm;

    /**
     * 规范化算法
     */
    @TableField("canonicalization")
    private String canonicalization;

    /**
     * 签名头部字段
     */
    @TableField("headers")
    private String headers;

    /**
     * 密钥类型
     */
    @TableField("key_type")
    private String keyType;

    /**
     * 测试模式
     */
    @TableField("test_mode")
    private Boolean testMode;

    /**
     * 子域名策略
     */
    @TableField("subdomain_policy")
    private String subdomainPolicy;

    /**
     * 服务类型
     */
    @TableField("service_type")
    private String serviceType;

    /**
     * 密钥过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /**
     * 是否自动轮换
     */
    @TableField("auto_rotate")
    private Boolean autoRotate;

    /**
     * 轮换间隔（天）
     */
    @TableField("rotate_interval")
    private Integer rotateInterval;

    /**
     * DNS记录状态
     */
    @TableField("dns_status")
    private String dnsStatus;

    /**
     * DNS记录值
     */
    @TableField("dns_record")
    private String dnsRecord;

    /**
     * 最后验证时间
     */
    @TableField("last_verified_at")
    private LocalDateTime lastVerifiedAt;

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