package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * DNSBL黑名单配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dnsbl_configs")
public class DnsblConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * DNSBL服务器列表（JSON格式）
     */
    @TableField("dnsbl_servers")
    private String dnsblServers;

    /**
     * SURBL服务器列表（JSON格式）
     */
    @TableField("surbl_servers")
    private String surblServers;

    /**
     * URI黑名单服务器列表
     */
    @TableField("uribl_servers")
    private String uriblServers;

    /**
     * 检查超时时间（毫秒）
     */
    @TableField("timeout")
    private Integer timeout;

    /**
     * 最大并发查询数
     */
    @TableField("max_concurrent_queries")
    private Integer maxConcurrentQueries;

    /**
     * 缓存时间（秒）
     */
    @TableField("cache_ttl")
    private Integer cacheTtl;

    /**
     * 是否启用IP黑名单检查
     */
    @TableField("check_ip_blacklist")
    private Boolean checkIpBlacklist;

    /**
     * 是否启用域名黑名单检查
     */
    @TableField("check_domain_blacklist")
    private Boolean checkDomainBlacklist;

    /**
     * 是否启用URL黑名单检查
     */
    @TableField("check_url_blacklist")
    private Boolean checkUrlBlacklist;

    /**
     * 黑名单命中动作
     */
    @TableField("blacklist_action")
    private String blacklistAction; // REJECT, QUARANTINE, TAG, SCORE

    /**
     * 黑名单得分权重
     */
    @TableField("blacklist_score")
    private Double blacklistScore;

    /**
     * 白名单IP列表
     */
    @TableField("whitelist_ips")
    private String whitelistIps;

    /**
     * 白名单域名列表
     */
    @TableField("whitelist_domains")
    private String whitelistDomains;

    /**
     * 自定义黑名单IP
     */
    @TableField("custom_blacklist_ips")
    private String customBlacklistIps;

    /**
     * 自定义黑名单域名
     */
    @TableField("custom_blacklist_domains")
    private String customBlacklistDomains;

    /**
     * 是否记录查询日志
     */
    @TableField("log_queries")
    private Boolean logQueries;

    /**
     * 日志文件路径
     */
    @TableField("log_file")
    private String logFile;

    /**
     * 是否启用统计信息
     */
    @TableField("enable_statistics")
    private Boolean enableStatistics;

    /**
     * 统计信息
     */
    @TableField("total_queries")
    private Long totalQueries;

    @TableField("blacklist_hits")
    private Long blacklistHits;

    @TableField("whitelist_hits")
    private Long whitelistHits;

    /**
     * 响应时间统计
     */
    @TableField("avg_response_time")
    private Double avgResponseTime;

    @TableField("max_response_time")
    private Long maxResponseTime;

    @TableField("min_response_time")
    private Long minResponseTime;

    /**
     * 服务器状态检查
     */
    @TableField("last_health_check")
    private LocalDateTime lastHealthCheck;

    @TableField("healthy_servers")
    private Integer healthyServers;

    @TableField("total_servers")
    private Integer totalServers;

    /**
     * 错误统计
     */
    @TableField("timeout_errors")
    private Long timeoutErrors;

    @TableField("dns_errors")
    private Long dnsErrors;

    @TableField("connection_errors")
    private Long connectionErrors;

    /**
     * 最后查询时间
     */
    @TableField("last_query_at")
    private LocalDateTime lastQueryAt;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 状态
     */
    @TableField("status")
    private String status;

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