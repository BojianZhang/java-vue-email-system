package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * DKIM密钥实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dkim_keys")
public class DkimKey {

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
     * 私钥（PEM格式）
     */
    @TableField("private_key")
    private String privateKey;

    /**
     * 公钥（PEM格式）
     */
    @TableField("public_key")
    private String publicKey;

    /**
     * DNS记录内容
     */
    @TableField("dns_record")
    private String dnsRecord;

    /**
     * 密钥长度（1024, 2048, 4096）
     */
    @TableField("key_length")
    private Integer keyLength;

    /**
     * 算法类型
     */
    @TableField("algorithm")
    private String algorithm;

    /**
     * 密钥状态
     */
    @TableField("status")
    private String status; // ACTIVE, INACTIVE, EXPIRED, REVOKED

    /**
     * 是否为当前活跃密钥
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 密钥用途
     */
    @TableField("key_usage")
    private String keyUsage; // EMAIL, ALL

    /**
     * 签名算法
     */
    @TableField("signature_algorithm")
    private String signatureAlgorithm; // rsa-sha256, ed25519-sha256

    /**
     * 规范化算法
     */
    @TableField("canonicalization")
    private String canonicalization; // relaxed/relaxed, simple/simple

    /**
     * 子域名策略
     */
    @TableField("subdomain_policy")
    private String subdomainPolicy; // strict, relaxed

    /**
     * 密钥生成时间
     */
    @TableField("generated_at")
    private LocalDateTime generatedAt;

    /**
     * 密钥过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

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
     * 自动轮换周期（天）
     */
    @TableField("rotation_period")
    private Integer rotationPeriod;

    /**
     * 下次轮换时间
     */
    @TableField("next_rotation_at")
    private LocalDateTime nextRotationAt;

    /**
     * 密钥指纹
     */
    @TableField("fingerprint")
    private String fingerprint;

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