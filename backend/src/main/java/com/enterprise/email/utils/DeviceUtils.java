package com.enterprise.email.utils;

import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备工具类
 */
public class DeviceUtils {

    /**
     * 解析User-Agent获取设备信息
     */
    public static Map<String, String> parseUserAgent(String userAgentString) {
        Map<String, String> deviceInfo = new HashMap<>();
        
        if (!StringUtils.hasText(userAgentString)) {
            deviceInfo.put("deviceType", "unknown");
            deviceInfo.put("os", "unknown");
            deviceInfo.put("browser", "unknown");
            return deviceInfo;
        }
        
        try {
            UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
            
            // 设备类型
            String deviceType = "desktop";
            if (userAgentString.toLowerCase().contains("mobile")) {
                deviceType = "mobile";
            } else if (userAgentString.toLowerCase().contains("tablet")) {
                deviceType = "tablet";
            }
            
            // 操作系统
            String os = "unknown";
            if (userAgent.getOperatingSystem() != null) {
                os = userAgent.getOperatingSystem().getName();
            }
            
            // 浏览器
            String browser = "unknown";
            if (userAgent.getBrowser() != null) {
                browser = userAgent.getBrowser().getName() + " " + 
                         userAgent.getBrowserVersion().getVersion();
            }
            
            deviceInfo.put("deviceType", deviceType);
            deviceInfo.put("os", os);
            deviceInfo.put("browser", browser);
            
        } catch (Exception e) {
            deviceInfo.put("deviceType", "unknown");
            deviceInfo.put("os", "unknown");
            deviceInfo.put("browser", "unknown");
        }
        
        return deviceInfo;
    }

    /**
     * 生成设备指纹
     */
    public static String generateDeviceFingerprint(String ipAddress, String userAgent) {
        try {
            String combined = ipAddress + "|" + userAgent;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes("UTF-8"));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            return String.valueOf(Math.abs((ipAddress + userAgent).hashCode()));
        }
    }

    /**
     * 判断是否为移动设备
     */
    public static boolean isMobileDevice(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return false;
        }
        
        String ua = userAgent.toLowerCase();
        return ua.contains("mobile") || 
               ua.contains("android") || 
               ua.contains("iphone") || 
               ua.contains("blackberry") ||
               ua.contains("windows phone");
    }

    /**
     * 判断是否为平板设备
     */
    public static boolean isTabletDevice(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return false;
        }
        
        String ua = userAgent.toLowerCase();
        return ua.contains("tablet") || ua.contains("ipad");
    }

    /**
     * 获取操作系统类型
     */
    public static String getOperatingSystem(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "unknown";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("windows")) {
            if (ua.contains("windows phone")) {
                return "Windows Phone";
            } else if (ua.contains("windows nt 10")) {
                return "Windows 10";
            } else if (ua.contains("windows nt 6.3")) {
                return "Windows 8.1";
            } else if (ua.contains("windows nt 6.2")) {
                return "Windows 8";
            } else if (ua.contains("windows nt 6.1")) {
                return "Windows 7";
            } else {
                return "Windows";
            }
        } else if (ua.contains("mac os")) {
            return "macOS";
        } else if (ua.contains("linux")) {
            return "Linux";
        } else if (ua.contains("android")) {
            return "Android";
        } else if (ua.contains("iphone")) {
            return "iOS";
        } else if (ua.contains("blackberry")) {
            return "BlackBerry";
        }
        
        return "unknown";
    }

    /**
     * 获取浏览器信息
     */
    public static String getBrowserInfo(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "unknown";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("chrome")) {
            return "Chrome";
        } else if (ua.contains("firefox")) {
            return "Firefox";
        } else if (ua.contains("safari") && !ua.contains("chrome")) {
            return "Safari";
        } else if (ua.contains("edge")) {
            return "Edge";
        } else if (ua.contains("opera")) {
            return "Opera";
        } else if (ua.contains("trident") || ua.contains("msie")) {
            return "Internet Explorer";
        }
        
        return "unknown";
    }
}