package com.enterprise.email.service.impl;

import com.enterprise.email.service.GeoLocationService;
import com.enterprise.email.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 地理位置服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoLocationServiceImpl implements GeoLocationService {

    private final RedisUtil redisUtil;
    private final RestTemplate restTemplate = new RestTemplate();

    // 地理位置API配置
    private static final String IPAPI_URL = "http://ip-api.com/json/";
    private static final String CACHE_PREFIX = "geo_location:";
    private static final long CACHE_DURATION = 24 * 60 * 60; // 24小时

    @Override
    public Map<String, Object> getLocationInfo(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || 
            "127.0.0.1".equals(ipAddress) || "localhost".equals(ipAddress)) {
            return getDefaultLocationInfo();
        }

        // 先检查缓存
        Map<String, Object> cachedInfo = getCachedLocationInfo(ipAddress);
        if (cachedInfo != null) {
            return cachedInfo;
        }

        // 从API获取地理位置信息
        Map<String, Object> locationInfo = fetchLocationFromAPI(ipAddress);
        
        // 缓存结果
        if (locationInfo != null) {
            cacheLocationInfo(ipAddress, locationInfo);
        }

        return locationInfo != null ? locationInfo : getDefaultLocationInfo();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCachedLocationInfo(String ipAddress) {
        try {
            Object cached = redisUtil.get(CACHE_PREFIX + ipAddress);
            if (cached instanceof Map) {
                return (Map<String, Object>) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存地理位置信息失败: ip={}", ipAddress, e);
        }
        return null;
    }

    @Override
    public void cacheLocationInfo(String ipAddress, Map<String, Object> locationInfo) {
        try {
            redisUtil.set(CACHE_PREFIX + ipAddress, locationInfo, CACHE_DURATION);
            log.debug("缓存地理位置信息: ip={}", ipAddress);
        } catch (Exception e) {
            log.error("缓存地理位置信息失败: ip={}", ipAddress, e);
        }
    }

    @Override
    public void cleanExpiredCache() {
        // Redis会自动清理过期的缓存，这里可以实现自定义清理逻辑
        log.info("清理过期的地理位置缓存");
    }

    /**
     * 从API获取地理位置信息
     */
    private Map<String, Object> fetchLocationFromAPI(String ipAddress) {
        try {
            String url = IPAPI_URL + ipAddress + "?fields=status,message,country,countryCode,region,regionName,city,lat,lon,timezone,isp,org,as,query";
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> apiData = response.getBody();
                
                // 检查API响应状态
                if ("success".equals(apiData.get("status"))) {
                    Map<String, Object> locationInfo = new HashMap<>();
                    locationInfo.put("country", apiData.get("country"));
                    locationInfo.put("countryCode", apiData.get("countryCode"));
                    locationInfo.put("region", apiData.get("regionName"));
                    locationInfo.put("city", apiData.get("city"));
                    locationInfo.put("latitude", parseDouble(apiData.get("lat")));
                    locationInfo.put("longitude", parseDouble(apiData.get("lon")));
                    locationInfo.put("timezone", apiData.get("timezone"));
                    locationInfo.put("isp", apiData.get("isp"));
                    locationInfo.put("org", apiData.get("org"));
                    locationInfo.put("ipAddress", ipAddress);
                    
                    log.debug("获取地理位置信息成功: ip={}, location={}, {}", 
                            ipAddress, apiData.get("city"), apiData.get("country"));
                    
                    return locationInfo;
                } else {
                    log.warn("IP地理位置API返回失败: ip={}, message={}", 
                            ipAddress, apiData.get("message"));
                }
            }
        } catch (Exception e) {
            log.error("调用IP地理位置API失败: ip={}", ipAddress, e);
        }
        
        return null;
    }

    /**
     * 获取默认地理位置信息（用于本地IP或获取失败的情况）
     */
    private Map<String, Object> getDefaultLocationInfo() {
        Map<String, Object> defaultInfo = new HashMap<>();
        defaultInfo.put("country", "未知");
        defaultInfo.put("countryCode", "XX");
        defaultInfo.put("region", "未知");
        defaultInfo.put("city", "未知");
        defaultInfo.put("latitude", null);
        defaultInfo.put("longitude", null);
        defaultInfo.put("timezone", "Asia/Shanghai");
        defaultInfo.put("isp", "未知");
        defaultInfo.put("org", "未知");
        defaultInfo.put("ipAddress", "127.0.0.1");
        
        return defaultInfo;
    }

    /**
     * 安全地解析Double值
     */
    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else {
                return Double.parseDouble(value.toString());
            }
        } catch (NumberFormatException e) {
            log.warn("解析地理坐标失败: value={}", value);
            return null;
        }
    }
}