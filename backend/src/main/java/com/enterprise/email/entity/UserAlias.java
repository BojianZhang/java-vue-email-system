package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 用户别名实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_aliases")
public class UserAlias extends BaseEntity {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @TableField("user_id")
    private Long userId;

    /**
     * 域名ID
     */
    @NotNull(message = "域名ID不能为空")
    @TableField("domain_id")
    private Long domainId;

    /**
     * 别名地址
     */
    @NotBlank(message = "别名地址不能为空")
    @Email(message = "别名地址格式不正确")
    @TableField("alias_address")
    private String aliasAddress;

    /**
     * 别名名称/描述
     */
    @TableField("alias_name")
    private String aliasName;

    /**
     * 是否为默认别名
     */
    @TableField("is_default")
    private Boolean isDefault;

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
    private String domainName;

    @TableField(exist = false)
    private String username;
}