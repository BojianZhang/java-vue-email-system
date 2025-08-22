package com.enterprise.email.controller;

import com.enterprise.email.common.ApiResponse;
import com.enterprise.email.service.SslCertificatePerformanceService;
import com.enterprise.email.service.SslCertificatePerformanceService.*;
import com.enterprise.email.service.impl.SslCertificateMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * SSL证书性能和监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ssl/performance")
@RequiredArgsConstructor
@Validated
@Tag(name = "SSL证书性能管理", description = "SSL证书性能优化和监控相关接口")
public class SslCertificatePerformanceController {

    private final SslCertificatePerformanceService performanceService;
    private final SslCertificateMetricsService metricsService;

    @Operation(summary = "批量验证SSL证书", description = "异步批量验证多个SSL证书的有效性")
    @PostMapping("/batch-validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> batchValidateCertificates(
            @Parameter(description = "证书ID列表") 
            @RequestBody @Valid @NotEmpty @Size(max = 50) List<Long> certificateIds) {
        
        log.info("开始批量验证证书: {} 个", certificateIds.size());
        
        CompletableFuture<List<ValidationResult>> future = 
            performanceService.batchValidateCertificates(certificateIds);
        
        // 异步处理，立即返回任务ID
        String taskId = "batch-validation-" + System.currentTimeMillis();
        
        future.thenAccept(results -> {
            log.info("批量验证完成: 任务ID={}, 结果数量={}", taskId, results.size());
            
            // 记录指标
            results.forEach(result -> {
                metricsService.recordCertificateValidation(
                    result.getDomain(), 
                    result.isValid(), 
                    java.time.Duration.ofMillis(result.getValidationTimeMs())
                );
            });
        }).exceptionally(throwable -> {
            log.error("批量验证失败: 任务ID={}", taskId, throwable);
            metricsService.recordError("batch_validation", "system", "processing_error", throwable.getMessage());
            return null;
        });
        
        return ApiResponse.success(taskId, "批量验证任务已启动");
    }

    @Operation(summary = "并行续期SSL证书", description = "并行续期多个SSL证书")
    @PostMapping("/parallel-renewal")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> parallelRenewCertificates(
            @Parameter(description = "证书ID列表") 
            @RequestBody @Valid @NotEmpty @Size(max = 20) List<Long> certificateIds) {
        
        log.info("开始并行续期证书: {} 个", certificateIds.size());
        
        CompletableFuture<BatchRenewalResult> future = 
            performanceService.parallelRenewCertificates(certificateIds);
        
        String taskId = "parallel-renewal-" + System.currentTimeMillis();
        
        future.thenAccept(result -> {
            log.info("并行续期完成: 任务ID={}, 成功={}, 失败={}", 
                taskId, result.getSuccessfulRenewals(), result.getFailedRenewals());
            
            // 记录续期指标
            for (int i = 0; i < result.getSuccessfulRenewals(); i++) {
                metricsService.recordCertificateRenewal("batch_domain_" + i, true);
            }
            for (int i = 0; i < result.getFailedRenewals(); i++) {
                metricsService.recordCertificateRenewal("batch_domain_" + i, false);
            }
        }).exceptionally(throwable -> {
            log.error("并行续期失败: 任务ID={}", taskId, throwable);
            metricsService.recordError("parallel_renewal", "system", "processing_error", throwable.getMessage());
            return null;
        });
        
        return ApiResponse.success(taskId, "并行续期任务已启动");
    }

    @Operation(summary = "获取预测性续期分析", description = "分析证书续期趋势并提供预测")
    @GetMapping("/predictive-analysis")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PredictiveAnalysis> getPredictiveAnalysis() {
        log.info("执行预测性续期分析");
        
        PredictiveAnalysis analysis = performanceService.analyzeCertificateRenewalTrends();
        
        return ApiResponse.success(analysis, "预测性分析完成");
    }

    @Operation(summary = "获取使用统计分析", description = "获取证书使用统计和分析数据")
    @GetMapping("/usage-analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UsageAnalytics> getUsageAnalytics() {
        log.info("生成使用统计分析");
        
        UsageAnalytics analytics = performanceService.generateUsageAnalytics();
        
        return ApiResponse.success(analytics, "使用统计分析完成");
    }

