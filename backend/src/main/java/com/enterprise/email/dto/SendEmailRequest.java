package com.enterprise.email.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 发送邮件请求DTO
 */
@Data
public class SendEmailRequest {

    /**
     * 收件人列表（多个邮箱用逗号分隔）
     */
    @NotBlank(message = "收件人不能为空")
    private String recipients;

    /**
     * 抄送列表（多个邮箱用逗号分隔）
     */
    private String cc;

    /**
     * 密送列表（多个邮箱用逗号分隔）
     */
    private String bcc;

    /**
     * 邮件主题
     */
    @Size(max = 500, message = "邮件主题不能超过500个字符")
    private String subject;

    /**
     * 邮件正文（纯文本）
     */
    private String contentText;

    /**
     * 邮件正文（HTML格式）
     */
    private String contentHtml;

    /**
     * 是否为重要邮件
     */
    private Boolean important = false;

    /**
     * 定时发送时间（可选）
     */
    private String scheduledTime;

    /**
     * 是否保存为草稿
     */
    private Boolean saveAsDraft = false;

    /**
     * 回复的邮件ID（如果是回复邮件）
     */
    private Long replyToEmailId;

    /**
     * 转发的邮件ID（如果是转发邮件）
     */
    private Long forwardEmailId;
}