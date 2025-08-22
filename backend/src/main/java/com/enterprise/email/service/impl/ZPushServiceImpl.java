package com.enterprise.email.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.email.entity.UserAlias;
import com.enterprise.email.entity.ZPushConfig;
import com.enterprise.email.mapper.ZPushConfigMapper;
import com.enterprise.email.service.ZPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Z-Push ActiveSync移动同步服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZPushServiceImpl implements ZPushService {

    private final ZPushConfigMapper zPushConfigMapper;
    
    // 内存中存储设备配对信息
    private final Map<String, Map<String, Object>> devicePartnerships = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastHeartbeat = new ConcurrentHashMap<>();

    @Override
    public boolean createZPushConfig(ZPushConfig config) {
        try {
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setEnabled(true);
            config.setStatus("ACTIVE");
            
            int result = zPushConfigMapper.insert(config);
            if (result > 0) {
                // 生成Z-Push配置文件
                generateZPushConfig(config);
                log.info("Z-Push配置创建成功: {}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("创建Z-Push配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateZPushConfig(ZPushConfig config) {
        try {
            config.setUpdatedAt(LocalDateTime.now());
            int result = zPushConfigMapper.updateById(config);
            if (result > 0) {
                // 重新生成配置文件
                generateZPushConfig(config);
                log.info("Z-Push配置更新成功: {}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("更新Z-Push配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteZPushConfig(Long configId) {
        try {
            ZPushConfig config = zPushConfigMapper.selectById(configId);
            if (config != null) {
                config.setDeleted(true);
                config.setUpdatedAt(LocalDateTime.now());
                int result = zPushConfigMapper.updateById(config);
                if (result > 0) {
                    log.info("Z-Push配置删除成功: {}", config.getDomain());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除Z-Push配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public ZPushConfig getZPushConfig(String domain) {
        return zPushConfigMapper.selectByDomain(domain);
    }

    @Override
    public List<ZPushConfig> getEnabledConfigs() {
        return zPushConfigMapper.selectEnabledConfigs();
    }

    @Override
    public String createDevicePartnership(UserAlias alias, String deviceId, String deviceType) {
        try {
            String partnershipKey = UUID.randomUUID().toString();
            
            Map<String, Object> partnership = new HashMap<>();
            partnership.put("deviceId", deviceId);
            partnership.put("deviceType", deviceType);
            partnership.put("userAlias", alias.getAliasEmail());
            partnership.put("userId", alias.getUserId());
            partnership.put("partnershipKey", partnershipKey);
            partnership.put("createdAt", LocalDateTime.now());
            partnership.put("lastSync", LocalDateTime.now());
            partnership.put("status", "ACTIVE");
            
            devicePartnerships.put(deviceId, partnership);
            lastHeartbeat.put(deviceId, LocalDateTime.now());
            
            log.info("设备配对创建成功: deviceId={}, user={}", deviceId, alias.getAliasEmail());
            return partnershipKey;
        } catch (Exception e) {
            log.error("创建设备配对失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean validateDevicePartnership(String deviceId, String partnershipKey) {
        Map<String, Object> partnership = devicePartnerships.get(deviceId);
        if (partnership != null) {
            String storedKey = (String) partnership.get("partnershipKey");
            boolean valid = partnershipKey.equals(storedKey);
            if (valid) {
                lastHeartbeat.put(deviceId, LocalDateTime.now());
            }
            return valid;
        }
        return false;
    }

    @Override
    public boolean removeDevicePartnership(String deviceId) {
        try {
            devicePartnerships.remove(deviceId);
            lastHeartbeat.remove(deviceId);
            log.info("设备配对删除成功: {}", deviceId);
            return true;
        } catch (Exception e) {
            log.error("删除设备配对失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> handlePushRequest(String deviceId, Map<String, Object> syncData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> partnership = devicePartnerships.get(deviceId);
            if (partnership == null) {
                response.put("status", "ERROR");
                response.put("message", "Device not registered");
                return response;
            }
            
            // 更新最后同步时间
            partnership.put("lastSync", LocalDateTime.now());
            lastHeartbeat.put(deviceId, LocalDateTime.now());
            
            // 处理同步数据
            List<Map<String, Object>> changes = new ArrayList<>();
            
            // 处理邮件同步
            if (syncData.containsKey("emailFolders")) {
                Map<String, Object> emailChanges = processEmailSync(deviceId, syncData);
                if (!emailChanges.isEmpty()) {
                    changes.add(emailChanges);
                }
            }
            
            // 处理日历同步
            if (syncData.containsKey("calendar")) {
                Map<String, Object> calendarChanges = processCalendarSync(deviceId, syncData);
                if (!calendarChanges.isEmpty()) {
                    changes.add(calendarChanges);
                }
            }
            
            // 处理联系人同步
            if (syncData.containsKey("contacts")) {
                Map<String, Object> contactChanges = processContactSync(deviceId, syncData);
                if (!contactChanges.isEmpty()) {
                    changes.add(contactChanges);
                }
            }
            
            response.put("status", "SUCCESS");
            response.put("changes", changes);
            response.put("syncTime", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("处理推送请求失败: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    @Override
    public boolean syncEmailsToDevice(UserAlias alias, String deviceId, String folderId) {
        try {
            Map<String, Object> partnership = devicePartnerships.get(deviceId);
            if (partnership == null) {
                return false;
            }
            
            // 模拟邮件同步逻辑
            log.info("同步邮件到设备: deviceId={}, folderId={}, user={}", 
                    deviceId, folderId, alias.getAliasEmail());
            
            // 这里应该实际从IMAP服务器获取邮件并推送到设备
            partnership.put("lastEmailSync", LocalDateTime.now());
            
            return true;
        } catch (Exception e) {
            log.error("同步邮件到设备失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean syncCalendarToDevice(UserAlias alias, String deviceId) {
        try {
            Map<String, Object> partnership = devicePartnerships.get(deviceId);
            if (partnership == null) {
                return false;
            }
            
            log.info("同步日历到设备: deviceId={}, user={}", deviceId, alias.getAliasEmail());
            partnership.put("lastCalendarSync", LocalDateTime.now());
            
            return true;
        } catch (Exception e) {
            log.error("同步日历到设备失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean syncContactsToDevice(UserAlias alias, String deviceId) {
        try {
            Map<String, Object> partnership = devicePartnerships.get(deviceId);
            if (partnership == null) {
                return false;
            }
            
            log.info("同步联系人到设备: deviceId={}, user={}", deviceId, alias.getAliasEmail());
            partnership.put("lastContactSync", LocalDateTime.now());
            
            return true;
        } catch (Exception e) {
            log.error("同步联系人到设备失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean syncTasksToDevice(UserAlias alias, String deviceId) {
        try {
            Map<String, Object> partnership = devicePartnerships.get(deviceId);
            if (partnership == null) {
                return false;
            }
            
            log.info("同步任务到设备: deviceId={}, user={}", deviceId, alias.getAliasEmail());
            partnership.put("lastTaskSync", LocalDateTime.now());
            
            return true;
        } catch (Exception e) {
            log.error("同步任务到设备失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> handleHeartbeat(String deviceId, Integer interval) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> partnership = devicePartnerships.get(deviceId);
            if (partnership == null) {
                response.put("status", "ERROR");
                response.put("message", "Device not registered");
                return response;
            }
            
            lastHeartbeat.put(deviceId, LocalDateTime.now());
            partnership.put("heartbeatInterval", interval);
            
            response.put("status", "SUCCESS");
            response.put("interval", interval);
            response.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("处理设备心跳失败: {}", e.getMessage(), e);
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    @Override
    public Map<String, Object> getSyncStatus(String deviceId) {
        Map<String, Object> status = new HashMap<>();
        
        Map<String, Object> partnership = devicePartnerships.get(deviceId);
        if (partnership != null) {
            status.putAll(partnership);
            status.put("lastHeartbeat", lastHeartbeat.get(deviceId));
            status.put("online", isDeviceOnline(deviceId));
        } else {
            status.put("status", "NOT_REGISTERED");
        }
        
        return status;
    }

    @Override
    public boolean forceSyncFolder(UserAlias alias, String deviceId, String folderId) {
        return syncEmailsToDevice(alias, deviceId, folderId);
    }

    @Override
    public Map<String, Object> getDeviceInfo(String deviceId) {
        return devicePartnerships.getOrDefault(deviceId, new HashMap<>());
    }

    @Override
    public boolean updateDeviceSecurityPolicy(String deviceId, Map<String, Object> policy) {
        try {
            Map<String, Object> partnership = devicePartnerships.get(deviceId);
            if (partnership != null) {
                partnership.put("securityPolicy", policy);
                partnership.put("policyUpdatedAt", LocalDateTime.now());
                log.info("设备安全策略更新成功: {}", deviceId);
                return true;
            }
        } catch (Exception e) {
            log.error("更新设备安全策略失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean remoteWipeDevice(String deviceId) {
        try {
            Map<String, Object> partnership = devicePartnerships.get(deviceId);
            if (partnership != null) {
                partnership.put("wipeRequested", true);
                partnership.put("wipeRequestedAt", LocalDateTime.now());
                log.info("远程擦除设备请求发送: {}", deviceId);
                return true;
            }
        } catch (Exception e) {
            log.error("远程擦除设备失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Map<String, Object> getZPushStatus(String domain) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            ZPushConfig config = getZPushConfig(domain);
            if (config != null) {
                status.put("domain", domain);
                status.put("enabled", config.getEnabled());
                status.put("status", config.getStatus());
                status.put("version", config.getVersion());
                status.put("lastSync", config.getLastSyncAt());
                
                // 统计连接的设备数量
                long connectedDevices = devicePartnerships.values().stream()
                    .filter(p -> isDeviceOnline((String) p.get("deviceId")))
                    .count();
                status.put("connectedDevices", connectedDevices);
                status.put("totalDevices", devicePartnerships.size());
            } else {
                status.put("status", "NOT_CONFIGURED");
            }
        } catch (Exception e) {
            log.error("获取Z-Push状态失败: {}", e.getMessage(), e);
            status.put("status", "ERROR");
            status.put("message", e.getMessage());
        }
        
        return status;
    }

    @Override
    public boolean restartZPushService(String domain) {
        try {
            // 这里应该实际重启Z-Push服务
            log.info("重启Z-Push服务: {}", domain);
            
            // 更新配置状态
            ZPushConfig config = getZPushConfig(domain);
            if (config != null) {
                config.setStatus("RESTARTING");
                updateZPushConfig(config);
                
                // 模拟重启过程
                Thread.sleep(2000);
                
                config.setStatus("ACTIVE");
                config.setLastSyncAt(LocalDateTime.now());
                updateZPushConfig(config);
            }
            
            return true;
        } catch (Exception e) {
            log.error("重启Z-Push服务失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String generateZPushConfig(ZPushConfig config) {
        try {
            StringBuilder configContent = new StringBuilder();
            configContent.append("<?php\n");
            configContent.append("// Z-Push Configuration for ").append(config.getDomain()).append("\n\n");
            
            // 基本配置
            configContent.append("define('TIMEZONE', 'Asia/Shanghai');\n");
            configContent.append("define('BACKEND_PROVIDER', '").append(config.getBackendType()).append("');\n");
            
            // IMAP配置
            if (config.getImapServer() != null) {
                configContent.append("define('IMAP_SERVER', '").append(config.getImapServer()).append("');\n");
                configContent.append("define('IMAP_PORT', ").append(config.getImapPort()).append(");\n");
                configContent.append("define('IMAP_OPTIONS', '/ssl").append(config.getImapSsl() ? "" : "/novalidate-cert").append("');\n");
            }
            
            // SMTP配置
            if (config.getSmtpServer() != null) {
                configContent.append("define('SMTP_SERVER', '").append(config.getSmtpServer()).append("');\n");
                configContent.append("define('SMTP_PORT', ").append(config.getSmtpPort()).append(");\n");
                configContent.append("define('SMTP_OPTIONS', '/ssl").append(config.getSmtpSsl() ? "" : "/novalidate-cert").append("');\n");
            }
            
            // 同步配置
            configContent.append("define('SYNC_CONTACTS_MAXAGE', 365);\n");
            configContent.append("define('SYNC_CALENDAR_MAXAGE', 365);\n");
            configContent.append("define('SYNC_TASKS_MAXAGE', 365);\n");
            
            // 推送配置
            if (config.getPushEnabled()) {
                configContent.append("define('PUSH_SOAP_INTERVAL', ").append(config.getHeartbeatInterval()).append(");\n");
                configContent.append("define('PUSH_LIFETIME', ").append(config.getPushLifetime()).append(");\n");
            }
            
            // 日志配置
            configContent.append("define('LOGBACKEND', 'filelog');\n");
            configContent.append("define('LOGFILEDIR', '").append(config.getLogFile()).append("');\n");
            configContent.append("define('LOGLEVEL', ").append(getLogLevel(config.getLogLevel())).append(");\n");
            
            // 写入配置文件
            String configPath = config.getInstallPath() + "/config.php";
            File configFile = new File(configPath);
            configFile.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(configContent.toString());
            }
            
            log.info("Z-Push配置文件生成成功: {}", configPath);
            return configPath;
            
        } catch (Exception e) {
            log.error("生成Z-Push配置文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean validateZPushConfig(ZPushConfig config) {
        try {
            // 验证必需字段
            if (config.getDomain() == null || config.getDomain().trim().isEmpty()) {
                log.error("域名不能为空");
                return false;
            }
            
            if (config.getInstallPath() == null || config.getInstallPath().trim().isEmpty()) {
                log.error("安装路径不能为空");
                return false;
            }
            
            if (config.getActivesyncUrl() == null || config.getActivesyncUrl().trim().isEmpty()) {
                log.error("ActiveSync URL不能为空");
                return false;
            }
            
            // 验证IMAP配置
            if (config.getImapServer() != null) {
                if (config.getImapPort() == null || config.getImapPort() <= 0 || config.getImapPort() > 65535) {
                    log.error("IMAP端口无效");
                    return false;
                }
            }
            
            // 验证SMTP配置
            if (config.getSmtpServer() != null) {
                if (config.getSmtpPort() == null || config.getSmtpPort() <= 0 || config.getSmtpPort() > 65535) {
                    log.error("SMTP端口无效");
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("验证Z-Push配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getSyncStatistics(String domain) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 统计设备连接信息
            long totalDevices = devicePartnerships.size();
            long onlineDevices = devicePartnerships.values().stream()
                .filter(p -> isDeviceOnline((String) p.get("deviceId")))
                .count();
            
            stats.put("totalDevices", totalDevices);
            stats.put("onlineDevices", onlineDevices);
            stats.put("offlineDevices", totalDevices - onlineDevices);
            
            // 统计同步活动
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAgo = now.minusHours(1);
            
            long recentSyncs = devicePartnerships.values().stream()
                .filter(p -> {
                    LocalDateTime lastSync = (LocalDateTime) p.get("lastSync");
                    return lastSync != null && lastSync.isAfter(oneHourAgo);
                })
                .count();
            
            stats.put("recentSyncs", recentSyncs);
            stats.put("domain", domain);
            stats.put("timestamp", now);
            
        } catch (Exception e) {
            log.error("获取同步统计信息失败: {}", e.getMessage(), e);
        }
        
        return stats;
    }

    @Override
    public boolean cleanupExpiredSyncData() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            
            // 清理长时间未活跃的设备配对
            devicePartnerships.entrySet().removeIf(entry -> {
                LocalDateTime lastHeartbeatTime = lastHeartbeat.get(entry.getKey());
                return lastHeartbeatTime != null && lastHeartbeatTime.isBefore(cutoff);
            });
            
            log.info("清理过期同步数据完成");
            return true;
        } catch (Exception e) {
            log.error("清理过期同步数据失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // 私有辅助方法
    private boolean isDeviceOnline(String deviceId) {
        LocalDateTime lastBeat = lastHeartbeat.get(deviceId);
        if (lastBeat == null) {
            return false;
        }
        return lastBeat.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    private Map<String, Object> processEmailSync(String deviceId, Map<String, Object> syncData) {
        Map<String, Object> changes = new HashMap<>();
        // 实现邮件同步逻辑
        changes.put("type", "email");
        changes.put("folders", syncData.get("emailFolders"));
        return changes;
    }

    private Map<String, Object> processCalendarSync(String deviceId, Map<String, Object> syncData) {
        Map<String, Object> changes = new HashMap<>();
        // 实现日历同步逻辑
        changes.put("type", "calendar");
        changes.put("events", syncData.get("calendar"));
        return changes;
    }

    private Map<String, Object> processContactSync(String deviceId, Map<String, Object> syncData) {
        Map<String, Object> changes = new HashMap<>();
        // 实现联系人同步逻辑
        changes.put("type", "contacts");
        changes.put("contacts", syncData.get("contacts"));
        return changes;
    }

    private int getLogLevel(String level) {
        switch (level != null ? level.toUpperCase() : "INFO") {
            case "DEBUG": return 7;
            case "INFO": return 6;
            case "WARN": return 4;
            case "ERROR": return 3;
            default: return 6;
        }
    }
}