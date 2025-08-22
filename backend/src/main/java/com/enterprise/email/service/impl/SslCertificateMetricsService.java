package com.enterprise.email.service.impl;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSL证书指标监控服务
 * 集成Prometheus/Micrometer进行性能监控
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SslCertificateMetricsService {

    private final MeterRegistry meterRegistry;
    
    // 计数器
    private Counter certificateObtainedCounter;
    private Counter certificateRenewalCounter;
    private Counter certificateValidationCounter;
    private Counter certificateErrorCounter;
    
    // 计时器
    private Timer certificateObtainTimer;
    private Timer certificateRenewalTimer;
    private Timer certificateValidationTimer;
    
    // 仪表盘
    private Gauge activeCertificatesGauge;
    private Gauge expiringCertificatesGauge;
    private Gauge expiredCertificatesGauge;
    
    // 存储实时数据
    private final AtomicInteger activeCertificatesCount = new AtomicInteger(0);
    private final AtomicInteger expiringCertificatesCount = new AtomicInteger(0);
    private final AtomicInteger expiredCertificatesCount = new AtomicInteger(0);
    private final Map<String, AtomicLong> domainMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> errorMetrics = new ConcurrentHashMap<>();

    @PostConstruct
    public void initMetrics() {
        log.info("初始化SSL证书监控指标");
        
        // 初始化计数器
        certificateObtainedCounter = Counter.builder("ssl.certificates.obtained")
            .description("成功获取的SSL证书总数")
            .tag("type", "total")
            .register(meterRegistry);

        certificateRenewalCounter = Counter.builder("ssl.certificates.renewed")
            .description("成功续期的SSL证书总数")
            .tag("type", "total")
            .register(meterRegistry);

        certificateValidationCounter = Counter.builder("ssl.certificates.validated")
            .description("验证的SSL证书总数")
            .register(meterRegistry);

        certificateErrorCounter = Counter.builder("ssl.certificates.errors")
            .description("SSL证书操作错误总数")
            .register(meterRegistry);

        // 初始化计时器
        certificateObtainTimer = Timer.builder("ssl.certificates.obtain.duration")
            .description("获取SSL证书耗时")
            .register(meterRegistry);

        certificateRenewalTimer = Timer.builder("ssl.certificates.renewal.duration")
            .description("续期SSL证书耗时")
            .register(meterRegistry);

        certificateValidationTimer = Timer.builder("ssl.certificates.validation.duration")
            .description("验证SSL证书耗时")
            .register(meterRegistry);

        // 初始化仪表盘
        activeCertificatesGauge = Gauge.builder("ssl.certificates.active")
            .description("当前活跃的SSL证书数量")
            .register(meterRegistry, this, obj -> activeCertificatesCount.get());

        expiringCertificatesGauge = Gauge.builder("ssl.certificates.expiring")
            .description("即将过期的SSL证书数量(30天内)")
            .register(meterRegistry, this, obj -> expiringCertificatesCount.get());

        expiredCertificatesGauge = Gauge.builder("ssl.certificates.expired")
            .description("已过期的SSL证书数量")
            .register(meterRegistry, this, obj -> expiredCertificatesCount.get());

        log.info("SSL证书监控指标初始化完成");
    }

    /**
     * 记录证书获取成功
     */
    public void recordCertificateObtained(String domain, String certificateType) {
        certificateObtainedCounter.increment(
            Tags.of(
                "domain", domain,
                "type", certificateType
            )
        );
        
        incrementDomainMetric(domain, "obtained");
        log.debug("记录证书获取成功: domain={}, type={}", domain, certificateType);
    }

    /**
     * 记录证书续期成功
     */
    public void recordCertificateRenewal(String domain, boolean successful) {
        if (successful) {
            certificateRenewalCounter.increment(
                Tags.of(
                    "domain", domain,
                    "status", "success"
                )
            );
            incrementDomainMetric(domain, "renewed");
        } else {
            certificateErrorCounter.increment(
                Tags.of(
                    "domain", domain,
                    "operation", "renewal",
                    "status", "failed"
                )
            );
            incrementErrorMetric("renewal_failed");
        }
        
        log.debug("记录证书续期结果: domain={}, successful={}", domain, successful);
    }

    /**
     * 记录证书验证
     */
    public void recordCertificateValidation(String domain, boolean valid, Duration duration) {
        certificateValidationCounter.increment(
            Tags.of(
                "domain", domain,
                "result", valid ? "valid" : "invalid"
            )
        );
        
        certificateValidationTimer.record(duration);
        
        if (!valid) {
            incrementErrorMetric("validation_failed");
        }
        
        log.debug("记录证书验证: domain={}, valid={}, duration={}ms", 
                domain, valid, duration.toMillis());
    }

    /**
     * 记录证书操作耗时
     */
    public Timer.Sample startTimer(String operation) {
        return Timer.start(meterRegistry);
    }

    /**
     * 停止计时并记录
     */
    public void stopTimer(Timer.Sample sample, String operation, String domain, boolean successful) {
        Timer timer;
        switch (operation.toLowerCase()) {
            case "obtain":
                timer = certificateObtainTimer;
                break;
            case "renewal":
                timer = certificateRenewalTimer;
                break;
            case "validation":
                timer = certificateValidationTimer;
                break;
            default:
                timer = Timer.builder("ssl.certificates." + operation + ".duration")
                    .description("SSL证书" + operation + "操作耗时")
                    .register(meterRegistry);
        }
        
        sample.stop(timer.tags(
            "domain", domain,
            "status", successful ? "success" : "failed"
        ));
    }

    /**
     * 更新证书状态统计
     */
    public void updateCertificateStatusCounts(int active, int expiring, int expired) {
        activeCertificatesCount.set(active);
        expiringCertificatesCount.set(expiring);
        expiredCertificatesCount.set(expired);
        
        log.debug("更新证书状态统计: active={}, expiring={}, expired={}", active, expiring, expired);
    }

    /**
     * 记录错误事件
     */
    public void recordError(String operation, String domain, String errorType, String errorMessage) {
        certificateErrorCounter.increment(
            Tags.of(
                "operation", operation,
                "domain", domain,
                "error_type", errorType
            )
        );
        
        incrementErrorMetric(errorType);
        
        log.debug("记录错误事件: operation={}, domain={}, errorType={}", operation, domain, errorType);
    }

    /**
     * 记录自定义指标
     */
    public void recordCustomMetric(String name, double value, String... tags) {
        Gauge.builder("ssl.certificates.custom." + name)
            .description("自定义SSL证书指标: " + name)
            .tags(tags)
            .register(meterRegistry, this, obj -> value);
    }

    /**
     * 获取域名指标
     */
    public Map<String, Long> getDomainMetrics() {
        Map<String, Long> result = new HashMap<>();
        domainMetrics.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    /**
     * 获取错误指标
     */
    public Map<String, Integer> getErrorMetrics() {
        Map<String, Integer> result = new HashMap<>();
        errorMetrics.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    /**
     * 获取系统健康指标
     */
    public Map<String, Object> getHealthMetrics() {
        Map<String, Object> health = new HashMap<>();
        
        // 基本统计
        health.put("activeCertificates", activeCertificatesCount.get());
        health.put("expiringCertificates", expiringCertificatesCount.get());
        health.put("expiredCertificates", expiredCertificatesCount.get());
        
        // 操作成功率
        double totalOperations = certificateObtainedCounter.count() + 
                               certificateRenewalCounter.count();
        double errorCount = certificateErrorCounter.count();
        double successRate = totalOperations > 0 ? 
            (totalOperations - errorCount) / totalOperations * 100 : 100.0;
        health.put("successRate", Math.round(successRate * 100.0) / 100.0);
        
        // 平均处理时间
        health.put("avgObtainTime", certificateObtainTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        health.put("avgRenewalTime", certificateRenewalTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        health.put("avgValidationTime", certificateValidationTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        
        // 总计数
        health.put("totalObtained", certificateObtainedCounter.count());
        health.put("totalRenewed", certificateRenewalCounter.count());
        health.put("totalValidated", certificateValidationCounter.count());
        health.put("totalErrors", certificateErrorCounter.count());
        
        return health;
    }

    /**
     * 重置指标
     */
    public void resetMetrics() {
        log.info("重置SSL证书监控指标");
        
        activeCertificatesCount.set(0);
        expiringCertificatesCount.set(0);
        expiredCertificatesCount.set(0);
        domainMetrics.clear();
        errorMetrics.clear();
        
        // 移除自定义仪表盘
        meterRegistry.getMeters().stream()
            .filter(meter -> meter.getId().getName().startsWith("ssl.certificates.custom."))
            .forEach(meterRegistry::remove);
    }

    /**
     * 创建监控报告
     */
    public String generateMetricsReport() {
        Map<String, Object> health = getHealthMetrics();
        StringBuilder report = new StringBuilder();
        
        report.append("SSL Certificate Metrics Report\n");
        report.append("==============================\n");
        report.append(String.format("Generated at: %s\n\n", new Date()));
        
        report.append("Certificate Status:\n");
        report.append(String.format("- Active: %s\n", health.get("activeCertificates")));
        report.append(String.format("- Expiring: %s\n", health.get("expiringCertificates")));
        report.append(String.format("- Expired: %s\n", health.get("expiredCertificates")));
        
        report.append("\nOperations Summary:\n");
        report.append(String.format("- Total Obtained: %.0f\n", health.get("totalObtained")));
        report.append(String.format("- Total Renewed: %.0f\n", health.get("totalRenewed")));
        report.append(String.format("- Total Validated: %.0f\n", health.get("totalValidated")));
        report.append(String.format("- Total Errors: %.0f\n", health.get("totalErrors")));
        report.append(String.format("- Success Rate: %.2f%%\n", health.get("successRate")));
        
        report.append("\nPerformance Metrics:\n");
        report.append(String.format("- Avg Obtain Time: %.2f ms\n", health.get("avgObtainTime")));
        report.append(String.format("- Avg Renewal Time: %.2f ms\n", health.get("avgRenewalTime")));
        report.append(String.format("- Avg Validation Time: %.2f ms\n", health.get("avgValidationTime")));
        
        if (!errorMetrics.isEmpty()) {
            report.append("\nError Breakdown:\n");
            errorMetrics.forEach((errorType, count) -> 
                report.append(String.format("- %s: %d\n", errorType, count.get())));
        }
        
        return report.toString();
    }

    // 私有辅助方法

    private void incrementDomainMetric(String domain, String operation) {
        String key = domain + "." + operation;
        domainMetrics.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    private void incrementErrorMetric(String errorType) {
        errorMetrics.computeIfAbsent(errorType, k -> new AtomicInteger(0)).incrementAndGet();
    }
}