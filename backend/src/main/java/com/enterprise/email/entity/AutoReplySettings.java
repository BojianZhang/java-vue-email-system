package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 自动回复设置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auto_reply_settings")
public class AutoReplySettings extends BaseEntity {

    /**
     * 用户别名ID
     */
    @NotNull(message = "用户别名ID不能为空")
    @TableField("alias_id")
    private Long aliasId;

    /**
     * 自动回复主题
     */
    @TableField("reply_subject")
    private String replySubject;

    /**
     * 自动回复内容
     */
    @TableField("reply_content")
    private String replyContent;

    /**
     * 回复内容类型 (TEXT-纯文本, HTML-富文本)
     */
    @TableField("content_type")
    private String contentType;

    /**
     * 是否启用
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 生效开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 生效结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 回复频率限制 (0-无限制, 1-每天一次, 2-每周一次)
     */
    @TableField("reply_frequency")
    private Integer replyFrequency;

    /**
     * 只对外部邮件回复
     */
    @TableField("external_only")
    private Boolean externalOnly;

    /**
     * 排除发件人列表 (逗号分隔)
     */
    @TableField("exclude_senders")
    private String excludeSenders;

    /**
     * 包含主题关键词 (逗号分隔，为空则对所有邮件回复)
     */
    @TableField("include_keywords")
    private String includeKeywords;

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