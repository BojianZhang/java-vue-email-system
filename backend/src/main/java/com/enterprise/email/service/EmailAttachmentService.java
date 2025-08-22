package com.enterprise.email.service;

import com.enterprise.email.entity.EmailAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 邮件附件服务接口
 */
public interface EmailAttachmentService {

    /**
     * 保存邮件附件
     */
    List<EmailAttachment> saveAttachments(Long emailId, List<MultipartFile> files) throws IOException;

    /**
     * 根据邮件ID获取附件列表
     */
    List<EmailAttachment> getAttachmentsByEmailId(Long emailId);

    /**
     * 根据附件ID获取附件
     */
    EmailAttachment getAttachmentById(Long attachmentId);

    /**
     * 下载附件
     */
    byte[] downloadAttachment(Long attachmentId, Long userId) throws IOException;

    /**
     * 删除附件
     */
    void deleteAttachment(Long attachmentId, Long userId);

    /**
     * 删除邮件的所有附件
     */
    void deleteAttachmentsByEmailId(Long emailId);

    /**
     * 获取附件的下载URL
     */
    String getAttachmentDownloadUrl(Long attachmentId);

    /**
     * 检查文件类型是否允许
     */
    boolean isAllowedFileType(String fileName, String contentType);

    /**
     * 获取文件的MD5值
     */
    String calculateMD5(MultipartFile file) throws IOException;

    /**
     * 清理过期的临时附件
     */
    void cleanExpiredAttachments();
}