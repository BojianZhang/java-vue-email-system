package com.enterprise.email.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 外部API调用配置
 */
@Configuration
public class ExternalApiConfig {

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        
        // 设置连接超时时间
        factory.setConnectTimeout(Duration.ofSeconds(10));
        
        // 设置读取超时时间
        factory.setReadTimeout(Duration.ofSeconds(30));
        
        return new RestTemplate(factory);
    }
}