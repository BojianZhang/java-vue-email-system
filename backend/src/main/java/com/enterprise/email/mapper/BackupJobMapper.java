package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.BackupJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 备份任务数据访问层
 */
@Mapper
public interface BackupJobMapper extends BaseMapper<BackupJob> {

    /**
     * 根据状态查询备份任务
     */
    @Select("SELECT * FROM backup_jobs WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<BackupJob> selectByStatus(@Param("status") String status);

    /**
     * 根据备份类型查询备份任务
     */
    @Select("SELECT * FROM backup_jobs WHERE backup_type = #{backupType} AND deleted = 0 ORDER BY created_at DESC")
    List<BackupJob> selectByBackupType(@Param("backupType") String backupType);

    /**
     * 查询待执行的定时备份任务
     */
    @Select("SELECT * FROM backup_jobs WHERE enabled = 1 AND schedule_config IS NOT NULL " +
            "AND (next_run_at IS NULL OR next_run_at <= NOW()) AND status != 'RUNNING' AND deleted = 0")
    List<BackupJob> selectScheduledJobs();

    /**
     * 查询正在运行的备份任务
     */
    @Select("SELECT * FROM backup_jobs WHERE status = 'RUNNING' AND deleted = 0")
    List<BackupJob> selectRunningJobs();

    /**
     * 根据创建者查询备份任务
     */
    @Select("SELECT * FROM backup_jobs WHERE created_by = #{createdBy} AND deleted = 0 ORDER BY created_at DESC")
    List<BackupJob> selectByCreatedBy(@Param("createdBy") Long createdBy);

    /**
     * 根据域名查询备份任务
     */
    @Select("SELECT * FROM backup_jobs WHERE target_domain = #{domain} AND deleted = 0 ORDER BY created_at DESC")
    List<BackupJob> selectByDomain(@Param("domain") String domain);

    /**
     * 根据用户查询备份任务
     */
    @Select("SELECT * FROM backup_jobs WHERE target_user = #{user} AND deleted = 0 ORDER BY created_at DESC")
    List<BackupJob> selectByUser(@Param("user") String user);

    /**
     * 查询需要清理的过期备份
     */
    @Select("SELECT * FROM backup_jobs WHERE auto_cleanup = 1 AND retention_days IS NOT NULL " +
            "AND completed_at <= DATE_SUB(NOW(), INTERVAL retention_days DAY) " +
            "AND status = 'COMPLETED' AND deleted = 0")
    List<BackupJob> selectExpiredBackups();

    /**
     * 查询备份统计信息
     */
    @Select("SELECT " +
            "status, " +
            "COUNT(*) as job_count, " +
            "SUM(backup_size) as total_size, " +
            "AVG(TIMESTAMPDIFF(MINUTE, started_at, completed_at)) as avg_duration " +
            "FROM backup_jobs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0 " +
            "GROUP BY status")
    List<Map<String, Object>> selectBackupStatistics(@Param("days") int days);

    /**
     * 查询存储使用统计
     */
    @Select("SELECT " +
            "storage_type, " +
            "COUNT(*) as job_count, " +
            "SUM(backup_size) as total_size, " +
            "SUM(compressed_size) as total_compressed_size " +
            "FROM backup_jobs WHERE status = 'COMPLETED' AND deleted = 0 " +
            "GROUP BY storage_type")
    List<Map<String, Object>> selectStorageUsageStatistics();

    /**
     * 查询备份类型统计
     */
    @Select("SELECT " +
            "backup_type, " +
            "COUNT(*) as job_count, " +
            "SUM(backed_up_count) as total_emails, " +
            "AVG(backup_size) as avg_size " +
            "FROM backup_jobs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0 " +
            "GROUP BY backup_type")
    List<Map<String, Object>> selectBackupTypeStatistics(@Param("days") int days);

    /**
     * 查询每日备份趋势
     */
    @Select("SELECT " +
            "DATE(created_at) as date, " +
            "COUNT(*) as job_count, " +
            "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_count, " +
            "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_count, " +
            "SUM(backup_size) as total_size " +
            "FROM backup_jobs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0 " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> selectDailyBackupTrend(@Param("days") int days);

    /**
     * 查询失败的备份任务
     */
    @Select("SELECT * FROM backup_jobs WHERE status = 'FAILED' " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 " +
            "ORDER BY created_at DESC")
    List<BackupJob> selectFailedJobs(@Param("hours") int hours);

    /**
     * 查询长时间运行的备份任务
     */
    @Select("SELECT * FROM backup_jobs WHERE status = 'RUNNING' " +
            "AND started_at <= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0")
    List<BackupJob> selectLongRunningJobs(@Param("hours") int hours);

    /**
     * 更新任务进度
     */
    @Update("UPDATE backup_jobs SET progress = #{progress}, backed_up_count = #{backedUpCount}, " +
            "estimated_completion = #{estimatedCompletion}, updated_at = NOW() WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, @Param("progress") Integer progress, 
                      @Param("backedUpCount") Long backedUpCount, 
                      @Param("estimatedCompletion") LocalDateTime estimatedCompletion);

    /**
     * 更新任务状态
     */
    @Update("UPDATE backup_jobs SET status = #{status}, error_message = #{errorMessage}, " +
            "completed_at = #{completedAt}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, 
                    @Param("errorMessage") String errorMessage, 
                    @Param("completedAt") LocalDateTime completedAt);

    /**
     * 更新下次执行时间
     */
    @Update("UPDATE backup_jobs SET next_run_at = #{nextRunAt}, last_run_at = #{lastRunAt}, " +
            "run_count = run_count + 1, updated_at = NOW() WHERE id = #{id}")
    int updateNextRun(@Param("id") Long id, @Param("nextRunAt") LocalDateTime nextRunAt, 
                     @Param("lastRunAt") LocalDateTime lastRunAt);

    /**
     * 增加成功次数
     */
    @Update("UPDATE backup_jobs SET success_count = success_count + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementSuccessCount(@Param("id") Long id);

    /**
     * 增加失败次数
     */
    @Update("UPDATE backup_jobs SET failure_count = failure_count + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementFailureCount(@Param("id") Long id);

    /**
     * 清理旧备份任务记录
     */
    @Select("DELETE FROM backup_jobs WHERE deleted = 1 AND updated_at <= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int cleanupOldJobs(@Param("days") int days);
}