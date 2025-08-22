package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * ClamAV防病毒配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("clamav_configs")
public class ClamAVConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * ClamAV服务器配置
     */
    @TableField("clamd_host")
    private String clamdHost;

    @TableField("clamd_port")
    private Integer clamdPort;

    @TableField("clamd_socket")
    private String clamdSocket;

    /**
     * 连接配置
     */
    @TableField("connection_timeout")
    private Integer connectionTimeout;

    @TableField("read_timeout")
    private Integer readTimeout;

    @TableField("max_connections")
    private Integer maxConnections;

    /**
     * 扫描配置
     */
    @TableField("scan_attachments")
    private Boolean scanAttachments;

    @TableField("scan_embedded")
    private Boolean scanEmbedded;

    @TableField("max_scan_size")
    private Long maxScanSize;

    @TableField("max_file_size")
    private Long maxFileSize;

    @TableField("max_recursion")
    private Integer maxRecursion;

    @TableField("max_files")
    private Integer maxFiles;

    /**
     * 病毒处理策略
     */
    @TableField("virus_action")
    private String virusAction; // REJECT, QUARANTINE, DELETE, TAG

    @TableField("quarantine_path")
    private String quarantinePath;

    @TableField("add_virus_header")
    private Boolean addVirusHeader;

    @TableField("virus_header_name")
    private String virusHeaderName;

    /**
     * 文件类型过滤
     */
    @TableField("scan_archives")
    private Boolean scanArchives;

    @TableField("scan_pe")
    private Boolean scanPe;

    @TableField("scan_ole2")
    private Boolean scanOle2;

    @TableField("scan_pdf")
    private Boolean scanPdf;

    @TableField("scan_html")
    private Boolean scanHtml;

    @TableField("scan_mail")
    private Boolean scanMail;

    /**
     * 启发式检测
     */
    @TableField("heuristic_scan")
    private Boolean heuristicScan;

    @TableField("detect_pua")
    private Boolean detectPua;

    @TableField("detect_broken")
    private Boolean detectBroken;

    @TableField("algorithmic_detection")
    private Boolean algorithmicDetection;

    /**
     * 签名更新配置
     */
    @TableField("freshclam_enabled")
    private Boolean freshclamEnabled;

    @TableField("update_interval")
    private Integer updateInterval; // 小时

    @TableField("mirror_url")
    private String mirrorUrl;

    @TableField("auto_update")
    private Boolean autoUpdate;

    /**
     * 日志配置
     */
    @TableField("log_level")
    private String logLevel;

    @TableField("log_file")
    private String logFile;

    @TableField("log_infected")
    private Boolean logInfected;

    @TableField("log_clean")
    private Boolean logClean;

    @TableField("log_time")
    private Boolean logTime;

    @TableField("log_file_max_size")
    private Long logFileMaxSize;

    /**
     * 性能配置
     */
    @TableField("scan_threads")
    private Integer scanThreads;

    @TableField("idle_timeout")
    private Integer idleTimeout;

    @TableField("max_queue")
    private Integer maxQueue;

    @TableField("stream_max_length")
    private Long streamMaxLength;

    /**
     * 白名单配置
     */
    @TableField("whitelist_files")
    private String whitelistFiles;

    @TableField("whitelist_signatures")
    private String whitelistSignatures;

    /**
     * 统计信息
     */
    @TableField("total_scanned")
    private Long totalScanned;

    @TableField("total_infected")
    private Long totalInfected;

    @TableField("last_virus_found")
    private String lastVirusFound;

    @TableField("last_scan_time")
    private LocalDateTime lastScanTime;

    /**
     * 病毒库信息
     */
    @TableField("signatures_version")
    private String signaturesVersion;

    @TableField("last_update_time")
    private LocalDateTime lastUpdateTime;

    @TableField("signatures_count")
    private Long signaturesCount;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 状态 (ACTIVE, INACTIVE, ERROR, UPDATING)
     */
    @TableField("status")
    private String status;

    /**
     * 版本信息
     */
    @TableField("version")
    private String version;

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