package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * SSL证书管理实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ssl_certificates")
public class SslCertificate extends BaseEntity {

    /**
     * 域名
     */
    @NotBlank(message = "域名不能为空")
    @TableField("domain")
    private String domain;

    /**
     * 证书类型 (LETS_ENCRYPT, UPLOADED, SELF_SIGNED)
     */
    @NotBlank(message = "证书类型不能为空")
    @TableField("cert_type")
    private String certType;

    /**
     * 证书状态 (ACTIVE, EXPIRED, PENDING, FAILED)
     */
    @TableField("status")
    private String status;

    /**
     * 证书文件路径
     */
    @TableField("cert_path")
    private String certPath;

    /**
     * 私钥文件路径
     */
    @TableField("key_path")
    private String keyPath;

    /**
     * 证书链文件路径
     */
    @TableField("chain_path")
    private String chainPath;

    /**
     * 证书颁发时间
     */
    @TableField("issued_at")
    private LocalDateTime issuedAt;

    /**
     * 证书过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /**
     * 是否启用自动续期
     */
    @TableField("auto_renew")
    private Boolean autoRenew;

    /**
     * 最后续期时间
     */
    @TableField("last_renewal")
    private LocalDateTime lastRenewal;

    /**
     * 续期失败次数
     */
    @TableField("renewal_failures")
    private Integer renewalFailures;

    /**
     * Let's Encrypt 挑战类型 (HTTP01, DNS01)
     */
    @TableField("challenge_type")
    private String challengeType;

    /**
     * 证书指纹
     */
    @TableField("fingerprint")
    private String fingerprint;

    /**
     * 证书详细信息 (JSON格式)
     */
    @TableField("cert_info")
    private String certInfo;

    /**
     * 邮箱地址（用于Let's Encrypt注册）
     */
    @TableField("email")
    private String email;

    /**
     * 是否应用到服务
     */
    @TableField("applied")
    private Boolean applied;

    /**
     * 应用时间
     */
    @TableField("applied_at")
    private LocalDateTime appliedAt;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 备注
     */
    @TableField("notes")
    private String notes;

    /**
     * 创建者ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 更新者ID
     */
    @TableField("updated_by")
    private Long updatedBy;
}