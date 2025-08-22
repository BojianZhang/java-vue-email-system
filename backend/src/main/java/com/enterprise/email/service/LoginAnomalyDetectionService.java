package com.enterprise.email.service;

import com.enterprise.email.entity.UserLoginLog;

/**
 * 登录异常检测服务接口
 */
public interface LoginAnomalyDetectionService {

    /**
     * 记录用户登录
     */
    void recordLogin(Long userId, String ipAddress, String userAgent);

    /**
     * 记录用户登出
     */
    void recordLogout(Long userId);

    /**
     * 更新用户活动时间
     */
    void updateUserActivity(Long userId, String ipAddress);

    /**
     * 检测登录异常
     */
    boolean detectLoginAnomaly(UserLoginLog loginLog);

    /**
     * 计算风险分数
     */
    int calculateRiskScore(UserLoginLog loginLog);

    /**
     * 检测地理位置异常
     */
    boolean detectGeographicAnomaly(Long userId, String country, String city, Double latitude, Double longitude);

    /**
     * 检测并发会话异常
     */
    boolean detectConcurrentSessionAnomaly(Long userId);

    /**
     * 检测登录频率异常
     */
    boolean detectLoginFrequencyAnomaly(Long userId, String ipAddress);

    /**
     * 强制结束用户会话
     */
    void terminateUserSessions(Long userId, String sessionTokenHash);
}