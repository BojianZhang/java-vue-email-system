package com.enterprise.email.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;
import java.net.*;
import java.io.*;

/**
 * 入侵检测系统 - 实时监控和威胁检测
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntrusionDetectionService {

    private final SecurityEventService securityEventService;
    private final AttackResponseService attackResponseService;
    private final NotificationService notificationService;

    @Value("${ids.enabled:true}")
    private boolean idsEnabled;

    @Value("${ids.sensitivity:medium}")
    private String sensitivity;

    @Value("${ids.network-monitoring:true}")
    private boolean networkMonitoring;

    // 威胁检测规则
    private final Map<String, ThreatRule> threatRules = new ConcurrentHashMap<>();
    
    // 网络连接监控
    private final Map<String, NetworkConnection> activeConnections = new ConcurrentHashMap<>();
    
    // 异常行为统计
    private final Map<String, BehaviorPattern> behaviorPatterns = new ConcurrentHashMap<>();
    
    // 监控任务执行器
    private final ScheduledExecutorService monitorExecutor = Executors.newScheduledThreadPool(4);

    /**
     * 启动入侵检测系统
     */
    public void startIDS() {
        if (!idsEnabled) {
            log.info("入侵检测系统未启用");
            return;
        }

        log.info("启动入侵检测系统...");
        
        // 1. 初始化威胁检测规则
        initializeThreatRules();
        
        // 2. 启动网络监控
        if (networkMonitoring) {
            startNetworkMonitoring();
        }
        
        // 3. 启动行为分析
        startBehaviorAnalysis();
        
        // 4. 启动文件完整性监控
        startFileIntegrityMonitoring();
        
        // 5. 启动系统资源监控
        startSystemResourceMonitoring();
        
        log.info("入侵检测系统启动完成");
    }

    /**
     * 初始化威胁检测规则
     */
    private void initializeThreatRules() {
        // SQL注入检测规则
        threatRules.put("SQL_INJECTION", new ThreatRule(
            "SQL_INJECTION",
            Pattern.compile("(?i)(union.*select|insert.*into|delete.*from|drop.*table|exec.*sp_)", Pattern.CASE_INSENSITIVE),
            ThreatLevel.HIGH,
            "SQL注入攻击检测"
        ));

        // XSS攻击检测规则
        threatRules.put("XSS_ATTACK", new ThreatRule(
            "XSS_ATTACK",
            Pattern.compile("(?i)(<script|javascript:|eval\\(|alert\\(|document\\.cookie)", Pattern.CASE_INSENSITIVE),
            ThreatLevel.HIGH,
            "XSS攻击检测"
        ));

        // 命令注入检测规则
        threatRules.put("COMMAND_INJECTION", new ThreatRule(
            "COMMAND_INJECTION",
            Pattern.compile("(?i)(cmd\\.exe|/bin/sh|bash|powershell|wget|curl.*http)", Pattern.CASE_INSENSITIVE),
            ThreatLevel.CRITICAL,
            "命令注入攻击检测"
        ));

        // 路径遍历检测规则
        threatRules.put("PATH_TRAVERSAL", new ThreatRule(
            "PATH_TRAVERSAL",
            Pattern.compile("(\\.\\./|\\.\\.\\\\/|%2e%2e%2f|etc/passwd|windows/system32)", Pattern.CASE_INSENSITIVE),
            ThreatLevel.HIGH,
            "路径遍历攻击检测"
        ));

        // 暴力破解检测规则
        threatRules.put("BRUTE_FORCE", new ThreatRule(
            "BRUTE_FORCE",
            Pattern.compile("(login|auth|signin)", Pattern.CASE_INSENSITIVE),
            ThreatLevel.MEDIUM,
            "暴力破解攻击检测"
        ));

        log.info("已加载 {} 个威胁检测规则", threatRules.size());
    }

    /**
     * 启动网络监控
     */
    private void startNetworkMonitoring() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                monitorNetworkConnections();
                detectSuspiciousNetworkActivity();
                monitorPortScanning();
            } catch (Exception e) {
                log.error("网络监控异常", e);
            }
        }, 0, 30, TimeUnit.SECONDS);

        log.info("网络监控已启动");
    }

    /**
     * 监控网络连接
     */
    private void monitorNetworkConnections() {
        try {
            // 使用netstat命令获取网络连接信息
            Process process = Runtime.getRuntime().exec("netstat -tuln");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                analyzeNetworkConnection(line);
            }
            
        } catch (Exception e) {
            log.error("监控网络连接失败", e);
        }
    }

    /**
     * 分析网络连接
     */
    private void analyzeNetworkConnection(String connectionLine) {
        // 解析网络连接信息并检测异常
        if (connectionLine.contains("ESTABLISHED") || connectionLine.contains("LISTEN")) {
            // 提取IP和端口信息
            String[] parts = connectionLine.trim().split("\\s+");
            if (parts.length >= 4) {
                String localAddress = parts[3];
                String foreignAddress = parts.length > 4 ? parts[4] : "";
                
                // 检测可疑连接
                checkSuspiciousConnection(localAddress, foreignAddress);
            }
        }
    }

    /**
     * 检测可疑网络活动
     */
    private void detectSuspiciousNetworkActivity() {
        try {
            // 检测异常的网络流量模式
            for (NetworkConnection conn : activeConnections.values()) {
                if (conn.isTimeoutExceeded() || conn.hasAbnormalTraffic()) {
                    handleSuspiciousNetworkActivity(conn);
                }
            }
            
            // 清理过期连接
            activeConnections.entrySet().removeIf(entry -> 
                entry.getValue().getLastActivity().isBefore(LocalDateTime.now().minusMinutes(5))
            );
            
        } catch (Exception e) {
            log.error("检测可疑网络活动失败", e);
        }
    }

    /**
     * 监控端口扫描
     */
    private void monitorPortScanning() {
        // 检测短时间内对多个端口的连接尝试
        Map<String, Integer> portAttempts = new ConcurrentHashMap<>();
        
        for (NetworkConnection conn : activeConnections.values()) {
            String sourceIp = conn.getSourceIp();
            portAttempts.merge(sourceIp, 1, Integer::sum);
        }
        
        portAttempts.forEach((ip, attempts) -> {
            if (attempts > 10) { // 短时间内尝试连接超过10个端口
                log.warn("检测到端口扫描行为: IP={}, 尝试次数={}", ip, attempts);
                handlePortScanningDetection(ip, attempts);
            }
        });
    }

    /**
     * 启动行为分析
     */
    private void startBehaviorAnalysis() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                analyzeBehaviorPatterns();
                detectAnomalousActivities();
            } catch (Exception e) {
                log.error("行为分析异常", e);
            }
        }, 0, 60, TimeUnit.SECONDS);

        log.info("行为分析已启动");
    }

    /**
     * 分析行为模式
     */
    private void analyzeBehaviorPatterns() {
        for (BehaviorPattern pattern : behaviorPatterns.values()) {
            if (pattern.isAnomalous()) {
                handleAnomalousBehavior(pattern);
            }
        }
    }

    /**
     * 检测异常活动
     */
    private void detectAnomalousActivities() {
        // 检测异常登录模式
        detectAnomalousLogins();
        
        // 检测异常数据访问
        detectAnomalousDataAccess();
        
        // 检测异常系统调用
        detectAnomalousSystemCalls();
    }

    /**
     * 启动文件完整性监控
     */
    private void startFileIntegrityMonitoring() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                checkCriticalFileIntegrity();
                monitorConfigurationChanges();
                detectUnauthorizedFileModifications();
            } catch (Exception e) {
                log.error("文件完整性监控异常", e);
            }
        }, 0, 300, TimeUnit.SECONDS); // 每5分钟检查一次

        log.info("文件完整性监控已启动");
    }

    /**
     * 检查关键文件完整性
     */
    private void checkCriticalFileIntegrity() {
        String[] criticalFiles = {
            "/etc/passwd",
            "/etc/shadow",
            "/etc/hosts",
            "/etc/ssh/sshd_config",
            "application.properties",
            "application.yml"
        };

        for (String filePath : criticalFiles) {
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    String currentHash = calculateFileHash(file);
                    String expectedHash = getExpectedFileHash(filePath);
                    
                    if (expectedHash != null && !currentHash.equals(expectedHash)) {
                        handleFileIntegrityViolation(filePath, expectedHash, currentHash);
                    }
                }
            } catch (Exception e) {
                log.error("检查文件完整性失败: {}", filePath, e);
            }
        }
    }

    /**
     * 启动系统资源监控
     */
    private void startSystemResourceMonitoring() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                monitorCpuUsage();
                monitorMemoryUsage();
                monitorDiskUsage();
                monitorNetworkTraffic();
            } catch (Exception e) {
                log.error("系统资源监控异常", e);
            }
        }, 0, 30, TimeUnit.SECONDS);

        log.info("系统资源监控已启动");
    }

    /**
     * 处理威胁检测
     */
    public void processThreatDetection(String content, String source, String requestInfo) {
        if (!idsEnabled) {
            return;
        }

        for (ThreatRule rule : threatRules.values()) {
            if (rule.matches(content)) {
                handleThreatDetection(rule, source, content, requestInfo);
            }
        }
    }

    /**
     * 处理威胁检测结果
     */
    private void handleThreatDetection(ThreatRule rule, String source, String content, String requestInfo) {
        log.warn("检测到威胁: 规则={}, 来源={}, 内容={}", rule.getName(), source, content);

        // 记录安全事件
        securityEventService.recordSecurityEvent(
            rule.getName(),
            source,
            String.format("威胁检测: %s, 请求信息: %s", content, requestInfo),
            mapThreatLevelToSecurityLevel(rule.getLevel())
        );

        // 触发响应
        switch (rule.getLevel()) {
            case CRITICAL:
                attackResponseService.handleHighSeverityAttack(source, rule.getName(), content);
                sendCriticalThreatAlert(rule, source, content);
                break;
            case HIGH:
                attackResponseService.handleHighSeverityAttack(source, rule.getName(), content);
                break;
            case MEDIUM:
                attackResponseService.handleMediumSeverityAttack(source, rule.getName(), content);
                break;
            case LOW:
                attackResponseService.handleLowSeverityAttack(source, rule.getName(), content);
                break;
        }
    }

    /**
     * 发送严重威胁告警
     */
    private void sendCriticalThreatAlert(ThreatRule rule, String source, String content) {
        String message = String.format(
            "🚨 严重威胁告警 🚨\n\n" +
            "检测到严重安全威胁:\n" +
            "威胁类型: %s\n" +
            "攻击来源: %s\n" +
            "威胁内容: %s\n" +
            "检测时间: %s\n\n" +
            "请立即采取应急措施！",
            rule.getName(), source, content, LocalDateTime.now()
        );

        notificationService.sendEmergencyAlert(message);
    }

    // 辅助方法实现
    private void checkSuspiciousConnection(String local, String foreign) {
        // 检测可疑连接的逻辑
    }

    private void handleSuspiciousNetworkActivity(NetworkConnection conn) {
        log.warn("检测到可疑网络活动: {}", conn);
        // 处理可疑网络活动
    }

    private void handlePortScanningDetection(String ip, int attempts) {
        attackResponseService.handleMediumSeverityAttack(ip, "PORT_SCANNING", "尝试次数: " + attempts);
    }

    private void handleAnomalousBehavior(BehaviorPattern pattern) {
        log.warn("检测到异常行为模式: {}", pattern);
        // 处理异常行为
    }

    private void detectAnomalousLogins() {
        // 检测异常登录模式
    }

    private void detectAnomalousDataAccess() {
        // 检测异常数据访问
    }

    private void detectAnomalousSystemCalls() {
        // 检测异常系统调用
    }

    private void monitorConfigurationChanges() {
        // 监控配置文件变化
    }

    private void detectUnauthorizedFileModifications() {
        // 检测未授权文件修改
    }

    private String calculateFileHash(File file) {
        // 计算文件哈希值
        return "";
    }

    private String getExpectedFileHash(String filePath) {
        // 获取预期的文件哈希值
        return null;
    }

    private void handleFileIntegrityViolation(String filePath, String expected, String actual) {
        log.error("文件完整性违规: 文件={}, 预期={}, 实际={}", filePath, expected, actual);
        
        securityEventService.recordSecurityEvent(
            "FILE_INTEGRITY_VIOLATION",
            "SYSTEM",
            String.format("文件: %s, 预期哈希: %s, 实际哈希: %s", filePath, expected, actual),
            AttackDetectionInterceptor.SecurityLevel.HIGH
        );
    }

    private void monitorCpuUsage() {
        // 监控CPU使用率
    }

    private void monitorMemoryUsage() {
        // 监控内存使用率
    }

    private void monitorDiskUsage() {
        // 监控磁盘使用率
    }

    private void monitorNetworkTraffic() {
        // 监控网络流量
    }

    private AttackDetectionInterceptor.SecurityLevel mapThreatLevelToSecurityLevel(ThreatLevel level) {
        switch (level) {
            case CRITICAL: return AttackDetectionInterceptor.SecurityLevel.CRITICAL;
            case HIGH: return AttackDetectionInterceptor.SecurityLevel.HIGH;
            case MEDIUM: return AttackDetectionInterceptor.SecurityLevel.MEDIUM;
            case LOW: return AttackDetectionInterceptor.SecurityLevel.LOW;
            default: return AttackDetectionInterceptor.SecurityLevel.MEDIUM;
        }
    }

    /**
     * 威胁级别枚举
     */
    public enum ThreatLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * 威胁检测规则
     */
    private static class ThreatRule {
        private final String name;
        private final Pattern pattern;
        private final ThreatLevel level;
        private final String description;

        public ThreatRule(String name, Pattern pattern, ThreatLevel level, String description) {
            this.name = name;
            this.pattern = pattern;
            this.level = level;
            this.description = description;
        }

        public boolean matches(String content) {
            return pattern.matcher(content).find();
        }

        // Getters
        public String getName() { return name; }
        public ThreatLevel getLevel() { return level; }
        public String getDescription() { return description; }
    }

    /**
     * 网络连接信息
     */
    private static class NetworkConnection {
        private final String sourceIp;
        private final String targetIp;
        private final int sourcePort;
        private final int targetPort;
        private final LocalDateTime establishedTime;
        private LocalDateTime lastActivity;
        private long bytesTransferred;

        public NetworkConnection(String sourceIp, String targetIp, int sourcePort, int targetPort) {
            this.sourceIp = sourceIp;
            this.targetIp = targetIp;
            this.sourcePort = sourcePort;
            this.targetPort = targetPort;
            this.establishedTime = LocalDateTime.now();
            this.lastActivity = LocalDateTime.now();
        }

        public boolean isTimeoutExceeded() {
            return lastActivity.isBefore(LocalDateTime.now().minusMinutes(30));
        }

        public boolean hasAbnormalTraffic() {
            return bytesTransferred > 100 * 1024 * 1024; // 100MB
        }

        // Getters
        public String getSourceIp() { return sourceIp; }
        public LocalDateTime getLastActivity() { return lastActivity; }
    }

    /**
     * 行为模式
     */
    private static class BehaviorPattern {
        private final String userId;
        private final Map<String, Integer> actionCounts = new ConcurrentHashMap<>();
        private LocalDateTime lastUpdate = LocalDateTime.now();

        public BehaviorPattern(String userId) {
            this.userId = userId;
        }

        public boolean isAnomalous() {
            // 简化的异常检测逻辑
            return actionCounts.values().stream().anyMatch(count -> count > 1000);
        }

        public String getUserId() { return userId; }
    }
}