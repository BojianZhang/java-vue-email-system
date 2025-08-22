package com.enterprise.email.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * SSL证书验证工具类
 */
@Slf4j
@Component
public class SslCertificateValidator {

    /**
     * 验证证书文件
     */
    public CertificateValidationResult validateCertificateFile(String certPath, String keyPath, String domain) {
        CertificateValidationResult result = new CertificateValidationResult();
        result.setValid(false);
        
        try {
            // 加载证书
            X509Certificate certificate = loadCertificate(certPath);
            if (certificate == null) {
                result.addError("无法加载证书文件: " + certPath);
                return result;
            }
            
            // 验证证书基本信息
            validateCertificateBasics(certificate, result);
            
            // 验证域名匹配
            if (domain != null && !domain.isEmpty()) {
                validateDomainMatch(certificate, domain, result);
            }
            
            // 验证私钥匹配（如果提供）
            if (keyPath != null && !keyPath.isEmpty()) {
                validatePrivateKeyMatch(certPath, keyPath, result);
            }
            
            // 计算证书指纹
            result.setFingerprint(calculateFingerprint(certificate));
            
            // 获取证书详细信息
            result.setCertificateInfo(extractCertificateInfo(certificate));
            
            result.setValid(result.getErrors().isEmpty());
            
        } catch (Exception e) {
            log.error("验证证书时发生异常", e);
            result.addError("证书验证异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 验证在线证书
     */
    public CertificateValidationResult validateOnlineCertificate(String domain, int port) {
        CertificateValidationResult result = new CertificateValidationResult();
        result.setValid(false);
        
        try {
            // 创建SSL上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new TrustAllTrustManager()}, null);
            
            // 连接到服务器
            SSLSocketFactory factory = sslContext.getSocketFactory();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(domain, port)) {
                socket.startHandshake();
                
                // 获取证书链
                Certificate[] certificates = socket.getSession().getPeerCertificates();
                if (certificates.length == 0) {
                    result.addError("未找到服务器证书");
                    return result;
                }
                
                X509Certificate certificate = (X509Certificate) certificates[0];
                
                // 验证证书
                validateCertificateBasics(certificate, result);
                validateDomainMatch(certificate, domain, result);
                
                // 获取协议和加密套件信息
                result.setSslProtocol(socket.getSession().getProtocol());
                result.setCipherSuite(socket.getSession().getCipherSuite());
                
                // 计算证书指纹
                result.setFingerprint(calculateFingerprint(certificate));
                
                // 获取证书详细信息
                result.setCertificateInfo(extractCertificateInfo(certificate));
                
                result.setValid(result.getErrors().isEmpty());
            }
            
        } catch (Exception e) {
            log.error("验证在线证书时发生异常: domain={}, port={}", domain, port, e);
            result.addError("在线证书验证异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 检查证书是否即将过期
     */
    public boolean isCertificateExpiringSoon(X509Certificate certificate, int daysThreshold) {
        try {
            Date expiryDate = certificate.getNotAfter();
            Date now = new Date();
            long diffInMillis = expiryDate.getTime() - now.getTime();
            long daysUntilExpiry = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
            
            return daysUntilExpiry <= daysThreshold;
        } catch (Exception e) {
            log.error("检查证书过期时间失败", e);
            return true; // 出错时认为需要关注
        }
    }
    
    /**
     * 获取证书剩余天数
     */
    public long getDaysUntilExpiry(X509Certificate certificate) {
        try {
            Date expiryDate = certificate.getNotAfter();
            Date now = new Date();
            long diffInMillis = expiryDate.getTime() - now.getTime();
            return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("计算证书剩余天数失败", e);
            return -1;
        }
    }
    
    /**
     * 验证证书链
     */
    public boolean validateCertificateChain(String certPath, String chainPath) {
        try {
            X509Certificate certificate = loadCertificate(certPath);
            if (certificate == null) {
                return false;
            }
            
            if (chainPath != null && !chainPath.isEmpty()) {
                X509Certificate chainCert = loadCertificate(chainPath);
                if (chainCert != null) {
                    // 验证证书链
                    try {
                        certificate.verify(chainCert.getPublicKey());
                        return true;
                    } catch (Exception e) {
                        log.debug("证书链验证失败", e);
                        return false;
                    }
                }
            }
            
            // 如果没有提供证书链，只验证证书本身
            try {
                certificate.checkValidity();
                return true;
            } catch (Exception e) {
                log.debug("证书有效性验证失败", e);
                return false;
            }
            
        } catch (Exception e) {
            log.error("验证证书链失败", e);
            return false;
        }
    }
    
    // 私有方法
    
    private X509Certificate loadCertificate(String certPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(certPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(fis);
        }
    }
    
    private void validateCertificateBasics(X509Certificate certificate, CertificateValidationResult result) {
        try {
            // 检查证书有效期
            certificate.checkValidity();
            
            Date notBefore = certificate.getNotBefore();
            Date notAfter = certificate.getNotAfter();
            Date now = new Date();
            
            result.setIssuedAt(LocalDateTime.ofInstant(notBefore.toInstant(), ZoneId.systemDefault()));
            result.setExpiresAt(LocalDateTime.ofInstant(notAfter.toInstant(), ZoneId.systemDefault()));
            
            // 检查是否即将过期（30天内）
            if (isCertificateExpiringSoon(certificate, 30)) {
                result.addWarning("证书将在30天内过期");
            }
            
            // 检查是否已过期
            if (now.after(notAfter)) {
                result.addError("证书已过期");
            }
            
            // 检查是否还未生效
            if (now.before(notBefore)) {
                result.addError("证书还未生效");
            }
            
        } catch (Exception e) {
            result.addError("证书有效期验证失败: " + e.getMessage());
        }
    }
    
    private void validateDomainMatch(X509Certificate certificate, String domain, CertificateValidationResult result) {
        try {
            boolean domainMatches = false;
            
            // 检查Subject Alternative Names (SAN)
            Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
            if (altNames != null) {
                for (List<?> altName : altNames) {
                    if (altName.size() >= 2 && altName.get(1) instanceof String) {
                        String dnsName = (String) altName.get(1);
                        if (matchesDomain(domain, dnsName)) {
                            domainMatches = true;
                            break;
                        }
                    }
                }
            }
            
            // 如果SAN中没有匹配，检查Common Name (CN)
            if (!domainMatches) {
                String dn = certificate.getSubjectDN().getName();
                if (dn.contains("CN=" + domain)) {
                    domainMatches = true;
                }
            }
            
            if (!domainMatches) {
                result.addError("证书域名与指定域名不匹配: " + domain);
            }
            
        } catch (Exception e) {
            result.addError("域名匹配验证失败: " + e.getMessage());
        }
    }
    
    private boolean matchesDomain(String domain, String certDomain) {
        if (domain.equals(certDomain)) {
            return true;
        }
        
        // 检查通配符匹配
        if (certDomain.startsWith("*.")) {
            String wildcardDomain = certDomain.substring(2);
            return domain.endsWith("." + wildcardDomain);
        }
        
        return false;
    }
    
    private void validatePrivateKeyMatch(String certPath, String keyPath, CertificateValidationResult result) {
        try {
            // 这里应该实现私钥与证书的匹配验证
            // 由于Java中私钥验证比较复杂，这里简化处理
            // 在实际应用中，可以使用openssl命令行工具进行验证
            
            result.addInfo("私钥匹配验证已跳过（需要在部署脚本中使用openssl验证）");
            
        } catch (Exception e) {
            result.addError("私钥匹配验证失败: " + e.getMessage());
        }
    }
    
    private String calculateFingerprint(X509Certificate certificate) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(certificate.getEncoded());
            StringBuilder fingerprint = new StringBuilder();
            for (byte b : digest) {
                fingerprint.append(String.format("%02x", b));
            }
            return fingerprint.toString();
        } catch (Exception e) {
            log.error("计算证书指纹失败", e);
            return null;
        }
    }
    
    private Map<String, Object> extractCertificateInfo(X509Certificate certificate) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            info.put("subject", certificate.getSubjectDN().toString());
            info.put("issuer", certificate.getIssuerDN().toString());
            info.put("serialNumber", certificate.getSerialNumber().toString());
            info.put("version", certificate.getVersion());
            info.put("signatureAlgorithm", certificate.getSigAlgName());
            info.put("publicKeyAlgorithm", certificate.getPublicKey().getAlgorithm());
            
            // 获取SAN扩展
            Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
            if (altNames != null) {
                List<String> dnsNames = new ArrayList<>();
                for (List<?> altName : altNames) {
                    if (altName.size() >= 2 && altName.get(1) instanceof String) {
                        dnsNames.add((String) altName.get(1));
                    }
                }
                info.put("subjectAlternativeNames", dnsNames);
            }
            
        } catch (Exception e) {
            log.error("提取证书信息失败", e);
            info.put("error", "无法提取证书信息: " + e.getMessage());
        }
        
        return info;
    }
    
    /**
     * 信任所有证书的TrustManager（仅用于测试）
     */
    private static class TrustAllTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // 信任所有客户端证书
        }
        
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // 信任所有服务器证书
        }
        
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
    
    /**
     * 证书验证结果类
     */
    public static class CertificateValidationResult {
        private boolean valid;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> info = new ArrayList<>();
        private String fingerprint;
        private LocalDateTime issuedAt;
        private LocalDateTime expiresAt;
        private String sslProtocol;
        private String cipherSuite;
        private Map<String, Object> certificateInfo;
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
        
        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
        
        public List<String> getInfo() { return info; }
        public void addInfo(String info) { this.info.add(info); }
        
        public String getFingerprint() { return fingerprint; }
        public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
        
        public LocalDateTime getIssuedAt() { return issuedAt; }
        public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
        
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        
        public String getSslProtocol() { return sslProtocol; }
        public void setSslProtocol(String sslProtocol) { this.sslProtocol = sslProtocol; }
        
        public String getCipherSuite() { return cipherSuite; }
        public void setCipherSuite(String cipherSuite) { this.cipherSuite = cipherSuite; }
        
        public Map<String, Object> getCertificateInfo() { return certificateInfo; }
        public void setCertificateInfo(Map<String, Object> certificateInfo) { this.certificateInfo = certificateInfo; }
    }
}