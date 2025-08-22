package com.enterprise.email.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.SslCertificate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * SSL证书管理服务接口
 */
public interface SslCertificateService {

    /**
     * 获取Let's Encrypt免费证书
     */
    SslCertificate obtainLetsEncryptCertificate(String domain, String email, String challengeType);

    /**
     * 上传自定义SSL证书
     */
    SslCertificate uploadCustomCertificate(String domain, MultipartFile certFile, 
                                         MultipartFile keyFile, MultipartFile chainFile);

    /**
     * 生成自签名证书
     */
    SslCertificate generateSelfSignedCertificate(String domain, int validityDays);

    /**
     * 续期证书
     */
    boolean renewCertificate(Long certificateId);

    /**
     * 应用证书到服务
     */
    boolean applyCertificate(Long certificateId);

    /**
     * 撤销证书应用
     */
    boolean revokeCertificate(Long certificateId);

    /**
     * 删除证书
     */
    boolean deleteCertificate(Long certificateId);

    /**
     * 获取证书列表
     */
    IPage<SslCertificate> getCertificatesPage(Page<SslCertificate> page, String domain, String status);

    /**
     * 根据域名获取证书
     */
    SslCertificate getCertificateByDomain(String domain);

    /**
     * 检查证书状态
     */
    void checkCertificateStatus(Long certificateId);

    /**
     * 自动续期到期证书
     */
    void autoRenewExpiringCertificates();

    /**
     * 验证域名控制权
     */
    boolean validateDomainControl(String domain);

    /**
     * 获取证书详细信息
     */
    Map<String, Object> getCertificateDetails(Long certificateId);

    /**
     * 导出证书文件
     */
    byte[] exportCertificate(Long certificateId, String format);

    /**
     * 获取支持的域名列表
     */
    List<String> getSupportedDomains();

    /**
     * 获取证书统计信息
     */
    Map<String, Object> getCertificateStats();

    /**
     * 测试证书配置
     */
    boolean testCertificateConfiguration(String domain);

    /**
     * 备份证书文件
     */
    boolean backupCertificates();

    /**
     * 恢复证书文件
     */
    boolean restoreCertificates(String backupPath);

    /**
     * 获取Nginx配置
     */
    String generateNginxConfig(Long certificateId);

    /**
     * 重载Nginx配置
     */
    boolean reloadNginxConfig();

    /**
     * 证书健康检查
     */
    Map<String, Object> performHealthCheck();

    /**
     * 证书类型枚举
     */
    enum CertType {
        LETS_ENCRYPT("LETS_ENCRYPT", "Let's Encrypt 免费证书"),
        UPLOADED("UPLOADED", "用户上传证书"),
        SELF_SIGNED("SELF_SIGNED", "自签名证书");

        private final String code;
        private final String description;

        CertType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    /**
     * 证书状态枚举
     */
    enum CertStatus {
        ACTIVE("ACTIVE", "有效"),
        EXPIRED("EXPIRED", "已过期"),
        PENDING("PENDING", "处理中"),
        FAILED("FAILED", "失败"),
        REVOKED("REVOKED", "已撤销");

        private final String code;
        private final String description;

        CertStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    /**
     * 挑战类型枚举
     */
    enum ChallengeType {
        HTTP01("HTTP01", "HTTP-01 验证"),
        DNS01("DNS01", "DNS-01 验证");

        private final String code;
        private final String description;

        ChallengeType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
}