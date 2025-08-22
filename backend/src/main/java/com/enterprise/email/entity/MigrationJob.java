package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邮件迁移任务实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("migration_jobs")
public class MigrationJob {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 迁移任务名称
     */
    @TableField("name")
    private String name;

    /**
     * 迁移类型
     */
    @TableField("migration_type")
    private String migrationType; // IMPORT, EXPORT, SYNC, TRANSFER

    /**
     * 源系统类型
     */
    @TableField("source_system")
    private String sourceSystem; // IMAP, POP3, EXCHANGE, GMAIL, OUTLOOK, ZIMBRA, POSTFIX

    /**
     * 目标系统类型
     */
    @TableField("target_system")
    private String targetSystem;

    /**
     * 源服务器配置
     */
    @TableField("source_config")
    private String sourceConfig; // JSON格式

    /**
     * 目标服务器配置
     */
    @TableField("target_config")
    private String targetConfig; // JSON格式

    /**
     * 源认证信息
     */
    @TableField("source_auth")
    private String sourceAuth; // 加密存储

    /**
     * 目标认证信息
     */
    @TableField("target_auth")
    private String targetAuth; // 加密存储

    /**
     * 迁移范围
     */
    @TableField("migration_scope")
    private String migrationScope; // ALL, DOMAIN, USER, MAILBOX, FOLDER, DATE_RANGE

    /**
     * 迁移策略
     */
    @TableField("migration_strategy")
    private String migrationStrategy; // COPY, MOVE, SYNC, INCREMENTAL

    /**
     * 冲突处理策略
     */
    @TableField("conflict_resolution")
    private String conflictResolution; // SKIP, OVERWRITE, RENAME, MERGE

    /**
     * 映射配置
     */
    @TableField("mapping_config")
    private String mappingConfig; // 用户、文件夹映射配置

    /**
     * 过滤条件
     */
    @TableField("filter_conditions")
    private String filterConditions; // JSON格式

    /**
     * 开始日期
     */
    @TableField("start_date")
    private LocalDateTime startDate;

    /**
     * 结束日期
     */
    @TableField("end_date")
    private LocalDateTime endDate;

    /**
     * 任务状态
     */
    @TableField("status")
    private String status; // PENDING, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED

    /**
     * 进度百分比
     */
    @TableField("progress")
    private Integer progress;

    /**
     * 当前阶段
     */
    @TableField("current_phase")
    private String currentPhase; // CONNECTING, SCANNING, MIGRATING, VERIFYING, COMPLETING

    /**
     * 已迁移邮件数
     */
    @TableField("migrated_count")
    private Long migratedCount;

    /**
     * 总邮件数
     */
    @TableField("total_count")
    private Long totalCount;

    /**
     * 跳过邮件数
     */
    @TableField("skipped_count")
    private Long skippedCount;

    /**
     * 错误邮件数
     */
    @TableField("error_count")
    private Long errorCount;

    /**
     * 迁移速度（邮件/秒）
     */
    @TableField("migration_speed")
    private Double migrationSpeed;

    /**
     * 数据传输量（字节）
     */
    @TableField("data_transferred")
    private Long dataTransferred;

    /**
     * 网络带宽使用
     */
    @TableField("bandwidth_usage")
    private Long bandwidthUsage;

    /**
     * 开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;

    /**
     * 估计完成时间
     */
    @TableField("estimated_completion")
    private LocalDateTime estimatedCompletion;

    /**
     * 暂停时间
     */
    @TableField("paused_at")
    private LocalDateTime pausedAt;

    /**
     * 暂停原因
     */
    @TableField("pause_reason")
    private String pauseReason;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 错误详情
     */
    @TableField("error_details")
    private String errorDetails;

    /**
     * 警告信息
     */
    @TableField("warnings")
    private String warnings;

    /**
     * 验证结果
     */
    @TableField("validation_result")
    private String validationResult;

    /**
     * 迁移日志路径
     */
    @TableField("log_file_path")
    private String logFilePath;

    /**
     * 报告文件路径
     */
    @TableField("report_file_path")
    private String reportFilePath;

    /**
     * 是否启用验证
     */
    @TableField("enable_validation")
    private Boolean enableValidation;

    /**
     * 是否保留原数据
     */
    @TableField("preserve_source")
    private Boolean preserveSource;

    /**
     * 是否启用增量同步
     */
    @TableField("enable_incremental")
    private Boolean enableIncremental;

    /**
     * 同步间隔（分钟）
     */
    @TableField("sync_interval")
    private Integer syncInterval;

    /**
     * 最大重试次数
     */
    @TableField("max_retries")
    private Integer maxRetries;

    /**
     * 当前重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 并发线程数
     */
    @TableField("thread_count")
    private Integer threadCount;

    /**
     * 批处理大小
     */
    @TableField("batch_size")
    private Integer batchSize;

    /**
     * 连接超时（秒）
     */
    @TableField("connection_timeout")
    private Integer connectionTimeout;

    /**
     * 读取超时（秒）
     */
    @TableField("read_timeout")
    private Integer readTimeout;

    /**
     * 优先级
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 通知配置
     */
    @TableField("notification_config")
    private String notificationConfig;

    /**
     * 创建者
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}