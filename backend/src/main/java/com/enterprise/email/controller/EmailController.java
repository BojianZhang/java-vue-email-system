package com.enterprise.email.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.dto.EmailDto;
import com.enterprise.email.dto.SendEmailRequest;
import com.enterprise.email.entity.Email;
import com.enterprise.email.entity.EmailAttachment;
import com.enterprise.email.service.EmailAttachmentService;
import com.enterprise.email.service.EmailService;
import com.enterprise.email.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 邮件管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Validated
@Tag(name = "邮件管理", description = "邮件收发、管理相关接口")
public class EmailController {

    private final EmailService emailService;
    private final EmailAttachmentService attachmentService;

    @Operation(summary = "发送邮件")
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendEmail(
            @Valid @RequestBody SendEmailRequest request,
            @Parameter(description = "别名ID") @RequestParam Long aliasId,
            @Parameter(description = "附件文件") @RequestParam(required = false) List<MultipartFile> attachments) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            emailService.sendEmail(request, userId, aliasId, attachments);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "邮件发送成功"
            ));
            
        } catch (Exception e) {
            log.error("发送邮件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "发送邮件失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "获取邮件列表")
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getEmails(
            @Parameter(description = "别名ID") @RequestParam(required = false) Long aliasId,
            @Parameter(description = "邮件类型") @RequestParam(required = false) String type,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            Page<Email> pageParam = new Page<>(page, size);
            
            var result = emailService.getEmailsByUser(userId, aliasId, type, keyword, pageParam);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result
            ));
            
        } catch (Exception e) {
            log.error("获取邮件列表失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取邮件列表失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "获取邮件详情")
    @GetMapping("/{emailId}")
    public ResponseEntity<Map<String, Object>> getEmailDetail(
            @Parameter(description = "邮件ID") @PathVariable Long emailId) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            EmailDto email = emailService.getEmailById(emailId, userId);
            
            if (email == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", email
            ));
            
        } catch (Exception e) {
            log.error("获取邮件详情失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取邮件详情失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "删除邮件")
    @DeleteMapping("/{emailId}")
    public ResponseEntity<Map<String, Object>> deleteEmail(
            @Parameter(description = "邮件ID") @PathVariable Long emailId) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            emailService.deleteEmail(emailId, userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "邮件删除成功"
            ));
            
        } catch (Exception e) {
            log.error("删除邮件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "删除邮件失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "标记邮件为已读")
    @PutMapping("/{emailId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @Parameter(description = "邮件ID") @PathVariable Long emailId) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            emailService.markAsRead(emailId, userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "标记成功"
            ));
            
        } catch (Exception e) {
            log.error("标记邮件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "标记邮件失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "标记邮件为重要")
    @PutMapping("/{emailId}/important")
    public ResponseEntity<Map<String, Object>> markAsImportant(
            @Parameter(description = "邮件ID") @PathVariable Long emailId,
            @Parameter(description = "是否重要") @RequestParam boolean important) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            emailService.markAsImportant(emailId, userId, important);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "标记成功"
            ));
            
        } catch (Exception e) {
            log.error("标记邮件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "标记邮件失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "同步邮件")
    @PostMapping("/sync/{aliasId}")
    public ResponseEntity<Map<String, Object>> syncEmails(
            @Parameter(description = "别名ID") @PathVariable Long aliasId) {
        
        try {
            emailService.syncEmailsForAlias(aliasId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "邮件同步成功"
            ));
            
        } catch (Exception e) {
            log.error("同步邮件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "同步邮件失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "获取新邮件")
    @GetMapping("/new/{aliasId}")
    public ResponseEntity<Map<String, Object>> getNewEmails(
            @Parameter(description = "别名ID") @PathVariable Long aliasId) {
        
        try {
            List<Email> newEmails = emailService.getNewEmails(aliasId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", newEmails,
                "count", newEmails.size()
            ));
            
        } catch (Exception e) {
            log.error("获取新邮件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取新邮件失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "获取邮件统计信息")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEmailStats(
            @Parameter(description = "别名ID") @RequestParam(required = false) Long aliasId) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            EmailDto.EmailStats stats = emailService.getEmailStats(userId, aliasId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));
            
        } catch (Exception e) {
            log.error("获取邮件统计失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取邮件统计失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "获取各别名未读邮件数")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCountByAlias() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            List<EmailDto.UnreadCount> unreadCounts = emailService.getUnreadCountByAlias(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", unreadCounts
            ));
            
        } catch (Exception e) {
            log.error("获取未读邮件数失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取未读邮件数失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "转发邮件")
    @PostMapping("/{emailId}/forward")
    public ResponseEntity<Map<String, Object>> forwardEmail(
            @Parameter(description = "邮件ID") @PathVariable Long emailId,
            @Valid @RequestBody SendEmailRequest request,
            @Parameter(description = "别名ID") @RequestParam Long aliasId) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            emailService.forwardEmail(emailId, request, userId, aliasId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "邮件转发成功"
            ));
            
        } catch (Exception e) {
            log.error("转发邮件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "转发邮件失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "回复邮件")
    @PostMapping("/{emailId}/reply")
    public ResponseEntity<Map<String, Object>> replyEmail(
            @Parameter(description = "邮件ID") @PathVariable Long emailId,
            @Valid @RequestBody SendEmailRequest request,
            @Parameter(description = "别名ID") @RequestParam Long aliasId,
            @Parameter(description = "是否全部回复") @RequestParam(defaultValue = "false") boolean replyAll) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            emailService.replyEmail(emailId, request, userId, aliasId, replyAll);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "邮件回复成功"
            ));
            
        } catch (Exception e) {
            log.error("回复邮件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "回复邮件失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "获取邮件附件列表")
    @GetMapping("/{emailId}/attachments")
    public ResponseEntity<Map<String, Object>> getEmailAttachments(
            @Parameter(description = "邮件ID") @PathVariable Long emailId) {
        
        try {
            List<EmailAttachment> attachments = attachmentService.getAttachmentsByEmailId(emailId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", attachments
            ));
            
        } catch (Exception e) {
            log.error("获取邮件附件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取邮件附件失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "下载附件")
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @Parameter(description = "附件ID") @PathVariable Long attachmentId) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            EmailAttachment attachment = attachmentService.getAttachmentById(attachmentId);
            
            if (attachment == null) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] data = attachmentService.downloadAttachment(attachmentId, userId);
            ByteArrayResource resource = new ByteArrayResource(data);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(data.length)
                    .body(resource);
                    
        } catch (IOException e) {
            log.error("下载附件失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Map<String, Object>> deleteAttachment(
            @Parameter(description = "附件ID") @PathVariable Long attachmentId) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            attachmentService.deleteAttachment(attachmentId, userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "附件删除成功"
            ));
            
        } catch (Exception e) {
            log.error("删除附件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "删除附件失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "上传临时附件")
    @PostMapping("/attachments/upload")
    public ResponseEntity<Map<String, Object>> uploadTempAttachment(
            @Parameter(description = "附件文件") @RequestParam("file") MultipartFile file) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "文件不能为空"
                ));
            }
            
            // 验证文件类型
            if (!attachmentService.isAllowedFileType(file.getOriginalFilename(), file.getContentType())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "不支持的文件类型"
                ));
            }
            
            // 保存临时附件
            List<EmailAttachment> attachments = attachmentService.saveAttachments(null, List.of(file));
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", attachments.get(0),
                "message", "文件上传成功"
            ));
            
        } catch (IOException e) {
            log.error("上传附件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "上传附件失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "批量标记邮件")
    @PutMapping("/batch/mark")
    public ResponseEntity<Map<String, Object>> batchMarkEmails(
            @Parameter(description = "邮件ID列表") @RequestBody @NotNull List<Long> emailIds,
            @Parameter(description = "操作类型") @RequestParam String action,
            @Parameter(description = "操作值") @RequestParam(required = false) String value) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            
            for (Long emailId : emailIds) {
                switch (action) {
                    case "read":
                        emailService.markAsRead(emailId, userId);
                        break;
                    case "important":
                        boolean important = Boolean.parseBoolean(value);
                        emailService.markAsImportant(emailId, userId, important);
                        break;
                    case "delete":
                        emailService.deleteEmail(emailId, userId);
                        break;
                    default:
                        return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "不支持的操作类型"
                        ));
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "批量操作成功"
            ));
            
        } catch (Exception e) {
            log.error("批量操作邮件失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "批量操作失败: " + e.getMessage()
            ));
        }
    }
}