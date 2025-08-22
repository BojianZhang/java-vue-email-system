package com.enterprise.email.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.common.ApiResponse;
import com.enterprise.email.entity.SslCertificate;
import com.enterprise.email.service.SslCertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * SSL证书管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ssl/certificates")
@RequiredArgsConstructor
@Tag(name = "SSL证书管理", description = "SSL证书的获取、上传、续期和管理")
public class SslCertificateController {

    private final SslCertificateService sslCertificateService;

    /**
     * 获取SSL证书列表
     */
    @GetMapping
    @Operation(summary = "获取SSL证书列表", description = "分页获取SSL证书列表，支持按域名和状态过滤")
    public ApiResponse<IPage<SslCertificate>> getCertificates(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String status) {
        
        try {
            Page<SslCertificate> page = new Page<>(current, size);
            IPage<SslCertificate> result = sslCertificateService.getCertificatesPage(page, domain, status);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取证书列表失败", e);
            return ApiResponse.error("获取证书列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取证书详细信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取证书详细信息", description = "根据ID获取证书的详细信息")
    public ApiResponse<Map<String, Object>> getCertificateDetails(@PathVariable Long id) {
        try {
            Map<String, Object> details = sslCertificateService.getCertificateDetails(id);
            if (details.isEmpty()) {
                return ApiResponse.error("证书不存在");
            }
            return ApiResponse.success(details);
        } catch (Exception e) {
            log.error("获取证书详细信息失败: id={}", id, e);
            return ApiResponse.error("获取证书详细信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取Let's Encrypt免费证书
     */
    @PostMapping("/lets-encrypt")
    @Operation(summary = "获取Let's Encrypt免费证书", description = "通过ACME协议自动获取Let's Encrypt免费SSL证书")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ApiResponse<SslCertificate> obtainLetsEncryptCertificate(
            @RequestBody @Valid LetsEncryptRequest request) {
        
        try {
            // 验证域名控制权
            if (!sslCertificateService.validateDomainControl(request.getDomain())) {
                return ApiResponse.error("域名验证失败，请确认域名控制权");
            }

            SslCertificate certificate = sslCertificateService.obtainLetsEncryptCertificate(
                request.getDomain(), 
                request.getEmail(), 
                request.getChallengeType()
            );
            
            return ApiResponse.success(certificate, "Let's Encrypt证书获取成功");
        } catch (Exception e) {
            log.error("获取Let's Encrypt证书失败: domain={}", request.getDomain(), e);
            return ApiResponse.error("获取Let's Encrypt证书失败: " + e.getMessage());
        }
    }

    /**
     * 上传自定义SSL证书
     */
    @PostMapping("/upload")
    @Operation(summary = "上传自定义SSL证书", description = "上传用户自己的SSL证书文件")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ApiResponse<SslCertificate> uploadCustomCertificate(
            @RequestParam @NotBlank String domain,
            @RequestParam @NotNull MultipartFile certFile,
            @RequestParam @NotNull MultipartFile keyFile,
            @RequestParam(required = false) MultipartFile chainFile) {
        
        try {
            SslCertificate certificate = sslCertificateService.uploadCustomCertificate(
                domain, certFile, keyFile, chainFile);
            
            return ApiResponse.success(certificate, "证书上传成功");
        } catch (Exception e) {
            log.error("上传证书失败: domain={}", domain, e);
            return ApiResponse.error("上传证书失败: " + e.getMessage());
        }
    }

    /**
     * 生成自签名证书
     */
    @PostMapping("/self-signed")
    @Operation(summary = "生成自签名证书", description = "为开发和测试环境生成自签名SSL证书")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SslCertificate> generateSelfSignedCertificate(
            @RequestBody @Valid SelfSignedRequest request) {
        
        try {
            SslCertificate certificate = sslCertificateService.generateSelfSignedCertificate(
                request.getDomain(), 
                request.getValidityDays()
            );
            
            return ApiResponse.success(certificate, "自签名证书生成成功");
        } catch (Exception e) {
            log.error("生成自签名证书失败: domain={}", request.getDomain(), e);
            return ApiResponse.error("生成自签名证书失败: " + e.getMessage());
        }
    }

    /**
     * 续期证书
     */
    @PostMapping("/{id}/renew")
    @Operation(summary = "续期证书", description = "手动续期Let's Encrypt证书")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ApiResponse<Void> renewCertificate(@PathVariable Long id) {
        try {
            boolean success = sslCertificateService.renewCertificate(id);
            if (success) {
                return ApiResponse.success("证书续期成功");
            } else {
                return ApiResponse.error("证书续期失败");
            }
        } catch (Exception e) {
            log.error("证书续期失败: id={}", id, e);
            return ApiResponse.error("证书续期失败: " + e.getMessage());
        }
    }

    /**
     * 应用证书到服务
     */
    @PostMapping("/{id}/apply")
    @Operation(summary = "应用证书", description = "将证书应用到Web服务器配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> applyCertificate(@PathVariable Long id) {
        try {
            boolean success = sslCertificateService.applyCertificate(id);
            if (success) {
                return ApiResponse.success("证书应用成功");
            } else {
                return ApiResponse.error("证书应用失败");
            }
        } catch (Exception e) {
            log.error("应用证书失败: id={}", id, e);
            return ApiResponse.error("应用证书失败: " + e.getMessage());
        }
    }

    /**
     * 撤销证书应用
     */
    @PostMapping("/{id}/revoke")
    @Operation(summary = "撤销证书应用", description = "从Web服务器配置中移除证书")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> revokeCertificate(@PathVariable Long id) {
        try {
            boolean success = sslCertificateService.revokeCertificate(id);
            if (success) {
                return ApiResponse.success("证书撤销成功");
            } else {
                return ApiResponse.error("证书撤销失败");
            }
        } catch (Exception e) {
            log.error("撤销证书失败: id={}", id, e);
            return ApiResponse.error("撤销证书失败: " + e.getMessage());
        }
    }

    /**
     * 删除证书
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除证书", description = "删除SSL证书及其相关文件")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCertificate(@PathVariable Long id) {
        try {
            boolean success = sslCertificateService.deleteCertificate(id);
            if (success) {
                return ApiResponse.success("证书删除成功");
            } else {
                return ApiResponse.error("证书删除失败");
            }
        } catch (Exception e) {
            log.error("删除证书失败: id={}", id, e);
            return ApiResponse.error("删除证书失败: " + e.getMessage());
        }
    }

    /**
     * 检查证书状态
     */
    @PostMapping("/{id}/check")
    @Operation(summary = "检查证书状态", description = "检查证书的有效性和状态")
    public ApiResponse<Void> checkCertificateStatus(@PathVariable Long id) {
        try {
            sslCertificateService.checkCertificateStatus(id);
            return ApiResponse.success("证书状态检查完成");
        } catch (Exception e) {
            log.error("检查证书状态失败: id={}", id, e);
            return ApiResponse.error("检查证书状态失败: " + e.getMessage());
        }
    }

    /**
     * 测试证书配置
     */
    @PostMapping("/test/{domain}")
    @Operation(summary = "测试证书配置", description = "测试域名的HTTPS证书配置是否正常")
    public ApiResponse<Map<String, Object>> testCertificateConfiguration(@PathVariable String domain) {
        try {
            boolean isValid = sslCertificateService.testCertificateConfiguration(domain);
            Map<String, Object> result = Map.of(
                "domain", domain,
                "isValid", isValid,
                "message", isValid ? "证书配置正常" : "证书配置异常"
            );
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("测试证书配置失败: domain={}", domain, e);
            return ApiResponse.error("测试证书配置失败: " + e.getMessage());
        }
    }

    /**
     * 导出证书
     */
    @GetMapping("/{id}/export")
    @Operation(summary = "导出证书", description = "导出证书文件，支持PEM和PKCS12格式")
    public ResponseEntity<byte[]> exportCertificate(
            @PathVariable Long id,
            @RequestParam(defaultValue = "PEM") String format) {
        
        try {
            byte[] certificateData = sslCertificateService.exportCertificate(id, format);
            if (certificateData == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                "certificate-" + id + "." + format.toLowerCase());

            return ResponseEntity.ok()
                .headers(headers)
                .body(certificateData);
                
        } catch (Exception e) {
            log.error("导出证书失败: id={}, format={}", id, format, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取支持的域名列表
     */
    @GetMapping("/supported-domains")
    @Operation(summary = "获取支持的域名", description = "获取系统支持SSL证书的域名列表")
    public ApiResponse<List<String>> getSupportedDomains() {
        try {
            List<String> domains = sslCertificateService.getSupportedDomains();
            return ApiResponse.success(domains);
        } catch (Exception e) {
            log.error("获取支持域名列表失败", e);
            return ApiResponse.error("获取支持域名列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取证书统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取证书统计", description = "获取SSL证书的统计信息和概览")
    public ApiResponse<Map<String, Object>> getCertificateStats() {
        try {
            Map<String, Object> stats = sslCertificateService.getCertificateStats();
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取证书统计失败", e);
            return ApiResponse.error("获取证书统计失败: " + e.getMessage());
        }
    }

    /**
     * 生成Nginx配置
     */
    @GetMapping("/{id}/nginx-config")
    @Operation(summary = "生成Nginx配置", description = "为指定证书生成Nginx SSL配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> generateNginxConfig(@PathVariable Long id) {
        try {
            String config = sslCertificateService.generateNginxConfig(id);
            if (config.isEmpty()) {
                return ApiResponse.error("证书不存在或无法生成配置");
            }
            return ApiResponse.success(config);
        } catch (Exception e) {
            log.error("生成Nginx配置失败: id={}", id, e);
            return ApiResponse.error("生成Nginx配置失败: " + e.getMessage());
        }
    }

    /**
     * 重载Nginx配置
     */
    @PostMapping("/nginx/reload")
    @Operation(summary = "重载Nginx配置", description = "重新加载Nginx配置以应用SSL证书更改")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> reloadNginxConfig() {
        try {
            boolean success = sslCertificateService.reloadNginxConfig();
            if (success) {
                return ApiResponse.success("Nginx配置重载成功");
            } else {
                return ApiResponse.error("Nginx配置重载失败");
            }
        } catch (Exception e) {
            log.error("重载Nginx配置失败", e);
            return ApiResponse.error("重载Nginx配置失败: " + e.getMessage());
        }
    }

    /**
     * 证书备份
     */
    @PostMapping("/backup")
    @Operation(summary = "备份证书", description = "备份所有SSL证书和配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> backupCertificates() {
        try {
            boolean success = sslCertificateService.backupCertificates();
            if (success) {
                return ApiResponse.success("证书备份完成");
            } else {
                return ApiResponse.error("证书备份失败");
            }
        } catch (Exception e) {
            log.error("证书备份失败", e);
            return ApiResponse.error("证书备份失败: " + e.getMessage());
        }
    }

    /**
     * 证书恢复
     */
    @PostMapping("/restore")
    @Operation(summary = "恢复证书", description = "从备份恢复SSL证书和配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restoreCertificates(@RequestParam String backupPath) {
        try {
            boolean success = sslCertificateService.restoreCertificates(backupPath);
            if (success) {
                return ApiResponse.success("证书恢复完成");
            } else {
                return ApiResponse.error("证书恢复失败");
            }
        } catch (Exception e) {
            log.error("证书恢复失败: backupPath={}", backupPath, e);
            return ApiResponse.error("证书恢复失败: " + e.getMessage());
        }
    }

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "系统健康检查", description = "检查SSL证书系统的健康状态")
    public ApiResponse<Map<String, Object>> performHealthCheck() {
        try {
            Map<String, Object> healthCheck = sslCertificateService.performHealthCheck();
            return ApiResponse.success(healthCheck);
        } catch (Exception e) {
            log.error("系统健康检查失败", e);
            return ApiResponse.error("系统健康检查失败: " + e.getMessage());
        }
    }

    // 请求DTO类
    public static class LetsEncryptRequest {
        @NotBlank(message = "域名不能为空")
        private String domain;
        
        @NotBlank(message = "邮箱不能为空")
        private String email;
        
        private String challengeType = "HTTP01";

        // Getters and setters
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getChallengeType() { return challengeType; }
        public void setChallengeType(String challengeType) { this.challengeType = challengeType; }
    }

    public static class SelfSignedRequest {
        @NotBlank(message = "域名不能为空")
        private String domain;
        
        private Integer validityDays = 365;

        // Getters and setters
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public Integer getValidityDays() { return validityDays; }
        public void setValidityDays(Integer validityDays) { this.validityDays = validityDays; }
    }
}