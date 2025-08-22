package com.enterprise.email.service;

import com.enterprise.email.entity.BackupJob;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 邮件备份服务接口
 */
public interface BackupService {

    /**
     * 创建备份任务
     */
    boolean createBackupJob(BackupJob backupJob);

    /**
     * 启动备份任务
     */
    boolean startBackupJob(Long jobId);

    /**
     * 暂停备份任务
     */
    boolean pauseBackupJob(Long jobId, String reason);

    /**
     * 恢复备份任务
     */
    boolean resumeBackupJob(Long jobId);

    /**
     * 取消备份任务
     */
    boolean cancelBackupJob(Long jobId, String reason);

    /**
     * 删除备份任务
     */
    boolean deleteBackupJob(Long jobId);

    /**
     * 获取备份任务详情
     */
    BackupJob getBackupJob(Long jobId);

    /**
     * 根据状态查询备份任务
     */
    List<BackupJob> getBackupJobsByStatus(String status);

    /**
     * 根据备份类型查询备份任务
     */
    List<BackupJob> getBackupJobsByType(String backupType);

    /**
     * 查询待执行的定时备份任务
     */
    List<BackupJob> getScheduledBackupJobs();

    /**
     * 查询正在运行的备份任务
     */
    List<BackupJob> getRunningBackupJobs();

    /**
     * 根据创建者查询备份任务
     */
    List<BackupJob> getBackupJobsByCreatedBy(Long createdBy);

    /**
     * 根据域名查询备份任务
     */
    List<BackupJob> getBackupJobsByDomain(String domain);

    /**
     * 根据用户查询备份任务
     */
    List<BackupJob> getBackupJobsByUser(String user);

    /**
     * 查询需要清理的过期备份
     */
    List<BackupJob> getExpiredBackups();

    /**
     * 查询失败的备份任务
     */
    List<BackupJob> getFailedBackupJobs(int hours);

    /**
     * 查询长时间运行的备份任务
     */
    List<BackupJob> getLongRunningBackupJobs(int hours);

    /**
     * 更新备份任务进度
     */
    boolean updateBackupProgress(Long jobId, Integer progress, Long backedUpCount, LocalDateTime estimatedCompletion);

    /**
     * 更新备份任务状态
     */
    boolean updateBackupStatus(Long jobId, String status, String errorMessage, LocalDateTime completedAt);

    /**
     * 更新下次执行时间
     */
    boolean updateNextRun(Long jobId, LocalDateTime nextRunAt, LocalDateTime lastRunAt);

    /**
     * 增加成功次数
     */
    boolean incrementSuccessCount(Long jobId);

    /**
     * 增加失败次数
     */
    boolean incrementFailureCount(Long jobId);

    /**
     * 执行完整备份
     */
    boolean performFullBackup(Long jobId);

    /**
     * 执行增量备份
     */
    boolean performIncrementalBackup(Long jobId);

    /**
     * 执行差异备份
     */
    boolean performDifferentialBackup(Long jobId);

    /**
     * 执行邮箱备份
     */
    boolean performMailboxBackup(Long jobId, String mailbox);

    /**
     * 执行域名备份
     */
    boolean performDomainBackup(Long jobId, String domain);

    /**
     * 验证备份完整性
     */
    Map<String, Object> validateBackupIntegrity(Long jobId);

    /**
     * 恢复备份数据
     */
    boolean restoreBackup(Long jobId, Map<String, Object> restoreOptions);

    /**
     * 测试备份配置
     */
    Map<String, Object> testBackupConfiguration(BackupJob backupJob);

    /**
     * 压缩备份文件
     */
    boolean compressBackupFile(Long jobId);

    /**
     * 加密备份文件
     */
    boolean encryptBackupFile(Long jobId);

    /**
     * 上传到远程存储
     */
    boolean uploadToRemoteStorage(Long jobId);

    /**
     * 从远程存储下载
     */
    boolean downloadFromRemoteStorage(Long jobId, String localPath);

    /**
     * 清理过期备份
     */
    boolean cleanupExpiredBackups();

    /**
     * 获取备份统计信息
     */
    Map<String, Object> getBackupStatistics(int days);

    /**
     * 获取存储使用统计
     */
    List<Map<String, Object>> getStorageUsageStatistics();

    /**
     * 获取备份类型统计
     */
    List<Map<String, Object>> getBackupTypeStatistics(int days);

    /**
     * 获取每日备份趋势
     */
    List<Map<String, Object>> getDailyBackupTrend(int days);

    /**
     * 生成备份报告
     */
    String generateBackupReport(Long jobId);

    /**
     * 导出备份配置
     */
    String exportBackupConfiguration(Long jobId);

    /**
     * 导入备份配置
     */
    boolean importBackupConfiguration(String configData);

    /**
     * 调度备份任务
     */
    boolean scheduleBackupJobs();

    /**
     * 监控备份任务
     */
    Map<String, Object> monitorBackupJobs();

    /**
     * 获取备份性能指标
     */
    Map<String, Object> getBackupPerformanceMetrics();

    /**
     * 优化备份策略
     */
    Map<String, Object> optimizeBackupStrategy(String domain);

    /**
     * 自动化备份管理
     */
    boolean automateBackupManagement();

    /**
     * 备份容灾检查
     */
    Map<String, Object> performDisasterRecoveryCheck();

    /**
     * 同步备份状态
     */
    boolean syncBackupStatus();

    /**
     * 清理旧备份任务记录
     */
    boolean cleanupOldBackupJobs(int days);
}