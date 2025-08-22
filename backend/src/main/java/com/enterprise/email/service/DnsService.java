package com.enterprise.email.service;

import com.enterprise.email.entity.DnsRecord;

import java.util.List;
import java.util.Map;

/**
 * DNS管理服务
 */
public interface DnsService {

    /**
     * 创建DNS记录
     */
    boolean createDnsRecord(DnsRecord record);

    /**
     * 更新DNS记录
     */
    boolean updateDnsRecord(DnsRecord record);

    /**
     * 删除DNS记录
     */
    boolean deleteDnsRecord(Long recordId);

    /**
     * 获取DNS记录
     */
    DnsRecord getDnsRecord(Long recordId);

    /**
     * 获取域名的DNS记录
     */
    List<DnsRecord> getDomainDnsRecords(String domain);

    /**
     * 根据类型获取DNS记录
     */
    List<DnsRecord> getDnsRecordsByType(String domain, String type);

    /**
     * 验证DNS记录
     */
    Map<String, Object> verifyDnsRecord(Long recordId);

    /**
     * 批量验证DNS记录
     */
    Map<String, Object> batchVerifyDnsRecords(List<Long> recordIds);

    /**
     * 查询DNS记录
     */
    Map<String, Object> queryDnsRecord(String name, String type);

    /**
     * 获取DNS统计信息
     */
    Map<String, Object> getDnsStatistics();

    /**
     * 自动配置邮件DNS记录
     */
    boolean autoConfigureEmailDns(String domain, Map<String, Object> config);

    /**
     * 生成标准邮件DNS记录
     */
    List<DnsRecord> generateStandardEmailDnsRecords(String domain, Map<String, Object> config);

    /**
     * 验证邮件DNS配置
     */
    Map<String, Object> verifyEmailDnsConfiguration(String domain);

    /**
     * 获取DNS配置建议
     */
    List<Map<String, Object>> getDnsConfigRecommendations(String domain);

    /**
     * 检测DNS配置问题
     */
    Map<String, Object> detectDnsConfigurationIssues(String domain);

    /**
     * 导入DNS记录
     */
    boolean importDnsRecords(String domain, List<Map<String, Object>> records);

    /**
     * 导出DNS记录
     */
    List<Map<String, Object>> exportDnsRecords(String domain);

    /**
     * 同步DNS记录到外部DNS服务商
     */
    boolean syncToExternalDns(String domain, String provider, Map<String, Object> credentials);

    /**
     * 从外部DNS服务商同步记录
     */
    boolean syncFromExternalDns(String domain, String provider, Map<String, Object> credentials);

    /**
     * 获取支持的DNS服务商
     */
    List<Map<String, Object>> getSupportedDnsProviders();

    /**
     * 测试DNS解析
     */
    Map<String, Object> testDnsResolution(String name, String type, String dnsServer);

    /**
     * 获取DNS传播状态
     */
    Map<String, Object> getDnsPropagationStatus(String name, String type);

    /**
     * 清理过期DNS记录
     */
    boolean cleanupExpiredDnsRecords();

    /**
     * 备份DNS记录
     */
    String backupDnsRecords(String domain);

    /**
     * 恢复DNS记录
     */
    boolean restoreDnsRecords(String domain, String backupData);

    /**
     * 监控DNS记录变化
     */
    Map<String, Object> monitorDnsChanges(String domain);

    /**
     * 获取DNS健康状态
     */
    Map<String, Object> getDnsHealthStatus(String domain);

    /**
     * 执行DNS维护任务
     */
    Map<String, Object> performDnsMaintenance();

    /**
     * 验证DNS服务器配置
     */
    Map<String, Object> verifyDnsServerConfiguration(String dnsServer);

    /**
     * 获取DNS查询历史
     */
    List<Map<String, Object>> getDnsQueryHistory(String name, String type, int limit);

    /**
     * 分析DNS性能
     */
    Map<String, Object> analyzeDnsPerformance(String domain);

    /**
     * 优化DNS配置
     */
    boolean optimizeDnsConfiguration(String domain, Map<String, Object> options);

    /**
     * 设置DNS监控
     */
    boolean setupDnsMonitoring(String domain, Map<String, Object> monitorConfig);

    /**
     * 获取DNS监控报告
     */
    Map<String, Object> getDnsMonitoringReport(String domain);
}