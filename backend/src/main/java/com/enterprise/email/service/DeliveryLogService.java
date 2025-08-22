package com.enterprise.email.service;

import com.enterprise.email.entity.DeliveryLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 投递日志服务
 */
public interface DeliveryLogService {

    /**
     * 记录投递日志
     */
    boolean logDelivery(DeliveryLog deliveryLog);

    /**
     * 更新投递状态
     */
    boolean updateDeliveryStatus(String messageId, String status, String responseMessage);

    /**
     * 获取投递日志
     */
    DeliveryLog getDeliveryLog(Long logId);

    /**
     * 根据邮件ID获取投递日志
     */
    List<DeliveryLog> getDeliveryLogsByMessageId(String messageId);

    /**
     * 根据状态获取投递日志
     */
    List<DeliveryLog> getDeliveryLogsByStatus(String status, int limit);

    /**
     * 获取失败的投递
     */
    List<DeliveryLog> getFailedDeliveries(int hours);

    /**
     * 获取需要重试的投递
     */
    List<DeliveryLog> getPendingRetries();

    /**
     * 处理投递重试
     */
    boolean processDeliveryRetry(Long logId);

    /**
     * 批量处理重试
     */
    Map<String, Object> batchProcessRetries();

    /**
     * 获取投递统计
     */
    Map<String, Object> getDeliveryStatistics(int hours);

    /**
     * 获取域名投递统计
     */
    List<Map<String, Object>> getDomainDeliveryStatistics(int hours);

    /**
     * 获取每小时投递统计
     */
    List<Map<String, Object>> getHourlyDeliveryStatistics(int hours);

    /**
     * 获取投递延迟统计
     */
    Map<String, Object> getDeliveryDelayStatistics(int hours);

    /**
     * 获取队列统计
     */
    Map<String, Object> getQueueStatistics(int hours);

    /**
     * 获取认证统计
     */
    List<Map<String, Object>> getAuthenticationStatistics(int hours);

    /**
     * 获取TLS使用统计
     */
    List<Map<String, Object>> getTlsUsageStatistics(int hours);

    /**
     * 获取大邮件统计
     */
    Map<String, Object> getLargeMessageStatistics(long sizeThreshold, int hours);

    /**
     * 获取错误代码统计
     */
    List<Map<String, Object>> getErrorCodeStatistics(int hours);

    /**
     * 搜索投递日志
     */
    List<DeliveryLog> searchDeliveryLogs(Map<String, Object> criteria);

    /**
     * 导出投递日志
     */
    String exportDeliveryLogs(Map<String, Object> criteria, String format);

    /**
     * 生成投递报告
     */
    String generateDeliveryReport(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 分析投递性能
     */
    Map<String, Object> analyzeDeliveryPerformance(int hours);

    /**
     * 检测投递问题
     */
    List<Map<String, Object>> detectDeliveryIssues();

    /**
     * 获取投递趋势
     */
    List<Map<String, Object>> getDeliveryTrends(int days);

    /**
     * 优化投递性能
     */
    List<Map<String, Object>> optimizeDeliveryPerformance();

    /**
     * 监控投递质量
     */
    Map<String, Object> monitorDeliveryQuality();

    /**
     * 设置投递告警
     */
    boolean setupDeliveryAlerts(Map<String, Object> alertConfig);

    /**
     * 检查投递告警
     */
    List<Map<String, Object>> checkDeliveryAlerts();

    /**
     * 清理旧投递日志
     */
    boolean cleanupOldLogs(int days);

    /**
     * 归档投递日志
     */
    boolean archiveDeliveryLogs(LocalDateTime before);

    /**
     * 压缩日志数据
     */
    boolean compressLogData(int days);

    /**
     * 恢复投递日志
     */
    boolean restoreDeliveryLogs(String archiveData);

    /**
     * 验证日志完整性
     */
    Map<String, Object> validateLogIntegrity();

    /**
     * 同步投递状态
     */
    boolean syncDeliveryStatus(String externalSystem);

    /**
     * 获取实时投递状态
     */
    Map<String, Object> getRealTimeDeliveryStatus();

    /**
     * 预测投递性能
     */
    Map<String, Object> predictDeliveryPerformance(int futureDays);

    /**
     * 分析投递模式
     */
    Map<String, Object> analyzeDeliveryPatterns();

    /**
     * 比较投递性能
     */
    Map<String, Object> compareDeliveryPerformance(LocalDateTime period1Start, LocalDateTime period1End,
                                                   LocalDateTime period2Start, LocalDateTime period2End);

    /**
     * 获取投递建议
     */
    List<Map<String, Object>> getDeliveryRecommendations();

    /**
     * 自动优化投递配置
     */
    boolean autoOptimizeDeliveryConfig();
}