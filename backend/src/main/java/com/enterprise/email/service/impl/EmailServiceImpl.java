package com.enterprise.email.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.email.dto.EmailDto;
import com.enterprise.email.dto.SendEmailRequest;
import com.enterprise.email.entity.*;
import com.enterprise.email.mapper.EmailMapper;
import com.enterprise.email.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 邮件服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl extends ServiceImpl<EmailMapper, Email> implements EmailService {

    private final SmtpService smtpService;
    private final ImapService imapService;
    private final EmailAttachmentService attachmentService;
    private final UserAliasService userAliasService;
    private final DomainService domainService;

    @Override
    @Transactional
    public void sendEmail(SendEmailRequest request, Long userId, Long aliasId, List<MultipartFile> attachments) {
        try {
            // 获取发件人别名
            UserAlias fromAlias = userAliasService.getById(aliasId);
            if (fromAlias == null || !fromAlias.getUserId().equals(userId)) {
                throw new RuntimeException("无效的发件人别名");
            }

            // 保存邮件记录
            Email email = new Email();
            email.setUserId(userId);
            email.setAliasId(aliasId);
            email.setSender(fromAlias.getAliasAddress());
            email.setRecipient(String.join(",", request.getRecipients()));
            email.setCc(request.getCc() != null ? String.join(",", request.getCc()) : null);
            email.setBcc(request.getBcc() != null ? String.join(",", request.getBcc()) : null);
            email.setSubject(request.getSubject());
            email.setContentText(request.getTextContent());
            email.setContentHtml(request.getHtmlContent());
            email.setEmailType(Email.TYPE_SENT);
            email.setSentTime(LocalDateTime.now());
            email.setStatus(Email.STATUS_SENDING);
            email.setIsRead(true); // 发送的邮件默认已读

            // 处理附件
            if (attachments != null && !attachments.isEmpty()) {
                email.setHasAttachment(true);
                email.setAttachmentCount(attachments.size());
            }

            // 保存邮件
            save(email);

            // 保存附件
            List<EmailAttachment> savedAttachments = null;
            if (attachments != null && !attachments.isEmpty()) {
                try {
                    savedAttachments = attachmentService.saveAttachments(email.getId(), attachments);
                } catch (Exception e) {
                    log.error("保存附件失败", e);
                    throw new RuntimeException("保存附件失败: " + e.getMessage());
                }
            }

            // 发送邮件
            List<String> attachmentPaths = null;
            if (savedAttachments != null) {
                attachmentPaths = savedAttachments.stream()
                        .map(EmailAttachment::getFilePath)
                        .collect(Collectors.toList());
            }

            smtpService.sendEmail(
                fromAlias,
                request.getRecipients(),
                request.getCc(),
                request.getBcc(),
                request.getSubject(),
                request.getTextContent(),
                request.getHtmlContent(),
                attachmentPaths
            );

            // 更新邮件状态为已发送
            email.setStatus(Email.STATUS_SENT);
            updateById(email);

            log.info("邮件发送成功: from={}, to={}, subject={}", 
                    fromAlias.getAliasAddress(), request.getRecipients(), request.getSubject());

        } catch (MessagingException e) {
            log.error("邮件发送失败", e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("邮件处理失败", e);
            throw new RuntimeException("邮件处理失败: " + e.getMessage());
        }
    }

    @Override
    public IPage<EmailDto> getEmailsByUser(Long userId, Long aliasId, String type, String keyword, Page<Email> page) {
        LambdaQueryWrapper<Email> wrapper = new LambdaQueryWrapper<>();
        
        // 基本条件
        wrapper.eq(Email::getUserId, userId);
        
        if (aliasId != null) {
            wrapper.eq(Email::getAliasId, aliasId);
        }
        
        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(Email::getEmailType, type);
        }
        
        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                .like(Email::getSubject, keyword)
                .or()
                .like(Email::getSender, keyword)
                .or()
                .like(Email::getRecipient, keyword)
                .or()
                .like(Email::getContentText, keyword)
            );
        }
        
        // 按时间倒序
        wrapper.orderByDesc(Email::getReceivedTime);
        
        IPage<Email> emailPage = page(page, wrapper);
        
        // 转换为DTO
        IPage<EmailDto> result = new Page<>(emailPage.getCurrent(), emailPage.getSize(), emailPage.getTotal());
        List<EmailDto> emailDtos = emailPage.getRecords().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        result.setRecords(emailDtos);
        
        return result;
    }

    @Override
    public EmailDto getEmailById(Long emailId, Long userId) {
        Email email = getById(emailId);
        if (email == null || !email.getUserId().equals(userId)) {
            return null;
        }
        
        // 标记为已读
        if (!email.getIsRead()) {
            email.setIsRead(true);
            updateById(email);
        }
        
        return convertToDto(email);
    }

    @Override
    @Transactional
    public void deleteEmail(Long emailId, Long userId) {
        Email email = getById(emailId);
        if (email == null || !email.getUserId().equals(userId)) {
            throw new RuntimeException("邮件不存在或无权限删除");
        }
        
        // 删除附件
        attachmentService.deleteAttachmentsByEmailId(emailId);
        
        // 删除邮件记录
        removeById(emailId);
        
        log.info("邮件删除成功: emailId={}, userId={}", emailId, userId);
    }

    @Override
    public void markAsRead(Long emailId, Long userId) {
        Email email = getById(emailId);
        if (email == null || !email.getUserId().equals(userId)) {
            throw new RuntimeException("邮件不存在或无权限操作");
        }
        
        if (!email.getIsRead()) {
            email.setIsRead(true);
            updateById(email);
        }
    }

    @Override
    public void markAsImportant(Long emailId, Long userId, boolean important) {
        Email email = getById(emailId);
        if (email == null || !email.getUserId().equals(userId)) {
            throw new RuntimeException("邮件不存在或无权限操作");
        }
        
        email.setIsImportant(important);
        updateById(email);
    }

    @Override
    @Transactional
    public void syncEmailsForAlias(Long aliasId) {
        try {
            UserAlias alias = userAliasService.getById(aliasId);
            if (alias == null) {
                log.warn("别名不存在: {}", aliasId);
                return;
            }
            
            Domain domain = domainService.getById(alias.getDomainId());
            if (domain == null) {
                log.warn("域名配置不存在: {}", alias.getDomainId());
                return;
            }
            
            // 连接IMAP服务器
            imapService.connect(alias, domain);
            
            // 同步收件箱邮件
            List<Email> inboxEmails = imapService.syncEmails(alias, "INBOX");
            saveOrUpdateBatch(inboxEmails);
            
            // 同步发件箱邮件
            List<Email> sentEmails = imapService.syncEmails(alias, "Sent");
            saveOrUpdateBatch(sentEmails);
            
            // 断开连接
            imapService.disconnect();
            
            log.info("邮件同步完成: aliasId={}, inbox={}, sent={}", 
                    aliasId, inboxEmails.size(), sentEmails.size());
            
        } catch (Exception e) {
            log.error("邮件同步失败: aliasId={}", aliasId, e);
            throw new RuntimeException("邮件同步失败: " + e.getMessage());
        }
    }

    @Override
    public List<Email> getNewEmails(Long aliasId) {
        try {
            UserAlias alias = userAliasService.getById(aliasId);
            if (alias == null) {
                return List.of();
            }
            
            Domain domain = domainService.getById(alias.getDomainId());
            if (domain == null) {
                return List.of();
            }
            
            // 连接IMAP服务器
            imapService.connect(alias, domain);
            
            // 获取新邮件
            List<Email> newEmails = imapService.getNewEmails(alias, "INBOX");
            
            // 保存新邮件
            if (!newEmails.isEmpty()) {
                saveOrUpdateBatch(newEmails);
            }
            
            // 断开连接
            imapService.disconnect();
            
            return newEmails;
            
        } catch (Exception e) {
            log.error("获取新邮件失败: aliasId={}", aliasId, e);
            return List.of();
        }
    }

    @Override
    public EmailDto.EmailStats getEmailStats(Long userId, Long aliasId) {
        LambdaQueryWrapper<Email> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Email::getUserId, userId);
        
        if (aliasId != null) {
            wrapper.eq(Email::getAliasId, aliasId);
        }
        
        // 统计总数
        Long totalCount = count(wrapper);
        
        // 统计未读数
        wrapper.eq(Email::getIsRead, false);
        Long unreadCount = count(wrapper);
        
        // 统计重要邮件数
        wrapper.clear();
        wrapper.eq(Email::getUserId, userId)
               .eq(Email::getIsImportant, true);
        if (aliasId != null) {
            wrapper.eq(Email::getAliasId, aliasId);
        }
        Long importantCount = count(wrapper);
        
        // 统计今日邮件数
        wrapper.clear();
        wrapper.eq(Email::getUserId, userId)
               .ge(Email::getReceivedTime, LocalDateTime.now().toLocalDate().atStartOfDay());
        if (aliasId != null) {
            wrapper.eq(Email::getAliasId, aliasId);
        }
        Long todayCount = count(wrapper);
        
        return EmailDto.EmailStats.builder()
                .totalCount(totalCount)
                .unreadCount(unreadCount)
                .importantCount(importantCount)
                .todayCount(todayCount)
                .build();
    }

    @Override
    public List<EmailDto.UnreadCount> getUnreadCountByAlias(Long userId) {
        // 这里应该使用SQL查询优化，简化实现
        List<UserAlias> aliases = userAliasService.getAliasesByUserId(userId);
        
        return aliases.stream().map(alias -> {
            LambdaQueryWrapper<Email> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Email::getUserId, userId)
                   .eq(Email::getAliasId, alias.getId())
                   .eq(Email::getIsRead, false);
            Long unreadCount = count(wrapper);
            
            return EmailDto.UnreadCount.builder()
                    .aliasId(alias.getId())
                    .aliasAddress(alias.getAliasAddress())
                    .unreadCount(unreadCount)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public void forwardEmail(Long emailId, SendEmailRequest request, Long userId, Long aliasId) {
        // 获取原邮件
        Email originalEmail = getById(emailId);
        if (originalEmail == null || !originalEmail.getUserId().equals(userId)) {
            throw new RuntimeException("原邮件不存在或无权限操作");
        }
        
        // 构建转发内容
        String forwardContent = buildForwardContent(originalEmail, request.getTextContent());
        String forwardHtmlContent = buildForwardHtmlContent(originalEmail, request.getHtmlContent());
        
        // 设置转发邮件内容
        SendEmailRequest forwardRequest = new SendEmailRequest();
        forwardRequest.setRecipients(request.getRecipients());
        forwardRequest.setCc(request.getCc());
        forwardRequest.setBcc(request.getBcc());
        forwardRequest.setSubject("Fwd: " + originalEmail.getSubject());
        forwardRequest.setTextContent(forwardContent);
        forwardRequest.setHtmlContent(forwardHtmlContent);
        
        // 发送转发邮件
        sendEmail(forwardRequest, userId, aliasId, null);
    }

    @Override
    public void replyEmail(Long emailId, SendEmailRequest request, Long userId, Long aliasId, boolean replyAll) {
        // 获取原邮件
        Email originalEmail = getById(emailId);
        if (originalEmail == null || !originalEmail.getUserId().equals(userId)) {
            throw new RuntimeException("原邮件不存在或无权限操作");
        }
        
        // 构建回复内容
        String replyContent = buildReplyContent(originalEmail, request.getTextContent());
        String replyHtmlContent = buildReplyHtmlContent(originalEmail, request.getHtmlContent());
        
        // 设置回复邮件内容
        SendEmailRequest replyRequest = new SendEmailRequest();
        replyRequest.getRecipients().add(originalEmail.getSender());
        
        if (replyAll) {
            // 全部回复时添加原邮件的其他收件人
            if (originalEmail.getCc() != null && !originalEmail.getCc().isEmpty()) {
                String[] ccAddresses = originalEmail.getCc().split(",");
                for (String cc : ccAddresses) {
                    replyRequest.getCc().add(cc.trim());
                }
            }
        }
        
        replyRequest.setSubject("Re: " + originalEmail.getSubject());
        replyRequest.setTextContent(replyContent);
        replyRequest.setHtmlContent(replyHtmlContent);
        
        // 发送回复邮件
        sendEmail(replyRequest, userId, aliasId, null);
    }

    /**
     * 转换Email实体为EmailDto
     */
    private EmailDto convertToDto(Email email) {
        EmailDto dto = EmailDto.builder()
                .id(email.getId())
                .userId(email.getUserId())
                .aliasId(email.getAliasId())
                .messageId(email.getMessageId())
                .messageUid(email.getMessageUid())
                .sender(email.getSender())
                .recipient(email.getRecipient())
                .cc(email.getCc())
                .bcc(email.getBcc())
                .subject(email.getSubject())
                .contentText(email.getContentText())
                .contentHtml(email.getContentHtml())
                .emailType(email.getEmailType())
                .sentTime(email.getSentTime())
                .receivedTime(email.getReceivedTime())
                .isRead(email.getIsRead())
                .isImportant(email.getIsImportant())
                .hasAttachment(email.getHasAttachment())
                .attachmentCount(email.getAttachmentCount())
                .sizeBytes(email.getSizeBytes())
                .status(email.getStatus())
                .build();
        
        // 获取附件信息
        if (email.getHasAttachment()) {
            List<EmailAttachment> attachments = attachmentService.getAttachmentsByEmailId(email.getId());
            dto.setAttachments(attachments);
        }
        
        return dto;
    }

    /**
     * 构建转发文本内容
     */
    private String buildForwardContent(Email originalEmail, String userContent) {
        StringBuilder sb = new StringBuilder();
        if (userContent != null && !userContent.trim().isEmpty()) {
            sb.append(userContent).append("\n\n");
        }
        
        sb.append("---------- Forwarded message ----------\n");
        sb.append("From: ").append(originalEmail.getSender()).append("\n");
        sb.append("Date: ").append(originalEmail.getSentTime()).append("\n");
        sb.append("Subject: ").append(originalEmail.getSubject()).append("\n");
        sb.append("To: ").append(originalEmail.getRecipient()).append("\n\n");
        sb.append(originalEmail.getContentText());
        
        return sb.toString();
    }

    /**
     * 构建转发HTML内容
     */
    private String buildForwardHtmlContent(Email originalEmail, String userHtmlContent) {
        StringBuilder sb = new StringBuilder();
        if (userHtmlContent != null && !userHtmlContent.trim().isEmpty()) {
            sb.append(userHtmlContent).append("<br><br>");
        }
        
        sb.append("<br>---------- Forwarded message ----------<br>");
        sb.append("<b>From:</b> ").append(originalEmail.getSender()).append("<br>");
        sb.append("<b>Date:</b> ").append(originalEmail.getSentTime()).append("<br>");
        sb.append("<b>Subject:</b> ").append(originalEmail.getSubject()).append("<br>");
        sb.append("<b>To:</b> ").append(originalEmail.getRecipient()).append("<br><br>");
        sb.append(originalEmail.getContentHtml());
        
        return sb.toString();
    }

    /**
     * 构建回复文本内容
     */
    private String buildReplyContent(Email originalEmail, String userContent) {
        StringBuilder sb = new StringBuilder();
        if (userContent != null && !userContent.trim().isEmpty()) {
            sb.append(userContent).append("\n\n");
        }
        
        sb.append("On ").append(originalEmail.getSentTime())
          .append(", ").append(originalEmail.getSender())
          .append(" wrote:\n\n");
        
        // 添加引用标记
        String[] lines = originalEmail.getContentText().split("\n");
        for (String line : lines) {
            sb.append("> ").append(line).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * 构建回复HTML内容
     */
    private String buildReplyHtmlContent(Email originalEmail, String userHtmlContent) {
        StringBuilder sb = new StringBuilder();
        if (userHtmlContent != null && !userHtmlContent.trim().isEmpty()) {
            sb.append(userHtmlContent).append("<br><br>");
        }
        
        sb.append("On ").append(originalEmail.getSentTime())
          .append(", ").append(originalEmail.getSender())
          .append(" wrote:<br><br>");
        
        sb.append("<blockquote style=\"margin:0 0 0 .8ex;border-left:1px #ccc solid;padding-left:1ex;\">");
        sb.append(originalEmail.getContentHtml());
        sb.append("</blockquote>");
        
        return sb.toString();
    }
}