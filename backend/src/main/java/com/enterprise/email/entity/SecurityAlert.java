package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 安全警报实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("security_alerts")
public class SecurityAlert extends BaseEntity {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @TableField("user_id")
    private Long userId;

    /**
     * 警报类型
     */
    @NotBlank(message = "警报类型不能为空")
    @TableField("alert_type")
    private String alertType;

    /**
     * 严重程度
     */
    @NotBlank(message = "严重程度不能为空")
    @TableField("severity")
    private String severity;

    /**
     * 警报标题
     */
    @NotBlank(message = "警报标题不能为空")
    @TableField("title")
    private String title;

    /**
     * 警报描述
     */
    @TableField("description")
    private String description;

    /**
     * 警报详细数据（JSON格式）
     */
    @TableField("alert_data")
    private String alertData;

    /**
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * User-Agent
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 地理位置
     */
    @TableField("location")
    private String location;

    /**
     * 是否已解决
     */
    @TableField("is_resolved")
    private Boolean isResolved;

    /**
     * 解决时间
     */
    @TableField("resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * 解决人ID
     */
    @TableField("resolved_by")
    private Long resolvedBy;

    /**
     * 解决备注
     */
    @TableField("resolution_notes")
    private String resolutionNotes;

    /**
     * 是否已通知
     */
    @TableField("is_notified")
    private Boolean isNotified;

    /**
     * 通知时间
     */
    @TableField("notified_at")
    private LocalDateTime notifiedAt;

    // 警报类型常量
    public static final String TYPE_LOGIN_ANOMALY = "login_anomaly";
    public static final String TYPE_MULTIPLE_LOCATIONS = "multiple_locations";
    public static final String TYPE_SUSPICIOUS_IP = "suspicious_ip";
    public static final String TYPE_BRUTE_FORCE = "brute_force";
    public static final String TYPE_NEW_DEVICE = "new_device";
    public static final String TYPE_TIME_ANOMALY = "time_anomaly";
    public static final String TYPE_CONCURRENT_SESSIONS = "concurrent_sessions";
    public static final String TYPE_GEOGRAPHIC_ANOMALY = "geographic_anomaly";
    public static final String TYPE_IP_REPUTATION = "ip_reputation";
    public static final String TYPE_LOGIN_FREQUENCY = "login_frequency";

    // 严重程度常量
    public static final String SEVERITY_LOW = "low";
    public static final String SEVERITY_MEDIUM = "medium";
    public static final String SEVERITY_HIGH = "high";
    public static final String SEVERITY_CRITICAL = "critical";

    // 关联查询字段
    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String email;

    @TableField(exist = false)
    private String resolvedByUsername;
}