package com.enterprise.email.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 攻击检测和防护拦截器
 */
@Slf4j
@Component
public class AttackDetectionInterceptor implements HandlerInterceptor {

    @Autowired
    private SecurityEventService securityEventService;
    
    @Autowired
    private AttackResponseService attackResponseService;

    // IP限流计数器
    private final ConcurrentHashMap<String, AtomicInteger> ipRequestCount = new ConcurrentHashMap<>();
    
    // IP黑名单
    private final ConcurrentHashMap<String, LocalDateTime> ipBlacklist = new ConcurrentHashMap<>();
    
    // 恶意请求模式
    private final String[] MALICIOUS_PATTERNS = {
        "union.*select", "insert.*into", "delete.*from", "update.*set",
        "<script", "javascript:", "eval\\(", "alert\\(",
        "../", "..\\\\", "cmd.exe", "/bin/", "etc/passwd",
        "base64_decode", "gzinflate", "str_rot13"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        
        try {
            // 1. 检查IP黑名单
            if (isInBlacklist(clientIp)) {
                log.warn("阻止黑名单IP访问: {}", clientIp);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            
            // 2. 频率限制检查
            if (isRateLimitExceeded(clientIp)) {
                log.warn("IP请求频率超限: {}", clientIp);
                securityEventService.recordSecurityEvent(
                    "RATE_LIMIT_EXCEEDED", 
                    clientIp, 
                    "请求频率超过限制",
                    SecurityLevel.MEDIUM
                );
                response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
                return false;
            }
            
            // 3. SQL注入检测
            if (containsSqlInjection(requestUri, queryString)) {
                log.error("检测到SQL注入攻击: IP={}, URI={}, Query={}", clientIp, requestUri, queryString);
                handleSqlInjectionAttack(clientIp, requestUri, queryString);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            
            // 4. XSS攻击检测
            if (containsXssAttack(requestUri, queryString)) {
                log.error("检测到XSS攻击: IP={}, URI={}, Query={}", clientIp, requestUri, queryString);
                handleXssAttack(clientIp, requestUri, queryString);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            
            // 5. 路径遍历攻击检测
            if (containsPathTraversal(requestUri)) {
                log.error("检测到路径遍历攻击: IP={}, URI={}", clientIp, requestUri);
                handlePathTraversalAttack(clientIp, requestUri);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            
            // 6. 恶意User-Agent检测
            if (isMaliciousUserAgent(userAgent)) {
                log.warn("检测到可疑User-Agent: IP={}, UserAgent={}", clientIp, userAgent);
                securityEventService.recordSecurityEvent(
                    "MALICIOUS_USER_AGENT", 
                    clientIp, 
                    "可疑的User-Agent: " + userAgent,
                    SecurityLevel.LOW
                );
            }
            
            // 7. 暴力破解检测
            if (isBruteForceAttack(request, clientIp)) {
                log.error("检测到暴力破解攻击: IP={}", clientIp);
                handleBruteForceAttack(clientIp);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("安全检查异常: IP={}, Error={}", clientIp, e.getMessage(), e);
            return true; // 异常时允许通过，避免影响正常服务
        }
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 检查IP是否在黑名单中
     */
    private boolean isInBlacklist(String ip) {
        LocalDateTime blacklistTime = ipBlacklist.get(ip);
        if (blacklistTime != null) {
            // 黑名单时间超过24小时自动解除
            if (blacklistTime.plusHours(24).isBefore(LocalDateTime.now())) {
                ipBlacklist.remove(ip);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 检查是否超过频率限制
     */
    private boolean isRateLimitExceeded(String ip) {
        AtomicInteger count = ipRequestCount.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();
        
        // 每分钟最多100个请求
        if (currentCount > 100) {
            // 超过限制，加入临时黑名单
            addToTemporaryBlacklist(ip, 10); // 10分钟
            return true;
        }
        
        return false;
    }

    /**
     * SQL注入检测
     */
    private boolean containsSqlInjection(String uri, String queryString) {
        String content = (uri + " " + (queryString != null ? queryString : "")).toLowerCase();
        
        String[] sqlPatterns = {
            "union.*select", "insert.*into", "delete.*from", "update.*set",
            "drop.*table", "create.*table", "alter.*table", "exec.*sp_",
            "exec.*xp_", "sp_.*password", "xp_.*cmdshell", "'.*or.*'.*=.*'",
            "\".*or.*\".*=.*\"", "or.*1.*=.*1", "and.*1.*=.*1",
            "having.*1.*=.*1", "waitfor.*delay", "benchmark\\("
        };
        
        for (String pattern : sqlPatterns) {
            if (content.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }

    /**
     * XSS攻击检测
     */
    private boolean containsXssAttack(String uri, String queryString) {
        String content = (uri + " " + (queryString != null ? queryString : "")).toLowerCase();
        
        String[] xssPatterns = {
            "<script", "javascript:", "eval\\(", "alert\\(", "confirm\\(",
            "prompt\\(", "onload.*=", "onerror.*=", "onclick.*=", "onmouseover.*=",
            "document\\.cookie", "document\\.write", "window\\.location",
            "iframe.*src", "object.*data", "embed.*src"
        };
        
        for (String pattern : xssPatterns) {
            if (content.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 路径遍历攻击检测
     */
    private boolean containsPathTraversal(String uri) {
        String[] traversalPatterns = {
            "../", "..\\\\", "....//", "....\\\\\\\\",
            "%2e%2e%2f", "%2e%2e%5c", "%252e%252e%252f",
            "etc/passwd", "etc/shadow", "windows/system32",
            "boot.ini", "win.ini", "autoexec.bat"
        };
        
        String normalizedUri = uri.toLowerCase();
        for (String pattern : traversalPatterns) {
            if (normalizedUri.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 恶意User-Agent检测
     */
    private boolean isMaliciousUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return true;
        }
        
        String[] maliciousAgents = {
            "sqlmap", "nikto", "nessus", "openvas", "nmap", "masscan",
            "dirb", "dirbuster", "gobuster", "wfuzz", "burp", "zap",
            "python-requests", "curl", "wget", "scanner"
        };
        
        String lowerAgent = userAgent.toLowerCase();
        for (String agent : maliciousAgents) {
            if (lowerAgent.contains(agent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 暴力破解攻击检测
     */
    private boolean isBruteForceAttack(HttpServletRequest request, String ip) {
        String uri = request.getRequestURI();
        
        // 检查是否是登录相关接口
        if (uri.contains("/login") || uri.contains("/auth")) {
            // 这里可以实现更复杂的暴力破解检测逻辑
            // 比如检查短时间内的失败登录次数
            return false; // 简化实现
        }
        
        return false;
    }

    /**
     * 处理SQL注入攻击
     */
    private void handleSqlInjectionAttack(String ip, String uri, String queryString) {
        securityEventService.recordSecurityEvent(
            "SQL_INJECTION_ATTACK", 
            ip, 
            String.format("URI: %s, Query: %s", uri, queryString),
            SecurityLevel.HIGH
        );
        
        // 立即加入黑名单
        addToBlacklist(ip);
        
        // 触发紧急响应
        attackResponseService.handleHighSeverityAttack(ip, "SQL_INJECTION", uri);
    }

    /**
     * 处理XSS攻击
     */
    private void handleXssAttack(String ip, String uri, String queryString) {
        securityEventService.recordSecurityEvent(
            "XSS_ATTACK", 
            ip, 
            String.format("URI: %s, Query: %s", uri, queryString),
            SecurityLevel.HIGH
        );
        
        addToTemporaryBlacklist(ip, 30); // 30分钟临时黑名单
        attackResponseService.handleMediumSeverityAttack(ip, "XSS", uri);
    }

    /**
     * 处理路径遍历攻击
     */
    private void handlePathTraversalAttack(String ip, String uri) {
        securityEventService.recordSecurityEvent(
            "PATH_TRAVERSAL_ATTACK", 
            ip, 
            "URI: " + uri,
            SecurityLevel.HIGH
        );
        
        addToBlacklist(ip);
        attackResponseService.handleHighSeverityAttack(ip, "PATH_TRAVERSAL", uri);
    }

    /**
     * 处理暴力破解攻击
     */
    private void handleBruteForceAttack(String ip) {
        securityEventService.recordSecurityEvent(
            "BRUTE_FORCE_ATTACK", 
            ip, 
            "暴力破解攻击",
            SecurityLevel.HIGH
        );
        
        addToTemporaryBlacklist(ip, 60); // 60分钟临时黑名单
        attackResponseService.handleHighSeverityAttack(ip, "BRUTE_FORCE", "");
    }

    /**
     * 添加到永久黑名单
     */
    private void addToBlacklist(String ip) {
        ipBlacklist.put(ip, LocalDateTime.now());
        log.warn("IP {} 已加入黑名单", ip);
    }

    /**
     * 添加到临时黑名单
     */
    private void addToTemporaryBlacklist(String ip, int minutes) {
        ipBlacklist.put(ip, LocalDateTime.now().minusHours(24).plusMinutes(minutes));
        log.warn("IP {} 已加入临时黑名单 {} 分钟", ip, minutes);
    }

    /**
     * 安全等级枚举
     */
    public enum SecurityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}