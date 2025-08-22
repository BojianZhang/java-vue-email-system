package com.enterprise.email.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.ExternalAliasSync;
import com.enterprise.email.service.ExternalAliasSyncService;
import com.enterprise.email.service.UserAliasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部别名同步控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/external-sync")
@RequiredArgsConstructor
@Tag(name = "外部别名同步", description = "外部平台别名名称同步管理接口")
public class ExternalAliasSyncController {

    private final ExternalAliasSyncService syncService;
    private final UserAliasService userAliasService;

    @Operation(summary = "创建外部别名同步配置")
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> createSyncConfig(
            @Valid @RequestBody ExternalAliasSync syncConfig,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证别名所有权
            if (!userAliasService.isAliasOwnedByUser(syncConfig.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "无权限为该别名创建同步配置");
                return ResponseEntity.badRequest().body(result);
            }

            // 验证平台配置
            if (!syncService.validatePlatformConfig(syncConfig)) {
                result.put("success", false);
                result.put("message", "平台配置信息不完整");
                return ResponseEntity.badRequest().body(result);
            }
            
            syncConfig.setCreatedBy(userId);
            syncConfig.setUpdatedBy(userId);
            
            boolean success = syncService.createSyncConfig(syncConfig);
            
            if (success) {
                result.put("success", true);
                result.put("message", "外部别名同步配置创建成功，将自动同步别名名称");
                result.put("data", syncConfig);
            } else {
                result.put("success", false);
                result.put("message", "外部别名同步配置创建失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("创建外部别名同步配置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "更新外部别名同步配置")
    @PutMapping("/update/{syncId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> updateSyncConfig(
            @PathVariable Long syncId,
            @Valid @RequestBody ExternalAliasSync syncConfig,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证配置存在且属于用户
            ExternalAliasSync existing = syncService.getSyncConfigById(syncId);
            if (existing == null || !userAliasService.isAliasOwnedByUser(existing.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "同步配置不存在或无权限修改");
                return ResponseEntity.badRequest().body(result);
            }

            // 验证平台配置
            if (!syncService.validatePlatformConfig(syncConfig)) {
                result.put("success", false);
                result.put("message", "平台配置信息不完整");
                return ResponseEntity.badRequest().body(result);
            }
            
            syncConfig.setId(syncId);
            syncConfig.setUpdatedBy(userId);
            
            boolean success = syncService.updateSyncConfig(syncConfig);
            
            if (success) {
                result.put("success", true);
                result.put("message", "外部别名同步配置更新成功");
                result.put("data", syncConfig);
            } else {
                result.put("success", false);
                result.put("message", "外部别名同步配置更新失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("更新外部别名同步配置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "删除外部别名同步配置")
    @DeleteMapping("/delete/{syncId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> deleteSyncConfig(
            @PathVariable Long syncId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证配置存在且属于用户
            ExternalAliasSync existing = syncService.getSyncConfigById(syncId);
            if (existing == null || !userAliasService.isAliasOwnedByUser(existing.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "同步配置不存在或无权限删除");
                return ResponseEntity.badRequest().body(result);
            }
            
            boolean success = syncService.deleteSyncConfig(syncId);
            
            if (success) {
                result.put("success", true);
                result.put("message", "外部别名同步配置删除成功");
            } else {
                result.put("success", false);
                result.put("message", "外部别名同步配置删除失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("删除外部别名同步配置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "获取用户的同步配置列表")
    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getSyncConfigsList(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            List<ExternalAliasSync> configs = syncService.getSyncConfigsByUserId(userId);
            
            result.put("success", true);
            result.put("data", configs);
            result.put("total", configs.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取同步配置列表异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "分页查询同步配置")
    @GetMapping("/page")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getSyncConfigsPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String syncStatus,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            Page<ExternalAliasSync> page = new Page<>(current, size);
            
            IPage<ExternalAliasSync> pageResult = syncService.getSyncConfigsPage(
                page, userId, platformType, syncStatus);
            
            result.put("success", true);
            result.put("data", pageResult.getRecords());
            result.put("total", pageResult.getTotal());
            result.put("current", pageResult.getCurrent());
            result.put("size", pageResult.getSize());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("分页查询同步配置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "立即同步指定别名名称")
    @PostMapping("/sync/{aliasId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> syncAliasName(
            @PathVariable Long aliasId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证别名所有权
            if (!userAliasService.isAliasOwnedByUser(aliasId, userId)) {
                result.put("success", false);
                result.put("message", "无权限同步该别名");
                return ResponseEntity.badRequest().body(result);
            }
            
            boolean success = syncService.syncAliasName(aliasId);
            
            if (success) {
                result.put("success", true);
                result.put("message", "别名名称同步成功");
            } else {
                result.put("success", false);
                result.put("message", "别名名称同步失败，请检查外部平台连接");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("同步别名名称异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "批量同步用户的所有别名")
    @PostMapping("/batch-sync")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> batchSyncUserAliases(Authentication authentication) {
        try {
            Long userId = Long.valueOf(authentication.getName());
            Map<String, Object> result = syncService.batchSyncUserAliases(userId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("批量同步用户别名异常", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "测试外部平台连接")
    @PostMapping("/test-connection")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> testPlatformConnection(
            @Valid @RequestBody ExternalAliasSync syncConfig,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 如果是更新现有配置，验证所有权
            if (syncConfig.getId() != null) {
                ExternalAliasSync existing = syncService.getSyncConfigById(syncConfig.getId());
                if (existing == null || !userAliasService.isAliasOwnedByUser(existing.getAliasId(), userId)) {
                    result.put("success", false);
                    result.put("message", "无权限测试该配置");
                    return ResponseEntity.badRequest().body(result);
                }
            } else if (syncConfig.getAliasId() != null) {
                // 新建配置，验证别名所有权
                if (!userAliasService.isAliasOwnedByUser(syncConfig.getAliasId(), userId)) {
                    result.put("success", false);
                    result.put("message", "无权限为该别名创建同步配置");
                    return ResponseEntity.badRequest().body(result);
                }
            }

            // 验证平台配置
            if (!syncService.validatePlatformConfig(syncConfig)) {
                result.put("success", false);
                result.put("message", "平台配置信息不完整");
                return ResponseEntity.badRequest().body(result);
            }
            
            boolean connected = syncService.testPlatformConnection(syncConfig);
            
            if (connected) {
                result.put("success", true);
                result.put("message", "外部平台连接测试成功");
            } else {
                result.put("success", false);
                result.put("message", "外部平台连接测试失败，请检查配置信息");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("测试外部平台连接异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "启用/禁用自动同步")
    @PatchMapping("/toggle-auto-sync/{syncId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> toggleAutoSync(
            @PathVariable Long syncId,
            @RequestBody Map<String, Boolean> toggleData,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            Boolean enabled = toggleData.get("enabled");
            
            if (enabled == null) {
                result.put("success", false);
                result.put("message", "缺少enabled参数");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 验证配置存在且属于用户
            ExternalAliasSync existing = syncService.getSyncConfigById(syncId);
            if (existing == null || !userAliasService.isAliasOwnedByUser(existing.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "同步配置不存在或无权限修改");
                return ResponseEntity.badRequest().body(result);
            }
            
            boolean success = syncService.toggleAutoSync(syncId, enabled);
            
            if (success) {
                result.put("success", true);
                result.put("message", enabled ? "自动同步已启用" : "自动同步已禁用");
            } else {
                result.put("success", false);
                result.put("message", "切换自动同步状态失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("切换自动同步状态异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "获取支持的平台类型列表")
    @GetMapping("/supported-platforms")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getSupportedPlatforms() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<ExternalAliasSyncService.PlatformType> platforms = syncService.getSupportedPlatforms();
            
            result.put("success", true);
            result.put("data", platforms);
            result.put("total", platforms.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取支持的平台类型异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "获取别名的同步配置")
    @GetMapping("/alias/{aliasId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getSyncConfigByAlias(
            @PathVariable Long aliasId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证别名所有权
            if (!userAliasService.isAliasOwnedByUser(aliasId, userId)) {
                result.put("success", false);
                result.put("message", "无权限查看该别名的同步配置");
                return ResponseEntity.badRequest().body(result);
            }
            
            ExternalAliasSync syncConfig = syncService.getSyncConfigByAliasId(aliasId);
            
            result.put("success", true);
            result.put("data", syncConfig);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取别名同步配置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}