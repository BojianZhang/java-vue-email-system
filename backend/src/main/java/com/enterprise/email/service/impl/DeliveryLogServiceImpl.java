package com.enterprise.email.service.impl;

import com.enterprise.email.entity.DeliveryLog;
import com.enterprise.email.mapper.DeliveryLogMapper;
import com.enterprise.email.service.DeliveryLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 投递日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryLogServiceImpl implements DeliveryLogService {

    private final DeliveryLogMapper deliveryLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    @Override
    public boolean logDelivery(DeliveryLog deliveryLog) {
        try {
            // 设置默认值
            setDefaultValues(deliveryLog);
            
            int result = deliveryLogMapper.insert(deliveryLog);
            if (result > 0) {
                log.debug("投递日志记录成功: messageId={}, recipient={}, status={}", 
                    deliveryLog.getMessageId(), deliveryLog.getRecipient(), deliveryLog.getStatus());
                return true;
            }
        } catch (Exception e) {
            log.error("记录投递日志失败: messageId={}, error={}", 
                deliveryLog.getMessageId(), e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateDeliveryStatus(String messageId, String status, String responseMessage) {
        try {
            List<DeliveryLog> logs = deliveryLogMapper.selectByMessageId(messageId);
            for (DeliveryLog log : logs) {
                log.setStatus(status);
                log.setResponseMessage(responseMessage);
                log.setUpdatedAt(LocalDateTime.now());
                
                if ("DELIVERED".equals(status)) {
                    log.setDeliveryCompletedAt(LocalDateTime.now());
                    log.setDeliveryResult("SUCCESS");
                } else if ("BOUNCED".equals(status) || "REJECTED".equals(status) || "FAILED".equals(status)) {
                    log.setDeliveryResult("PERM_FAILURE");
                } else if ("DEFERRED".equals(status)) {
                    log.setDeliveryResult("TEMP_FAILURE");
                    log.setRetryCount((log.getRetryCount() != null ? log.getRetryCount() : 0) + 1);
                    // 设置下次重试时间（指数退避）
                    int retryDelay = calculateRetryDelay(log.getRetryCount());
                    log.setNextRetryAt(LocalDateTime.now().plusMinutes(retryDelay));
                }
                
                deliveryLogMapper.updateById(log);
            }
            
            log.info("投递状态更新成功: messageId={}, status={}", messageId, status);
            return true;
            
        } catch (Exception e) {
            log.error("更新投递状态失败: messageId={}, status={}, error={}", messageId, status, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public DeliveryLog getDeliveryLog(Long logId) {
        return deliveryLogMapper.selectById(logId);
    }

    @Override
    public List<DeliveryLog> getDeliveryLogsByMessageId(String messageId) {
        return deliveryLogMapper.selectByMessageId(messageId);
    }

    @Override
    public List<DeliveryLog> getDeliveryLogsByStatus(String status, int limit) {
        return deliveryLogMapper.selectByStatus(status, limit);
    }

    @Override
    public List<DeliveryLog> getFailedDeliveries(int hours) {
        return deliveryLogMapper.selectFailedDeliveries(hours);
    }

    @Override
    public List<DeliveryLog> getPendingRetries() {
        return deliveryLogMapper.selectPendingRetries();
    }

    @Override
    public boolean processDeliveryRetry(Long logId) {
        try {
            DeliveryLog log = deliveryLogMapper.selectById(logId);
            if (log == null) {
                return false;
            }

            // 检查是否超过最大重试次数
            if (log.getRetryCount() >= log.getMaxRetries()) {
                log.setStatus("FAILED");
                log.setDeliveryResult("PERM_FAILURE");
                log.setErrorDetails("超过最大重试次数");
                deliveryLogMapper.updateById(log);
                return false;
            }

            // 重新投递逻辑（这里简化为状态更新）
            log.setStatus("SENDING");
            log.setRetryCount(log.getRetryCount() + 1);
            log.setUpdatedAt(LocalDateTime.now());
            
            // 模拟重新投递
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(1000); // 模拟投递时间
                    // 这里应该调用实际的SMTP投递逻辑
                    updateDeliveryStatus(log.getMessageId(), "DELIVERED", "250 OK");
                } catch (Exception e) {
                    updateDeliveryStatus(log.getMessageId(), "DEFERRED", "Retry failed: " + e.getMessage());
                }
            }, executor);

            deliveryLogMapper.updateById(log);
            log.info("投递重试已启动: logId={}, messageId={}", logId, log.getMessageId());
            return true;

        } catch (Exception e) {
            log.error("处理投递重试失败: logId={}, error={}", logId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> batchProcessRetries() {
        Map<String, Object> result = new HashMap<>();
        List<DeliveryLog> pendingRetries = getPendingRetries();
        
        int successCount = 0;
        int failureCount = 0;

        for (DeliveryLog log : pendingRetries) {
            try {
                if (processDeliveryRetry(log.getId())) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("批量处理重试失败: logId={}, error={}", log.getId(), e.getMessage(), e);
                failureCount++;
            }
        }

        result.put("total", pendingRetries.size());
        result.put("success", successCount);
        result.put("failure", failureCount);
        result.put("processedAt", LocalDateTime.now());

        log.info("批量处理投递重试完成: total={}, success={}, failure={}", 
            pendingRetries.size(), successCount, failureCount);

        return result;
    }

    @Override
    public Map<String, Object> getDeliveryStatistics(int hours) {
        try {
            List<Map<String, Object>> stats = deliveryLogMapper.selectDeliveryStatistics(hours);
            
            Map<String, Object> result = new HashMap<>();
            result.put("byStatus", stats);
            
            // 计算总体统计
            long totalCount = 0;
            long deliveredCount = 0;
            long failedCount = 0;
            double totalDelay = 0;
            double totalProcessingTime = 0;
            
            for (Map<String, Object> stat : stats) {
                String status = (String) stat.get("status");
                long count = ((Number) stat.get("count")).longValue();
                totalCount += count;
                
                if ("DELIVERED".equals(status)) {
                    deliveredCount += count;
                }
                if (Arrays.asList("BOUNCED", "REJECTED", "FAILED").contains(status)) {
                    failedCount += count;
                }
                
                Double avgDelay = (Double) stat.get("avg_delay");
                if (avgDelay != null) {
                    totalDelay += avgDelay * count;
                }
                
                Double avgProcessingTime = (Double) stat.get("avg_processing_time");
                if (avgProcessingTime != null) {
                    totalProcessingTime += avgProcessingTime * count;
                }
            }
            
            result.put("totalCount", totalCount);
            result.put("deliveredCount", deliveredCount);
            result.put("failedCount", failedCount);
            result.put("pendingCount", totalCount - deliveredCount - failedCount);
            
            if (totalCount > 0) {
                result.put("deliveryRate", Math.round((double) deliveredCount / totalCount * 10000.0) / 100.0);
                result.put("failureRate", Math.round((double) failedCount / totalCount * 10000.0) / 100.0);
                result.put("avgDelay", Math.round(totalDelay / totalCount * 100.0) / 100.0);
                result.put("avgProcessingTime", Math.round(totalProcessingTime / totalCount * 100.0) / 100.0);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("获取投递统计失败: hours={}, error={}", hours, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public List<Map<String, Object>> getDomainDeliveryStatistics(int hours) {
        return deliveryLogMapper.selectDomainDeliveryStatistics(hours);
    }

    @Override
    public List<Map<String, Object>> getHourlyDeliveryStatistics(int hours) {
        return deliveryLogMapper.selectHourlyDeliveryStatistics(hours);
    }

    @Override
    public Map<String, Object> getDeliveryDelayStatistics(int hours) {
        return deliveryLogMapper.selectDeliveryDelayStatistics(hours);
    }

    @Override
    public Map<String, Object> getQueueStatistics(int hours) {
        return deliveryLogMapper.selectQueueStatistics(hours);
    }

    @Override
    public List<Map<String, Object>> getAuthenticationStatistics(int hours) {
        return deliveryLogMapper.selectAuthenticationStatistics(hours);
    }

    @Override
    public List<Map<String, Object>> getTlsUsageStatistics(int hours) {
        return deliveryLogMapper.selectTlsUsageStatistics(hours);
    }

    @Override
    public Map<String, Object> getLargeMessageStatistics(long sizeThreshold, int hours) {
        return deliveryLogMapper.selectLargeMessageStatistics(sizeThreshold, hours);
    }

    @Override
    public List<Map<String, Object>> getErrorCodeStatistics(int hours) {
        return deliveryLogMapper.selectErrorCodeStatistics(hours);
    }

    @Override
    public List<DeliveryLog> searchDeliveryLogs(Map<String, Object> criteria) {
        try {
            // 简化实现 - 实际应使用动态查询
            if (criteria.containsKey("messageId")) {
                return getDeliveryLogsByMessageId((String) criteria.get("messageId"));
            } else if (criteria.containsKey("sender")) {
                return deliveryLogMapper.selectBySender((String) criteria.get("sender"), 100);
            } else if (criteria.containsKey("recipient")) {
                return deliveryLogMapper.selectByRecipient((String) criteria.get("recipient"), 100);
            } else if (criteria.containsKey("status")) {
                return getDeliveryLogsByStatus((String) criteria.get("status"), 100);
            } else {
                // 返回最近的日志
                return deliveryLogMapper.selectByTimeRange(
                    LocalDateTime.now().minusHours(24), 
                    LocalDateTime.now()
                );
            }
        } catch (Exception e) {
            log.error("搜索投递日志失败: criteria={}, error={}", criteria, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public String exportDeliveryLogs(Map<String, Object> criteria, String format) {
        try {
            List<DeliveryLog> logs = searchDeliveryLogs(criteria);
            
            switch (format.toLowerCase()) {
                case "json":
                    return objectMapper.writeValueAsString(logs);
                case "csv":
                    return convertLogsToCsv(logs);
                default:
                    return objectMapper.writeValueAsString(logs);
            }
            
        } catch (Exception e) {
            log.error("导出投递日志失败: criteria={}, format={}, error={}", criteria, format, e.getMessage(), e);
            return "";
        }
    }

    @Override
    public String generateDeliveryReport(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            StringBuilder report = new StringBuilder();
            report.append("投递报告\n");
            report.append("===================\n");
            report.append("时间范围: ").append(startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                  .append(" 到 ").append(endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

            List<DeliveryLog> logs = deliveryLogMapper.selectByTimeRange(startTime, endTime);
            
            // 统计各状态的数量
            Map<String, Long> statusCounts = logs.stream()
                .collect(HashMap::new,
                    (map, log) -> map.merge(log.getStatus(), 1L, Long::sum),
                    (map1, map2) -> { map1.putAll(map2); return map1; });

            report.append("投递统计:\n");
            statusCounts.forEach((status, count) -> 
                report.append("  ").append(status).append(": ").append(count).append("\n"));

            // 计算成功率
            long total = logs.size();
            long delivered = statusCounts.getOrDefault("DELIVERED", 0L);
            if (total > 0) {
                double successRate = (double) delivered / total * 100;
                report.append("\n成功率: ").append(String.format("%.2f%%", successRate)).append("\n");
            }

            return report.toString();
            
        } catch (Exception e) {
            log.error("生成投递报告失败: startTime={}, endTime={}, error={}", 
                startTime, endTime, e.getMessage(), e);
            return "报告生成失败: " + e.getMessage();
        }
    }

    @Override
    public Map<String, Object> analyzeDeliveryPerformance(int hours) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            Map<String, Object> stats = getDeliveryStatistics(hours);
            Map<String, Object> delayStats = getDeliveryDelayStatistics(hours);
            List<Map<String, Object>> hourlyStats = getHourlyDeliveryStatistics(hours);
            
            analysis.put("overallStats", stats);
            analysis.put("delayStatistics", delayStats);
            analysis.put("hourlyTrends", hourlyStats);
            
            // 性能评级
            double deliveryRate = (Double) stats.getOrDefault("deliveryRate", 0.0);
            String performanceGrade;
            if (deliveryRate >= 95) {
                performanceGrade = "优秀";
            } else if (deliveryRate >= 90) {
                performanceGrade = "良好";
            } else if (deliveryRate >= 80) {
                performanceGrade = "一般";
            } else {
                performanceGrade = "需要改进";
            }
            
            analysis.put("performanceGrade", performanceGrade);
            analysis.put("analyzedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("分析投递性能失败: hours={}, error={}", hours, e.getMessage(), e);
            analysis.put("error", e.getMessage());
        }
        
        return analysis;
    }

    @Override
    public List<Map<String, Object>> detectDeliveryIssues() {
        List<Map<String, Object>> issues = new ArrayList<>();
        
        try {
            // 检测高失败率
            Map<String, Object> stats = getDeliveryStatistics(24);
            double failureRate = (Double) stats.getOrDefault("failureRate", 0.0);
            if (failureRate > 10) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "HIGH_FAILURE_RATE");
                issue.put("severity", "HIGH");
                issue.put("description", String.format("失败率过高: %.2f%%", failureRate));
                issue.put("recommendation", "检查SMTP配置和目标服务器状态");
                issues.add(issue);
            }
            
            // 检测投递延迟
            Map<String, Object> delayStats = getDeliveryDelayStatistics(24);
            Double avgDelay = (Double) delayStats.get("avg_delay");
            if (avgDelay != null && avgDelay > 30000) { // 30秒
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "HIGH_DELIVERY_DELAY");
                issue.put("severity", "MEDIUM");
                issue.put("description", String.format("投递延迟过高: %.2f毫秒", avgDelay));
                issue.put("recommendation", "优化SMTP连接池配置");
                issues.add(issue);
            }
            
            // 检测重试队列积压
            List<DeliveryLog> pendingRetries = getPendingRetries();
            if (pendingRetries.size() > 100) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "RETRY_QUEUE_BACKLOG");
                issue.put("severity", "MEDIUM");
                issue.put("description", String.format("重试队列积压: %d条", pendingRetries.size()));
                issue.put("recommendation", "增加重试处理线程数");
                issues.add(issue);
            }
            
        } catch (Exception e) {
            log.error("检测投递问题失败: {}", e.getMessage(), e);
        }
        
        return issues;
    }

    // 简化实现的其他方法
    @Override
    public List<Map<String, Object>> getDeliveryTrends(int days) { 
        List<Map<String, Object>> trends = new ArrayList<>();
        for (int i = days; i >= 0; i--) {
            Map<String, Object> trend = new HashMap<>();
            trend.put("date", LocalDateTime.now().minusDays(i).toLocalDate());
            trend.put("totalCount", 100 + i * 10); // 模拟数据
            trend.put("deliveredCount", 90 + i * 9);
            trend.put("failedCount", 10 + i);
            trends.add(trend);
        }
        return trends;
    }

    @Override
    public List<Map<String, Object>> optimizeDeliveryPerformance() { return new ArrayList<>(); }

    @Override
    public Map<String, Object> monitorDeliveryQuality() { return new HashMap<>(); }

    @Override
    public boolean setupDeliveryAlerts(Map<String, Object> alertConfig) { return true; }

    @Override
    public List<Map<String, Object>> checkDeliveryAlerts() { return new ArrayList<>(); }

    @Override
    public boolean cleanupOldLogs(int days) {
        try {
            int deleted = deliveryLogMapper.cleanupOldLogs(days);
            log.info("清理旧投递日志完成: deleted={}, days={}", deleted, days);
            return true;
        } catch (Exception e) {
            log.error("清理旧投递日志失败: days={}, error={}", days, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean archiveDeliveryLogs(LocalDateTime before) { return true; }

    @Override
    public boolean compressLogData(int days) { return true; }

    @Override
    public boolean restoreDeliveryLogs(String archiveData) { return true; }

    @Override
    public Map<String, Object> validateLogIntegrity() { return new HashMap<>(); }

    @Override
    public boolean syncDeliveryStatus(String externalSystem) { return true; }

    @Override
    public Map<String, Object> getRealTimeDeliveryStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", LocalDateTime.now());
        status.put("pendingRetries", getPendingRetries().size());
        status.put("recentFailures", getFailedDeliveries(1).size());
        return status;
    }

    @Override
    public Map<String, Object> predictDeliveryPerformance(int futureDays) { return new HashMap<>(); }

    @Override
    public Map<String, Object> analyzeDeliveryPatterns() { return new HashMap<>(); }

    @Override
    public Map<String, Object> compareDeliveryPerformance(LocalDateTime period1Start, LocalDateTime period1End,
                                                          LocalDateTime period2Start, LocalDateTime period2End) { 
        return new HashMap<>(); 
    }

    @Override
    public List<Map<String, Object>> getDeliveryRecommendations() { return new ArrayList<>(); }

    @Override
    public boolean autoOptimizeDeliveryConfig() { return true; }

    // ========== 私有辅助方法 ==========

    private void setDefaultValues(DeliveryLog deliveryLog) {
        if (deliveryLog.getCreatedAt() == null) {
            deliveryLog.setCreatedAt(LocalDateTime.now());
        }
        if (deliveryLog.getUpdatedAt() == null) {
            deliveryLog.setUpdatedAt(LocalDateTime.now());
        }
        if (deliveryLog.getRetryCount() == null) {
            deliveryLog.setRetryCount(0);
        }
        if (deliveryLog.getMaxRetries() == null) {
            deliveryLog.setMaxRetries(3);
        }
        if (deliveryLog.getDeliveryStartedAt() == null) {
            deliveryLog.setDeliveryStartedAt(LocalDateTime.now());
        }
        if (deliveryLog.getProtocol() == null) {
            deliveryLog.setProtocol("SMTP");
        }
    }

    private int calculateRetryDelay(int retryCount) {
        // 指数退避算法: 2^retryCount 分钟，最大60分钟
        return Math.min((int) Math.pow(2, retryCount), 60);
    }

    private String convertLogsToCsv(List<DeliveryLog> logs) {
        StringBuilder csv = new StringBuilder();
        csv.append("MessageId,Sender,Recipient,Status,CreatedAt,DeliveryDelay\n");
        
        for (DeliveryLog log : logs) {
            csv.append(log.getMessageId()).append(",")
               .append(log.getSender()).append(",")
               .append(log.getRecipient()).append(",")
               .append(log.getStatus()).append(",")
               .append(log.getCreatedAt()).append(",")
               .append(log.getDeliveryDelay()).append("\n");
        }
        
        return csv.toString();
    }
}