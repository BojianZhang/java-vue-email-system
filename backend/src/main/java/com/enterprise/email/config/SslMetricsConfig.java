package com.enterprise.email.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuator.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * SSL证书监控指标配置
 */
@Slf4j
@Configuration
public class SslMetricsConfig {

    @Value("${management.metrics.export.prometheus.enabled:true}")
    private boolean prometheusEnabled;

    @Value("${spring.application.name:ssl-certificate-system}")
    private String applicationName;

    /**
     * Prometheus指标注册表
     */
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        if (prometheusEnabled) {
            log.info("启用Prometheus指标收集");
            return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        } else {
            log.info("使用简单指标注册表");
            return new SimpleMeterRegistry();
        }
    }

    /**
     * 自定义指标注册表配置
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config()
                .commonTags(
                    "application", applicationName,
                    "service", "ssl-certificate-management"
                );
            
            log.info("配置通用指标标签: application={}, service=ssl-certificate-management", applicationName);
        };
    }
}