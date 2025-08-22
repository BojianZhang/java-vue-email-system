package com.enterprise.email.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.email.entity.SecuritySetting;

import java.util.Map;

/**
 * 安全配置服务接口
 */
public interface SecuritySettingService extends IService<SecuritySetting> {

    /**
     * 获取配置值（字符串）
     */
    String getStringSetting(String key, String defaultValue);

    /**
     * 获取配置值（整数）
     */
    int getIntSetting(String key, int defaultValue);

    /**
     * 获取配置值（布尔）
     */
    boolean getBooleanSetting(String key, boolean defaultValue);

    /**
     * 设置配置值
     */
    void setSetting(String key, String value, Long updatedBy);

    /**
     * 批量更新配置
     */
    void batchUpdateSettings(Map<String, String> settings, Long updatedBy);

    /**
     * 重置配置为默认值
     */
    void resetToDefaultSettings(Long updatedBy);

    /**
     * 获取所有配置（按分类分组）
     */
    Map<String, Map<String, Object>> getAllSettingsGroupedByCategory();

    /**
     * 获取指定分类的配置
     */
    Map<String, Object> getSettingsByCategory(String category);

    /**
     * 初始化默认配置
     */
    void initializeDefaultSettings();
}