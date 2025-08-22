package com.enterprise.email.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.AliasForwardRule;
import com.enterprise.email.service.AliasForwardRuleService;
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
 * 别名转发规则控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/alias-forward")
@RequiredArgsConstructor
@Tag(name = "别名转发规则", description = "别名转发规则管理接口")
public class AliasForwardRuleController {

    private final AliasForwardRuleService forwardRuleService;
    private final UserAliasService userAliasService;

    @Operation(summary = "创建转发规则")
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> createForwardRule(
            @Valid @RequestBody AliasForwardRule forwardRule,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证别名所有权
            if (!userAliasService.isAliasOwnedByUser(forwardRule.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "无权限为该别名创建转发规则");
                return ResponseEntity.badRequest().body(result);
            }
            
            forwardRule.setCreatedBy(userId);
            forwardRule.setUpdatedBy(userId);
            
            boolean success = forwardRuleService.createForwardRule(forwardRule);
            
            if (success) {
                result.put("success", true);
                result.put("message", "转发规则创建成功");
                result.put("data", forwardRule);
            } else {
                result.put("success", false);
                result.put("message", "转发规则创建失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("创建转发规则异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "更新转发规则")
    @PutMapping("/update/{ruleId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> updateForwardRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody AliasForwardRule forwardRule,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证规则存在且属于用户
            AliasForwardRule existing = forwardRuleService.getForwardRuleById(ruleId);
            if (existing == null || !userAliasService.isAliasOwnedByUser(existing.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "转发规则不存在或无权限修改");
                return ResponseEntity.badRequest().body(result);
            }
            
            forwardRule.setId(ruleId);
            forwardRule.setUpdatedBy(userId);
            
            boolean success = forwardRuleService.updateForwardRule(forwardRule);
            
            if (success) {
                result.put("success", true);
                result.put("message", "转发规则更新成功");
                result.put("data", forwardRule);
            } else {
                result.put("success", false);
                result.put("message", "转发规则更新失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("更新转发规则异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "删除转发规则")
    @DeleteMapping("/delete/{ruleId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> deleteForwardRule(
            @PathVariable Long ruleId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证规则存在且属于用户
            AliasForwardRule existing = forwardRuleService.getForwardRuleById(ruleId);
            if (existing == null || !userAliasService.isAliasOwnedByUser(existing.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "转发规则不存在或无权限删除");
                return ResponseEntity.badRequest().body(result);
            }
            
            boolean success = forwardRuleService.deleteForwardRule(ruleId);
            
            if (success) {
                result.put("success", true);
                result.put("message", "转发规则删除成功");
            } else {
                result.put("success", false);
                result.put("message", "转发规则删除失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("删除转发规则异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "获取用户的所有转发规则")
    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getForwardRulesList(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            List<AliasForwardRule> rules = forwardRuleService.getForwardRulesByUserId(userId);
            
            result.put("success", true);
            result.put("data", rules);
            result.put("total", rules.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取转发规则列表异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "分页查询转发规则")
    @GetMapping("/page")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getForwardRulesPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String aliasAddress,
            @RequestParam(required = false) String forwardTo,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            Page<AliasForwardRule> page = new Page<>(current, size);
            
            IPage<AliasForwardRule> pageResult = forwardRuleService.getForwardRulesPage(
                page, userId, aliasAddress, forwardTo);
            
            result.put("success", true);
            result.put("data", pageResult.getRecords());
            result.put("total", pageResult.getTotal());
            result.put("current", pageResult.getCurrent());
            result.put("size", pageResult.getSize());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("分页查询转发规则异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "获取别名的转发规则")
    @GetMapping("/alias/{aliasId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getForwardRulesByAlias(
            @PathVariable Long aliasId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证别名所有权
            if (!userAliasService.isAliasOwnedByUser(aliasId, userId)) {
                result.put("success", false);
                result.put("message", "无权限查看该别名的转发规则");
                return ResponseEntity.badRequest().body(result);
            }
            
            List<AliasForwardRule> rules = forwardRuleService.getForwardRulesByAliasId(aliasId);
            
            result.put("success", true);
            result.put("data", rules);
            result.put("total", rules.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取别名转发规则异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "启用/禁用转发规则")
    @PatchMapping("/toggle/{ruleId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> toggleForwardRuleStatus(
            @PathVariable Long ruleId,
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
            
            // 验证规则存在且属于用户
            AliasForwardRule existing = forwardRuleService.getForwardRuleById(ruleId);
            if (existing == null || !userAliasService.isAliasOwnedByUser(existing.getAliasId(), userId)) {
                result.put("success", false);
                result.put("message", "转发规则不存在或无权限修改");
                return ResponseEntity.badRequest().body(result);
            }
            
            boolean success = forwardRuleService.toggleForwardRuleStatus(ruleId, isActive);
            
            if (success) {
                result.put("success", true);
                result.put("message", "转发规则状态切换成功");
            } else {
                result.put("success", false);
                result.put("message", "转发规则状态切换失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("切换转发规则状态异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(summary = "批量删除转发规则")
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> batchDeleteForwardRules(
            @RequestBody List<Long> ruleIds,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.valueOf(authentication.getName());
            
            // 验证所有规则都属于用户
            for (Long ruleId : ruleIds) {
                AliasForwardRule existing = forwardRuleService.getForwardRuleById(ruleId);
                if (existing == null || !userAliasService.isAliasOwnedByUser(existing.getAliasId(), userId)) {
                    result.put("success", false);
                    result.put("message", "存在无权限删除的转发规则");
                    return ResponseEntity.badRequest().body(result);
                }
            }
            
            boolean success = forwardRuleService.batchDeleteForwardRules(ruleIds);
            
            if (success) {
                result.put("success", true);
                result.put("message", "批量删除转发规则成功");
            } else {
                result.put("success", false);
                result.put("message", "批量删除转发规则失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("批量删除转发规则异常", e);
            result.put("success", false);
            result.put("message", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}