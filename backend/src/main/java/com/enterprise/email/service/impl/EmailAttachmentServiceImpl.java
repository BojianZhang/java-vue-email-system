package com.enterprise.email.service.impl;

import com.enterprise.email.entity.EmailAttachment;
import com.enterprise.email.mapper.EmailAttachmentMapper;
import com.enterprise.email.service.EmailAttachmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 邮件附件服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAttachmentServiceImpl implements EmailAttachmentService {

    private final EmailAttachmentMapper attachmentMapper;

    @Value("${email.attachment.path:/data/email/attachments}")
    private String attachmentBasePath;

    @Value("${email.attachment.max-size:50MB}")
    private String maxFileSize;

    @Value("${email.attachment.allowed-types:jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,zip,rar}")
    private String allowedFileTypes;

    // 允许的文件MIME类型
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "application/msword", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel", 
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint", 
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain", "application/zip", "application/x-rar-compressed"
    );

    @Override
    public List<EmailAttachment> saveAttachments(Long emailId, List<MultipartFile> files) throws IOException {
        List<EmailAttachment> attachments = new ArrayList<>();
        
        if (files == null || files.isEmpty()) {
            return attachments;
        }
        
        // 确保附件目录存在
        Path attachmentDir = Paths.get(attachmentBasePath);
        if (!Files.exists(attachmentDir)) {
            Files.createDirectories(attachmentDir);
        }
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    EmailAttachment attachment = saveAttachment(emailId, file);
                    if (attachment != null) {
                        attachments.add(attachment);
                    }
                } catch (Exception e) {
                    log.error("保存附件失败: {}, error: {}", file.getOriginalFilename(), e.getMessage(), e);
                    throw new IOException("保存附件失败: " + file.getOriginalFilename(), e);
                }
            }
        }
        
        return attachments;
    }

    @Override
    public List<EmailAttachment> getAttachmentsByEmailId(Long emailId) {
        LambdaQueryWrapper<EmailAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmailAttachment::getEmailId, emailId)
               .orderByAsc(EmailAttachment::getCreatedTime);
        return attachmentMapper.selectList(wrapper);
    }

    @Override
    public EmailAttachment getAttachmentById(Long attachmentId) {
        return attachmentMapper.selectById(attachmentId);
    }

    @Override
    public byte[] downloadAttachment(Long attachmentId, Long userId) throws IOException {
        EmailAttachment attachment = getAttachmentById(attachmentId);
        if (attachment == null) {
            throw new IOException("附件不存在");
        }
        
        // 这里应该添加权限检查，确保用户有权限下载该附件
        // 简化实现，实际应该检查邮件的所有者
        
        Path filePath = Paths.get(attachment.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IOException("附件文件不存在: " + attachment.getFileName());
        }
        
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("读取附件文件失败: {}, error: {}", attachment.getFileName(), e.getMessage());
            throw new IOException("读取附件失败", e);
        }
    }

    @Override
    public void deleteAttachment(Long attachmentId, Long userId) {
        EmailAttachment attachment = getAttachmentById(attachmentId);
        if (attachment == null) {
            log.warn("要删除的附件不存在: {}", attachmentId);
            return;
        }
        
        // 删除数据库记录
        attachmentMapper.deleteById(attachmentId);
        
        // 删除文件
        try {
            Path filePath = Paths.get(attachment.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("附件文件删除成功: {}", attachment.getFileName());
            }
        } catch (IOException e) {
            log.error("删除附件文件失败: {}, error: {}", attachment.getFileName(), e.getMessage());
        }
    }

    @Override
    public void deleteAttachmentsByEmailId(Long emailId) {
        List<EmailAttachment> attachments = getAttachmentsByEmailId(emailId);
        
        for (EmailAttachment attachment : attachments) {
            try {
                Path filePath = Paths.get(attachment.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            } catch (IOException e) {
                log.error("删除附件文件失败: {}, error: {}", attachment.getFileName(), e.getMessage());
            }
        }
        
        // 删除数据库记录
        LambdaQueryWrapper<EmailAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmailAttachment::getEmailId, emailId);
        attachmentMapper.delete(wrapper);
        
        log.info("邮件附件删除完成: emailId={}, count={}", emailId, attachments.size());
    }

    @Override
    public String getAttachmentDownloadUrl(Long attachmentId) {
        return "/api/email/attachments/" + attachmentId + "/download";
    }

    @Override
    public boolean isAllowedFileType(String fileName, String contentType) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        
        // 检查文件扩展名
        String extension = getFileExtension(fileName).toLowerCase();
        Set<String> allowedExtensions = Set.of(allowedFileTypes.split(","));
        
        if (!allowedExtensions.contains(extension)) {
            log.warn("不允许的文件扩展名: {}", extension);
            return false;
        }
        
        // 检查MIME类型
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            log.warn("不允许的文件MIME类型: {}", contentType);
            return false;
        }
        
        return true;
    }

    @Override
    public String calculateMD5(MultipartFile file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            try (InputStream inputStream = file.getInputStream()) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
            
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用", e);
            throw new IOException("计算文件MD5失败", e);
        }
    }

    @Override
    public void cleanExpiredAttachments() {
        try {
            // 删除30天前的临时附件（状态为TEMP的附件）
            LocalDateTime expiredTime = LocalDateTime.now().minusDays(30);
            
            LambdaQueryWrapper<EmailAttachment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(EmailAttachment::getStatus, EmailAttachment.STATUS_TEMP)
                   .lt(EmailAttachment::getCreatedTime, expiredTime);
            
            List<EmailAttachment> expiredAttachments = attachmentMapper.selectList(wrapper);
            
            for (EmailAttachment attachment : expiredAttachments) {
                try {
                    // 删除文件
                    Path filePath = Paths.get(attachment.getFilePath());
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                    }
                    
                    // 删除数据库记录
                    attachmentMapper.deleteById(attachment.getId());
                    
                } catch (Exception e) {
                    log.error("清理过期附件失败: {}, error: {}", 
                             attachment.getFileName(), e.getMessage());
                }
            }
            
            log.info("清理过期附件完成: count={}", expiredAttachments.size());
            
        } catch (Exception e) {
            log.error("清理过期附件任务失败", e);
        }
    }

    /**
     * 保存单个附件
     */
    private EmailAttachment saveAttachment(Long emailId, MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IOException("无效的文件名");
        }
        
        // 验证文件类型
        if (!isAllowedFileType(originalFilename, file.getContentType())) {
            throw new IOException("不支持的文件类型: " + originalFilename);
        }
        
        // 生成唯一文件名
        String uniqueFileName = generateUniqueFileName(originalFilename);
        String relativePath = generateRelativePath(uniqueFileName);
        Path fullPath = Paths.get(attachmentBasePath, relativePath);
        
        // 确保目录存在
        Files.createDirectories(fullPath.getParent());
        
        // 保存文件
        try (InputStream inputStream = file.getInputStream();
             OutputStream outputStream = Files.newOutputStream(fullPath)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        // 计算文件MD5
        String md5 = calculateMD5(file);
        
        // 创建附件记录
        EmailAttachment attachment = new EmailAttachment();
        attachment.setEmailId(emailId);
        attachment.setFileName(originalFilename);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setFilePath(fullPath.toString());
        attachment.setMd5Hash(md5);
        attachment.setStatus(EmailAttachment.STATUS_NORMAL);
        attachment.setCreatedTime(LocalDateTime.now());
        
        // 保存到数据库
        attachmentMapper.insert(attachment);
        
        log.info("附件保存成功: filename={}, size={}, path={}", 
                originalFilename, file.getSize(), fullPath);
        
        return attachment;
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String baseName = getBaseName(originalFilename);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        return baseName + "_" + timestamp + "_" + uuid + "." + extension;
    }

    /**
     * 生成相对路径（按年月日分目录存储）
     */
    private String generateRelativePath(String fileName) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%d/%02d/%02d/%s", 
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), fileName);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * 获取文件基本名称（不含扩展名）
     */
    private String getBaseName(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
}