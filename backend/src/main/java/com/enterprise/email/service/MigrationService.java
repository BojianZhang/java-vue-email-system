package com.enterprise.email.service;

import com.enterprise.email.entity.MigrationJob;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 邮件迁移服务接口
 */
public interface MigrationService {

    /**
     * 创建迁移任务
     */
    boolean createMigrationJob(MigrationJob migrationJob);

    /**
     * 启动迁移任务
     */
    boolean startMigrationJob(Long jobId);

    /**
     * 暂停迁移任务
     */
    boolean pauseMigrationJob(Long jobId, String reason);

    /**
     * 恢复迁移任务
     */
    boolean resumeMigrationJob(Long jobId);

    /**
     * 取消迁移任务
     */
    boolean cancelMigrationJob(Long jobId, String reason);

    /**
     * 删除迁移任务
     */
    boolean deleteMigrationJob(Long jobId);

    /**
     * 获取迁移任务详情
     */
    MigrationJob getMigrationJob(Long jobId);

    /**
     * 根据状态查询迁移任务
     */
    List<MigrationJob> getMigrationJobsByStatus(String status);

    /**
     * 根据迁移类型查询迁移任务
     */
    List<MigrationJob> getMigrationJobsByType(String migrationType);

    /**
     * 根据源系统查询迁移任务
     */
    List<MigrationJob> getMigrationJobsBySourceSystem(String sourceSystem);

    /**
     * 查询正在运行的迁移任务
     */
    List<MigrationJob> getActiveMigrationJobs();

    /**
     * 查询待执行的迁移任务
     */
    List<MigrationJob> getPendingMigrationJobs();

    /**
     * 根据创建者查询迁移任务
     */
    List<MigrationJob> getMigrationJobsByCreatedBy(Long createdBy);

    /**
     * 查询需要增量同步的任务
     */
    List<MigrationJob> getIncrementalSyncJobs();

    /**
     * 查询长时间运行的迁移任务
     */
    List<MigrationJob> getLongRunningMigrationJobs(int hours);

    /**
     * 查询失败的迁移任务
     */
    List<MigrationJob> getFailedMigrationJobs(int hours);

    /**
     * 更新迁移任务进度
     */
    boolean updateMigrationProgress(Long jobId, Integer progress, String currentPhase, 
                                   Long migratedCount, Double migrationSpeed, LocalDateTime estimatedCompletion);

    /**
     * 更新迁移任务状态
     */
    boolean updateMigrationStatus(Long jobId, String status, String errorMessage, LocalDateTime completedAt);

    /**
     * 增加重试次数
     */
    boolean incrementRetryCount(Long jobId);

    /**
     * 更新数据传输量
     */
    boolean updateDataTransfer(Long jobId, Long dataTransferred, Long bandwidthUsage);

    /**
     * 执行IMAP导入迁移
     */
    boolean performImapImport(Long jobId);

    /**
     * 执行POP3导入迁移
     */
    boolean performPop3Import(Long jobId);

    /**
     * 执行Exchange迁移
     */
    boolean performExchangeMigration(Long jobId);

    /**
     * 执行Gmail迁移
     */
    boolean performGmailMigration(Long jobId);

    /**
     * 执行Outlook迁移
     */
    boolean performOutlookMigration(Long jobId);

    /**
     * 执行Zimbra迁移
     */
    boolean performZimbraMigration(Long jobId);

    /**
     * 执行Postfix迁移
     */
    boolean performPostfixMigration(Long jobId);

    /**
     * 执行增量同步
     */
    boolean performIncrementalSync(Long jobId);

    /**
     * 验证迁移完整性
     */
    Map<String, Object> validateMigrationIntegrity(Long jobId);

    /**
     * 测试源连接
     */
    Map<String, Object> testSourceConnection(MigrationJob migrationJob);

    /**
     * 测试目标连接
     */
    Map<String, Object> testTargetConnection(MigrationJob migrationJob);

    /**
     * 预览迁移数据
     */
    Map<String, Object> previewMigrationData(Long jobId, int limit);

    /**
     * 处理冲突解决
     */
    boolean resolveConflicts(Long jobId, List<Map<String, Object>> conflicts);

    /**
     * 应用映射配置
     */
    boolean applyMappingConfiguration(Long jobId, Map<String, Object> mappingConfig);

    /**
     * 应用过滤条件
     */
    boolean applyFilterConditions(Long jobId, Map<String, Object> filterConditions);

    /**
     * 获取迁移统计信息
     */
    Map<String, Object> getMigrationStatistics(int days);

    /**
     * 获取系统迁移统计
     */
    List<Map<String, Object>> getSystemMigrationStatistics(int days);

    /**
     * 获取迁移性能统计
     */
    List<Map<String, Object>> getPerformanceStatistics(int days);

    /**
     * 获取每日迁移趋势
     */
    List<Map<String, Object>> getDailyMigrationTrend(int days);

    /**
     * 获取错误分析统计
     */
    List<Map<String, Object>> getErrorAnalysis(int days, int limit);

    /**
     * 生成迁移报告
     */
    String generateMigrationReport(Long jobId);

    /**
     * 导出迁移配置
     */
    String exportMigrationConfiguration(Long jobId);

    /**
     * 导入迁移配置
     */
    boolean importMigrationConfiguration(String configData);

    /**
     * 克隆迁移任务
     */
    MigrationJob cloneMigrationJob(Long jobId, String newName);

    /**
     * 批量创建迁移任务
     */
    List<Long> batchCreateMigrationJobs(List<MigrationJob> migrationJobs);

    /**
     * 调度迁移任务
     */
    boolean scheduleMigrationJobs();

    /**
     * 监控迁移任务
     */
    Map<String, Object> monitorMigrationJobs();

    /**
     * 获取迁移性能指标
     */
    Map<String, Object> getMigrationPerformanceMetrics();

    /**
     * 优化迁移策略
     */
    Map<String, Object> optimizeMigrationStrategy(Long jobId);

    /**
     * 自动化迁移管理
     */
    boolean automateMigrationManagement();

    /**
     * 迁移质量检查
     */
    Map<String, Object> performQualityCheck(Long jobId);

    /**
     * 同步迁移状态
     */
    boolean syncMigrationStatus();

    /**
     * 清理旧迁移任务记录
     */
    boolean cleanupOldMigrationJobs(int days);
}