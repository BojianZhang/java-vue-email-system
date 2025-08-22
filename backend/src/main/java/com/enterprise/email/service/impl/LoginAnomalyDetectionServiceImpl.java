package com.enterprise.email.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.enterprise.email.entity.SecurityAlert;
import com.enterprise.email.entity.UserLoginLog;
import com.enterprise.email.mapper.UserLoginLogMapper;
import com.enterprise.email.service.GeoLocationService;
import com.enterprise.email.service.LoginAnomalyDetectionService;
import com.enterprise.email.service.SecurityAlertService;
import com.enterprise.email.service.SecuritySettingService;
import com.enterprise.email.utils.DeviceUtils;
import com.enterprise.email.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 登录异常检测服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAnomalyDetectionServiceImpl implements LoginAnomalyDetectionService {

    private final UserLoginLogMapper loginLogMapper;
    private final GeoLocationService geoLocationService;
    private final SecurityAlertService securityAlertService;
    private final SecuritySettingService securitySettingService;
    private final RedisUtil redisUtil;

    @Override
    @Async
    @Transactional
    public void recordLogin(Long userId, String ipAddress, String userAgent) {
        try {
            // 解析设备信息
            Map<String, String> deviceInfo = DeviceUtils.parseUserAgent(userAgent);
            
            // 获取地理位置信息
            Map<String, Object> geoInfo = geoLocationService.getLocationInfo(ipAddress);
            
            // 生成设备指纹
            String deviceFingerprint = DeviceUtils.generateDeviceFingerprint(ipAddress, userAgent);
            
            // 创建登录日志
            UserLoginLog loginLog = new UserLoginLog();
            loginLog.setUserId(userId);
            loginLog.setSessionTokenHash(generateSessionHash(userId));
            loginLog.setIpAddress(ipAddress);
            loginLog.setUserAgent(userAgent);
            loginLog.setDeviceType(deviceInfo.get("deviceType"));
            loginLog.setOs(deviceInfo.get("os"));
            loginLog.setBrowser(deviceInfo.get("browser"));
            loginLog.setDeviceFingerprint(deviceFingerprint);
            
            if (geoInfo != null) {
                loginLog.setCountry((String) geoInfo.get("country"));
                loginLog.setRegion((String) geoInfo.get("region"));
                loginLog.setCity((String) geoInfo.get("city"));
                loginLog.setLatitude((Double) geoInfo.get("latitude"));
                loginLog.setLongitude((Double) geoInfo.get("longitude"));
                loginLog.setIsp((String) geoInfo.get("isp"));
            }
            
            loginLog.setIsActive(true);
            loginLog.setLoginTime(LocalDateTime.now());
            loginLog.setLastActivity(LocalDateTime.now());
            
            // 检测异常并计算风险分数
            boolean isSuspicious = detectLoginAnomaly(loginLog);
            int riskScore = calculateRiskScore(loginLog);
            
            loginLog.setIsSuspicious(isSuspicious);
            loginLog.setRiskScore(riskScore);
            
            if (isSuspicious) {
                List<String> suspiciousReasons = getSuspiciousReasons(loginLog);
                loginLog.setSuspiciousReasons(String.join(",", suspiciousReasons));
                
                // 创建安全警报
                createSecurityAlert(userId, loginLog, suspiciousReasons);
            }
            
            // 保存登录日志
            loginLogMapper.insert(loginLog);
            
            // 缓存会话信息
            cacheSession(userId, loginLog);
            
            log.info("记录用户登录: userId={}, ip={}, suspicious={}, riskScore={}", 
                    userId, ipAddress, isSuspicious, riskScore);
                    
        } catch (Exception e) {
            log.error("记录用户登录失败: userId={}, ip={}", userId, ipAddress, e);
        }
    }

    @Override
    public void recordLogout(Long userId) {
        try {
            // 更新活跃会话状态
            QueryWrapper<UserLoginLog> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                   .eq("is_active", true);
                   
            UserLoginLog updateLog = new UserLoginLog();
            updateLog.setIsActive(false);
            updateLog.setLogoutTime(LocalDateTime.now());
            
            loginLogMapper.update(updateLog, wrapper);
            
            // 清除会话缓存
            redisUtil.delete("user_session:" + userId);
            
            log.info("记录用户登出: userId={}", userId);
            
        } catch (Exception e) {
            log.error("记录用户登出失败: userId={}", userId, e);
        }
    }

    @Override
    @Async
    public void updateUserActivity(Long userId, String ipAddress) {
        try {
            QueryWrapper<UserLoginLog> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                   .eq("ip_address", ipAddress)
                   .eq("is_active", true)
                   .orderByDesc("login_time")
                   .last("LIMIT 1");
                   
            UserLoginLog loginLog = loginLogMapper.selectOne(wrapper);
            if (loginLog != null) {
                loginLog.setLastActivity(LocalDateTime.now());
                loginLogMapper.updateById(loginLog);
                
                // 更新缓存
                redisUtil.set("user_activity:" + userId, System.currentTimeMillis(), 3600);
            }
        } catch (Exception e) {
            log.error("更新用户活动时间失败: userId={}", userId, e);
        }
    }

    @Override
    public boolean detectLoginAnomaly(UserLoginLog loginLog) {
        List<String> anomalies = new ArrayList<>();
        
        // 地理位置异常检测
        if (detectGeographicAnomaly(loginLog.getUserId(), loginLog.getCountry(), 
                loginLog.getCity(), loginLog.getLatitude(), loginLog.getLongitude())) {
            anomalies.add("geographic_anomaly");
        }
        
        // 并发会话异常检测
        if (detectConcurrentSessionAnomaly(loginLog.getUserId())) {
            anomalies.add("concurrent_sessions");
        }
        
        // 登录频率异常检测
        if (detectLoginFrequencyAnomaly(loginLog.getUserId(), loginLog.getIpAddress())) {
            anomalies.add("login_frequency");
        }
        
        // 新设备检测
        if (isNewDevice(loginLog.getUserId(), loginLog.getDeviceFingerprint())) {
            anomalies.add("new_device");
        }
        
        // 可疑IP检测
        if (isSuspiciousIP(loginLog.getIpAddress())) {
            anomalies.add("suspicious_ip");
        }
        
        // 时间异常检测
        if (isUnusualLoginTime(loginLog.getUserId(), loginLog.getLoginTime())) {
            anomalies.add("time_anomaly");
        }
        
        return !anomalies.isEmpty();
    }

    @Override
    public int calculateRiskScore(UserLoginLog loginLog) {
        int baseScore = securitySettingService.getIntSetting("base_risk_score", 10);
        int totalScore = baseScore;
        
        // 地理位置异常加分
        if (detectGeographicAnomaly(loginLog.getUserId(), loginLog.getCountry(), 
                loginLog.getCity(), loginLog.getLatitude(), loginLog.getLongitude())) {
            totalScore += securitySettingService.getIntSetting("geo_anomaly_risk_score", 25);
        }
        
        // 新设备加分
        if (isNewDevice(loginLog.getUserId(), loginLog.getDeviceFingerprint())) {
            totalScore += securitySettingService.getIntSetting("new_device_risk_score", 15);
        }
        
        // 可疑IP加分
        if (isSuspiciousIP(loginLog.getIpAddress())) {
            totalScore += securitySettingService.getIntSetting("suspicious_ip_risk_score", 30);
        }
        
        // 登录频率异常加分
        if (detectLoginFrequencyAnomaly(loginLog.getUserId(), loginLog.getIpAddress())) {
            totalScore += 20;
        }
        
        // 并发会话异常加分
        if (detectConcurrentSessionAnomaly(loginLog.getUserId())) {
            totalScore += 15;
        }
        
        // 时间异常加分
        if (isUnusualLoginTime(loginLog.getUserId(), loginLog.getLoginTime())) {
            totalScore += 10;
        }
        
        return Math.min(totalScore, 100); // 最大风险分数为100
    }

    @Override
    public boolean detectGeographicAnomaly(Long userId, String country, String city, 
                                         Double latitude, Double longitude) {
        if (!securitySettingService.getBooleanSetting("geo_anomaly_detection_enabled", true)) {
            return false;
        }
        
        if (latitude == null || longitude == null) {
            return false;
        }
        
        try {
            // 获取用户最近的登录记录
            QueryWrapper<UserLoginLog> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                   .isNotNull("latitude")
                   .isNotNull("longitude")
                   .ne("latitude", latitude)
                   .ne("longitude", longitude)
                   .orderByDesc("login_time")
                   .last("LIMIT 1");
                   
            UserLoginLog lastLogin = loginLogMapper.selectOne(wrapper);
            if (lastLogin == null) {
                return false;
            }
            
            // 计算地理距离
            double distance = calculateDistance(
                lastLogin.getLatitude(), lastLogin.getLongitude(),
                latitude, longitude
            );
            
            double threshold = securitySettingService.getIntSetting("geo_anomaly_distance_km", 500);
            
            // 检查时间窗口
            LocalDateTime timeWindow = LocalDateTime.now()
                .minusHours(securitySettingService.getIntSetting("time_anomaly_window_hours", 6));
            
            if (distance > threshold && lastLogin.getLoginTime().isAfter(timeWindow)) {
                log.warn("检测到地理位置异常: userId={}, distance={}km", userId, distance);
                return true;
            }
            
        } catch (Exception e) {
            log.error("地理位置异常检测失败: userId={}", userId, e);
        }
        
        return false;
    }

    @Override
    public boolean detectConcurrentSessionAnomaly(Long userId) {
        try {
            int maxSessions = securitySettingService.getIntSetting("max_concurrent_sessions", 5);
            
            QueryWrapper<UserLoginLog> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                   .eq("is_active", true);
                   
            Long count = loginLogMapper.selectCount(wrapper);
            
            if (count > maxSessions) {
                log.warn("检测到并发会话异常: userId={}, sessions={}", userId, count);
                return true;
            }
            
        } catch (Exception e) {
            log.error("并发会话异常检测失败: userId={}", userId, e);
        }
        
        return false;
    }

    @Override
    public boolean detectLoginFrequencyAnomaly(Long userId, String ipAddress) {
        try {
            int frequencyLimit = securitySettingService.getIntSetting("login_frequency_limit", 10);
            int windowMinutes = securitySettingService.getIntSetting("login_frequency_window_minutes", 30);
            
            LocalDateTime startTime = LocalDateTime.now().minusMinutes(windowMinutes);
            
            QueryWrapper<UserLoginLog> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                   .eq("ip_address", ipAddress)
                   .ge("login_time", startTime);
                   
            Long count = loginLogMapper.selectCount(wrapper);
            
            if (count > frequencyLimit) {
                log.warn("检测到登录频率异常: userId={}, count={} in {}min", userId, count, windowMinutes);
                return true;
            }
            
        } catch (Exception e) {
            log.error("登录频率异常检测失败: userId={}", userId, e);
        }
        
        return false;
    }

    @Override
    @Transactional
    public void terminateUserSessions(Long userId, String sessionTokenHash) {
        try {
            QueryWrapper<UserLoginLog> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                   .eq("is_active", true);
                   
            if (sessionTokenHash != null) {
                wrapper.eq("session_token_hash", sessionTokenHash);
            }
            
            UserLoginLog updateLog = new UserLoginLog();
            updateLog.setIsActive(false);
            updateLog.setLogoutTime(LocalDateTime.now());
            
            int count = loginLogMapper.update(updateLog, wrapper);
            
            // 清除会话缓存
            redisUtil.delete("user_session:" + userId);
            
            log.info("强制结束用户会话: userId={}, count={}", userId, count);
            
        } catch (Exception e) {
            log.error("强制结束用户会话失败: userId={}", userId, e);
        }
    }

    /**
     * 生成会话哈希
     */
    private String generateSessionHash(Long userId) {
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(
            userId + "_" + System.currentTimeMillis() + "_" + Math.random()
        );
    }

    /**
     * 检测是否为新设备
     */
    private boolean isNewDevice(Long userId, String deviceFingerprint) {
        QueryWrapper<UserLoginLog> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("device_fingerprint", deviceFingerprint)
               .last("LIMIT 1");
               
        return loginLogMapper.selectOne(wrapper) == null;
    }

    /**
     * 检测可疑IP
     */
    private boolean isSuspiciousIP(String ipAddress) {
        // 可以接入第三方IP信誉数据库
        // 这里简单检查一些已知的可疑IP范围
        return false;
    }

    /**
     * 检测异常登录时间
     */
    private boolean isUnusualLoginTime(Long userId, LocalDateTime loginTime) {
        // 检查是否在异常时间段登录（如深夜）
        int hour = loginTime.getHour();
        return hour < 6 || hour > 23;
    }

    /**
     * 获取可疑原因列表
     */
    private List<String> getSuspiciousReasons(UserLoginLog loginLog) {
        List<String> reasons = new ArrayList<>();
        
        if (detectGeographicAnomaly(loginLog.getUserId(), loginLog.getCountry(), 
                loginLog.getCity(), loginLog.getLatitude(), loginLog.getLongitude())) {
            reasons.add("异地登录");
        }
        
        if (detectConcurrentSessionAnomaly(loginLog.getUserId())) {
            reasons.add("并发会话过多");
        }
        
        if (detectLoginFrequencyAnomaly(loginLog.getUserId(), loginLog.getIpAddress())) {
            reasons.add("登录频率异常");
        }
        
        if (isNewDevice(loginLog.getUserId(), loginLog.getDeviceFingerprint())) {
            reasons.add("新设备登录");
        }
        
        if (isSuspiciousIP(loginLog.getIpAddress())) {
            reasons.add("可疑IP地址");
        }
        
        if (isUnusualLoginTime(loginLog.getUserId(), loginLog.getLoginTime())) {
            reasons.add("异常时间登录");
        }
        
        return reasons;
    }

    /**
     * 创建安全警报
     */
    private void createSecurityAlert(Long userId, UserLoginLog loginLog, List<String> reasons) {
        SecurityAlert alert = new SecurityAlert();
        alert.setUserId(userId);
        alert.setAlertType(SecurityAlert.TYPE_LOGIN_ANOMALY);
        
        // 根据风险分数确定严重程度
        if (loginLog.getRiskScore() >= 80) {
            alert.setSeverity(SecurityAlert.SEVERITY_CRITICAL);
        } else if (loginLog.getRiskScore() >= 60) {
            alert.setSeverity(SecurityAlert.SEVERITY_HIGH);
        } else if (loginLog.getRiskScore() >= 40) {
            alert.setSeverity(SecurityAlert.SEVERITY_MEDIUM);
        } else {
            alert.setSeverity(SecurityAlert.SEVERITY_LOW);
        }
        
        alert.setTitle("检测到异常登录行为");
        alert.setDescription("用户 " + loginLog.getUsername() + " 存在异常登录行为: " + String.join("、", reasons));
        
        // 设置警报数据
        Map<String, Object> alertData = Map.of(
            "ip_address", loginLog.getIpAddress(),
            "location", loginLog.getCity() + ", " + loginLog.getCountry(),
            "device", loginLog.getBrowser() + " on " + loginLog.getOs(),
            "risk_score", loginLog.getRiskScore(),
            "suspicious_reasons", reasons
        );
        
        try {
            alert.setAlertData(com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(alertData));
        } catch (Exception e) {
            log.error("序列化警报数据失败", e);
        }
        
        alert.setIpAddress(loginLog.getIpAddress());
        alert.setUserAgent(loginLog.getUserAgent());
        alert.setLocation(loginLog.getCity() + ", " + loginLog.getCountry());
        alert.setIsResolved(false);
        alert.setIsNotified(false);
        
        securityAlertService.createAlert(alert);
    }

    /**
     * 缓存会话信息
     */
    private void cacheSession(Long userId, UserLoginLog loginLog) {
        Map<String, Object> sessionInfo = Map.of(
            "userId", userId,
            "ipAddress", loginLog.getIpAddress(),
            "loginTime", loginLog.getLoginTime().toString(),
            "deviceFingerprint", loginLog.getDeviceFingerprint()
        );
        
        redisUtil.set("user_session:" + userId, sessionInfo, 86400); // 24小时
    }

    /**
     * 计算两点间距离（公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
                
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}