package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 用户登录日志实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_login_logs")
public class UserLoginLog extends BaseEntity {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @TableField("user_id")
    private Long userId;

    /**
     * 会话令牌哈希
     */
    @TableField("session_token_hash")
    private String sessionTokenHash;

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
     * 设备类型
     */
    @TableField("device_type")
    private String deviceType;

    /**
     * 操作系统
     */
    @TableField("os")
    private String os;

    /**
     * 浏览器
     */
    @TableField("browser")
    private String browser;

    /**
     * 设备指纹
     */
    @TableField("device_fingerprint")
    private String deviceFingerprint;

    /**
     * 国家
     */
    @TableField("country")
    private String country;

    /**
     * 地区
     */
    @TableField("region")
    private String region;

    /**
     * 城市
     */
    @TableField("city")
    private String city;

    /**
     * 纬度
     */
    @TableField("latitude")
    private Double latitude;

    /**
     * 经度
     */
    @TableField("longitude")
    private Double longitude;

    /**
     * ISP
     */
    @TableField("isp")
    private String isp;

    /**
     * 风险分数
     */
    @TableField("risk_score")
    private Integer riskScore;

    /**
     * 是否可疑
     */
    @TableField("is_suspicious")
    private Boolean isSuspicious;

    /**
     * 可疑原因
     */
    @TableField("suspicious_reasons")
    private String suspiciousReasons;

    /**
     * 是否活跃
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 登录时间
     */
    @TableField("login_time")
    private LocalDateTime loginTime;

    /**
     * 登出时间
     */
    @TableField("logout_time")
    private LocalDateTime logoutTime;

    /**
     * 最后活跃时间
     */
    @TableField("last_activity")
    private LocalDateTime lastActivity;

    // 关联查询字段
    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String email;
}