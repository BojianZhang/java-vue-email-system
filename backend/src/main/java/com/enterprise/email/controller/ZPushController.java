package com.enterprise.email.controller;

import com.enterprise.email.entity.ZPushConfig;
import com.enterprise.email.service.ZPushService;
import com.enterprise.email.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Z-Push ActiveSync移动同步控制器
 */
@Tag(name = "Z-Push ActiveSync移动同步", description = "Z-Push ActiveSync移动同步管理API")
@RestController
@RequestMapping("/api/zpush")
@RequiredArgsConstructor
public class ZPushController {

    private final ZPushService zPushService;

    @Operation(summary = "创建Z-Push配置")
    @PostMapping("/config")
    public Result<Boolean> createConfig(@Valid @RequestBody ZPushConfig config) {
        boolean success = zPushService.createZPushConfig(config);
        return success ? Result.success(true) : Result.error("创建Z-Push配置失败");
    }

    @Operation(summary = "更新Z-Push配置")
    @PutMapping("/config")
    public Result<Boolean> updateConfig(@Valid @RequestBody ZPushConfig config) {
        boolean success = zPushService.updateZPushConfig(config);
        return success ? Result.success(true) : Result.error("更新Z-Push配置失败");
    }

    @Operation(summary = "删除Z-Push配置")
    @DeleteMapping("/config/{configId}")
    public Result<Boolean> deleteConfig(@PathVariable Long configId) {
        boolean success = zPushService.deleteZPushConfig(configId);
        return success ? Result.success(true) : Result.error("删除Z-Push配置失败");
    }

    @Operation(summary = "获取Z-Push配置")
    @GetMapping("/config/{domain}")
    public Result<ZPushConfig> getConfig(@PathVariable String domain) {
        ZPushConfig config = zPushService.getZPushConfig(domain);
        return config != null ? Result.success(config) : Result.error("Z-Push配置不存在");
    }

    @Operation(summary = "获取所有启用的Z-Push配置")
    @GetMapping("/configs/enabled")
    public Result<List<ZPushConfig>> getEnabledConfigs() {
        List<ZPushConfig> configs = zPushService.getEnabledConfigs();
        return Result.success(configs);
    }

    @Operation(summary = "创建设备配对")
    @PostMapping("/device/partnership")
    public Result<String> createDevicePartnership(
            @RequestParam String aliasEmail,
            @RequestParam String deviceId,
            @RequestParam String deviceType) {
        // 这里需要根据aliasEmail获取UserAlias对象
        // 简化处理，实际应该从数据库获取
        String partnershipKey = zPushService.createDevicePartnership(null, deviceId, deviceType);
        return partnershipKey != null ? Result.success(partnershipKey) : Result.error("创建设备配对失败");
    }

    @Operation(summary = "验证设备配对")
    @PostMapping("/device/validate")
    public Result<Boolean> validateDevicePartnership(
            @RequestParam String deviceId,
            @RequestParam String partnershipKey) {
        boolean valid = zPushService.validateDevicePartnership(deviceId, partnershipKey);
        return Result.success(valid);
    }

    @Operation(summary = "删除设备配对")
    @DeleteMapping("/device/{deviceId}")
    public Result<Boolean> removeDevicePartnership(@PathVariable String deviceId) {
        boolean success = zPushService.removeDevicePartnership(deviceId);
        return success ? Result.success(true) : Result.error("删除设备配对失败");
    }

    @Operation(summary = "处理推送请求")
    @PostMapping("/push/{deviceId}")
    public Result<Map<String, Object>> handlePushRequest(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> syncData) {
        Map<String, Object> response = zPushService.handlePushRequest(deviceId, syncData);
        return Result.success(response);
    }

    @Operation(summary = "强制同步文件夹")
    @PostMapping("/sync/folder")
    public Result<Boolean> forceSyncFolder(
            @RequestParam String aliasEmail,
            @RequestParam String deviceId,
            @RequestParam String folderId) {
        // 简化处理，实际应该从数据库获取UserAlias
        boolean success = zPushService.forceSyncFolder(null, deviceId, folderId);
        return success ? Result.success(true) : Result.error("强制同步失败");
    }

    @Operation(summary = "处理设备心跳")
    @PostMapping("/heartbeat/{deviceId}")
    public Result<Map<String, Object>> handleHeartbeat(
            @PathVariable String deviceId,
            @RequestParam Integer interval) {
        Map<String, Object> response = zPushService.handleHeartbeat(deviceId, interval);
        return Result.success(response);
    }

    @Operation(summary = "获取同步状态")
    @GetMapping("/sync/status/{deviceId}")
    public Result<Map<String, Object>> getSyncStatus(@PathVariable String deviceId) {
        Map<String, Object> status = zPushService.getSyncStatus(deviceId);
        return Result.success(status);
    }

    @Operation(summary = "获取设备信息")
    @GetMapping("/device/{deviceId}")
    public Result<Map<String, Object>> getDeviceInfo(@PathVariable String deviceId) {
        Map<String, Object> deviceInfo = zPushService.getDeviceInfo(deviceId);
        return Result.success(deviceInfo);
    }

    @Operation(summary = "更新设备安全策略")
    @PutMapping("/device/{deviceId}/policy")
    public Result<Boolean> updateDeviceSecurityPolicy(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> policy) {
        boolean success = zPushService.updateDeviceSecurityPolicy(deviceId, policy);
        return success ? Result.success(true) : Result.error("更新设备安全策略失败");
    }

    @Operation(summary = "远程擦除设备")
    @PostMapping("/device/{deviceId}/wipe")
    public Result<Boolean> remoteWipeDevice(@PathVariable String deviceId) {
        boolean success = zPushService.remoteWipeDevice(deviceId);
        return success ? Result.success(true) : Result.error("远程擦除设备失败");
    }

    @Operation(summary = "获取Z-Push服务状态")
    @GetMapping("/status/{domain}")
    public Result<Map<String, Object>> getZPushStatus(@PathVariable String domain) {
        Map<String, Object> status = zPushService.getZPushStatus(domain);
        return Result.success(status);
    }

    @Operation(summary = "重启Z-Push服务")
    @PostMapping("/restart/{domain}")
    public Result<Boolean> restartZPushService(@PathVariable String domain) {
        boolean success = zPushService.restartZPushService(domain);
        return success ? Result.success(true) : Result.error("重启Z-Push服务失败");
    }

    @Operation(summary = "验证Z-Push配置")
    @PostMapping("/config/validate")
    public Result<Boolean> validateConfig(@RequestBody ZPushConfig config) {
        boolean valid = zPushService.validateZPushConfig(config);
        return Result.success(valid);
    }

    @Operation(summary = "获取同步统计信息")
    @GetMapping("/statistics/{domain}")
    public Result<Map<String, Object>> getSyncStatistics(@PathVariable String domain) {
        Map<String, Object> stats = zPushService.getSyncStatistics(domain);
        return Result.success(stats);
    }

    @Operation(summary = "清理过期同步数据")
    @PostMapping("/cleanup")
    public Result<Boolean> cleanupExpiredSyncData() {
        boolean success = zPushService.cleanupExpiredSyncData();
        return success ? Result.success(true) : Result.error("清理过期数据失败");
    }
}