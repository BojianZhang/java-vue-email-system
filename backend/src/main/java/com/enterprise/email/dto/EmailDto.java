package com.enterprise.email.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDto {

    private Long id;
    private Long userId;
    private Long aliasId;
    private String messageUid;
    private String messageId;
    
    @NotBlank(message = "发件人不能为空")
    @Email(message = "发件人邮箱格式不正确")
    private String sender;
    
    @NotBlank(message = "收件人不能为空")
    private String recipient;
    
    private String cc;
    private String bcc;
    
    @Size(max = 500, message = "邮件主题不能超过500个字符")
    private String subject;
    
    private String contentText;
    private String contentHtml;
    private Long sizeBytes;
    private String emailType;
    private Boolean isRead;
    private Boolean isImportant;
    private Boolean hasAttachment;
    private Integer attachmentCount;
    private LocalDateTime sentTime;
    private LocalDateTime receivedTime;
    private String status;
    
    // 关联信息
    private String aliasAddress;
    private String username;
    private List<AttachmentDto> attachments;
    
    /**
     * 邮件统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailStats {
        private Long totalEmails;
        private Long unreadEmails;
        private Long inboxEmails;
        private Long sentEmails;
        private Long draftEmails;
        private Long trashEmails;
        private Long importantEmails;
        private Long todayEmails;
        private Long storageUsed; // 字节
    }
    
    /**
     * 未读邮件数统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnreadCount {
        private Long aliasId;
        private String aliasAddress;
        private Long unreadCount;
    }
    
    /**
     * 附件信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentDto {
        private Long id;
        private String fileName;
        private String originalName;
        private String contentType;
        private Long fileSize;
        private String filePath;
        private String downloadUrl;
    }
}