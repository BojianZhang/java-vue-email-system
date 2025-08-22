package com.enterprise.email.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.enterprise.email.entity.SslCertificate;
import com.enterprise.email.mapper.SslCertificateMapper;
import com.enterprise.email.service.SslCertificateService;
import com.enterprise.email.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * SSL证书监控和自动续期服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SslCertificateMonitorService {

    private final SslCertificateService sslCertificateService;
    private final SslCertificateMapper certificateMapper;
    private final NotificationService notificationService;

    @Value("${ssl.auto-renewal.enabled:true}")
    private Boolean autoRenewalEnabled;

    @Value("${ssl.auto-renewal.days-before:30}")
    private Integer renewalDaysBefore;

    @Value("${ssl.max-renewal-failures:5}")
    private Integer maxRenewalFailures;

    @Value("${ssl.notification.enabled:true}")
    private Boolean notificationEnabled;

    @Value("${ssl.notification.email:admin@example.com}")
    private String notificationEmail;

    /**
     * 每日凌晨2点执行证书自动续期检查
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void scheduledCertificateRenewal() {
        if (!autoRenewalEnabled) {
            log.info("SSL证书自动续期功能已禁用");
            return;
        }

        log.info("开始执行SSL证书自动续期检查");
        
        try {
            // 查找需要续期的证书
            LocalDateTime renewalThreshold = LocalDateTime.now().plusDays(renewalDaysBefore);
            List<SslCertificate> certificatesForRenewal = certificateMapper
                .findCertificatesForAutoRenewal(renewalThreshold, maxRenewalFailures);
            
            log.info("发现 {} 个证书需要续期", certificatesForRenewal.size());
            
            if (certificatesForRenewal.isEmpty()) {
                log.info("没有需要续期的证书");
                return;
            }

            // 异步续期证书
            List<CompletableFuture<Void>> renewalTasks = certificatesForRenewal.stream()
                .map(cert -> renewCertificateAsync(cert))
                .toList();

            // 等待所有续期任务完成
            CompletableFuture.allOf(renewalTasks.toArray(new CompletableFuture[0]))
                .orTimeout(30, TimeUnit.MINUTES)
                .join();

            log.info("SSL证书自动续期检查完成");
            
            // 发送续期总结通知
            sendRenewalSummaryNotification(certificatesForRenewal);
            
        } catch (Exception e) {
            log.error("SSL证书自动续期检查失败", e);
            sendErrorNotification("SSL证书自动续期检查失败", e.getMessage());
        }
    }

    /**
     * 每小时检查证书状态
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scheduledCertificateStatusCheck() {
        log.debug("开始执行证书状态检查");
        
        try {
            // 检查所有活跃证书的状态
            QueryWrapper<SslCertificate> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", "ACTIVE");
            List<SslCertificate> activeCertificates = certificateMapper.selectList(queryWrapper);
            
            for (SslCertificate cert : activeCertificates) {
                try {
                    sslCertificateService.checkCertificateStatus(cert.getId());
                    Thread.sleep(1000); // 避免过于频繁的检查
                } catch (Exception e) {
                    log.error("检查证书状态失败: domain={}, id={}", cert.getDomain(), cert.getId(), e);
                }
            }
            
            log.debug("证书状态检查完成，检查了 {} 个证书", activeCertificates.size());
            
        } catch (Exception e) {
            log.error("证书状态检查失败", e);
        }
    }

    /**
     * 每日凌晨1点清理过期的失败证书记录
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void scheduledFailedCertificateCleanup() {
        log.info("开始清理过期的失败证书记录");
        
        try {
            // 清理7天前的失败证书记录
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            int cleanedCount = certificateMapper.cleanupFailedCertificates(cutoffTime);
            
            log.info("清理完成，删除了 {} 个过期的失败证书记录", cleanedCount);
            
        } catch (Exception e) {
            log.error("清理失败证书记录失败", e);
        }
    }

    /**
     * 每周一凌晨3点执行证书系统健康检查
     */
    @Scheduled(cron = "0 0 3 * * MON")
    public void scheduledSystemHealthCheck() {
        log.info("开始执行SSL证书系统健康检查");
        
        try {
            Map<String, Object> healthCheck = sslCertificateService.performHealthCheck();
            String overallHealth = (String) healthCheck.get("overallHealth");
            
            log.info("SSL证书系统健康状态: {}", overallHealth);
            
            if (!"HEALTHY".equals(overallHealth)) {
                // 系统不健康，发送警告通知
                sendHealthCheckWarning(healthCheck);
            }
            
            // 发送周度健康报告
            sendWeeklyHealthReport(healthCheck);
            
        } catch (Exception e) {
            log.error("SSL证书系统健康检查失败", e);
            sendErrorNotification("SSL证书系统健康检查失败", e.getMessage());
        }
    }

    /**
     * 每日早上8点发送证书到期提醒
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduledExpiryNotification() {
        if (!notificationEnabled) {
            return;
        }

        log.info("开始检查即将过期的证书");
        
        try {
            // 查找7天内过期的证书
            LocalDateTime sevenDaysLater = LocalDateTime.now().plusDays(7);
            QueryWrapper<SslCertificate> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", "ACTIVE")
                      .le("expires_at", sevenDaysLater)
                      .gt("expires_at", LocalDateTime.now());
            
            List<SslCertificate> expiringSoon = certificateMapper.selectList(queryWrapper);
            
            if (!expiringSoon.isEmpty()) {
                sendExpiryNotification(expiringSoon);
                log.info("发送了 {} 个证书的到期提醒", expiringSoon.size());
            }
            
        } catch (Exception e) {
            log.error("发送证书到期提醒失败", e);
        }
    }

    /**
     * 异步续期单个证书
     */
    @Async("sslTaskExecutor")
    public CompletableFuture<Void> renewCertificateAsync(SslCertificate certificate) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("开始续期证书: domain={}, id={}", certificate.getDomain(), certificate.getId());
                
                boolean success = sslCertificateService.renewCertificate(certificate.getId());
                
                if (success) {
                    log.info("证书续期成功: domain={}", certificate.getDomain());
                } else {
                    log.error("证书续期失败: domain={}", certificate.getDomain());
                    
                    // 发送单个证书续期失败通知
                    if (notificationEnabled) {
                        sendCertificateRenewalFailureNotification(certificate);
                    }
                }
                
                // 为避免对ACME服务器造成压力，添加延迟
                Thread.sleep(5000);
                
            } catch (Exception e) {
                log.error("证书续期异常: domain={}, id={}", certificate.getDomain(), certificate.getId(), e);
                
                if (notificationEnabled) {
                    sendCertificateRenewalFailureNotification(certificate, e.getMessage());
                }
            }
        });
    }

    /**
     * 发送续期总结通知
     */
    private void sendRenewalSummaryNotification(List<SslCertificate> certificates) {
        if (!notificationEnabled) {
            return;
        }

        try {
            long successCount = certificates.stream()
                .filter(cert -> "ACTIVE".equals(cert.getStatus()))
                .count();
            long failureCount = certificates.size() - successCount;

            String subject = "SSL证书自动续期总结报告";
            StringBuilder content = new StringBuilder();
            content.append("SSL证书自动续期任务已完成。\n\n");
            content.append("续期统计:\n");
            content.append("- 总数: ").append(certificates.size()).append("\n");
            content.append("- 成功: ").append(successCount).append("\n");
            content.append("- 失败: ").append(failureCount).append("\n\n");

            if (failureCount > 0) {
                content.append("失败的证书:\n");
                certificates.stream()
                    .filter(cert -> !"ACTIVE".equals(cert.getStatus()))
                    .forEach(cert -> content.append("- ").append(cert.getDomain())
                        .append(" (").append(cert.getErrorMessage()).append(")\n"));
            }

            notificationService.sendEmailNotification(notificationEmail, subject, content.toString());

        } catch (Exception e) {
            log.error("发送续期总结通知失败", e);
        }
    }

    /**
     * 发送证书到期提醒
     */
    private void sendExpiryNotification(List<SslCertificate> expiringSoon) {
        try {
            String subject = "SSL证书即将过期提醒";
            StringBuilder content = new StringBuilder();
            content.append("以下SSL证书即将在7天内过期，请注意:\n\n");

            for (SslCertificate cert : expiringSoon) {
                content.append("域名: ").append(cert.getDomain()).append("\n");
                content.append("过期时间: ").append(cert.getExpiresAt()).append("\n");
                content.append("自动续期: ").append(cert.getAutoRenew() ? "是" : "否").append("\n");
                content.append("证书类型: ").append(cert.getCertType()).append("\n\n");
            }

            content.append("请及时处理证书续期事宜。");

            notificationService.sendEmailNotification(notificationEmail, subject, content.toString());

        } catch (Exception e) {
            log.error("发送证书到期提醒失败", e);
        }
    }

    /**
     * 发送单个证书续期失败通知
     */
    private void sendCertificateRenewalFailureNotification(SslCertificate certificate) {
        sendCertificateRenewalFailureNotification(certificate, certificate.getErrorMessage());
    }

    private void sendCertificateRenewalFailureNotification(SslCertificate certificate, String errorMessage) {
        try {
            String subject = "SSL证书续期失败通知 - " + certificate.getDomain();
            StringBuilder content = new StringBuilder();
            content.append("SSL证书续期失败:\n\n");
            content.append("域名: ").append(certificate.getDomain()).append("\n");
            content.append("证书ID: ").append(certificate.getId()).append("\n");
            content.append("失败次数: ").append(certificate.getRenewalFailures()).append("\n");
            content.append("错误信息: ").append(errorMessage).append("\n\n");
            content.append("请登录系统查看详细信息并手动处理。");

            notificationService.sendEmailNotification(notificationEmail, subject, content.toString());

        } catch (Exception e) {
            log.error("发送证书续期失败通知失败", e);
        }
    }

    /**
     * 发送系统健康检查警告
     */
    private void sendHealthCheckWarning(Map<String, Object> healthCheck) {
        try {
            String subject = "SSL证书系统健康检查警告";
            StringBuilder content = new StringBuilder();
            content.append("SSL证书系统健康检查发现问题:\n\n");
            content.append("总体状态: ").append(healthCheck.get("overallHealth")).append("\n\n");
            content.append("详细信息:\n");
            
            healthCheck.forEach((key, value) -> {
                if (!"overallHealth".equals(key)) {
                    content.append("- ").append(key).append(": ").append(value).append("\n");
                }
            });

            content.append("\n请及时检查系统配置和依赖项。");

            notificationService.sendEmailNotification(notificationEmail, subject, content.toString());

        } catch (Exception e) {
            log.error("发送健康检查警告失败", e);
        }
    }

    /**
     * 发送周度健康报告
     */
    private void sendWeeklyHealthReport(Map<String, Object> healthCheck) {
        try {
            String subject = "SSL证书系统周度健康报告";
            StringBuilder content = new StringBuilder();
            content.append("SSL证书系统周度健康报告:\n\n");
            
            // 系统状态
            content.append("系统状态: ").append(healthCheck.get("overallHealth")).append("\n\n");
            
            // 证书统计
            content.append("证书统计:\n");
            content.append("- 总证书数: ").append(healthCheck.get("totalCertificates")).append("\n");
            content.append("- 有效证书: ").append(healthCheck.get("activeCertificates")).append("\n");
            content.append("- 过期证书: ").append(healthCheck.get("expiredCertificates")).append("\n");
            content.append("- 即将过期: ").append(healthCheck.get("expiringCertificates")).append("\n");
            content.append("- Let's Encrypt: ").append(healthCheck.get("letsEncryptCertificates")).append("\n");
            content.append("- 用户上传: ").append(healthCheck.get("uploadedCertificates")).append("\n\n");
            
            // 系统组件状态
            content.append("系统组件:\n");
            content.append("- 证书目录: ").append(healthCheck.get("certificatesDirectoryExists") ? "正常" : "异常").append("\n");
            content.append("- Nginx配置: ").append(healthCheck.get("nginxConfigDirectoryExists") ? "正常" : "异常").append("\n");
            content.append("- Certbot: ").append(healthCheck.get("certbotAvailable") ? "可用" : "不可用").append("\n");
            content.append("- OpenSSL: ").append(healthCheck.get("opensslAvailable") ? "可用" : "不可用").append("\n");
            content.append("- Nginx: ").append(healthCheck.get("nginxAvailable") ? "可用" : "不可用").append("\n");

            notificationService.sendEmailNotification(notificationEmail, subject, content.toString());

        } catch (Exception e) {
            log.error("发送周度健康报告失败", e);
        }
    }

    /**
     * 发送错误通知
     */
    private void sendErrorNotification(String title, String errorMessage) {
        if (!notificationEnabled) {
            return;
        }

        try {
            String subject = "SSL证书系统错误通知 - " + title;
            String content = "SSL证书系统发生错误:\n\n" +
                           "错误类型: " + title + "\n" +
                           "错误信息: " + errorMessage + "\n\n" +
                           "请及时检查系统日志并处理。";

            notificationService.sendEmailNotification(notificationEmail, subject, content);

        } catch (Exception e) {
            log.error("发送错误通知失败", e);
        }
    }
}