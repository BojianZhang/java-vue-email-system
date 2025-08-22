package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 安全配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("security_settings")
public class SecuritySetting extends BaseEntity {

    /**
     * 配置键
     */
    @NotBlank(message = "配置键不能为空")
    @TableField("setting_key")
    private String settingKey;

    /**
     * 配置值
     */
    @NotBlank(message = "配置值不能为空")
    @TableField("setting_value")
    private String settingValue;

    /**
     * 配置类型
     */
    @TableField("setting_type")
    private String settingType;

    /**
     * 配置描述
     */
    @TableField("description")
    private String description;

    /**
     * 配置分类
     */
    @TableField("category")
    private String category;

    /**
     * 是否为系统配置
     */
    @TableField("is_system")
    private Boolean isSystem;

    /**
     * 更新者ID
     */
    @TableField("updated_by")
    private Long updatedBy;

    // 配置类型常量
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_JSON = "json";

    // 配置分类常量
    public static final String CATEGORY_LOGIN_MONITORING = "login_monitoring";
    public static final String CATEGORY_ANOMALY_DETECTION = "anomaly_detection";
    public static final String CATEGORY_RISK_SCORING = "risk_scoring";
    public static final String CATEGORY_NOTIFICATIONS = "notifications";
    public static final String CATEGORY_SESSION_MANAGEMENT = "session_management";
    public static final String CATEGORY_DEVICE_MANAGEMENT = "device_management";
    public static final String CATEGORY_LOGIN_SECURITY = "login_security";
    public static final String CATEGORY_LOGGING = "logging";
    public static final String CATEGORY_SYSTEM = "system";

    // 关联查询字段
    @TableField(exist = false)
    private String updatedByUsername;
}