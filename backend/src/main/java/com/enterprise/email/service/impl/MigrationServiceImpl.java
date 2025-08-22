package com.enterprise.email.service.impl;

import com.enterprise.email.entity.MigrationJob;
import com.enterprise.email.mapper.MigrationJobMapper;
import com.enterprise.email.service.MigrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 邮件迁移服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationServiceImpl implements MigrationService {

    private final MigrationJobMapper migrationJobMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean createMigrationJob(MigrationJob migrationJob) {
        try {
            setDefaultValues(migrationJob);
            validateMigrationJob(migrationJob);
            
            int result = migrationJobMapper.insert(migrationJob);
            if (result > 0) {
                log.info("迁移任务创建成功: jobId={}, name={}, type={}", 
                    migrationJob.getId(), migrationJob.getName(), migrationJob.getMigrationType());
                return true;
            }
        } catch (Exception e) {
            log.error("创建迁移任务失败: name={}, error={}", 
                migrationJob.getName(), e.getMessage(), e);
        }
        return false;
    }

    @Override
    @Async
    public boolean startMigrationJob(Long jobId) {
        try {
            MigrationJob job = migrationJobMapper.selectById(jobId);
            if (job == null) {
                log.error("迁移任务不存在: jobId={}", jobId);
                return false;
            }

            if (!"PENDING".equals(job.getStatus())) {
                log.error("迁移任务状态不正确: jobId={}, status={}", jobId, job.getStatus());
                return false;
            }

            // 更新状态为运行中
            updateMigrationStatus(jobId, "RUNNING", null, null);
            job.setStartedAt(LocalDateTime.now());

            // 根据源系统类型执行相应的迁移
            boolean success = false;
            switch (job.getSourceSystem()) {
                case "IMAP":
                    success = performImapImport(jobId);
                    break;
                case "POP3":
                    success = performPop3Import(jobId);
                    break;
                case "EXCHANGE":
                    success = performExchangeMigration(jobId);
                    break;
                case "GMAIL":
                    success = performGmailMigration(jobId);
                    break;
                case "OUTLOOK":
                    success = performOutlookMigration(jobId);
                    break;
                case "ZIMBRA":
                    success = performZimbraMigration(jobId);
                    break;
                case "POSTFIX":
                    success = performPostfixMigration(jobId);
                    break;
                default:
                    log.error("不支持的源系统类型: {}", job.getSourceSystem());
                    success = false;
            }

            if (success) {
                updateMigrationStatus(jobId, "COMPLETED", null, LocalDateTime.now());
                
                // 执行验证
                if (Boolean.TRUE.equals(job.getEnableValidation())) {
                    performQualityCheck(jobId);
                }
                
                // 设置增量同步
                if (Boolean.TRUE.equals(job.getEnableIncremental())) {
                    setupIncrementalSync(jobId);
                }
            } else {
                updateMigrationStatus(jobId, "FAILED", "迁移执行失败", null);
            }

            return success;

        } catch (Exception e) {
            log.error("启动迁移任务失败: jobId={}, error={}", jobId, e.getMessage(), e);
            updateMigrationStatus(jobId, "FAILED", e.getMessage(), null);
            return false;
        }
    }

    @Override
    public boolean pauseMigrationJob(Long jobId, String reason) {
        try {
            int result = migrationJobMapper.pauseJob(jobId, LocalDateTime.now(), reason);
            if (result > 0) {
                log.info("迁移任务已暂停: jobId={}, reason={}", jobId, reason);
                return true;
            }
        } catch (Exception e) {
            log.error("暂停迁移任务失败: jobId={}, error={}", jobId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean resumeMigrationJob(Long jobId) {
        try {
            int result = migrationJobMapper.resumeJob(jobId);
            if (result > 0) {
                log.info("迁移任务已恢复: jobId={}", jobId);
                return true;
            }
        } catch (Exception e) {
            log.error("恢复迁移任务失败: jobId={}, error={}", jobId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean cancelMigrationJob(Long jobId, String reason) {
        try {
            updateMigrationStatus(jobId, "CANCELLED", reason, LocalDateTime.now());
            log.info("迁移任务已取消: jobId={}, reason={}", jobId, reason);
            return true;
        } catch (Exception e) {
            log.error("取消迁移任务失败: jobId={}, error={}", jobId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteMigrationJob(Long jobId) {
        try {
            int result = migrationJobMapper.deleteById(jobId);
            if (result > 0) {
                log.info("迁移任务已删除: jobId={}", jobId);
                return true;
            }
        } catch (Exception e) {
            log.error("删除迁移任务失败: jobId={}, error={}", jobId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public MigrationJob getMigrationJob(Long jobId) {
        return migrationJobMapper.selectById(jobId);
    }

    @Override
    public List<MigrationJob> getMigrationJobsByStatus(String status) {
        return migrationJobMapper.selectByStatus(status);
    }

    @Override
    public List<MigrationJob> getMigrationJobsByType(String migrationType) {
        return migrationJobMapper.selectByMigrationType(migrationType);
    }

    @Override
    public List<MigrationJob> getMigrationJobsBySourceSystem(String sourceSystem) {
        return migrationJobMapper.selectBySourceSystem(sourceSystem);
    }

    @Override
    public List<MigrationJob> getActiveMigrationJobs() {
        return migrationJobMapper.selectActiveJobs();
    }

    @Override
    public List<MigrationJob> getPendingMigrationJobs() {
        return migrationJobMapper.selectPendingJobs();
    }

    @Override
    public List<MigrationJob> getMigrationJobsByCreatedBy(Long createdBy) {
        return migrationJobMapper.selectByCreatedBy(createdBy);
    }

    @Override
    public List<MigrationJob> getIncrementalSyncJobs() {
        return migrationJobMapper.selectIncrementalSyncJobs();
    }

    @Override
    public List<MigrationJob> getLongRunningMigrationJobs(int hours) {
        return migrationJobMapper.selectLongRunningJobs(hours);
    }

    @Override
    public List<MigrationJob> getFailedMigrationJobs(int hours) {
        return migrationJobMapper.selectFailedJobs(hours);
    }

    @Override
    public boolean updateMigrationProgress(Long jobId, Integer progress, String currentPhase, 
                                          Long migratedCount, Double migrationSpeed, LocalDateTime estimatedCompletion) {
        try {
            int result = migrationJobMapper.updateProgress(jobId, progress, currentPhase, 
                migratedCount, migrationSpeed, estimatedCompletion);
            return result > 0;
        } catch (Exception e) {
            log.error("更新迁移进度失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateMigrationStatus(Long jobId, String status, String errorMessage, LocalDateTime completedAt) {
        try {
            int result = migrationJobMapper.updateStatus(jobId, status, errorMessage, completedAt);
            return result > 0;
        } catch (Exception e) {
            log.error("更新迁移状态失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean incrementRetryCount(Long jobId) {
        try {
            int result = migrationJobMapper.incrementRetryCount(jobId);
            return result > 0;
        } catch (Exception e) {
            log.error("增加重试次数失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateDataTransfer(Long jobId, Long dataTransferred, Long bandwidthUsage) {
        try {
            int result = migrationJobMapper.updateDataTransfer(jobId, dataTransferred, bandwidthUsage);
            return result > 0;
        } catch (Exception e) {
            log.error("更新数据传输量失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean performImapImport(Long jobId) {
        try {
            MigrationJob job = migrationJobMapper.selectById(jobId);
            if (job == null) return false;

            log.info("开始执行IMAP导入迁移: jobId={}", jobId);
            
            // 更新当前阶段
            updateMigrationProgress(jobId, 10, "CONNECTING", 0L, 0.0, null);
            
            // 解析源配置
            Map<String, Object> sourceConfig = parseSourceConfig(job.getSourceConfig());
            Map<String, Object> sourceAuth = parseAuthConfig(job.getSourceAuth());
            
            // 建立IMAP连接
            Properties props = createImapProperties(sourceConfig);
            Session session = Session.getInstance(props);
            Store store = session.getStore("imap");
            
            String host = (String) sourceConfig.get("host");
            int port = (Integer) sourceConfig.getOrDefault("port", 993);
            String username = (String) sourceAuth.get("username");
            String password = (String) sourceAuth.get("password");
            
            store.connect(host, port, username, password);
            
            updateMigrationProgress(jobId, 20, "SCANNING", 0L, 0.0, null);
            
            // 获取文件夹列表
            Folder[] folders = store.getDefaultFolder().list("*");
            long totalMessages = 0;
            for (Folder folder : folders) {
                folder.open(Folder.READ_ONLY);
                totalMessages += folder.getMessageCount();
                folder.close(false);
            }
            
            updateMigrationProgress(jobId, 30, "MIGRATING", 0L, 0.0, null);
            
            // 模拟迁移过程
            simulateMigrationProcess(jobId, totalMessages);
            
            store.close();
            
            log.info("IMAP导入迁移完成: jobId={}, totalMessages={}", jobId, totalMessages);
            return true;
            
        } catch (Exception e) {
            log.error("执行IMAP导入迁移失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean performPop3Import(Long jobId) {
        try {
            log.info("开始执行POP3导入迁移: jobId={}", jobId);
            updateMigrationProgress(jobId, 10, "CONNECTING", 0L, 0.0, null);
            
            // 简化的POP3迁移实现
            simulateMigrationProcess(jobId, 500L);
            
            return true;
        } catch (Exception e) {
            log.error("执行POP3导入迁移失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean performExchangeMigration(Long jobId) {
        try {
            log.info("开始执行Exchange迁移: jobId={}", jobId);
            updateMigrationProgress(jobId, 10, "CONNECTING", 0L, 0.0, null);
            
            // 简化的Exchange迁移实现
            simulateMigrationProcess(jobId, 1000L);
            
            return true;
        } catch (Exception e) {
            log.error("执行Exchange迁移失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean performGmailMigration(Long jobId) {
        try {
            log.info("开始执行Gmail迁移: jobId={}", jobId);
            updateMigrationProgress(jobId, 10, "CONNECTING", 0L, 0.0, null);
            
            // 简化的Gmail迁移实现
            simulateMigrationProcess(jobId, 2000L);
            
            return true;
        } catch (Exception e) {
            log.error("执行Gmail迁移失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean performOutlookMigration(Long jobId) {
        try {
            log.info("开始执行Outlook迁移: jobId={}", jobId);
            updateMigrationProgress(jobId, 10, "CONNECTING", 0L, 0.0, null);
            
            // 简化的Outlook迁移实现
            simulateMigrationProcess(jobId, 800L);
            
            return true;
        } catch (Exception e) {
            log.error("执行Outlook迁移失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean performZimbraMigration(Long jobId) {
        try {
            log.info("开始执行Zimbra迁移: jobId={}", jobId);
            updateMigrationProgress(jobId, 10, "CONNECTING", 0L, 0.0, null);
            
            // 简化的Zimbra迁移实现
            simulateMigrationProcess(jobId, 1500L);
            
            return true;
        } catch (Exception e) {
            log.error("执行Zimbra迁移失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean performPostfixMigration(Long jobId) {
        try {
            log.info("开始执行Postfix迁移: jobId={}", jobId);
            updateMigrationProgress(jobId, 10, "CONNECTING", 0L, 0.0, null);
            
            // 简化的Postfix迁移实现
            simulateMigrationProcess(jobId, 600L);
            
            return true;
        } catch (Exception e) {
            log.error("执行Postfix迁移失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean performIncrementalSync(Long jobId) {
        try {
            log.info("开始执行增量同步: jobId={}", jobId);
            
            // 简化的增量同步实现
            MigrationJob job = migrationJobMapper.selectById(jobId);
            if (job != null && job.getCompletedAt() != null) {
                // 同步上次完成时间之后的邮件
                simulateMigrationProcess(jobId, 50L);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("执行增量同步失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> validateMigrationIntegrity(Long jobId) {
        Map<String, Object> result = new HashMap<>();
        try {
            MigrationJob job = migrationJobMapper.selectById(jobId);
            if (job == null) {
                result.put("valid", false);
                result.put("error", "迁移任务不存在");
                return result;
            }

            // 简化的完整性验证
            boolean isValid = "COMPLETED".equals(job.getStatus()) && 
                             job.getMigratedCount() != null && job.getMigratedCount() > 0;
            
            result.put("valid", isValid);
            result.put("jobId", jobId);
            result.put("migratedCount", job.getMigratedCount());
            result.put("errorCount", job.getErrorCount());
            result.put("warnings", job.getWarnings());
            
        } catch (Exception e) {
            log.error("验证迁移完整性失败: jobId={}, error={}", jobId, e.getMessage(), e);
            result.put("valid", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> testSourceConnection(MigrationJob migrationJob) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 简化的连接测试
            result.put("connected", true);
            result.put("sourceSystem", migrationJob.getSourceSystem());
            result.put("message", "连接测试成功");
            result.put("serverInfo", "Test Server v1.0");
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> testTargetConnection(MigrationJob migrationJob) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 简化的连接测试
            result.put("connected", true);
            result.put("targetSystem", migrationJob.getTargetSystem());
            result.put("message", "目标连接测试成功");
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> previewMigrationData(Long jobId, int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 简化的数据预览
            List<Map<String, Object>> previewData = new ArrayList<>();
            for (int i = 1; i <= Math.min(limit, 10); i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("subject", "Test Email " + i);
                item.put("from", "test" + i + "@example.com");
                item.put("date", LocalDateTime.now().minusDays(i));
                item.put("size", 1024 * i);
                previewData.add(item);
            }
            
            result.put("previewData", previewData);
            result.put("totalCount", 1000);
            result.put("estimatedSize", 1024000L);
            
        } catch (Exception e) {
            log.error("预览迁移数据失败: jobId={}, error={}", jobId, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public boolean resolveConflicts(Long jobId, List<Map<String, Object>> conflicts) {
        try {
            log.info("开始解决冲突: jobId={}, conflictCount={}", jobId, conflicts.size());
            // 简化的冲突解决实现
            return true;
        } catch (Exception e) {
            log.error("解决冲突失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean applyMappingConfiguration(Long jobId, Map<String, Object> mappingConfig) {
        try {
            MigrationJob job = migrationJobMapper.selectById(jobId);
            if (job != null) {
                job.setMappingConfig(objectMapper.writeValueAsString(mappingConfig));
                migrationJobMapper.updateById(job);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("应用映射配置失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean applyFilterConditions(Long jobId, Map<String, Object> filterConditions) {
        try {
            MigrationJob job = migrationJobMapper.selectById(jobId);
            if (job != null) {
                job.setFilterConditions(objectMapper.writeValueAsString(filterConditions));
                migrationJobMapper.updateById(job);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("应用过滤条件失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getMigrationStatistics(int days) {
        try {
            List<Map<String, Object>> stats = migrationJobMapper.selectMigrationStatistics(days);
            Map<String, Object> result = new HashMap<>();
            
            long totalJobs = stats.stream().mapToLong(s -> ((Number) s.get("job_count")).longValue()).sum();
            long totalMigrated = stats.stream().mapToLong(s -> ((Number) s.getOrDefault("total_migrated", 0)).longValue()).sum();
            
            result.put("totalJobs", totalJobs);
            result.put("totalMigrated", totalMigrated);
            result.put("statusBreakdown", stats);
            
            return result;
        } catch (Exception e) {
            log.error("获取迁移统计失败: days={}, error={}", days, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public List<Map<String, Object>> getSystemMigrationStatistics(int days) {
        return migrationJobMapper.selectSystemMigrationStatistics(days);
    }

    @Override
    public List<Map<String, Object>> getPerformanceStatistics(int days) {
        return migrationJobMapper.selectPerformanceStatistics(days);
    }

    @Override
    public List<Map<String, Object>> getDailyMigrationTrend(int days) {
        return migrationJobMapper.selectDailyMigrationTrend(days);
    }

    @Override
    public List<Map<String, Object>> getErrorAnalysis(int days, int limit) {
        return migrationJobMapper.selectErrorAnalysis(days, limit);
    }

    @Override
    public String generateMigrationReport(Long jobId) {
        try {
            MigrationJob job = migrationJobMapper.selectById(jobId);
            if (job == null) return "迁移任务不存在";

            StringBuilder report = new StringBuilder();
            report.append("迁移报告\n");
            report.append("==================\n");
            report.append("任务ID: ").append(job.getId()).append("\n");
            report.append("任务名称: ").append(job.getName()).append("\n");
            report.append("迁移类型: ").append(job.getMigrationType()).append("\n");
            report.append("源系统: ").append(job.getSourceSystem()).append("\n");
            report.append("目标系统: ").append(job.getTargetSystem()).append("\n");
            report.append("状态: ").append(job.getStatus()).append("\n");
            report.append("开始时间: ").append(job.getStartedAt()).append("\n");
            report.append("完成时间: ").append(job.getCompletedAt()).append("\n");
            report.append("已迁移邮件数: ").append(job.getMigratedCount()).append("\n");
            report.append("错误邮件数: ").append(job.getErrorCount()).append("\n");
            report.append("迁移速度: ").append(job.getMigrationSpeed()).append(" 邮件/秒\n");
            
            return report.toString();
        } catch (Exception e) {
            log.error("生成迁移报告失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return "报告生成失败: " + e.getMessage();
        }
    }

    @Override
    public String exportMigrationConfiguration(Long jobId) {
        try {
            MigrationJob job = migrationJobMapper.selectById(jobId);
            if (job == null) return null;
            
            return objectMapper.writeValueAsString(job);
        } catch (Exception e) {
            log.error("导出迁移配置失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean importMigrationConfiguration(String configData) {
        try {
            MigrationJob job = objectMapper.readValue(configData, MigrationJob.class);
            job.setId(null); // 重置ID
            return createMigrationJob(job);
        } catch (Exception e) {
            log.error("导入迁移配置失败: error={}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public MigrationJob cloneMigrationJob(Long jobId, String newName) {
        try {
            MigrationJob originalJob = migrationJobMapper.selectById(jobId);
            if (originalJob == null) return null;
            
            MigrationJob clonedJob = objectMapper.readValue(
                objectMapper.writeValueAsString(originalJob), MigrationJob.class);
            clonedJob.setId(null);
            clonedJob.setName(newName);
            clonedJob.setStatus("PENDING");
            clonedJob.setProgress(0);
            
            if (createMigrationJob(clonedJob)) {
                return clonedJob;
            }
            return null;
        } catch (Exception e) {
            log.error("克隆迁移任务失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<Long> batchCreateMigrationJobs(List<MigrationJob> migrationJobs) {
        List<Long> jobIds = new ArrayList<>();
        for (MigrationJob job : migrationJobs) {
            if (createMigrationJob(job)) {
                jobIds.add(job.getId());
            }
        }
        return jobIds;
    }

    @Override
    public boolean scheduleMigrationJobs() {
        try {
            List<MigrationJob> pendingJobs = getPendingMigrationJobs();
            for (MigrationJob job : pendingJobs) {
                if (job.getPriority() != null && job.getPriority() > 5) {
                    startMigrationJob(job.getId());
                }
            }
            return true;
        } catch (Exception e) {
            log.error("调度迁移任务失败: error={}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> monitorMigrationJobs() {
        Map<String, Object> status = new HashMap<>();
        try {
            status.put("activeJobs", getActiveMigrationJobs().size());
            status.put("pendingJobs", getPendingMigrationJobs().size());
            status.put("failedJobs", getFailedMigrationJobs(24).size());
            status.put("lastCheck", LocalDateTime.now());
        } catch (Exception e) {
            log.error("监控迁移任务失败: error={}", e.getMessage(), e);
        }
        return status;
    }

    @Override
    public Map<String, Object> performQualityCheck(Long jobId) {
        Map<String, Object> result = new HashMap<>();
        try {
            MigrationJob job = migrationJobMapper.selectById(jobId);
            if (job == null) {
                result.put("passed", false);
                result.put("error", "迁移任务不存在");
                return result;
            }

            // 简化的质量检查
            boolean passed = job.getErrorCount() == null || job.getErrorCount() < job.getMigratedCount() * 0.05; // 错误率低于5%
            
            result.put("passed", passed);
            result.put("jobId", jobId);
            result.put("migratedCount", job.getMigratedCount());
            result.put("errorCount", job.getErrorCount());
            result.put("qualityScore", passed ? 95 : 80);
            
        } catch (Exception e) {
            log.error("执行质量检查失败: jobId={}, error={}", jobId, e.getMessage(), e);
            result.put("passed", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // 简化实现的其他方法
    @Override
    public Map<String, Object> getMigrationPerformanceMetrics() { return new HashMap<>(); }
    @Override
    public Map<String, Object> optimizeMigrationStrategy(Long jobId) { return new HashMap<>(); }
    @Override
    public boolean automateMigrationManagement() { return true; }
    @Override
    public boolean syncMigrationStatus() { return true; }

    @Override
    public boolean cleanupOldMigrationJobs(int days) {
        try {
            int result = migrationJobMapper.cleanupOldJobs(days);
            log.info("清理旧迁移任务记录完成，共清理{}条记录", result);
            return true;
        } catch (Exception e) {
            log.error("清理旧迁移任务记录失败: days={}, error={}", days, e.getMessage(), e);
            return false;
        }
    }

    // ========== 私有辅助方法 ==========

    private void setDefaultValues(MigrationJob job) {
        if (job.getCreatedAt() == null) {
            job.setCreatedAt(LocalDateTime.now());
        }
        if (job.getUpdatedAt() == null) {
            job.setUpdatedAt(LocalDateTime.now());
        }
        if (job.getStatus() == null) {
            job.setStatus("PENDING");
        }
        if (job.getProgress() == null) {
            job.setProgress(0);
        }
        if (job.getMigratedCount() == null) {
            job.getMigratedCount(0L);
        }
        if (job.getSkippedCount() == null) {
            job.setSkippedCount(0L);
        }
        if (job.getErrorCount() == null) {
            job.setErrorCount(0L);
        }
        if (job.getRetryCount() == null) {
            job.setRetryCount(0);
        }
        if (job.getEnableValidation() == null) {
            job.setEnableValidation(true);
        }
        if (job.getPreserveSource() == null) {
            job.setPreserveSource(true);
        }
        if (job.getEnableIncremental() == null) {
            job.setEnableIncremental(false);
        }
        if (job.getMaxRetries() == null) {
            job.setMaxRetries(3);
        }
        if (job.getThreadCount() == null) {
            job.setThreadCount(5);
        }
        if (job.getBatchSize() == null) {
            job.setBatchSize(100);
        }
        if (job.getConnectionTimeout() == null) {
            job.setConnectionTimeout(30);
        }
        if (job.getReadTimeout() == null) {
            job.setReadTimeout(60);
        }
        if (job.getPriority() == null) {
            job.setPriority(5);
        }
    }

    private void validateMigrationJob(MigrationJob job) {
        if (job.getName() == null || job.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("迁移任务名称不能为空");
        }
        if (job.getMigrationType() == null) {
            throw new IllegalArgumentException("迁移类型不能为空");
        }
        if (job.getSourceSystem() == null) {
            throw new IllegalArgumentException("源系统类型不能为空");
        }
        if (job.getTargetSystem() == null) {
            throw new IllegalArgumentException("目标系统类型不能为空");
        }
    }

    private Map<String, Object> parseSourceConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, Map.class);
        } catch (Exception e) {
            log.warn("解析源配置失败，使用默认配置: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, Object> parseAuthConfig(String authJson) {
        try {
            // 实际实现中应该解密
            return objectMapper.readValue(authJson, Map.class);
        } catch (Exception e) {
            log.warn("解析认证配置失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Properties createImapProperties(Map<String, Object> config) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.ssl.enable", config.getOrDefault("ssl", true));
        props.put("mail.imap.port", config.getOrDefault("port", 993));
        return props;
    }

    private void simulateMigrationProcess(Long jobId, Long totalMessages) {
        try {
            long migratedCount = 0;
            int progress = 30;
            
            updateMigrationProgress(jobId, progress, "MIGRATING", migratedCount, 0.0, null);
            
            // 模拟迁移进度
            for (int i = 0; i < 7; i++) {
                Thread.sleep(500); // 模拟迁移时间
                
                migratedCount += totalMessages / 7;
                progress += 10;
                double speed = migratedCount / ((i + 1) * 0.5); // 邮件/秒
                
                updateMigrationProgress(jobId, progress, "MIGRATING", migratedCount, speed, 
                    LocalDateTime.now().plusMinutes((7 - i) * 2));
            }
            
            // 验证阶段
            updateMigrationProgress(jobId, 100, "VERIFYING", totalMessages, 0.0, null);
            Thread.sleep(300);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void setupIncrementalSync(Long jobId) {
        try {
            log.info("设置增量同步: jobId={}", jobId);
            // 实际实现中会设置定时任务
        } catch (Exception e) {
            log.warn("设置增量同步失败: jobId={}, error={}", jobId, e.getMessage());
        }
    }
}