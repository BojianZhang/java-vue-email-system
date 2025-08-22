package com.enterprise.email.controller;

import com.enterprise.email.common.ApiResponse;
import com.enterprise.email.service.impl.SslCertificateMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.endpoint.annotation.Endpoint;
import org.springframework.boot.actuator.endpoint.annotation.ReadOperation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SSL证书Prometheus指标端点
 */
@Slf4j
@RestController
@RequestMapping("/api/ssl/metrics")
@RequiredArgsConstructor
@Tag(name = "SSL证书指标端点", description = "SSL证书Prometheus指标导出")
public class SslMetricsController {

    private final MeterRegistry meterRegistry;
    private final SslCertificateMetricsService metricsService;

    @Operation(summary = "获取Prometheus指标", description = "获取SSL证书相关的Prometheus指标数据")
    @GetMapping(value = "/prometheus", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public String getPrometheusMetrics() {
        log.debug("导出Prometheus指标");
        
        if (meterRegistry instanceof PrometheusMeterRegistry) {
            return ((PrometheusMeterRegistry) meterRegistry).scrape();
        } else {
            log.warn("当前不是Prometheus指标注册表，返回空指标");
            return "# Prometheus metrics not available\n";
        }
    }

    @Operation(summary = "获取健康指标", description = "获取SSL证书系统健康指标")
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Object> getHealthMetrics() {
        log.debug("获取健康指标");
        
        var healthMetrics = metricsService.getHealthMetrics();
        
        return ApiResponse.success(healthMetrics, "健康指标获取成功");
    }
}

/**
 * SSL证书指标Actuator端点
 */
@Component
@Endpoint(id = "ssl-certificates")
@Slf4j
@RequiredArgsConstructor
class SslCertificateActuatorEndpoint {

    private final SslCertificateMetricsService metricsService;

    @ReadOperation
    public Object sslCertificateMetrics() {
        log.debug("通过Actuator获取SSL证书指标");
        
        return metricsService.getHealthMetrics();
    }
}