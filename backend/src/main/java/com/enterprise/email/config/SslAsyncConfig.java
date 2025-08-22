package com.enterprise.email.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * SSL证书异步处理配置
 */
@Slf4j
@Configuration
@EnableAsync
public class SslAsyncConfig {

    @Value("${ssl.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${ssl.async.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${ssl.async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${ssl.async.keep-alive-seconds:300}")
    private int keepAliveSeconds;

    @Value("${ssl.async.thread-name-prefix:ssl-task-}")
    private String threadNamePrefix;

    /**
     * SSL任务执行器 - 用于证书操作
     */
    @Bean("sslTaskExecutor")
    public Executor sslTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(corePoolSize);
        
        // 最大线程数
        executor.setMaxPoolSize(maxPoolSize);
        
        // 队列容量
        executor.setQueueCapacity(queueCapacity);
        
        // 线程保活时间
        executor.setKeepAliveSeconds(keepAliveSeconds);
        
        // 线程名前缀
        executor.setThreadNamePrefix(threadNamePrefix);
        
        // 拒绝策略 - 由调用者运行
        executor.setRejectedExecutionHandler(new CustomRejectedExecutionHandler());
        
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 关闭等待时间
        executor.setAwaitTerminationSeconds(60);
        
        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);
        
        executor.initialize();
        
        log.info("SSL任务执行器已初始化: 核心线程={}, 最大线程={}, 队列容量={}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }

    /**
     * SSL监控执行器 - 用于监控和健康检查
     */
    @Bean("sslMonitorExecutor")
    public Executor sslMonitorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 监控任务通常较轻量，使用较小的线程池
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(300);
        executor.setThreadNamePrefix("ssl-monitor-");
        
        executor.setRejectedExecutionHandler(new CustomRejectedExecutionHandler());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setAllowCoreThreadTimeOut(true);
        
        executor.initialize();
        
        log.info("SSL监控执行器已初始化");
        
        return executor;
    }

    /**
     * SSL续期执行器 - 专用于证书续期操作
     */
    @Bean("sslRenewalExecutor")
    public Executor sslRenewalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 续期操作需要控制并发数，避免对ACME服务器造成压力
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(20);
        executor.setKeepAliveSeconds(600);
        executor.setThreadNamePrefix("ssl-renewal-");
        
        // 续期操作使用更保守的拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        
        executor.initialize();
        
        log.info("SSL续期执行器已初始化");
        
        return executor;
    }

    /**
     * 自定义拒绝执行处理器
     */
    private static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            String taskName = r.getClass().getSimpleName();
            
            log.warn("SSL任务被拒绝执行: 任务={}, 活跃线程={}, 队列大小={}, 完成任务数={}", 
                    taskName, 
                    executor.getActiveCount(), 
                    executor.getQueue().size(), 
                    executor.getCompletedTaskCount());
            
            // 尝试直接在当前线程中执行
            if (!executor.isShutdown()) {
                try {
                    r.run();
                } catch (Exception e) {
                    log.error("在当前线程执行被拒绝的SSL任务失败", e);
                }
            }
        }
    }
}