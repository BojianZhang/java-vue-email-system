package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 用户可信设备实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_trusted_devices")
public class UserTrustedDevice extends BaseEntity {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @TableField("user_id")
    private Long userId;

    /**
     * 设备指纹
     */
    @NotBlank(message = "设备指纹不能为空")
    @TableField("device_fingerprint")
    private String deviceFingerprint;

    /**
     * 设备名称
     */
    @TableField("device_name")
    private String deviceName;

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
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 地理位置
     */
    @TableField("location")
    private String location;

    /**
     * 是否信任
     */
    @TableField("is_trusted")
    private Boolean isTrusted;

    /**
     * 信任时间
     */
    @TableField("trusted_at")
    private LocalDateTime trustedAt;

    /**
     * 最后使用时间
     */
    @TableField("last_used")
    private LocalDateTime lastUsed;

    /**
     * 过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    // 关联查询字段
    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String email;
}