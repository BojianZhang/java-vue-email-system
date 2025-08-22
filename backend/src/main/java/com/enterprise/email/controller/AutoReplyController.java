package com.enterprise.email.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.AutoReplySettings;
import com.enterprise.email.service.AutoReplyService;
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
 * 自动回复设置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auto-reply")
@RequiredArgsConstructor
@Tag(name = "自动回复设置", description = "自动回复设置管理接口")
public class AutoReplyController {

    private final AutoReplyService autoReplyService;
    private final UserAliasService userAliasService;

    @Operation(summary = "创建自动回复设置")
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> createAutoReply(
            @Valid @RequestBody AutoReplySettings autoReply,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证别名所有权
            if (!userAliasService.isAliasOwnedByUser(autoReply.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "无权限为该别名设置自动回复");
                return ResponseEntity.badRequest().body(result);
            }
            
            autoReply.setCreatedBy(userId);
            autoReply.setUpdatedBy(userId);
            
            boolean success = autoReplyService.createAutoReply(autoReply);
            
            if (success) {
                result.put("success", true);
                result.put("message", "自动回复设置创建成功");
                result.put("data", autoReply);
            } else {
                result.put("success", false);
                result.put("message", "自动回复设置创建失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("创建自动回复设置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "更新自动回复设置")
    @PutMapping("/update/{autoReplyId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> updateAutoReply(
            @PathVariable Long autoReplyId,
            @Valid @RequestBody AutoReplySettings autoReply,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证设置存在且属于用户
            AutoReplySettings existing = autoReplyService.getAutoReplyById(autoReplyId);
            if (existing == null || !userAliasService.isAliasOwnedByUser(existing.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "自动回复设置不存在或无权限修改");
                return ResponseEntity.badRequest().body(result);
            }
            
            autoReply.setId(autoReplyId);
            autoReply.setUpdatedBy(userId);
            
            boolean success = autoReplyService.updateAutoReply(autoReply);
            
            if (success) {
                result.put("success", true);
                result.put("message", "自动回复设置更新成功");
                result.put("data", autoReply);
            } else {
                result.put("success", false);
                result.put("message", "自动回复设置更新失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("更新自动回复设置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "删除自动回复设置")
    @DeleteMapping("/delete/{autoReplyId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> deleteAutoReply(
            @PathVariable Long autoReplyId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证设置存在且属于用户
            AutoReplySettings existing = autoReplyService.getAutoReplyById(autoReplyId);
            if (existing == null || !userAliasService.isAliasOwnedByUser(existing.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "自动回复设置不存在或无权限删除");
                return ResponseEntity.badRequest().body(result);
            }
            
            boolean success = autoReplyService.deleteAutoReply(autoReplyId);
            
            if (success) {
                result.put("success", true);
                result.put("message", "自动回复设置删除成功");
            } else {
                result.put("success", false);
                result.put("message", "自动回复设置删除失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("删除自动回复设置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "获取用户的所有自动回复设置")
    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAutoRepliesList(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            List<AutoReplySettings> autoReplies = autoReplyService.getAutoRepliesByUserId(userId);
            
            result.put("success", true);
            result.put("data", autoReplies);
            result.put("total", autoReplies.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取自动回复设置列表异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "分页查询自动回复设置")
    @GetMapping("/page")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAutoRepliesPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String aliasAddress,
            @RequestParam(required = false) Boolean isActive,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            Page<AutoReplySettings> page = new Page<>(current, size);
            
            IPage<AutoReplySettings> pageResult = autoReplyService.getAutoRepliesPage(
                page, userId, aliasAddress, isActive);
            
            result.put("success", true);
            result.put("data", pageResult.getRecords());
            result.put("total", pageResult.getTotal());
            result.put("current", pageResult.getCurrent());
            result.put("size", pageResult.getSize());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("分页查询自动回复设置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "获取别名的自动回复设置")
    @GetMapping("/alias/{aliasId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAutoReplyByAlias(
            @PathVariable Long aliasId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证别名所有权
            if (!userAliasService.isAliasOwnedByUser(aliasId, userId)) {
                result.put("success", false);
                result.put("message", "无权限查看该别名的自动回复设置");
                return ResponseEntity.badRequest().body(result);
            }
            
            AutoReplySettings autoReply = autoReplyService.getAutoReplyByAliasId(aliasId);
            
            result.put("success", true);
            result.put("data", autoReply);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取别名自动回复设置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "启用/禁用自动回复")
    @PatchMapping("/toggle/{autoReplyId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> toggleAutoReplyStatus(
            @PathVariable Long autoReplyId,
            @RequestBody Map<String, Boolean> statusMap,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            Boolean isActive = statusMap.get("isActive");
            
            if (isActive == null) {
                result.put("success", false);
                result.put("message", "缺少isActive参数");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 验证设置存在且属于用户
            AutoReplySettings existing = autoReplyService.getAutoReplyById(autoReplyId);
            if (existing == null || !userAliasService.isAliasOwnedByUser(existing.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "自动回复设置不存在或无权限修改");
                return ResponseEntity.badRequest().body(result);
            }
            
            boolean success = autoReplyService.toggleAutoReplyStatus(autoReplyId, isActive);
            
            if (success) {
                result.put("success", true);
                result.put("message", "自动回复状态切换成功");
            } else {
                result.put("success", false);
                result.put("message", "自动回复状态切换失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("切换自动回复状态异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "测试自动回复条件")
    @PostMapping("/test")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> testAutoReply(
            @RequestBody Map<String, String> testData,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String aliasAddress = testData.get("aliasAddress");
            String fromEmail = testData.get("fromEmail");
            String subject = testData.get("subject");
            
            if (aliasAddress == null || fromEmail == null || subject == null) {
                result.put("success", false);
                result.put("message", "缺少必需的测试参数");
                return ResponseEntity.badRequest().body(result);
            }
            
            boolean shouldReply = autoReplyService.shouldSendAutoReply(aliasAddress, fromEmail, subject);
            AutoReplySettings settings = autoReplyService.getActiveAutoReplyByAlias(aliasAddress);
            
            result.put("success", true);
            result.put("shouldReply", shouldReply);
            result.put("settings", settings);
            
            if (shouldReply) {
                result.put("message", "符合自动回复条件，将发送自动回复");
            } else {
                result.put("message", "不符合自动回复条件");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("测试自动回复异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "获取即将过期的自动回复设置")
    @GetMapping("/expiring-soon")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getExpiringSoonAutoReplies() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<AutoReplySettings> expiringSoon = autoReplyService.getExpiringSoonAutoReplies();
            
            result.put("success", true);
            result.put("data", expiringSoon);
            result.put("total", expiringSoon.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取即将过期的自动回复设置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "更新过期的自动回复设置")
    @PostMapping("/update-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateExpiredAutoReplies() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            int count = autoReplyService.updateExpiredAutoReplies();
            
            result.put("success", true);
            result.put("message", "更新过期的自动回复设置成功");
            result.put("count", count);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("更新过期的自动回复设置异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}