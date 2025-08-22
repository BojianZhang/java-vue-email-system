package com.enterprise.email.controller;

import com.enterprise.email.entity.UserAlias;
import com.enterprise.email.service.UserAliasService;
import com.enterprise.email.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 用户别名管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/aliases")
@RequiredArgsConstructor
@Validated
@Tag(name = "别名管理", description = "用户别名管理相关接口")
public class UserAliasController {

    private final UserAliasService userAliasService;

    @Operation(summary = "获取用户别名列表")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserAliases() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            List<UserAlias> aliases = userAliasService.getAliasesByUserId(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", aliases
            ));
            
        } catch (Exception e) {
            log.error("获取用户别名列表失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取别名列表失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "获取别名统计信息")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAliasStats() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            List<UserAliasService.AliasStats> stats = userAliasService.getAliasStatsForUser(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));
            
        } catch (Exception e) {
            log.error("获取别名统计信息失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取别名统计信息失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "获取默认别名")
    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> getDefaultAlias() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            UserAlias defaultAlias = userAliasService.getDefaultAlias(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", defaultAlias
            ));
            
        } catch (Exception e) {
            log.error("获取默认别名失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取默认别名失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "创建新别名")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAlias(
            @Valid @RequestBody CreateAliasRequest request) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            
            UserAlias alias = userAliasService.createAlias(
                userId,
                request.getDomainId(),
                request.getAliasAddress(),
                request.getAliasName()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", alias,
                "message", "别名创建成功"
            ));
            
        } catch (Exception e) {
            log.error("创建别名失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "创建别名失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "更新别名")
    @PutMapping("/{aliasId}")
    public ResponseEntity<Map<String, Object>> updateAlias(
            @Parameter(description = "别名ID") @PathVariable Long aliasId,
            @Valid @RequestBody UpdateAliasRequest request) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            
            userAliasService.updateAlias(
                aliasId,
                userId,
                request.getAliasName(),
                request.getIsDefault()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "别名更新成功"
            ));
            
        } catch (Exception e) {
            log.error("更新别名失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "更新别名失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "删除别名")
    @DeleteMapping("/{aliasId}")
    public ResponseEntity<Map<String, Object>> deleteAlias(
            @Parameter(description = "别名ID") @PathVariable Long aliasId) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            userAliasService.deleteAlias(aliasId, userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "别名删除成功"
            ));
            
        } catch (Exception e) {
            log.error("删除别名失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "删除别名失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "设置默认别名")
    @PutMapping("/{aliasId}/default")
    public ResponseEntity<Map<String, Object>> setDefaultAlias(
            @Parameter(description = "别名ID") @PathVariable Long aliasId) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            userAliasService.setDefaultAlias(aliasId, userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "默认别名设置成功"
            ));
            
        } catch (Exception e) {
            log.error("设置默认别名失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "设置默认别名失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "切换到指定别名")
    @PostMapping("/{aliasId}/switch")
    public ResponseEntity<Map<String, Object>> switchToAlias(
            @Parameter(description = "别名ID") @PathVariable Long aliasId) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            UserAlias alias = userAliasService.switchToAlias(aliasId, userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", alias,
                "message", "别名切换成功"
            ));
            
        } catch (Exception e) {
            log.error("切换别名失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "切换别名失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "检查别名地址是否可用")
    @GetMapping("/check-availability")
    public ResponseEntity<Map<String, Object>> checkAliasAvailability(
            @Parameter(description = "别名地址") @RequestParam @Email String aliasAddress,
            @Parameter(description = "域名ID") @RequestParam Long domainId) {
        
        try {
            boolean available = userAliasService.isAliasAddressAvailable(aliasAddress, domainId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "available", available,
                "message", available ? "别名地址可用" : "别名地址已被使用"
            ));
            
        } catch (Exception e) {
            log.error("检查别名可用性失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "检查别名可用性失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 创建别名请求对象
     */
    public static class CreateAliasRequest {
        @NotBlank(message = "别名地址不能为空")
        @Email(message = "别名地址格式不正确")
        private String aliasAddress;
        
        private String aliasName;
        
        private Long domainId;

        // Getters and setters
        public String getAliasAddress() { return aliasAddress; }
        public void setAliasAddress(String aliasAddress) { this.aliasAddress = aliasAddress; }
        
        public String getAliasName() { return aliasName; }
        public void setAliasName(String aliasName) { this.aliasName = aliasName; }
        
        public Long getDomainId() { return domainId; }
        public void setDomainId(Long domainId) { this.domainId = domainId; }
    }

    /**
     * 更新别名请求对象
     */
    public static class UpdateAliasRequest {
        private String aliasName;
        private Boolean isDefault;

        // Getters and setters
        public String getAliasName() { return aliasName; }
        public void setAliasName(String aliasName) { this.aliasName = aliasName; }
        
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    }
}