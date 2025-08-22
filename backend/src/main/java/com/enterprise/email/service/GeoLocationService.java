package com.enterprise.email.service;

import java.util.Map;

/**
 * 地理位置服务接口
 */
public interface GeoLocationService {

    /**
     * 根据IP地址获取地理位置信息
     */
    Map<String, Object> getLocationInfo(String ipAddress);

    /**
     * 获取缓存的地理位置信息
     */
    Map<String, Object> getCachedLocationInfo(String ipAddress);

    /**
     * 缓存地理位置信息
     */
    void cacheLocationInfo(String ipAddress, Map<String, Object> locationInfo);

    /**
     * 清除过期的地理位置缓存
     */
    void cleanExpiredCache();
}