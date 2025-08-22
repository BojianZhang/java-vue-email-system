package com.enterprise.email.utils;

import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web工具类
 */
public class WebUtils {

    /**
     * 获取客户端真实IP地址
     */
    public static String getClientIP(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 多级代理的情况下，第一个IP为真实IP
                if (ip.contains(",")) {
                    return ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 从请求中获取JWT令牌
     */
    public static String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 获取User-Agent
     */
    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * 判断是否为Ajax请求
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        String xRequestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(xRequestedWith);
    }

    /**
     * 判断是否为移动端请求
     */
    public static boolean isMobileRequest(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if (!StringUtils.hasText(userAgent)) {
            return false;
        }
        
        userAgent = userAgent.toLowerCase();
        return userAgent.contains("mobile") || 
               userAgent.contains("android") || 
               userAgent.contains("iphone") || 
               userAgent.contains("ipad") ||
               userAgent.contains("blackberry") ||
               userAgent.contains("windows phone");
    }
}