    @Operation(summary = "收集性能指标", description = "收集当前系统性能指标")
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PerformanceMetrics> getPerformanceMetrics() {
        log.info("收集性能指标");
        
        PerformanceMetrics metrics = performanceService.collectPerformanceMetrics();
        
        return ApiResponse.success(metrics, "性能指标收集完成");
    }

    @Operation(summary = "启动智能负载均衡续期", description = "启动智能负载均衡的证书续期任务")
    @PostMapping("/intelligent-renewal")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> startIntelligentRenewal() {
        log.info("启动智能负载均衡续期");
        
        CompletableFuture<Void> future = performanceService.intelligentLoadBalancedRenewal();
        
        String taskId = "intelligent-renewal-" + System.currentTimeMillis();
        
        future.thenRun(() -> {
            log.info("智能续期任务完成: 任务ID={}", taskId);
        }).exceptionally(throwable -> {
            log.error("智能续期任务失败: 任务ID={}", taskId, throwable);
            metricsService.recordError("intelligent_renewal", "system", "processing_error", throwable.getMessage());
            return null;
        });
        
        return ApiResponse.success(taskId, "智能续期任务已启动");
    }

    @Operation(summary = "获取监控指标摘要", description = "获取SSL证书监控指标摘要")
    @GetMapping("/monitoring/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getMonitoringSummary() {
        log.info("获取监控指标摘要");
        
        Map<String, Object> healthMetrics = metricsService.getHealthMetrics();
        
        return ApiResponse.success(healthMetrics, "监控指标摘要获取成功");
    }

    @Operation(summary = "获取域名指标", description = "获取按域名分组的指标数据")
    @GetMapping("/monitoring/domains")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Long>> getDomainMetrics() {
        log.info("获取域名指标");
        
        Map<String, Long> domainMetrics = metricsService.getDomainMetrics();
        
        return ApiResponse.success(domainMetrics, "域名指标获取成功");
    }

    @Operation(summary = "获取错误指标", description = "获取错误统计指标")
    @GetMapping("/monitoring/errors")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Integer>> getErrorMetrics() {
        log.info("获取错误指标");
        
        Map<String, Integer> errorMetrics = metricsService.getErrorMetrics();
        
        return ApiResponse.success(errorMetrics, "错误指标获取成功");
    }

    @Operation(summary = "生成监控报告", description = "生成详细的SSL证书监控报告")
    @GetMapping("/monitoring/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> generateMonitoringReport() {
        log.info("生成监控报告");
        
        String report = metricsService.generateMetricsReport();
        
        return ApiResponse.success(report, "监控报告生成成功");
    }

    @Operation(summary = "重置监控指标", description = "重置所有监控指标数据")
    @PostMapping("/monitoring/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resetMetrics() {
        log.info("重置监控指标");
        
        metricsService.resetMetrics();
        
        return ApiResponse.success(null, "监控指标已重置");
    }

    @Operation(summary = "记录自定义指标", description = "记录自定义SSL证书指标")
    @PostMapping("/monitoring/custom")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> recordCustomMetric(
            @Parameter(description = "指标名称") @RequestParam String metricName,
            @Parameter(description = "指标值") @RequestParam double value,
            @Parameter(description = "标签") @RequestParam(required = false) String... tags) {
        
        log.info("记录自定义指标: {}={}", metricName, value);
        
        metricsService.recordCustomMetric(metricName, value, tags != null ? tags : new String[0]);
        
        return ApiResponse.success(null, "自定义指标记录成功");
    }

    @Operation(summary = "更新证书状态统计", description = "更新证书状态统计数据")
    @PostMapping("/monitoring/status-update")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateCertificateStatusCounts(
            @Parameter(description = "活跃证书数") @RequestParam int active,
            @Parameter(description = "即将过期证书数") @RequestParam int expiring,
            @Parameter(description = "已过期证书数") @RequestParam int expired) {
        
        log.info("更新证书状态统计: active={}, expiring={}, expired={}", active, expiring, expired);
        
        metricsService.updateCertificateStatusCounts(active, expiring, expired);
        
        return ApiResponse.success(null, "证书状态统计已更新");
    }
}