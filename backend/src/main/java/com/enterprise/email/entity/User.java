package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度在3-50个字符")
    @TableField("username")
    private String username;

    /**
     * 邮箱地址
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @TableField("email")
    private String email;

    /**
     * 密码（加密后）
     */
    @JsonIgnore
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6个字符")
    @TableField("password")
    private String password;

    /**
     * 显示名称
     */
    @TableField("display_name")
    private String displayName;

    /**
     * 用户角色
     */
    @TableField("role")
    private String role;

    /**
     * 账户状态
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 邮箱验证状态
     */
    @TableField("email_verified")
    private Boolean emailVerified;

    /**
     * 最后登录时间
     */
    @TableField("last_login")
    private LocalDateTime lastLogin;

    /**
     * 最后登录IP
     */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /**
     * 登录次数
     */
    @TableField("login_count")
    private Integer loginCount;

    /**
     * 邮箱存储配额（MB）
     */
    @TableField("storage_quota")
    private Long storageQuota;

    /**
     * 已使用存储空间（MB）
     */
    @TableField("storage_used")
    private Long storageUsed;

    /**
     * 用户头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 时区
     */
    @TableField("timezone")
    private String timezone;

    /**
     * 语言
     */
    @TableField("language")
    private String language;

    /**
     * 备注信息
     */
    @TableField("notes")
    private String notes;

    // 常量定义
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";
    
    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(this.role);
    }

    /**
     * 判断账户是否可用
     */
    public boolean isAccountNonLocked() {
        return this.isActive != null && this.isActive;
    }
}