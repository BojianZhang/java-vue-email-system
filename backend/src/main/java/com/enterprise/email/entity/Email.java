package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 邮件实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("emails")
public class Email extends BaseEntity {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @TableField("user_id")
    private Long userId;

    /**
     * 别名ID（如果通过别名收发）
     */
    @TableField("alias_id")
    private Long aliasId;

    /**
     * 邮件UID（IMAP服务器中的唯一标识）
     */
    @TableField("message_uid")
    private String messageUid;

    /**
     * 邮件ID（Message-ID头）
     */
    @TableField("message_id")
    private String messageId;

    /**
     * 发件人
     */
    @NotBlank(message = "发件人不能为空")
    @TableField("sender")
    private String sender;

    /**
     * 收件人
     */
    @NotBlank(message = "收件人不能为空")
    @TableField("recipient")
    private String recipient;

    /**
     * 抄送
     */
    @TableField("cc")
    private String cc;

    /**
     * 密送
     */
    @TableField("bcc")
    private String bcc;

    /**
     * 邮件主题
     */
    @TableField("subject")
    private String subject;

    /**
     * 邮件内容（文本）
     */
    @TableField("content_text")
    private String contentText;

    /**
     * 邮件内容（HTML）
     */
    @TableField("content_html")
    private String contentHtml;

    /**
     * 邮件大小（字节）
     */
    @TableField("size_bytes")
    private Long sizeBytes;

    /**
     * 邮件类型（inbox/sent/draft/trash）
     */
    @TableField("email_type")
    private String emailType;

    /**
     * 是否已读
     */
    @TableField("is_read")
    private Boolean isRead;

    /**
     * 是否重要
     */
    @TableField("is_important")
    private Boolean isImportant;

    /**
     * 是否有附件
     */
    @TableField("has_attachment")
    private Boolean hasAttachment;

    /**
     * 附件数量
     */
    @TableField("attachment_count")
    private Integer attachmentCount;

    /**
     * 邮件发送/接收时间
     */
    @TableField("sent_time")
    private LocalDateTime sentTime;

    /**
     * 邮件接收时间
     */
    @TableField("received_time")
    private LocalDateTime receivedTime;

    /**
     * 原始邮件头信息
     */
    @TableField("headers")
    private String headers;

    /**
     * 邮件文件路径（如果存储在文件系统）
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 邮件状态
     */
    @TableField("status")
    private String status;

    // 邮件类型常量
    public static final String TYPE_INBOX = "inbox";
    public static final String TYPE_SENT = "sent";
    public static final String TYPE_DRAFT = "draft";
    public static final String TYPE_TRASH = "trash";

    // 邮件状态常量
    public static final String STATUS_NEW = "new";
    public static final String STATUS_PROCESSED = "processed";
    public static final String STATUS_ERROR = "error";

    // 关联查询字段
    @TableField(exist = false)
    private String aliasAddress;

    @TableField(exist = false)
    private String username;
}