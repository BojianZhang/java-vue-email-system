package com.enterprise.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.email.entity.MigrationJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 迁移任务数据访问层
 */
@Mapper
public interface MigrationJobMapper extends BaseMapper<MigrationJob> {

    /**
     * 根据状态查询迁移任务
     */
    @Select("SELECT * FROM migration_jobs WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<MigrationJob> selectByStatus(@Param("status") String status);

    /**
     * 根据迁移类型查询迁移任务
     */
    @Select("SELECT * FROM migration_jobs WHERE migration_type = #{migrationType} AND deleted = 0 ORDER BY created_at DESC")
    List<MigrationJob> selectByMigrationType(@Param("migrationType") String migrationType);

    /**
     * 根据源系统查询迁移任务
     */
    @Select("SELECT * FROM migration_jobs WHERE source_system = #{sourceSystem} AND deleted = 0 ORDER BY created_at DESC")
    List<MigrationJob> selectBySourceSystem(@Param("sourceSystem") String sourceSystem);

    /**
     * 查询正在运行的迁移任务
     */
    @Select("SELECT * FROM migration_jobs WHERE status IN ('RUNNING', 'PAUSED') AND deleted = 0")
    List<MigrationJob> selectActiveJobs();

    /**
     * 查询待执行的迁移任务
     */
    @Select("SELECT * FROM migration_jobs WHERE status = 'PENDING' AND deleted = 0 ORDER BY priority DESC, created_at ASC")
    List<MigrationJob> selectPendingJobs();

    /**
     * 根据创建者查询迁移任务
     */
    @Select("SELECT * FROM migration_jobs WHERE created_by = #{createdBy} AND deleted = 0 ORDER BY created_at DESC")
    List<MigrationJob> selectByCreatedBy(@Param("createdBy") Long createdBy);

    /**
     * 查询需要增量同步的任务
     */
    @Select("SELECT * FROM migration_jobs WHERE enable_incremental = 1 AND status = 'COMPLETED' " +
            "AND sync_interval IS NOT NULL AND " +
            "(completed_at IS NULL OR completed_at <= DATE_SUB(NOW(), INTERVAL sync_interval MINUTE)) " +
            "AND deleted = 0")
    List<MigrationJob> selectIncrementalSyncJobs();

    /**
     * 查询长时间运行的迁移任务
     */
    @Select("SELECT * FROM migration_jobs WHERE status = 'RUNNING' " +
            "AND started_at <= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0")
    List<MigrationJob> selectLongRunningJobs(@Param("hours") int hours);

    /**
     * 查询失败的迁移任务
     */
    @Select("SELECT * FROM migration_jobs WHERE status = 'FAILED' " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND deleted = 0 " +
            "ORDER BY created_at DESC")
    List<MigrationJob> selectFailedJobs(@Param("hours") int hours);

    /**
     * 查询迁移统计信息
     */
    @Select("SELECT " +
            "status, " +
            "COUNT(*) as job_count, " +
            "SUM(migrated_count) as total_migrated, " +
            "SUM(data_transferred) as total_data, " +
            "AVG(TIMESTAMPDIFF(MINUTE, started_at, completed_at)) as avg_duration " +
            "FROM migration_jobs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0 " +
            "GROUP BY status")
    List<Map<String, Object>> selectMigrationStatistics(@Param("days") int days);

    /**
     * 查询系统迁移统计
     */
    @Select("SELECT " +
            "source_system, " +
            "target_system, " +
            "COUNT(*) as job_count, " +
            "SUM(migrated_count) as total_migrated, " +
            "AVG(migration_speed) as avg_speed " +
            "FROM migration_jobs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0 " +
            "GROUP BY source_system, target_system")
    List<Map<String, Object>> selectSystemMigrationStatistics(@Param("days") int days);

    /**
     * 查询迁移性能统计
     */
    @Select("SELECT " +
            "migration_type, " +
            "AVG(migration_speed) as avg_speed, " +
            "AVG(data_transferred / TIMESTAMPDIFF(SECOND, started_at, completed_at)) as avg_throughput, " +
            "AVG(bandwidth_usage) as avg_bandwidth " +
            "FROM migration_jobs WHERE status = 'COMPLETED' " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0 " +
            "GROUP BY migration_type")
    List<Map<String, Object>> selectPerformanceStatistics(@Param("days") int days);

    /**
     * 查询每日迁移趋势
     */
    @Select("SELECT " +
            "DATE(created_at) as date, " +
            "COUNT(*) as job_count, " +
            "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_count, " +
            "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_count, " +
            "SUM(migrated_count) as total_migrated, " +
            "SUM(data_transferred) as total_data " +
            "FROM migration_jobs WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0 " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> selectDailyMigrationTrend(@Param("days") int days);

    /**
     * 查询错误分析统计
     */
    @Select("SELECT " +
            "error_message, " +
            "COUNT(*) as error_count, " +
            "MAX(created_at) as last_occurrence " +
            "FROM migration_jobs WHERE status = 'FAILED' AND error_message IS NOT NULL " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND deleted = 0 " +
            "GROUP BY error_message ORDER BY error_count DESC LIMIT #{limit}")
    List<Map<String, Object>> selectErrorAnalysis(@Param("days") int days, @Param("limit") int limit);

    /**
     * 更新任务进度
     */
    @Update("UPDATE migration_jobs SET progress = #{progress}, current_phase = #{currentPhase}, " +
            "migrated_count = #{migratedCount}, migration_speed = #{migrationSpeed}, " +
            "estimated_completion = #{estimatedCompletion}, updated_at = NOW() WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, @Param("progress") Integer progress, 
                      @Param("currentPhase") String currentPhase, @Param("migratedCount") Long migratedCount, 
                      @Param("migrationSpeed") Double migrationSpeed, 
                      @Param("estimatedCompletion") LocalDateTime estimatedCompletion);

    /**
     * 更新任务状态
     */
    @Update("UPDATE migration_jobs SET status = #{status}, error_message = #{errorMessage}, " +
            "completed_at = #{completedAt}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, 
                    @Param("errorMessage") String errorMessage, 
                    @Param("completedAt") LocalDateTime completedAt);

    /**
     * 暂停任务
     */
    @Update("UPDATE migration_jobs SET status = 'PAUSED', paused_at = #{pausedAt}, " +
            "pause_reason = #{pauseReason}, updated_at = NOW() WHERE id = #{id}")
    int pauseJob(@Param("id") Long id, @Param("pausedAt") LocalDateTime pausedAt, 
                @Param("pauseReason") String pauseReason);

    /**
     * 恢复任务
     */
    @Update("UPDATE migration_jobs SET status = 'RUNNING', paused_at = NULL, " +
            "pause_reason = NULL, updated_at = NOW() WHERE id = #{id}")
    int resumeJob(@Param("id") Long id);

    /**
     * 增加重试次数
     */
    @Update("UPDATE migration_jobs SET retry_count = retry_count + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementRetryCount(@Param("id") Long id);

    /**
     * 更新数据传输量
     */
    @Update("UPDATE migration_jobs SET data_transferred = #{dataTransferred}, " +
            "bandwidth_usage = #{bandwidthUsage}, updated_at = NOW() WHERE id = #{id}")
    int updateDataTransfer(@Param("id") Long id, @Param("dataTransferred") Long dataTransferred, 
                          @Param("bandwidthUsage") Long bandwidthUsage);

    /**
     * 清理旧迁移任务记录
     */
    @Select("DELETE FROM migration_jobs WHERE deleted = 1 AND updated_at <= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int cleanupOldJobs(@Param("days") int days);
}