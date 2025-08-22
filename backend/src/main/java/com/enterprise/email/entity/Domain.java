package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 域名实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("domains")
public class Domain extends BaseEntity {

    /**
     * 域名
     */
    @NotBlank(message = "域名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "域名格式不正确")
    @TableField("domain_name")
    private String domainName;

    /**
     * 域名描述
     */
    @TableField("description")
    private String description;

    /**
     * 是否启用
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 是否为默认域名
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * MX记录
     */
    @TableField("mx_record")
    private String mxRecord;

    /**
     * SMTP服务器
     */
    @TableField("smtp_host")
    private String smtpHost;

    /**
     * SMTP端口
     */
    @TableField("smtp_port")
    private Integer smtpPort;

    /**
     * SMTP是否启用SSL
     */
    @TableField("smtp_ssl")
    private Boolean smtpSsl;

    /**
     * IMAP服务器
     */
    @TableField("imap_host")
    private String imapHost;

    /**
     * IMAP端口
     */
    @TableField("imap_port")
    private Integer imapPort;

    /**
     * IMAP是否启用SSL
     */
    @TableField("imap_ssl")
    private Boolean imapSsl;

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