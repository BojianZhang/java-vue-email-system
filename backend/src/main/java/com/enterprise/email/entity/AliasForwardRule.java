package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 别名转发规则实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alias_forward_rules")
public class AliasForwardRule extends BaseEntity {

    /**
     * 用户别名ID
     */
    @NotNull(message = "用户别名ID不能为空")
    @TableField("alias_id")
    private Long aliasId;

    /**
     * 转发目标地址
     */
    @NotBlank(message = "转发目标地址不能为空")
    @Email(message = "转发目标地址格式不正确")
    @TableField("forward_to")
    private String forwardTo;

    /**
     * 转发规则名称
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 转发条件类型 (ALL-全部转发, SUBJECT-主题包含, FROM-发件人匹配, TO-收件人匹配)
     */
    @TableField("condition_type")
    private String conditionType;

    /**
     * 转发条件值
     */
    @TableField("condition_value")
    private String conditionValue;

    /**
     * 是否保留原邮件
     */
    @TableField("keep_original")
    private Boolean keepOriginal;

    /**
     * 是否启用
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 转发优先级
     */
    @TableField("priority")
    private Integer priority;

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
    private String domainName;
}