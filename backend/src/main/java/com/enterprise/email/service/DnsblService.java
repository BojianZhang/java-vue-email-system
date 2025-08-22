package com.enterprise.email.service;

import com.enterprise.email.entity.DnsblConfig;

import java.util.List;
import java.util.Map;

/**
 * DNSBL黑名单检查服务
 */
public interface DnsblService {

    /**
     * 创建DNSBL配置
     */
    boolean createDnsblConfig(DnsblConfig config);

    /**
     * 更新DNSBL配置
     */
    boolean updateDnsblConfig(DnsblConfig config);

    /**
     * 删除DNSBL配置
     */
    boolean deleteDnsblConfig(Long configId);

    /**
     * 获取DNSBL配置
     */
    DnsblConfig getDnsblConfig(String domain);

    /**
     * 获取所有启用的DNSBL配置
     */
    List<DnsblConfig> getEnabledConfigs();

    /**
     * 检查IP是否在黑名单中
     */
    Map<String, Object> checkIpBlacklist(String ip, String domain);

    /**
     * 检查域名是否在黑名单中
     */
    Map<String, Object> checkDomainBlacklist(String domain, String checkDomain);

    /**
     * 检查URL是否在黑名单中
     */
    Map<String, Object> checkUrlBlacklist(String url, String domain);

    /**
     * 批量检查IP黑名单
     */
    Map<String, Object> batchCheckIpBlacklist(List<String> ips, String domain);

    /**
     * 批量检查域名黑名单
     */
    Map<String, Object> batchCheckDomainBlacklist(List<String> domains, String checkDomain);

    /**
     * 综合黑名单检查（IP + 域名 + URL）
     */
    Map<String, Object> comprehensiveBlacklistCheck(String ip, String fromDomain, List<String> urls, String domain);

    /**
     * 添加到自定义黑名单
     */
    boolean addToCustomBlacklist(String domain, String entry, String type);

    /**
     * 从自定义黑名单移除
     */
    boolean removeFromCustomBlacklist(String domain, String entry, String type);

    /**
     * 获取自定义黑名单
     */
    Map<String, Object> getCustomBlacklist(String domain);

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
     * 测试DNSBL服务器连通性
     */
    Map<String, Object> testDnsblServers(String domain);

    /**
     * 健康检查所有DNSBL服务器
     */
    Map<String, Object> healthCheckServers(String domain);

    /**
     * 获取DNSBL统计信息
     */
    Map<String, Object> getDnsblStatistics(String domain);

    /**
     * 获取黑名单查询历史
     */
    List<Map<String, Object>> getQueryHistory(String domain, int limit);

    /**
     * 清理查询历史
     */
    boolean cleanupQueryHistory(String domain, int days);

    /**
     * 更新DNSBL服务器列表
     */
    boolean updateDnsblServers(String domain, List<Map<String, Object>> servers);

    /**
     * 获取推荐的DNSBL服务器
     */
    List<Map<String, Object>> getRecommendedDnsblServers();

    /**
     * 自动配置DNSBL服务器
     */
    boolean autoConfigureDnsblServers(String domain);

    /**
     * 验证DNSBL配置
     */
    boolean validateDnsblConfig(DnsblConfig config);

    /**
     * 导出DNSBL配置
     */
    String exportDnsblConfig(String domain);

    /**
     * 导入DNSBL配置
     */
    boolean importDnsblConfig(String domain, String configData);

    /**
     * 获取实时黑名单状态
     */
    Map<String, Object> getRealTimeBlacklistStatus(String domain);

    /**
     * 优化DNSBL性能配置
     */
    boolean optimizeDnsblPerformance(String domain);

    /**
     * 分析黑名单命中趋势
     */
    Map<String, Object> analyzeBlacklistTrends(String domain, String period);

    /**
     * 生成黑名单报告
     */
    String generateBlacklistReport(String domain, String startDate, String endDate);

    /**
     * 缓存管理
     */
    boolean clearDnsblCache(String domain);

    /**
     * 获取缓存统计
     */
    Map<String, Object> getCacheStatistics(String domain);

    /**
     * 设置黑名单阈值
     */
    boolean setBlacklistThreshold(String domain, Map<String, Object> thresholds);

    /**
     * 获取黑名单阈值
     */
    Map<String, Object> getBlacklistThreshold(String domain);

    /**
     * 自动学习和更新黑名单
     */
    boolean autoLearnBlacklist(String domain, boolean enabled);

    /**
     * 获取黑名单学习状态
     */
    Map<String, Object> getBlacklistLearningStatus(String domain);
}