package com.enterprise.email.service;

import com.enterprise.email.entity.UserAlias;
import com.enterprise.email.entity.ZPushConfig;

import java.util.List;
import java.util.Map;

/**
 * Z-Push ActiveSync移动同步服务
 */
public interface ZPushService {

    /**
     * 创建Z-Push配置
     */
    boolean createZPushConfig(ZPushConfig config);

    /**
     * 更新Z-Push配置
     */
    boolean updateZPushConfig(ZPushConfig config);

    /**
     * 删除Z-Push配置
     */
    boolean deleteZPushConfig(Long configId);

    /**
     * 根据域名获取Z-Push配置
     */
    ZPushConfig getZPushConfig(String domain);

    /**
     * 获取所有启用的Z-Push配置
     */
    List<ZPushConfig> getEnabledConfigs();

    /**
     * 为用户别名创建ActiveSync设备配对
     */
    String createDevicePartnership(UserAlias alias, String deviceId, String deviceType);

    /**
     * 验证设备配对
     */
    boolean validateDevicePartnership(String deviceId, String partnershipKey);

    /**
     * 删除设备配对
     */
    boolean removeDevicePartnership(String deviceId);

    /**
     * 处理ActiveSync推送请求
     */
    Map<String, Object> handlePushRequest(String deviceId, Map<String, Object> syncData);

    /**
     * 同步邮件到移动设备
     */
    boolean syncEmailsToDevice(UserAlias alias, String deviceId, String folderId);

    /**
     * 同步日历到移动设备
     */
    boolean syncCalendarToDevice(UserAlias alias, String deviceId);

    /**
     * 同步联系人到移动设备
     */
    boolean syncContactsToDevice(UserAlias alias, String deviceId);

    /**
     * 同步任务到移动设备
     */
    boolean syncTasksToDevice(UserAlias alias, String deviceId);

    /**
     * 处理设备心跳
     */
    Map<String, Object> handleHeartbeat(String deviceId, Integer interval);

    /**
     * 获取同步状态
     */
    Map<String, Object> getSyncStatus(String deviceId);

    /**
     * 强制同步指定文件夹
     */
    boolean forceSyncFolder(UserAlias alias, String deviceId, String folderId);

    /**
     * 获取设备信息
     */
    Map<String, Object> getDeviceInfo(String deviceId);

    /**
     * 更新设备安全策略
     */
    boolean updateDeviceSecurityPolicy(String deviceId, Map<String, Object> policy);

    /**
     * 远程擦除设备
     */
    boolean remoteWipeDevice(String deviceId);

    /**
     * 获取Z-Push服务状态
     */
    Map<String, Object> getZPushStatus(String domain);

    /**
     * 重启Z-Push服务
     */
    boolean restartZPushService(String domain);

    /**
     * 生成ActiveSync配置文件
     */
    String generateZPushConfig(ZPushConfig config);

    /**
     * 验证Z-Push配置
     */
    boolean validateZPushConfig(ZPushConfig config);

    /**
     * 获取同步统计信息
     */
    Map<String, Object> getSyncStatistics(String domain);

    /**
     * 清理过期的同步数据
     */
    boolean cleanupExpiredSyncData();
}