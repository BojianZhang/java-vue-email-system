package com.enterprise.email.service;

import com.enterprise.email.entity.SslCertificate;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * SSL证书性能优化和批量处理服务
 */
public interface SslCertificatePerformanceService {

    /**
     * 异步批量验证证书
     */
    @Async
    CompletableFuture<List<ValidationResult>> batchValidateCertificates(List<Long> certificateIds);

    /**
     * 并行续期多个证书
     */
    @Async
    CompletableFuture<BatchRenewalResult> parallelRenewCertificates(List<Long> certificateIds);

    /**
     * 预测性续期分析
     */
    PredictiveAnalysis analyzeCertificateRenewalTrends();

    /**
     * 证书使用统计分析
     */
    UsageAnalytics generateUsageAnalytics();

    /**
     * 性能指标收集
     */
    PerformanceMetrics collectPerformanceMetrics();

    /**
     * 智能负载均衡续期
     */
    CompletableFuture<Void> intelligentLoadBalancedRenewal();

    /**
     * 验证结果类
     */
    class ValidationResult {
        private Long certificateId;
        private String domain;
        private boolean valid;
        private List<String> issues;
        private long validationTimeMs;
        
        // Getters and setters
        public Long getCertificateId() { return certificateId; }
        public void setCertificateId(Long certificateId) { this.certificateId = certificateId; }
        
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getIssues() { return issues; }
        public void setIssues(List<String> issues) { this.issues = issues; }
        
        public long getValidationTimeMs() { return validationTimeMs; }
        public void setValidationTimeMs(long validationTimeMs) { this.validationTimeMs = validationTimeMs; }
    }

    /**
     * 批量续期结果类
     */
    class BatchRenewalResult {
        private int totalCertificates;
        private int successfulRenewals;
        private int failedRenewals;
        private List<String> failureReasons;
        private long totalProcessingTimeMs;
        
        // Getters and setters
        public int getTotalCertificates() { return totalCertificates; }
        public void setTotalCertificates(int totalCertificates) { this.totalCertificates = totalCertificates; }
        
        public int getSuccessfulRenewals() { return successfulRenewals; }
        public void setSuccessfulRenewals(int successfulRenewals) { this.successfulRenewals = successfulRenewals; }
        
        public int getFailedRenewals() { return failedRenewals; }
        public void setFailedRenewals(int failedRenewals) { this.failedRenewals = failedRenewals; }
        
        public List<String> getFailureReasons() { return failureReasons; }
        public void setFailureReasons(List<String> failureReasons) { this.failureReasons = failureReasons; }
        
        public long getTotalProcessingTimeMs() { return totalProcessingTimeMs; }
        public void setTotalProcessingTimeMs(long totalProcessingTimeMs) { this.totalProcessingTimeMs = totalProcessingTimeMs; }
    }

    /**
     * 预测性分析结果类
     */
    class PredictiveAnalysis {
        private List<RenewalPrediction> upcomingRenewals;
        private int certificatesExpiringIn7Days;
        private int certificatesExpiringIn30Days;
        private double averageRenewalSuccessRate;
        private List<String> recommendations;
        
        // Getters and setters
        public List<RenewalPrediction> getUpcomingRenewals() { return upcomingRenewals; }
        public void setUpcomingRenewals(List<RenewalPrediction> upcomingRenewals) { this.upcomingRenewals = upcomingRenewals; }
        
        public int getCertificatesExpiringIn7Days() { return certificatesExpiringIn7Days; }
        public void setCertificatesExpiringIn7Days(int certificatesExpiringIn7Days) { this.certificatesExpiringIn7Days = certificatesExpiringIn7Days; }
        
        public int getCertificatesExpiringIn30Days() { return certificatesExpiringIn30Days; }
        public void setCertificatesExpiringIn30Days(int certificatesExpiringIn30Days) { this.certificatesExpiringIn30Days = certificatesExpiringIn30Days; }
        
