package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 目录同步日志实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("directory_sync_logs")
public class DirectorySyncLog {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 同步类型
     */
    @TableField("sync_type")
    private String syncType; // LDAP, AD

    /**
     * 配置ID
     */
    @TableField("config_id")
    private Long configId;

    /**
     * 配置名称
     */
    @TableField("config_name")
    private String configName;

    /**
     * 同步操作类型
     */
    @TableField("operation_type")
    private String operationType; // FULL_SYNC, INCREMENTAL_SYNC, USER_SYNC, GROUP_SYNC

    /**
     * 同步状态
     */
    @TableField("status")
    private String status; // RUNNING, SUCCESS, FAILED, CANCELLED

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
     * 耗时（毫秒）
     */
    @TableField("duration_ms")
    private Long durationMs;

    /**
     * 处理的用户数
     */
    @TableField("users_processed")
    private Integer usersProcessed;

    /**
     * 创建的用户数
     */
    @TableField("users_created")
    private Integer usersCreated;

    /**
     * 更新的用户数
     */
    @TableField("users_updated")
    private Integer usersUpdated;

    /**
     * 禁用的用户数
     */
    @TableField("users_disabled")
    private Integer usersDisabled;

    /**
     * 删除的用户数
     */
    @TableField("users_deleted")
    private Integer usersDeleted;

    /**
     * 处理的组数
     */
    @TableField("groups_processed")
    private Integer groupsProcessed;

    /**
     * 创建的组数
     */
    @TableField("groups_created")
    private Integer groupsCreated;

    /**
     * 更新的组数
     */
    @TableField("groups_updated")
    private Integer groupsUpdated;

    /**
     * 删除的组数
     */
    @TableField("groups_deleted")
    private Integer groupsDeleted;

    /**
     * 错误数量
     */
    @TableField("error_count")
    private Integer errorCount;

    /**
     * 警告数量
     */
    @TableField("warning_count")
    private Integer warningCount;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 错误详情
     */
    @TableField("error_details")
    private String errorDetails; // JSON格式

    /**
     * 警告信息
     */
    @TableField("warnings")
    private String warnings; // JSON格式

    /**
     * 同步详情
     */
    @TableField("sync_details")
    private String syncDetails; // JSON格式

    /**
     * 处理的记录总数
     */
    @TableField("total_records")
    private Integer totalRecords;

    /**
     * 成功处理的记录数
     */
    @TableField("successful_records")
    private Integer successfulRecords;

    /**
     * 失败处理的记录数
     */
    @TableField("failed_records")
    private Integer failedRecords;

    /**
     * 数据源信息
     */
    @TableField("data_source_info")
    private String dataSourceInfo; // JSON格式

    /**
     * 同步范围
     */
    @TableField("sync_scope")
    private String syncScope;

    /**
     * 同步过滤器
     */
    @TableField("sync_filter")
    private String syncFilter;

    /**
     * 触发方式
     */
    @TableField("trigger_type")
    private String triggerType; // MANUAL, SCHEDULED, AUTO

    /**
     * 触发者
     */
    @TableField("triggered_by")
    private Long triggeredBy;

    /**
     * 日志文件路径
     */
    @TableField("log_file_path")
    private String logFilePath;

    /**
     * 报告文件路径
     */
    @TableField("report_file_path")
    private String reportFilePath;

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