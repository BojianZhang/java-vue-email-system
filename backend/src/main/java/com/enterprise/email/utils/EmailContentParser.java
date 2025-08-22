package com.enterprise.email.utils;

import com.enterprise.email.entity.EmailAttachment;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 邮件内容解析工具类
 */
@Slf4j
public class EmailContentParser {

    /**
     * 解析邮件内容
     */
    public static ParseResult parseContent(Message message) throws MessagingException, IOException {
        ParseResult result = new ParseResult();
        
        Object content = message.getContent();
        
        if (content instanceof String) {
            // 纯文本邮件
            String contentType = message.getContentType();
            if (contentType.toLowerCase().contains("html")) {
                result.setHtmlContent((String) content);
            } else {
                result.setTextContent((String) content);
            }
        } else if (content instanceof MimeMultipart) {
            // 多部分邮件
            parseMultipart((MimeMultipart) content, result);
        }
        
        return result;
    }

    /**
     * 解析多部分邮件内容
     */
    private static void parseMultipart(MimeMultipart multipart, ParseResult result) 
            throws MessagingException, IOException {
        
        int count = multipart.getCount();
        
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            
            if (bodyPart instanceof MimeBodyPart) {
                MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;
                parseBodyPart(mimeBodyPart, result);
            }
        }
    }

    /**
     * 解析邮件体部分
     */
    private static void parseBodyPart(MimeBodyPart bodyPart, ParseResult result) 
            throws MessagingException, IOException {
        
        String disposition = bodyPart.getDisposition();
        String contentType = bodyPart.getContentType();
        
        if (Part.ATTACHMENT.equalsIgnoreCase(disposition) || 
            Part.INLINE.equalsIgnoreCase(disposition)) {
            // 附件处理
            parseAttachment(bodyPart, result);
        } else if (contentType.toLowerCase().contains("text/plain")) {
            // 纯文本内容
            Object content = bodyPart.getContent();
            if (content instanceof String) {
                result.setTextContent((String) content);
            }
        } else if (contentType.toLowerCase().contains("text/html")) {
            // HTML内容
            Object content = bodyPart.getContent();
            if (content instanceof String) {
                result.setHtmlContent((String) content);
            }
        } else if (contentType.toLowerCase().contains("multipart")) {
            // 嵌套的多部分内容
            Object content = bodyPart.getContent();
            if (content instanceof MimeMultipart) {
                parseMultipart((MimeMultipart) content, result);
            }
        }
    }

    /**
     * 解析附件
     */
    private static void parseAttachment(MimeBodyPart bodyPart, ParseResult result) 
            throws MessagingException, IOException {
        
        String fileName = bodyPart.getFileName();
        if (fileName != null) {
            try {
                fileName = MimeUtility.decodeText(fileName);
            } catch (UnsupportedEncodingException e) {
                log.warn("解码附件文件名失败: {}", fileName);
            }
            
            AttachmentInfo attachment = new AttachmentInfo();
            attachment.setFileName(fileName);
            attachment.setContentType(bodyPart.getContentType());
            attachment.setSize(bodyPart.getSize());
            attachment.setContentId(bodyPart.getContentID());
            
            // 判断是否为内嵌图片
            String disposition = bodyPart.getDisposition();
            attachment.setInline(Part.INLINE.equalsIgnoreCase(disposition));
            
            result.getAttachments().add(attachment);
        }
    }

    /**
     * 保存附件到文件系统
     */
    public static String saveAttachment(MimeBodyPart bodyPart, String savePath) 
            throws MessagingException, IOException {
        
        String fileName = bodyPart.getFileName();
        if (fileName != null) {
            try {
                fileName = MimeUtility.decodeText(fileName);
            } catch (UnsupportedEncodingException e) {
                log.warn("解码附件文件名失败: {}", fileName);
            }
            
            // 确保保存目录存在
            File saveDir = new File(savePath);
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }
            
            // 生成唯一文件名
            String uniqueFileName = generateUniqueFileName(fileName);
            String fullPath = savePath + File.separator + uniqueFileName;
            
            // 保存文件
            try (InputStream inputStream = bodyPart.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(fullPath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            log.info("附件保存成功: {}", fullPath);
            return fullPath;
        }
        
        return null;
    }

    /**
     * 生成唯一文件名
     */
    private static String generateUniqueFileName(String originalName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = "";
        
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
            originalName = originalName.substring(0, dotIndex);
        }
        
        return originalName + "_" + timestamp + extension;
    }

    /**
     * 解析结果类
     */
    @Data
    public static class ParseResult {
        private String textContent;
        private String htmlContent;
        private List<AttachmentInfo> attachments = new ArrayList<>();
        
        public boolean hasAttachments() {
            return !attachments.isEmpty();
        }
        
        public int getAttachmentCount() {
            return attachments.size();
        }
    }

    /**
     * 附件信息类
     */
    @Data
    public static class AttachmentInfo {
        private String fileName;
        private String contentType;
        private int size;
        private String contentId;
        private boolean inline;
        private String filePath;
    }
}