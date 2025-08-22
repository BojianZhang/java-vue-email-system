package com.enterprise.email.service;

import com.enterprise.email.entity.DmarcReport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DMARC报告服务
 */
public interface DmarcReportService {

    /**
     * 创建DMARC报告
     */
    boolean createDmarcReport(DmarcReport report);

    /**
     * 处理DMARC报告
     */
    boolean processDmarcReport(String reportContent, String format);

    /**
     * 解析DMARC报告
     */
    DmarcReport parseDmarcReport(String reportContent, String format);

    /**
     * 获取DMARC报告
     */
    DmarcReport getDmarcReport(Long reportId);

    /**
     * 根据域名获取DMARC报告
     */
    List<DmarcReport> getDmarcReportsByDomain(String domain);

    /**
     * 根据时间范围获取DMARC报告
     */
    List<DmarcReport> getDmarcReportsByDateRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取失败的认证报告
     */
    List<DmarcReport> getFailedAuthenticationReports(int hours);

    /**
     * 获取高风险报告
     */
    List<DmarcReport> getHighRiskReports(int hours);

    /**
     * 获取不合规报告
     */
    List<DmarcReport> getNonCompliantReports(int hours);

    /**
     * 获取DMARC统计
     */
    Map<String, Object> getDmarcStatistics(int hours);

    /**
     * 获取域名DMARC统计
     */
    List<Map<String, Object>> getDomainDmarcStatistics(int hours);

    /**
     * 获取发送方IP统计
     */
    List<Map<String, Object>> getSourceIpStatistics(int hours, int limit);

    /**
     * 获取DMARC策略效果统计
     */
    List<Map<String, Object>> getPolicyEffectivenessStatistics(int hours);

    /**
     * 获取认证失败原因统计
     */
    List<Map<String, Object>> getAuthFailureReasonStatistics(int hours);

    /**
     * 获取每日DMARC趋势
     */
    List<Map<String, Object>> getDailyDmarcTrend(int days);

    /**
     * 分析DMARC合规性
     */
    Map<String, Object> analyzeDmarcCompliance(String domain);

    /**
     * 检测DMARC策略问题
     */
    List<Map<String, Object>> detectDmarcPolicyIssues(String domain);

    /**
     * 生成DMARC建议
     */
    List<Map<String, Object>> generateDmarcRecommendations(String domain);

    /**
     * 优化DMARC策略
     */
    Map<String, Object> optimizeDmarcPolicy(String domain);

    /**
     * 验证DMARC配置
     */
    Map<String, Object> validateDmarcConfiguration(String domain);

    /**
     * 测试DMARC策略
     */
    Map<String, Object> testDmarcPolicy(String domain, Map<String, Object> testData);

    /**
     * 自动调整DMARC策略
     */
    boolean autoAdjustDmarcPolicy(String domain, Map<String, Object> criteria);

    /**
     * 监控DMARC合规性
     */
    Map<String, Object> monitorDmarcCompliance();

    /**
     * 设置DMARC告警
     */
    boolean setupDmarcAlerts(String domain, Map<String, Object> alertConfig);

    /**
     * 检查DMARC告警
     */
    List<Map<String, Object>> checkDmarcAlerts();

    /**
     * 生成DMARC报告
     */
    String generateDmarcReport(String domain, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 导出DMARC数据
     */
    String exportDmarcData(Map<String, Object> criteria, String format);

    /**
     * 导入DMARC报告
     */
    boolean importDmarcReports(String reportData, String format);

    /**
     * 聚合DMARC报告
     */
    Map<String, Object> aggregateDmarcReports(String domain, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 比较DMARC性能
     */
    Map<String, Object> compareDmarcPerformance(String domain, LocalDateTime period1Start, LocalDateTime period1End,
                                                LocalDateTime period2Start, LocalDateTime period2End);

    /**
     * 预测DMARC趋势
     */
    Map<String, Object> predictDmarcTrends(String domain, int futureDays);

    /**
     * 分析发送方行为
     */
    Map<String, Object> analyzeSenderBehavior(String sourceIp);

    /**
     * 检测异常发送活动
     */
    List<Map<String, Object>> detectAnomalousSendingActivity();

    /**
     * 识别恶意发送方
     */
    List<Map<String, Object>> identifyMaliciousSenders();

    /**
     * 分析域名欺骗尝试
     */
    List<Map<String, Object>> analyzeDomainSpoofingAttempts(String domain);

    /**
     * 生成威胁情报
     */
    Map<String, Object> generateThreatIntelligence();

    /**
     * 更新威胁指标
     */
    boolean updateThreatIndicators(List<Map<String, Object>> indicators);

    /**
     * 集成外部威胁情报
     */
    boolean integrateThreatIntelligence(String source, Map<String, Object> config);

    /**
     * 自动化DMARC响应
     */
    boolean automatedDmarcResponse(Map<String, Object> incident);

    /**
     * 清理过期报告
     */
    boolean cleanupExpiredReports(int days);

    /**
     * 归档DMARC报告
     */
    boolean archiveDmarcReports(LocalDateTime before);

    /**
     * 压缩报告数据
     */
    boolean compressReportData(int days);

    /**
     * 恢复DMARC报告
     */
    boolean restoreDmarcReports(String archiveData);

    /**
     * 同步外部DMARC报告
     */
    boolean syncExternalDmarcReports(String source, Map<String, Object> config);

    /**
     * 验证报告完整性
     */
    Map<String, Object> validateReportIntegrity();

    /**
     * 获取实时DMARC状态
     */
    Map<String, Object> getRealTimeDmarcStatus();

    /**
     * 计算DMARC评分
     */
    Map<String, Object> calculateDmarcScore(String domain);

    /**
     * 生成合规报告
     */
    String generateComplianceReport(String domain, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 创建DMARC仪表板
     */
    Map<String, Object> createDmarcDashboard(String domain);
}