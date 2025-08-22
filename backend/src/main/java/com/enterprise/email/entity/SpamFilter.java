package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 反垃圾邮件过滤器配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spam_filters")
public class SpamFilter extends BaseEntity {
    
    /**
     * 域名ID
     */
    private Long domainId;
    
    /**
     * 用户ID（可选，用于用户级别配置）
     */
    private Long userId;
    
    /**
     * 过滤器名称
     */
    private String filterName;
    
    /**
     * Rspamd配置
     */
    private String rspamdConfig;
    
    /**
     * 垃圾邮件分数阈值
     */
    private Double spamThreshold;
    
    /**
     * 拒绝分数阈值
     */
    private Double rejectThreshold;
    
    /**
     * 灰名单启用状态
     */
    private Boolean greylistEnabled;
    
    /**
     * DNSBL黑名单检查
     */
    private Boolean dnsblEnabled;
    
    /**
     * SPF检查策略: none, softfail, fail, reject
     */
    private String spfPolicy;
    
    /**
     * DKIM签名验证
     */
    private Boolean dkimVerify;
    
    /**
     * DMARC策略执行
     */
    private String dmarcPolicy;
    
    /**
     * 贝叶斯过滤器启用
     */
    private Boolean bayesEnabled;
    
    /**
     * 自动学习垃圾邮件
     */
    private Boolean autoLearn;
    
    /**
     * 白名单规则
     */
    private String whitelistRules;
    
    /**
     * 黑名单规则
     */
    private String blacklistRules;
    
    /**
     * 自定义规则
     */
    private String customRules;
    
    /**
     * 垃圾邮件处理动作: tag, move, reject, discard
     */
    private String spamAction;
    
    /**
     * 垃圾邮件移动到的文件夹
     */
    private String spamFolder;
    
    /**
     * 启用状态
     */
    private Boolean enabled;
    
    /**
     * 统计信息：检查的邮件数
     */
    private Long checkedCount;
    
    /**
     * 统计信息：垃圾邮件数
     */
    private Long spamCount;
    
    /**
     * 统计信息：病毒邮件数
     */
    private Long virusCount;
    
    /**
     * 最后更新时间
     */
    private Date lastUpdated;
}