        public double getAverageRenewalSuccessRate() { return averageRenewalSuccessRate; }
        public void setAverageRenewalSuccessRate(double averageRenewalSuccessRate) { this.averageRenewalSuccessRate = averageRenewalSuccessRate; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }

    /**
     * 续期预测类
     */
    class RenewalPrediction {
        private String domain;
        private java.time.LocalDateTime expectedRenewalDate;
        private double renewalProbability;
        private String riskLevel;
        
        // Getters and setters
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public java.time.LocalDateTime getExpectedRenewalDate() { return expectedRenewalDate; }
        public void setExpectedRenewalDate(java.time.LocalDateTime expectedRenewalDate) { this.expectedRenewalDate = expectedRenewalDate; }
        
        public double getRenewalProbability() { return renewalProbability; }
        public void setRenewalProbability(double renewalProbability) { this.renewalProbability = renewalProbability; }
        
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    }

    /**
     * 使用统计类
     */
    class UsageAnalytics {
        private int totalCertificates;
        private long totalTraffic;
        private java.util.Map<String, Integer> certificatesByType;
        private java.util.Map<String, Integer> certificatesByStatus;
        private java.util.Map<String, Long> trafficByDomain;
        private double averageRenewalTime;
        
        // Getters and setters
        public int getTotalCertificates() { return totalCertificates; }
        public void setTotalCertificates(int totalCertificates) { this.totalCertificates = totalCertificates; }
        
        public long getTotalTraffic() { return totalTraffic; }
        public void setTotalTraffic(long totalTraffic) { this.totalTraffic = totalTraffic; }
        
        public java.util.Map<String, Integer> getCertificatesByType() { return certificatesByType; }
        public void setCertificatesByType(java.util.Map<String, Integer> certificatesByType) { this.certificatesByType = certificatesByType; }
        
        public java.util.Map<String, Integer> getCertificatesByStatus() { return certificatesByStatus; }
        public void setCertificatesByStatus(java.util.Map<String, Integer> certificatesByStatus) { this.certificatesByStatus = certificatesByStatus; }
        
        public java.util.Map<String, Long> getTrafficByDomain() { return trafficByDomain; }
        public void setTrafficByDomain(java.util.Map<String, Long> trafficByDomain) { this.trafficByDomain = trafficByDomain; }
        
        public double getAverageRenewalTime() { return averageRenewalTime; }
        public void setAverageRenewalTime(double averageRenewalTime) { this.averageRenewalTime = averageRenewalTime; }
    }

    /**
     * 性能指标类
     */
    class PerformanceMetrics {
        private double certificateValidationLatency;
        private double renewalSuccessRate;
        private long averageRenewalDuration;
        private int concurrentOperations;
        private double systemResourceUsage;
        private java.util.Map<String, Object> additionalMetrics;
        
        // Getters and setters
        public double getCertificateValidationLatency() { return certificateValidationLatency; }
        public void setCertificateValidationLatency(double certificateValidationLatency) { this.certificateValidationLatency = certificateValidationLatency; }
        
        public double getRenewalSuccessRate() { return renewalSuccessRate; }
        public void setRenewalSuccessRate(double renewalSuccessRate) { this.renewalSuccessRate = renewalSuccessRate; }
        
        public long getAverageRenewalDuration() { return averageRenewalDuration; }
        public void setAverageRenewalDuration(long averageRenewalDuration) { this.averageRenewalDuration = averageRenewalDuration; }
        
        public int getConcurrentOperations() { return concurrentOperations; }
        public void setConcurrentOperations(int concurrentOperations) { this.concurrentOperations = concurrentOperations; }
        
        public double getSystemResourceUsage() { return systemResourceUsage; }
        public void setSystemResourceUsage(double systemResourceUsage) { this.systemResourceUsage = systemResourceUsage; }
        
        public java.util.Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }
        public void setAdditionalMetrics(java.util.Map<String, Object> additionalMetrics) { this.additionalMetrics = additionalMetrics; }
    }
}