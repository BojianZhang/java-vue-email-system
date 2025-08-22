package com.enterprise.email.service;

import com.enterprise.email.entity.RspamdConfig;

import java.util.List;
import java.util.Map;

/**
 * Rspamd反垃圾邮件服务
 */
public interface RspamdService {

    /**
     * 创建Rspamd配置
     */
    boolean createRspamdConfig(RspamdConfig config);

    /**
     * 更新Rspamd配置
     */
    boolean updateRspamdConfig(RspamdConfig config);

    /**
     * 删除Rspamd配置
     */
    boolean deleteRspamdConfig(Long configId);

    /**
     * 根据域名获取Rspamd配置
     */
    RspamdConfig getRspamdConfig(String domain);

    /**
     * 获取所有启用的Rspamd配置
     */
    List<RspamdConfig> getEnabledConfigs();

    /**
     * 检查邮件是否为垃圾邮件
     */
    Map<String, Object> checkSpam(String emailContent, String senderIp, String domain);

    /**
     * 学习垃圾邮件
     */
    boolean learnSpam(String emailContent, boolean isSpam);

    /**
     * 训练贝叶斯过滤器
     */
    boolean trainBayesFilter(List<String> spamEmails, List<String> hamEmails);

    /**
     * 添加到白名单
     */
    boolean addToWhitelist(String domain, String entry, String type);

    /**
     * 从白名单移除
     */
    boolean removeFromWhitelist(String domain, String entry, String type);

    /**
     * 获取白名单
     */
    Map<String, Object> getWhitelist(String domain);

    /**
     * 添加到黑名单
     */
    boolean addToBlacklist(String domain, String entry, String type);

    /**
     * 从黑名单移除
     */
    boolean removeFromBlacklist(String domain, String entry, String type);

    /**
     * 获取黑名单
     */
    Map<String, Object> getBlacklist(String domain);

    /**
     * 获取垃圾邮件统计信息
     */
    Map<String, Object> getSpamStatistics(String domain);

    /**
     * 获取Rspamd服务状态
     */
    Map<String, Object> getRspamdStatus(String domain);

    /**
     * 重启Rspamd服务
     */
    boolean restartRspamdService(String domain);

    /**
     * 生成Rspamd配置文件
     */
    String generateRspamdConfig(RspamdConfig config);

    /**
     * 验证Rspamd配置
     */
    boolean validateRspamdConfig(RspamdConfig config);

    /**
     * 获取历史记录
     */
    List<Map<String, Object>> getSpamHistory(String domain, int limit);

    /**
     * 清理历史记录
     */
    boolean cleanupHistory(String domain, int days);

    /**
     * 更新DNSBL服务器列表
     */
    boolean updateDnsblServers(String domain, List<String> servers);

    /**
     * 测试DNSBL服务器
     */
    Map<String, Object> testDnsblServers(String domain);

    /**
     * 获取神经网络训练状态
     */
    Map<String, Object> getNeuralNetworkStatus(String domain);

    /**
     * 训练神经网络
     */
    boolean trainNeuralNetwork(String domain, boolean forceRetrain);

    /**
     * 获取模糊哈希统计
     */
    Map<String, Object> getFuzzyStats(String domain);

    /**
     * 添加模糊哈希规则
     */
    boolean addFuzzyRule(String domain, String hash, String flag);

    /**
     * 删除模糊哈希规则
     */
    boolean removeFuzzyRule(String domain, String hash);

    /**
     * 获取自定义规则
     */
    List<Map<String, Object>> getCustomRules(String domain);

    /**
     * 添加自定义规则
     */
    boolean addCustomRule(String domain, String ruleName, String ruleContent);

    /**
     * 删除自定义规则
     */
    boolean removeCustomRule(String domain, String ruleName);

    /**
     * 验证自定义规则语法
     */
    boolean validateCustomRule(String ruleContent);

    /**
     * 获取性能指标
     */
    Map<String, Object> getPerformanceMetrics(String domain);

    /**
     * 优化性能配置
     */
    boolean optimizePerformance(String domain);

    /**
     * 导出配置
     */
    String exportConfiguration(String domain);

    /**
     * 导入配置
     */
    boolean importConfiguration(String domain, String configData);

    /**
     * 批量处理邮件
     */
    List<Map<String, Object>> batchCheckSpam(List<String> emails, String domain);
}