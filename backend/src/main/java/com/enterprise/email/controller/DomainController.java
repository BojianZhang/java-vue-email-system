package com.enterprise.email.controller;

import com.enterprise.email.entity.Domain;
import com.enterprise.email.service.DomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 域名管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/domains")
@RequiredArgsConstructor
@Validated
@Tag(name = "域名管理", description = "邮件域名配置管理接口")
public class DomainController {

    private final DomainService domainService;

    @Operation(summary = "获取所有可用域名")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDomains() {
        try {
            List<Domain> domains = domainService.getAllActiveDomains();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", domains
            ));
            
        } catch (Exception e) {
            log.error("获取域名列表失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取域名列表失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "根据ID获取域名详情")
    @GetMapping("/{domainId}")
    public ResponseEntity<Map<String, Object>> getDomainById(
            @Parameter(description = "域名ID") @PathVariable Long domainId) {
        
        try {
            Domain domain = domainService.getById(domainId);
            
            if (domain == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", domain
            ));
            
        } catch (Exception e) {
            log.error("获取域名详情失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取域名详情失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "创建域名配置")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createDomain(
            @Valid @RequestBody Domain domain) {
        
        try {
            Domain createdDomain = domainService.createDomain(domain);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", createdDomain,
                "message", "域名配置创建成功"
            ));
            
        } catch (Exception e) {
            log.error("创建域名配置失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "创建域名配置失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "更新域名配置")
    @PutMapping("/{domainId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateDomain(
            @Parameter(description = "域名ID") @PathVariable Long domainId,
            @Valid @RequestBody Domain domain) {
        
        try {
            domainService.updateDomain(domainId, domain);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "域名配置更新成功"
            ));
            
        } catch (Exception e) {
            log.error("更新域名配置失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "更新域名配置失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "删除域名配置")
    @DeleteMapping("/{domainId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteDomain(
            @Parameter(description = "域名ID") @PathVariable Long domainId) {
        
        try {
            domainService.deleteDomain(domainId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "域名配置删除成功"
            ));
            
        } catch (Exception e) {
            log.error("删除域名配置失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "删除域名配置失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "测试SMTP连接")
    @PostMapping("/{domainId}/test-smtp")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testSmtpConnection(
            @Parameter(description = "域名ID") @PathVariable Long domainId) {
        
        try {
            Domain domain = domainService.getById(domainId);
            if (domain == null) {
                return ResponseEntity.notFound().build();
            }
            
            boolean success = domainService.testSmtpConnection(domain);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "connected", success,
                "message", success ? "SMTP连接测试成功" : "SMTP连接测试失败"
            ));
            
        } catch (Exception e) {
            log.error("SMTP连接测试失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "connected", false,
                "message", "SMTP连接测试失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "测试IMAP连接")
    @PostMapping("/{domainId}/test-imap")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testImapConnection(
            @Parameter(description = "域名ID") @PathVariable Long domainId) {
        
        try {
            Domain domain = domainService.getById(domainId);
            if (domain == null) {
                return ResponseEntity.notFound().build();
            }
            
            boolean success = domainService.testImapConnection(domain);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "connected", success,
                "message", success ? "IMAP连接测试成功" : "IMAP连接测试失败"
            ));
            
        } catch (Exception e) {
            log.error("IMAP连接测试失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "connected", false,
                "message", "IMAP连接测试失败: " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "验证域名配置")
    @PostMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> validateDomain(
            @Valid @RequestBody Domain domain) {
        
        try {
            boolean valid = domainService.validateDomainConfig(domain);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", valid,
                "message", valid ? "域名配置有效" : "域名配置无效"
            ));
            
        } catch (Exception e) {
            log.error("验证域名配置失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "valid", false,
                "message", "验证域名配置失败: " + e.getMessage()
            ));
        }
    }
}