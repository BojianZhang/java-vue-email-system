package com.enterprise.email.service;

import com.enterprise.email.entity.Domain;
import com.enterprise.email.entity.UserAlias;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;

/**
 * SMTP邮件发送服务接口
 */
public interface SmtpService {

    /**
     * 发送邮件
     */
    void sendEmail(UserAlias fromAlias, List<String> recipients, List<String> cc, List<String> bcc,
                   String subject, String textContent, String htmlContent, 
                   List<String> attachmentPaths) throws MessagingException;

    /**
     * 发送简单文本邮件
     */
    void sendSimpleEmail(String from, String to, String subject, String content) throws MessagingException;

    /**
     * 发送HTML邮件
     */
    void sendHtmlEmail(String from, String to, String subject, String htmlContent) throws MessagingException;

    /**
     * 发送带附件的邮件
     */
    void sendEmailWithAttachments(String from, List<String> recipients, String subject, 
                                  String content, List<String> attachmentPaths) throws MessagingException;

    /**
     * 创建MIME消息
     */
    MimeMessage createMimeMessage(UserAlias fromAlias, Domain domain) throws MessagingException;

    /**
     * 验证邮件地址格式
     */
    boolean isValidEmail(String email);

    /**
     * 解析收件人列表
     */
    List<String> parseRecipients(String recipients);

    /**
     * 获取SMTP配置
     */
    Map<String, Object> getSmtpConfig(Domain domain);
}