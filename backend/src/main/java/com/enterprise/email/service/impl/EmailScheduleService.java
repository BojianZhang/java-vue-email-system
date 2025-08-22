package com.enterprise.email.service.impl;

import com.enterprise.email.entity.UserAlias;
import com.enterprise.email.service.EmailService;
import com.enterprise.email.service.EmailAttachmentService;
import com.enterprise.email.service.UserAliasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 邮件定时任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailScheduleService {

    private final EmailService emailService;
    private final UserAliasService userAliasService;
    private final EmailAttachmentService attachmentService;

    /**
     * 定时同步邮件 - 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void syncAllEmails() {
        log.info("开始定时同步邮件");
        
        try {
            List<UserAlias> aliases = userAliasService.getAllActiveAliases();
            
            for (UserAlias alias : aliases) {
                syncEmailsAsync(alias.getId());
            }
            
            log.info("定时同步邮件任务启动完成: aliases={}", aliases.size());
            
        } catch (Exception e) {
            log.error("定时同步邮件任务失败", e);
        }
    }

    /**
     * 定时清理过期附件 - 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredAttachments() {
        log.info("开始清理过期附件");
        
        try {
            attachmentService.cleanExpiredAttachments();
            log.info("清理过期附件任务完成");
            
        } catch (Exception e) {
            log.error("清理过期附件任务失败", e);
        }
    }

    /**
     * 检查新邮件 - 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 1分钟
    public void checkNewEmails() {
        try {
            List<UserAlias> aliases = userAliasService.getAllActiveAliases();
            
            for (UserAlias alias : aliases) {
                checkNewEmailsAsync(alias.getId());
            }
            
        } catch (Exception e) {
            log.error("检查新邮件任务失败", e);
        }
    }

    /**
     * 异步同步邮件
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> syncEmailsAsync(Long aliasId) {
        try {
            emailService.syncEmailsForAlias(aliasId);
            log.debug("邮件同步完成: aliasId={}", aliasId);
        } catch (Exception e) {
            log.error("邮件同步失败: aliasId={}", aliasId, e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步检查新邮件
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> checkNewEmailsAsync(Long aliasId) {
        try {
            emailService.getNewEmails(aliasId);
            log.debug("新邮件检查完成: aliasId={}", aliasId);
        } catch (Exception e) {
            log.error("新邮件检查失败: aliasId={}", aliasId, e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 手动触发全量同步
     */
    public void manualSyncAll() {
        log.info("手动触发全量邮件同步");
        syncAllEmails();
    }

    /**
     * 手动触发单个别名同步
     */
    public void manualSyncAlias(Long aliasId) {
        log.info("手动触发邮件同步: aliasId={}", aliasId);
        syncEmailsAsync(aliasId);
    }
}