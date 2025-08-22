package com.enterprise.email.service.impl;

import com.enterprise.email.entity.Domain;
import com.enterprise.email.entity.EmailQueue;
import com.enterprise.email.entity.SmtpConfig;
import com.enterprise.email.entity.UserAlias;
import com.enterprise.email.mapper.EmailQueueMapper;
import com.enterprise.email.mapper.SmtpConfigMapper;
import com.enterprise.email.service.SmtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * SMTP邮件发送服务实现类
 * 支持Haraka SMTP服务器集成和邮件队列处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpServiceImpl implements SmtpService {

    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    private final SmtpConfigMapper smtpConfigMapper;
    private final EmailQueueMapper emailQueueMapper;

    @Value("${app.email.smtp.host:localhost}")
    private String defaultSmtpHost;

    @Value("${app.email.smtp.port:25}")
    private Integer defaultSmtpPort;

    @Value("${app.email.smtp.username:}")
    private String defaultSmtpUsername;

    @Value("${app.email.smtp.password:}")
    private String defaultSmtpPassword;

    @Value("${app.email.smtp.use-ssl:false}")
    private Boolean defaultUseSsl;

    @Value("${app.email.smtp.use-starttls:true}")
    private Boolean defaultUseStarttls;

    // 缓存不同域名的JavaMailSender
    private final Map<String, JavaMailSender> mailSenderCache = new ConcurrentHashMap<>();

    @Override
    public void sendEmail(UserAlias fromAlias, List<String> recipients, List<String> cc, List<String> bcc,
                         String subject, String textContent, String htmlContent, 
                         List<String> attachmentPaths) throws MessagingException {
        
        log.info("发送邮件: from={}, to={}, subject={}", fromAlias.getAliasAddress(), recipients, subject);
        
        // 创建邮件队列记录
        EmailQueue emailQueue = new EmailQueue();
        emailQueue.setFromEmail(fromAlias.getAliasAddress());
        emailQueue.setToEmail(String.join(",", recipients));
        if (cc != null && !cc.isEmpty()) {
            emailQueue.setCcEmail(String.join(",", cc));
        }
        if (bcc != null && !bcc.isEmpty()) {
            emailQueue.setBccEmail(String.join(",", bcc));
        }
        emailQueue.setSubject(subject);
        emailQueue.setContent(htmlContent != null ? htmlContent : textContent);
        emailQueue.setContentType(htmlContent != null ? "HTML" : "TEXT");
        emailQueue.setPriority("NORMAL");
        emailQueue.setStatus("PENDING");
        emailQueue.setRetryCount(0);
        emailQueue.setMaxRetries(3);
        emailQueue.setDomain(extractDomain(fromAlias.getAliasAddress()));
        emailQueue.setUserId(fromAlias.getUserId());
        
        // 设置附件信息
        if (attachmentPaths != null && !attachmentPaths.isEmpty()) {
            emailQueue.setAttachmentsJson(String.join(",", attachmentPaths));
        }
        
        // 保存到队列
        emailQueueMapper.insert(emailQueue);
        
        // 立即发送
        sendEmailFromQueue(emailQueue);
    }

    @Override
    public void sendSimpleEmail(String from, String to, String subject, String content) throws MessagingException {
        EmailQueue emailQueue = new EmailQueue();
        emailQueue.setFromEmail(from);
        emailQueue.setToEmail(to);
        emailQueue.setSubject(subject);
        emailQueue.setContent(content);
        emailQueue.setContentType("TEXT");
        emailQueue.setPriority("NORMAL");
        emailQueue.setStatus("PENDING");
        emailQueue.setRetryCount(0);
        emailQueue.setMaxRetries(3);
        emailQueue.setDomain(extractDomain(from));
        
        emailQueueMapper.insert(emailQueue);
        sendEmailFromQueue(emailQueue);
    }

    @Override
    public void sendHtmlEmail(String from, String to, String subject, String htmlContent) throws MessagingException {
        EmailQueue emailQueue = new EmailQueue();
        emailQueue.setFromEmail(from);
        emailQueue.setToEmail(to);
        emailQueue.setSubject(subject);
        emailQueue.setContent(htmlContent);
        emailQueue.setContentType("HTML");
        emailQueue.setPriority("NORMAL");
        emailQueue.setStatus("PENDING");
        emailQueue.setRetryCount(0);
        emailQueue.setMaxRetries(3);
        emailQueue.setDomain(extractDomain(from));
        
        emailQueueMapper.insert(emailQueue);
        sendEmailFromQueue(emailQueue);
    }

    @Override
    public void sendEmailWithAttachments(String from, List<String> recipients, String subject, 
                                       String content, List<String> attachmentPaths) throws MessagingException {
        EmailQueue emailQueue = new EmailQueue();
        emailQueue.setFromEmail(from);
        emailQueue.setToEmail(String.join(",", recipients));
        emailQueue.setSubject(subject);
        emailQueue.setContent(content);
        emailQueue.setContentType("TEXT");
        emailQueue.setPriority("NORMAL");
        emailQueue.setStatus("PENDING");
        emailQueue.setRetryCount(0);
        emailQueue.setMaxRetries(3);
        emailQueue.setDomain(extractDomain(from));
        emailQueue.setAttachmentsJson(String.join(",", attachmentPaths));
        
        emailQueueMapper.insert(emailQueue);
        sendEmailFromQueue(emailQueue);
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendEmailAsync(EmailQueue emailQueue) {
        try {
            sendEmailFromQueue(emailQueue);
        } catch (Exception e) {
            log.error("异步发送邮件失败: {}", e.getMessage(), e);
            handleEmailFailure(emailQueue, e.getMessage());
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendBatchEmails(List<EmailQueue> emailQueues) {
        log.info("批量发送邮件: {} 封", emailQueues.size());
        
        for (EmailQueue emailQueue : emailQueues) {
            try {
                sendEmailFromQueue(emailQueue);
                Thread.sleep(100); // 添加延迟避免SMTP服务器压力
            } catch (Exception e) {
                log.error("批量发送邮件失败: {}", e.getMessage(), e);
                handleEmailFailure(emailQueue, e.getMessage());
            }
        }
    }

    @Override
    public boolean sendEmail(EmailQueue emailQueue) {
        try {
            sendEmailFromQueue(emailQueue);
            return true;
        } catch (Exception e) {
            log.error("发送邮件失败: {}", e.getMessage(), e);
            handleEmailFailure(emailQueue, e.getMessage());
            return false;
        }
    }

    @Override
    public MimeMessage createMimeMessage(UserAlias fromAlias, Domain domain) throws MessagingException {
        JavaMailSender mailSender = getMailSender(domain.getDomainName());
        return mailSender.createMimeMessage();
    }

    @Override
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    @Override
    public List<String> parseRecipients(String recipients) {
        if (recipients == null || recipients.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> result = new ArrayList<>();
        String[] emails = recipients.split("[,;]");
        
        for (String email : emails) {
            String trimmed = email.trim();
            if (isValidEmail(trimmed)) {
                result.add(trimmed);
            } else {
                log.warn("无效的邮箱地址: {}", trimmed);
            }
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getSmtpConfig(Domain domain) {
        SmtpConfig config = getSmtpConfigByDomain(domain.getDomainName());
        if (config != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("host", config.getSmtpHost());
            result.put("port", config.getSmtpPort());
            result.put("useSsl", config.getUseSsl());
            result.put("useStarttls", config.getUseStarttls());
            result.put("authType", config.getAuthType());
            result.put("enabled", config.getEnabled());
            result.put("status", config.getStatus());
            return result;
        }
        
        // 默认配置
        Map<String, Object> config2 = new HashMap<>();
        config2.put("host", domain.getSmtpHost());
        config2.put("port", domain.getSmtpPort());
        config2.put("ssl", domain.getSmtpSsl());
        config2.put("auth", true);
        return config2;
    }

    /**
     * 获取或创建JavaMailSender
     */
    private JavaMailSender getOrCreateMailSender(Domain domain) {
        String key = domain.getDomainName();
        
        if (!mailSenderCache.containsKey(key)) {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(domain.getSmtpHost());
            mailSender.setPort(domain.getSmtpPort());
            mailSender.setUsername(""); // 这里需要配置SMTP用户名
            mailSender.setPassword(""); // 这里需要配置SMTP密码
            
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.timeout", "30000");
            properties.put("mail.smtp.connectiontimeout", "30000");
            
            if (domain.getSmtpSsl()) {
                properties.put("mail.smtp.ssl.enable", "true");
                properties.put("mail.smtp.ssl.trust", domain.getSmtpHost());
            } else {
                properties.put("mail.smtp.starttls.enable", "true");
                properties.put("mail.smtp.starttls.required", "true");
            }
            
            mailSender.setJavaMailProperties(properties);
            mailSenderCache.put(key, mailSender);
        }
        
        return mailSenderCache.get(key);
    }

    /**
     * 根据别名获取域名配置
     */
    private Domain getDomainForAlias(UserAlias alias) {
        // 这里需要从数据库查询域名配置
        // 简化实现，实际应该注入DomainService
        Domain domain = new Domain();
        domain.setDomainName("example.com");
        domain.setSmtpHost("smtp.example.com");
        domain.setSmtpPort(587);
        domain.setSmtpSsl(true);
        return domain;
    }

    /**
     * 创建临时别名对象
     */
    private UserAlias createTempAlias(String email) {
        UserAlias alias = new UserAlias();
        alias.setAliasAddress(email);
        alias.setAliasName(email.split("@")[0]);
        return alias;
    }
}