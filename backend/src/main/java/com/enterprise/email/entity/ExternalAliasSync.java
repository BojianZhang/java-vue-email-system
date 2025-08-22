package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 外部平台别名同步配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("external_alias_sync")
public class ExternalAliasSync extends BaseEntity {

    /**
     * 用户别名ID
     */
    @NotNull(message = "用户别名ID不能为空")
    @TableField("alias_id")
    private Long aliasId;

    /**
     * 外部平台类型 (POSTE_IO, MAIL_COW, ZIMBRA, EXCHANGE, CUSTOM)
     */
    @NotBlank(message = "外部平台类型不能为空")
    @TableField("platform_type")
    private String platformType;

    /**
     * 外部平台服务器地址
     */
    @NotBlank(message = "外部平台地址不能为空")
    @TableField("platform_url")
    private String platformUrl;

    /**
     * 外部平台API密钥或访问令牌
     */
    @TableField("api_key")
    private String apiKey;

    /**
     * 外部平台用户名
     */
    @TableField("external_username")
    private String externalUsername;

    /**
     * 外部平台密码（加密存储）
     */
    @TableField("external_password")
    private String externalPassword;

    /**
     * 外部平台上的别名ID或标识
     */
    @TableField("external_alias_id")
    private String externalAliasId;

    /**
     * 外部平台上的别名地址
     */
    @TableField("external_alias_address")
    private String externalAliasAddress;

    /**
     * 外部平台上的别名名称（我们要同步的）
     */
    @TableField("external_alias_name")
    private String externalAliasName;

    /**
     * 外部平台上的别名描述
     */
    @TableField("external_alias_description")
    private String externalAliasDescription;

    /**
     * 是否启用自动同步
     */
    @TableField("auto_sync_enabled")
    private Boolean autoSyncEnabled;

    /**
     * 同步频率（分钟）
     */
    @TableField("sync_frequency_minutes")
    private Integer syncFrequencyMinutes;

    /**
     * 最后同步时间
     */
    @TableField("last_sync_time")
    private LocalDateTime lastSyncTime;

    /**
     * 最后同步状态 (SUCCESS, FAILED, PENDING)
     */
    @TableField("last_sync_status")
    private String lastSyncStatus;

    /**
     * 最后同步错误信息
     */
    @TableField("last_sync_error")
    private String lastSyncError;

    /**
     * 同步重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 是否启用
     */
    @TableField("is_active")
    private Boolean isActive;

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

    // 关联查询字段
    @TableField(exist = false)
    private String aliasAddress;

    @TableField(exist = false)
    private String localAliasName;
}