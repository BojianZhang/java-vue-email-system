package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Rspamd反垃圾邮件配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("rspamd_configs")
public class RspamdConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField("domain")
    private String domain;

    /**
     * Rspamd服务器地址
     */
    @TableField("rspamd_host")
    private String rspamdHost;

    @TableField("rspamd_port")
    private Integer rspamdPort;

    /**
     * Rspamd Web界面配置
     */
    @TableField("web_interface_host")
    private String webInterfaceHost;

    @TableField("web_interface_port")
    private Integer webInterfacePort;

    @TableField("web_password")
    private String webPassword;

    /**
     * 垃圾邮件阈值配置
     */
    @TableField("spam_threshold")
    private Double spamThreshold;

    @TableField("reject_threshold")
    private Double rejectThreshold;

    @TableField("greylisting_threshold")
    private Double greylistingThreshold;

    /**
     * 行为配置
     */
    @TableField("add_spam_header")
    private Boolean addSpamHeader;

    @TableField("quarantine_spam")
    private Boolean quarantineSpam;

    @TableField("reject_spam")
    private Boolean rejectSpam;

    /**
     * 贝叶斯过滤配置
     */
    @TableField("bayes_enabled")
    private Boolean bayesEnabled;

    @TableField("bayes_autolearn")
    private Boolean bayesAutolearn;

    @TableField("bayes_autolearn_threshold_spam")
    private Double bayesAutolearnThresholdSpam;

    @TableField("bayes_autolearn_threshold_ham")
    private Double bayesAutolearnThresholdHam;

    /**
     * DKIM验证配置
     */
    @TableField("dkim_enabled")
    private Boolean dkimEnabled;

    @TableField("dkim_check_policy")
    private String dkimCheckPolicy;

    /**
     * SPF验证配置
     */
    @TableField("spf_enabled")
    private Boolean spfEnabled;

    @TableField("spf_check_policy")
    private String spfCheckPolicy;

    /**
     * DMARC验证配置
     */
    @TableField("dmarc_enabled")
    private Boolean dmarcEnabled;

    @TableField("dmarc_check_policy")
    private String dmarcCheckPolicy;

    /**
     * 黑名单配置
     */
    @TableField("dnsbl_enabled")
    private Boolean dnsblEnabled;

    @TableField("dnsbl_servers")
    private String dnsblServers;

    @TableField("surbl_enabled")
    private Boolean surblEnabled;

    @TableField("surbl_servers")
    private String surblServers;

    /**
     * 白名单配置
     */
    @TableField("whitelist_enabled")
    private Boolean whitelistEnabled;

    @TableField("whitelist_domains")
    private String whitelistDomains;

    @TableField("whitelist_ips")
    private String whitelistIps;

    /**
     * 速率限制配置
     */
    @TableField("ratelimit_enabled")
    private Boolean ratelimitEnabled;

    @TableField("ratelimit_bounce_to")
    private String ratelimitBounceTo;

    @TableField("ratelimit_bounce_to_ip")
    private String ratelimitBounceToIp;

    @TableField("ratelimit_to")
    private String ratelimitTo;

    @TableField("ratelimit_to_ip")
    private String ratelimitToIp;

    /**
     * 历史记录配置
     */
    @TableField("history_enabled")
    private Boolean historyEnabled;

    @TableField("history_rows")
    private Integer historyRows;

    /**
     * 神经网络配置
     */
    @TableField("neural_enabled")
    private Boolean neuralEnabled;

    @TableField("neural_short_enabled")
    private Boolean neuralShortEnabled;

    @TableField("neural_long_enabled")
    private Boolean neuralLongEnabled;

    /**
     * 模糊哈希配置
     */
    @TableField("fuzzy_enabled")
    private Boolean fuzzyEnabled;

    @TableField("fuzzy_servers")
    private String fuzzyServers;

    /**
     * 自定义规则配置
     */
    @TableField("custom_rules")
    private String customRules;

    /**
     * 日志配置
     */
    @TableField("log_level")
    private String logLevel;

    @TableField("log_file")
    private String logFile;

    /**
     * 性能配置
     */
    @TableField("worker_processes")
    private Integer workerProcesses;

    @TableField("max_workers")
    private Integer maxWorkers;

    /**
     * Redis配置
     */
    @TableField("redis_host")
    private String redisHost;

    @TableField("redis_port")
    private Integer redisPort;

    @TableField("redis_password")
    private String redisPassword;

    @TableField("redis_db")
    private Integer redisDb;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 状态 (ACTIVE, INACTIVE, ERROR, LEARNING)
     */
    @TableField("status")
    private String status;

    /**
     * 版本信息
     */
    @TableField("version")
    private String version;

    /**
     * 最后更新统计时间
     */
    @TableField("last_stats_update")
    private LocalDateTime lastStatsUpdate;

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