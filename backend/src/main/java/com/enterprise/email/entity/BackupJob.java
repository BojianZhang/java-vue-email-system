package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邮件备份任务实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("backup_jobs")
public class BackupJob {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 备份任务名称
     */
    @TableField("name")
    private String name;

    /**
     * 备份类型
     */
    @TableField("backup_type")
    private String backupType; // FULL, INCREMENTAL, DIFFERENTIAL, MAILBOX, DOMAIN

    /**
     * 备份范围
     */
    @TableField("backup_scope")
    private String backupScope; // ALL, DOMAIN, USER, MAILBOX, FOLDER

    /**
     * 目标域名
     */
    @TableField("target_domain")
    private String targetDomain;

    /**
     * 目标用户
     */
    @TableField("target_user")
    private String targetUser;

    /**
     * 目标邮箱
     */
    @TableField("target_mailbox")
    private String targetMailbox;

    /**
     * 备份策略
     */
    @TableField("backup_strategy")
    private String backupStrategy; // MBOX, MAILDIR, PST, EML, JSON

    /**
     * 压缩类型
     */
    @TableField("compression_type")
    private String compressionType; // NONE, GZIP, ZIP, 7Z

    /**
     * 加密类型
     */
    @TableField("encryption_type")
    private String encryptionType; // NONE, AES256, PGP

    /**
     * 加密密钥
     */
    @TableField("encryption_key")
    private String encryptionKey;

    /**
     * 备份路径
     */
    @TableField("backup_path")
    private String backupPath;

    /**
     * 远程存储配置
     */
    @TableField("remote_storage")
    private String remoteStorage; // JSON格式

    /**
     * 存储类型
     */
    @TableField("storage_type")
    private String storageType; // LOCAL, S3, FTP, SFTP, WEBDAV

    /**
     * 调度配置
     */
    @TableField("schedule_config")
    private String scheduleConfig; // Cron表达式

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 任务状态
     */
    @TableField("status")
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED

    /**
     * 进度百分比
     */
    @TableField("progress")
    private Integer progress;

    /**
     * 已备份邮件数
     */
    @TableField("backed_up_count")
    private Long backedUpCount;

    /**
     * 总邮件数
     */
    @TableField("total_count")
    private Long totalCount;

    /**
     * 备份大小（字节）
     */
    @TableField("backup_size")
    private Long backupSize;

    /**
     * 压缩后大小（字节）
     */
    @TableField("compressed_size")
    private Long compressedSize;

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
     * 下次执行时间
     */
    @TableField("next_run_at")
    private LocalDateTime nextRunAt;

    /**
     * 最后执行时间
     */
    @TableField("last_run_at")
    private LocalDateTime lastRunAt;

    /**
     * 执行次数
     */
    @TableField("run_count")
    private Integer runCount;

    /**
     * 成功次数
     */
    @TableField("success_count")
    private Integer successCount;

    /**
     * 失败次数
     */
    @TableField("failure_count")
    private Integer failureCount;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 错误堆栈
     */
    @TableField("error_stack")
    private String errorStack;

    /**
     * 备份文件路径
     */
    @TableField("backup_file_path")
    private String backupFilePath;

    /**
     * 备份文件名
     */
    @TableField("backup_file_name")
    private String backupFileName;

    /**
     * 校验和
     */
    @TableField("checksum")
    private String checksum;

    /**
     * 备份元数据
     */
    @TableField("metadata")
    private String metadata;

    /**
     * 保留天数
     */
    @TableField("retention_days")
    private Integer retentionDays;

    /**
     * 是否自动清理
     */
    @TableField("auto_cleanup")
    private Boolean autoCleanup;

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