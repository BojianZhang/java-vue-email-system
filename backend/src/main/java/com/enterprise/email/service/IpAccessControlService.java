package com.enterprise.email.service;

import com.enterprise.email.entity.IpAccessControl;

import java.util.List;
import java.util.Map;

/**
 * IP访问控制服务
 */
public interface IpAccessControlService {

    /**
     * 创建访问控制规则
     */
    boolean createAccessRule(IpAccessControl rule);

    /**
     * 更新访问控制规则
     */
    boolean updateAccessRule(IpAccessControl rule);

    /**
     * 删除访问控制规则
     */
    boolean deleteAccessRule(Long ruleId);

    /**
     * 获取访问控制规则
     */
    IpAccessControl getAccessRule(Long ruleId);

    /**
     * 获取所有有效规则
     */
    List<IpAccessControl> getActiveRules();

    /**
     * 根据服务类型获取规则
     */
    List<IpAccessControl> getRulesByService(String service);

    /**
     * 检查IP访问权限
     */
    Map<String, Object> checkIpAccess(String ipAddress, String service, Integer port);

    /**
     * 验证IP访问
     */
    boolean validateIpAccess(String ipAddress, String service);

    /**
     * 阻止IP地址
     */
    boolean blockIpAddress(String ipAddress, String reason, Integer durationMinutes);

    /**
     * 解除IP阻止
     */
    boolean unblockIpAddress(String ipAddress);

    /**
     * 临时阻止IP
     */
    boolean temporaryBlockIp(String ipAddress, int durationMinutes, String reason);

    /**
     * 添加IP到白名单
     */
    boolean addIpToWhitelist(String ipAddress, String description);

    /**
     * 从白名单移除IP
     */
    boolean removeIpFromWhitelist(String ipAddress);

    /**
     * 设置速率限制
     */
    boolean setRateLimit(String ipAddress, int limit, int timeWindowSeconds);

    /**
     * 检查速率限制
     */
    boolean checkRateLimit(String ipAddress, String service);

    /**
     * 获取IP地理位置信息
     */
    Map<String, Object> getIpGeolocation(String ipAddress);

    /**
     * 根据地理位置创建规则
     */
    boolean createGeolocationRule(String countryCode, String ruleType, String action);

    /**
     * 批量阻止国家/地区
     */
    boolean batchBlockCountries(List<String> countryCodes, String reason);

    /**
     * 批量允许国家/地区
     */
    boolean batchAllowCountries(List<String> countryCodes);

    /**
     * 获取访问统计
     */
    Map<String, Object> getAccessStatistics();

    /**
     * 获取被阻止的IP统计
     */
    Map<String, Object> getBlockedIpStatistics();

    /**
     * 获取最活跃的IP
     */
    List<Map<String, Object>> getMostActiveIps(int limit);

    /**
     * 获取最近阻止的IP
     */
    List<IpAccessControl> getRecentlyBlockedIps(int hours, int limit);

    /**
     * 分析IP访问模式
     */
    Map<String, Object> analyzeIpAccessPatterns();

    /**
     * 检测异常IP活动
     */
    List<Map<String, Object>> detectAnomalousIpActivity();

    /**
     * 自动阻止恶意IP
     */
    boolean autoBlockMaliciousIps(Map<String, Object> criteria);

    /**
     * 获取IP信誉信息
     */
    Map<String, Object> getIpReputation(String ipAddress);

    /**
     * 批量导入访问规则
     */
    Map<String, Object> batchImportRules(List<Map<String, Object>> rules);

    /**
     * 导出访问规则
     */
    List<Map<String, Object>> exportAccessRules(String format);

    /**
     * 验证访问规则
     */
    Map<String, Object> validateAccessRule(IpAccessControl rule);

    /**
     * 测试访问规则
     */
    Map<String, Object> testAccessRule(Long ruleId, String testIp);

    /**
     * 启用/禁用规则
     */
    boolean toggleRule(Long ruleId, boolean enabled);

    /**
     * 批量启用/禁用规则
     */
    boolean batchToggleRules(List<Long> ruleIds, boolean enabled);

    /**
     * 清理过期临时规则
     */
    boolean cleanupExpiredTemporaryRules();

    /**
     * 重置速率限制计数
     */
    boolean resetRateLimitCounts();

    /**
     * 获取规则冲突
     */
    List<Map<String, Object>> getRuleConflicts();

    /**
     * 解决规则冲突
     */
    boolean resolveRuleConflicts(List<Map<String, Object>> resolutions);

    /**
     * 优化访问规则
     */
    List<Map<String, Object>> optimizeAccessRules();

    /**
     * 同步到防火墙
     */
    boolean syncToFirewall(String firewallType, Map<String, Object> config);

    /**
     * 从防火墙同步
     */
    boolean syncFromFirewall(String firewallType, Map<String, Object> config);

    /**
     * 生成防火墙规则
     */
    String generateFirewallRules(String firewallType);

    /**
     * 获取安全建议
     */
    List<Map<String, Object>> getSecurityRecommendations();

    /**
     * 执行安全扫描
     */
    Map<String, Object> performSecurityScan();

    /**
     * 生成安全报告
     */
    String generateSecurityReport(String startDate, String endDate);

    /**
     * 备份访问控制配置
     */
    String backupAccessControlConfiguration();

    /**
     * 恢复访问控制配置
     */
    boolean restoreAccessControlConfiguration(String backupData);
}