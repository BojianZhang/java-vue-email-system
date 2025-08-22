package com.enterprise.email.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.email.entity.SslCertificate;
import com.enterprise.email.mapper.SslCertificateMapper;
import com.enterprise.email.service.SslCertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * SSL证书管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SslCertificateServiceImpl extends ServiceImpl<SslCertificateMapper, SslCertificate>
        implements SslCertificateService {

    private final SslCertificateMapper certificateMapper;

    @Value("${ssl.certs.directory:/opt/ssl/certs}")
    private String certsDirectory;

    @Value("${ssl.nginx.config:/etc/nginx/conf.d}")
    private String nginxConfigDir;

    @Value("${ssl.acme.directory:/opt/ssl/acme}")
    private String acmeDirectory;

    @Value("${ssl.backup.directory:/opt/ssl/backups}")
    private String backupDirectory;

    @Value("${ssl.lets-encrypt.staging:false}")
    private Boolean letsEncryptStaging;

    @Override
    @Transactional
    public SslCertificate obtainLetsEncryptCertificate(String domain, String email, String challengeType) {
        log.info("开始为域名 {} 获取 Let's Encrypt 证书", domain);
        
        try {
            // 检查域名是否已有证书
            SslCertificate existingCert = getCertificateByDomain(domain);
            if (existingCert != null && CertStatus.ACTIVE.getCode().equals(existingCert.getStatus())) {
                log.info("域名 {} 已存在有效证书", domain);
                return existingCert;
            }

            // 创建证书记录
            SslCertificate certificate = new SslCertificate();
            certificate.setDomain(domain);
            certificate.setCertType(CertType.LETS_ENCRYPT.getCode());
            certificate.setStatus(CertStatus.PENDING.getCode());
            certificate.setEmail(email);
            certificate.setChallengeType(challengeType);
            certificate.setAutoRenew(true);
            certificate.setRenewalFailures(0);
            certificate.setApplied(false);
            
            save(certificate);

            // 执行ACME客户端获取证书
            boolean success = executeAcmeClient(certificate);
            
            if (success) {
                // 解析证书信息
                parseCertificateInfo(certificate);
                
                // 更新证书状态
                certificate.setStatus(CertStatus.ACTIVE.getCode());
                certificate.setIssuedAt(LocalDateTime.now());
                
                updateById(certificate);
                
                log.info("成功为域名 {} 获取 Let's Encrypt 证书", domain);
                return certificate;
            } else {
                certificate.setStatus(CertStatus.FAILED.getCode());
                certificate.setErrorMessage("Let's Encrypt 证书获取失败");
                updateById(certificate);
                throw new RuntimeException("Let's Encrypt 证书获取失败");
            }
            
        } catch (Exception e) {
            log.error("获取 Let's Encrypt 证书失败: domain={}", domain, e);
            throw new RuntimeException("获取 Let's Encrypt 证书失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SslCertificate uploadCustomCertificate(String domain, MultipartFile certFile, 
                                                MultipartFile keyFile, MultipartFile chainFile) {
        log.info("开始上传域名 {} 的自定义证书", domain);
        
        try {
            // 验证文件
            validateCertificateFiles(certFile, keyFile);
            
            // 创建证书目录
            Path domainDir = createCertificateDirectory(domain);
            
            // 保存文件
            String certPath = saveUploadedFile(certFile, domainDir, "cert.pem");
            String keyPath = saveUploadedFile(keyFile, domainDir, "key.pem");
            String chainPath = chainFile != null ? saveUploadedFile(chainFile, domainDir, "chain.pem") : null;
            
            // 验证证书有效性
            X509Certificate x509Cert = loadX509Certificate(certPath);
            validateCertificate(x509Cert, domain);
            
            // 创建证书记录
            SslCertificate certificate = new SslCertificate();
            certificate.setDomain(domain);
            certificate.setCertType(CertType.UPLOADED.getCode());
            certificate.setStatus(CertStatus.ACTIVE.getCode());
            certificate.setCertPath(certPath);
            certificate.setKeyPath(keyPath);
            certificate.setChainPath(chainPath);
            certificate.setIssuedAt(LocalDateTime.now());
            certificate.setExpiresAt(convertToLocalDateTime(x509Cert.getNotAfter()));
            certificate.setFingerprint(getCertificateFingerprint(x509Cert));
            certificate.setAutoRenew(false);
            certificate.setApplied(false);
            
            // 设置证书详细信息
            certificate.setCertInfo(buildCertificateInfo(x509Cert));
            
            save(certificate);
            
            log.info("成功上传域名 {} 的自定义证书", domain);
            return certificate;
            
        } catch (Exception e) {
            log.error("上传自定义证书失败: domain={}", domain, e);
            throw new RuntimeException("上传自定义证书失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SslCertificate generateSelfSignedCertificate(String domain, int validityDays) {
        log.info("开始为域名 {} 生成自签名证书", domain);
        
        try {
            // 创建证书目录
            Path domainDir = createCertificateDirectory(domain);
            
            // 生成自签名证书
            String certPath = domainDir.resolve("cert.pem").toString();
            String keyPath = domainDir.resolve("key.pem").toString();
            
            boolean success = generateSelfSignedCert(domain, certPath, keyPath, validityDays);
            
            if (success) {
                // 加载生成的证书
                X509Certificate x509Cert = loadX509Certificate(certPath);
                
                // 创建证书记录
                SslCertificate certificate = new SslCertificate();
                certificate.setDomain(domain);
                certificate.setCertType(CertType.SELF_SIGNED.getCode());
                certificate.setStatus(CertStatus.ACTIVE.getCode());
                certificate.setCertPath(certPath);
                certificate.setKeyPath(keyPath);
                certificate.setIssuedAt(LocalDateTime.now());
                certificate.setExpiresAt(convertToLocalDateTime(x509Cert.getNotAfter()));
                certificate.setFingerprint(getCertificateFingerprint(x509Cert));
                certificate.setAutoRenew(false);
                certificate.setApplied(false);
                certificate.setCertInfo(buildCertificateInfo(x509Cert));
                
                save(certificate);
                
                log.info("成功为域名 {} 生成自签名证书", domain);
                return certificate;
            } else {
                throw new RuntimeException("自签名证书生成失败");
            }
            
        } catch (Exception e) {
            log.error("生成自签名证书失败: domain={}", domain, e);
            throw new RuntimeException("生成自签名证书失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean renewCertificate(Long certificateId) {
        log.info("开始续期证书: id={}", certificateId);
        
        try {
            SslCertificate certificate = getById(certificateId);
            if (certificate == null) {
                log.error("证书不存在: id={}", certificateId);
                return false;
            }
            
            if (!CertType.LETS_ENCRYPT.getCode().equals(certificate.getCertType())) {
                log.error("只有 Let's Encrypt 证书支持自动续期: id={}", certificateId);
                return false;
            }
            
            // 执行续期
            certificate.setStatus(CertStatus.PENDING.getCode());
            updateById(certificate);
            
            boolean success = executeAcmeClient(certificate);
            
            if (success) {
                parseCertificateInfo(certificate);
                certificate.setStatus(CertStatus.ACTIVE.getCode());
                certificate.setLastRenewal(LocalDateTime.now());
                certificate.setRenewalFailures(0);
                certificate.setErrorMessage(null);
                
                // 如果证书已应用，重新应用
                if (Boolean.TRUE.equals(certificate.getApplied())) {
                    applyCertificate(certificateId);
                }
                
                updateById(certificate);
                log.info("证书续期成功: id={}", certificateId);
                return true;
            } else {
                certificate.setStatus(CertStatus.FAILED.getCode());
                certificate.setRenewalFailures(certificate.getRenewalFailures() + 1);
                certificate.setErrorMessage("证书续期失败");
                updateById(certificate);
                
                log.error("证书续期失败: id={}", certificateId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("证书续期异常: id={}", certificateId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean applyCertificate(Long certificateId) {
        log.info("开始应用证书: id={}", certificateId);
        
        try {
            SslCertificate certificate = getById(certificateId);
            if (certificate == null) {
                log.error("证书不存在: id={}", certificateId);
                return false;
            }
            
            // 生成Nginx配置
            String nginxConfig = generateNginxConfig(certificateId);
            String configPath = nginxConfigDir + "/" + certificate.getDomain() + "-ssl.conf";
            
            // 写入配置文件
            Files.write(Paths.get(configPath), nginxConfig.getBytes());
            
            // 重载Nginx配置
            boolean reloaded = reloadNginxConfig();
            
            if (reloaded) {
                certificate.setApplied(true);
                certificate.setAppliedAt(LocalDateTime.now());
                updateById(certificate);
                
                log.info("证书应用成功: id={}", certificateId);
                return true;
            } else {
                log.error("Nginx配置重载失败: id={}", certificateId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("应用证书失败: id={}", certificateId, e);
            return false;
        }
    }

    @Override
    public boolean revokeCertificate(Long certificateId) {
        log.info("开始撤销证书应用: id={}", certificateId);
        
        try {
            SslCertificate certificate = getById(certificateId);
            if (certificate == null) {
                return false;
            }
            
            // 删除Nginx配置文件
            String configPath = nginxConfigDir + "/" + certificate.getDomain() + "-ssl.conf";
            Files.deleteIfExists(Paths.get(configPath));
            
            // 重载Nginx配置
            boolean reloaded = reloadNginxConfig();
            
            if (reloaded) {
                certificate.setApplied(false);
                certificate.setAppliedAt(null);
                updateById(certificate);
                
                log.info("证书撤销成功: id={}", certificateId);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("撤销证书失败: id={}", certificateId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteCertificate(Long certificateId) {
        log.info("开始删除证书: id={}", certificateId);
        
        try {
            SslCertificate certificate = getById(certificateId);
            if (certificate == null) {
                return false;
            }
            
            // 先撤销应用
            if (Boolean.TRUE.equals(certificate.getApplied())) {
                revokeCertificate(certificateId);
            }
            
            // 删除证书文件
            deleteCertificateFiles(certificate);
            
            // 删除数据库记录
            removeById(certificateId);
            
            log.info("证书删除成功: id={}", certificateId);
            return true;
            
        } catch (Exception e) {
            log.error("删除证书失败: id={}", certificateId, e);
            return false;
        }
    }

    @Override
    public IPage<SslCertificate> getCertificatesPage(Page<SslCertificate> page, String domain, String status) {
        QueryWrapper<SslCertificate> queryWrapper = new QueryWrapper<>();
        
        if (domain != null && !domain.trim().isEmpty()) {
            queryWrapper.like("domain", domain);
        }
        
        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByDesc("create_time");
        
        return page(page, queryWrapper);
    }

    @Override
    public SslCertificate getCertificateByDomain(String domain) {
        QueryWrapper<SslCertificate> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("domain", domain);
        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("LIMIT 1");
        
        return getOne(queryWrapper);
    }

    @Override
    public void checkCertificateStatus(Long certificateId) {
        SslCertificate certificate = getById(certificateId);
        if (certificate == null) {
            return;
        }
        
        try {
            // 检查证书文件是否存在
            if (!Files.exists(Paths.get(certificate.getCertPath()))) {
                certificate.setStatus(CertStatus.FAILED.getCode());
                certificate.setErrorMessage("证书文件不存在");
                updateById(certificate);
                return;
            }
            
            // 检查证书是否过期
            X509Certificate x509Cert = loadX509Certificate(certificate.getCertPath());
            Date now = new Date();
            
            if (x509Cert.getNotAfter().before(now)) {
                certificate.setStatus(CertStatus.EXPIRED.getCode());
                updateById(certificate);
                log.warn("证书已过期: domain={}", certificate.getDomain());
            }
            
        } catch (Exception e) {
            log.error("检查证书状态失败: id={}", certificateId, e);
            certificate.setStatus(CertStatus.FAILED.getCode());
            certificate.setErrorMessage("证书状态检查失败: " + e.getMessage());
            updateById(certificate);
        }
    }

    @Override
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void autoRenewExpiringCertificates() {
        log.info("开始执行证书自动续期任务");
        
        try {
            // 查找30天内即将过期的Let's Encrypt证书
            LocalDateTime expireThreshold = LocalDateTime.now().plusDays(30);
            
            QueryWrapper<SslCertificate> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("cert_type", CertType.LETS_ENCRYPT.getCode());
            queryWrapper.eq("auto_renew", true);
            queryWrapper.eq("status", CertStatus.ACTIVE.getCode());
            queryWrapper.le("expires_at", expireThreshold);
            queryWrapper.lt("renewal_failures", 5); // 失败次数少于5次
            
            List<SslCertificate> expiringCerts = list(queryWrapper);
            
            log.info("发现 {} 个即将过期的证书需要续期", expiringCerts.size());
            
            for (SslCertificate cert : expiringCerts) {
                try {
                    renewCertificate(cert.getId());
                    // 避免频繁操作
                    Thread.sleep(5000);
                } catch (Exception e) {
                    log.error("自动续期证书失败: domain={}", cert.getDomain(), e);
                }
            }
            
            log.info("证书自动续期任务完成");
            
        } catch (Exception e) {
            log.error("证书自动续期任务执行失败", e);
        }
    }

    // 私有辅助方法
    private boolean executeAcmeClient(SslCertificate certificate) {
        try {
            String domain = certificate.getDomain();
            String email = certificate.getEmail();
            String challengeType = certificate.getChallengeType();
            
            // 创建ACME目录
            Path acmeDir = Paths.get(acmeDirectory, domain);
            Files.createDirectories(acmeDir);
            
            // 构建certbot命令
            List<String> command = new ArrayList<>();
            command.add("certbot");
            command.add("certonly");
            command.add("--non-interactive");
            command.add("--agree-tos");
            command.add("--email");
            command.add(email);
            
            if (Boolean.TRUE.equals(letsEncryptStaging)) {
                command.add("--staging");
            }
            
            // 选择验证方式
            if ("DNS01".equals(challengeType)) {
                command.add("--manual");
                command.add("--preferred-challenges");
                command.add("dns");
            } else {
                command.add("--webroot");
                command.add("--webroot-path");
                command.add("/var/www/html");
            }
            
            command.add("--cert-path");
            command.add(acmeDir.resolve("cert.pem").toString());
            command.add("--key-path");
            command.add(acmeDir.resolve("key.pem").toString());
            command.add("--fullchain-path");
            command.add(acmeDir.resolve("fullchain.pem").toString());
            command.add("--chain-path");
            command.add(acmeDir.resolve("chain.pem").toString());
            
            command.add("-d");
            command.add(domain);
            
            // 执行命令
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(acmeDir.toFile());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("Certbot output: {}", line);
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // 移动证书文件到正确位置
                moveCertificateFiles(certificate, acmeDir);
                return true;
            } else {
                log.error("Certbot执行失败, 退出码: {}, 输出: {}", exitCode, output.toString());
                certificate.setErrorMessage("Certbot执行失败: " + output.toString());
                return false;
            }
            
        } catch (Exception e) {
            log.error("执行ACME客户端失败", e);
            certificate.setErrorMessage("ACME客户端执行异常: " + e.getMessage());
            return false;
        }
    }

    private void moveCertificateFiles(SslCertificate certificate, Path acmeDir) throws IOException {
        String domain = certificate.getDomain();
        Path certDir = createCertificateDirectory(domain);
        
        // 移动证书文件
        Path certFile = acmeDir.resolve("cert.pem");
        Path keyFile = acmeDir.resolve("key.pem");
        Path chainFile = acmeDir.resolve("chain.pem");
        Path fullchainFile = acmeDir.resolve("fullchain.pem");
        
        if (Files.exists(certFile)) {
            Files.move(certFile, certDir.resolve("cert.pem"));
            certificate.setCertPath(certDir.resolve("cert.pem").toString());
        }
        
        if (Files.exists(keyFile)) {
            Files.move(keyFile, certDir.resolve("key.pem"));
            certificate.setKeyPath(certDir.resolve("key.pem").toString());
        }
        
        if (Files.exists(chainFile)) {
            Files.move(chainFile, certDir.resolve("chain.pem"));
            certificate.setChainPath(certDir.resolve("chain.pem").toString());
        }
        
        if (Files.exists(fullchainFile)) {
            Files.move(fullchainFile, certDir.resolve("fullchain.pem"));
        }
    }

    private Path createCertificateDirectory(String domain) throws IOException {
        Path certDir = Paths.get(certsDirectory, domain);
        Files.createDirectories(certDir);
        return certDir;
    }

    private void validateCertificateFiles(MultipartFile certFile, MultipartFile keyFile) {
        if (certFile == null || certFile.isEmpty()) {
            throw new IllegalArgumentException("证书文件不能为空");
        }
        
        if (keyFile == null || keyFile.isEmpty()) {
            throw new IllegalArgumentException("私钥文件不能为空");
        }
        
        // 验证文件格式
        String certContent = new String(certFile.getBytes());
        String keyContent = new String(keyFile.getBytes());
        
        if (!certContent.contains("-----BEGIN CERTIFICATE-----")) {
            throw new IllegalArgumentException("无效的证书文件格式");
        }
        
        if (!keyContent.contains("-----BEGIN PRIVATE KEY-----") && 
            !keyContent.contains("-----BEGIN RSA PRIVATE KEY-----")) {
            throw new IllegalArgumentException("无效的私钥文件格式");
        }
    }

    private String saveUploadedFile(MultipartFile file, Path directory, String filename) throws IOException {
        Path filePath = directory.resolve(filename);
        Files.write(filePath, file.getBytes());
        return filePath.toString();
    }

    // 其他辅助方法将在下一个文件中继续...

    private X509Certificate loadX509Certificate(String certPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(certPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(fis);
        }
    }

    private void validateCertificate(X509Certificate cert, String domain) throws Exception {
        // 检查证书是否过期
        cert.checkValidity();
        
        // 检查域名匹配
        Collection<List<?>> altNames = cert.getSubjectAlternativeNames();
        boolean domainMatches = false;
        
        if (altNames != null) {
            for (List<?> altName : altNames) {
                if (altName.size() >= 2 && altName.get(1) instanceof String) {
                    String dnsName = (String) altName.get(1);
                    if (domain.equals(dnsName) || dnsName.startsWith("*.") && domain.endsWith(dnsName.substring(1))) {
                        domainMatches = true;
                        break;
                    }
                }
            }
        }
        
        // 检查CN
        if (!domainMatches) {
            String dn = cert.getSubjectDN().getName();
            if (dn.contains("CN=" + domain)) {
                domainMatches = true;
            }
        }
        
        if (!domainMatches) {
            throw new IllegalArgumentException("证书域名与指定域名不匹配");
        }
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
    }

    private String getCertificateFingerprint(X509Certificate cert) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(cert.getEncoded());
        StringBuilder fingerprint = new StringBuilder();
        for (byte b : digest) {
            fingerprint.append(String.format("%02x", b));
        }
        return fingerprint.toString();
    }

    private String buildCertificateInfo(X509Certificate cert) {
        Map<String, Object> info = new HashMap<>();
        info.put("subject", cert.getSubjectDN().toString());
        info.put("issuer", cert.getIssuerDN().toString());
        info.put("serialNumber", cert.getSerialNumber().toString());
        info.put("version", cert.getVersion());
        info.put("notBefore", cert.getNotBefore().toString());
        info.put("notAfter", cert.getNotAfter().toString());
        info.put("sigAlgName", cert.getSigAlgName());
        
        try {
            Collection<List<?>> altNames = cert.getSubjectAlternativeNames();
            if (altNames != null) {
                List<String> dnsNames = new ArrayList<>();
                for (List<?> altName : altNames) {
                    if (altName.size() >= 2 && altName.get(1) instanceof String) {
                        dnsNames.add((String) altName.get(1));
                    }
                }
                info.put("subjectAltNames", dnsNames);
            }
        } catch (Exception e) {
            log.debug("无法获取SAN信息", e);
        }
        
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(info);
    }

    private boolean generateSelfSignedCert(String domain, String certPath, String keyPath, int validityDays) {
        try {
            List<String> command = Arrays.asList(
                "openssl", "req", "-new", "-newkey", "rsa:2048", "-days", String.valueOf(validityDays),
                "-nodes", "-x509", "-keyout", keyPath, "-out", certPath,
                "-subj", "/C=CN/ST=State/L=City/O=Organization/CN=" + domain
            );
            
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            return exitCode == 0;
            
        } catch (Exception e) {
            log.error("生成自签名证书失败", e);
            return false;
        }
    }

    private void parseCertificateInfo(SslCertificate certificate) throws Exception {
        X509Certificate x509Cert = loadX509Certificate(certificate.getCertPath());
        
        certificate.setExpiresAt(convertToLocalDateTime(x509Cert.getNotAfter()));
        certificate.setFingerprint(getCertificateFingerprint(x509Cert));
        certificate.setCertInfo(buildCertificateInfo(x509Cert));
    }

    private void deleteCertificateFiles(SslCertificate certificate) {
        try {
            if (certificate.getCertPath() != null) {
                Files.deleteIfExists(Paths.get(certificate.getCertPath()));
            }
            if (certificate.getKeyPath() != null) {
                Files.deleteIfExists(Paths.get(certificate.getKeyPath()));
            }
            if (certificate.getChainPath() != null) {
                Files.deleteIfExists(Paths.get(certificate.getChainPath()));
            }
            
            // 删除证书目录（如果为空）
            Path certDir = Paths.get(certsDirectory, certificate.getDomain());
            if (Files.exists(certDir) && Files.list(certDir).count() == 0) {
                Files.delete(certDir);
            }
        } catch (Exception e) {
            log.error("删除证书文件失败", e);
        }
    }

    @Override
    public boolean validateDomainControl(String domain) {
        try {
            // 简单的域名解析验证
            java.net.InetAddress.getByName(domain);
            return true;
        } catch (Exception e) {
            log.error("域名验证失败: {}", domain, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getCertificateDetails(Long certificateId) {
        SslCertificate certificate = getById(certificateId);
        if (certificate == null) {
            return Collections.emptyMap();
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("certificate", certificate);
        
        try {
            if (certificate.getCertPath() != null && Files.exists(Paths.get(certificate.getCertPath()))) {
                X509Certificate x509Cert = loadX509Certificate(certificate.getCertPath());
                details.put("daysUntilExpiry", calculateDaysUntilExpiry(x509Cert));
                details.put("isExpired", x509Cert.getNotAfter().before(new Date()));
                details.put("isValid", isValidDate(x509Cert));
            }
        } catch (Exception e) {
            log.error("获取证书详细信息失败", e);
            details.put("error", e.getMessage());
        }
        
        return details;
    }

    @Override
    public byte[] exportCertificate(Long certificateId, String format) {
        SslCertificate certificate = getById(certificateId);
        if (certificate == null) {
            return null;
        }
        
        try {
            if ("PEM".equalsIgnoreCase(format)) {
                return Files.readAllBytes(Paths.get(certificate.getCertPath()));
            } else if ("P12".equalsIgnoreCase(format) || "PKCS12".equalsIgnoreCase(format)) {
                return exportToPKCS12(certificate);
            }
        } catch (Exception e) {
            log.error("导出证书失败", e);
        }
        
        return null;
    }

    @Override
    public List<String> getSupportedDomains() {
        // 可以从配置文件或数据库获取支持的域名列表
        return Arrays.asList("localhost", "*.example.com", "mail.example.com");
    }

    @Override
    public Map<String, Object> getCertificateStats() {
        Map<String, Object> stats = new HashMap<>();
        
        QueryWrapper<SslCertificate> queryWrapper = new QueryWrapper<>();
        stats.put("totalCertificates", count(queryWrapper));
        
        queryWrapper.clear();
        queryWrapper.eq("status", CertStatus.ACTIVE.getCode());
        stats.put("activeCertificates", count(queryWrapper));
        
        queryWrapper.clear();
        queryWrapper.eq("status", CertStatus.EXPIRED.getCode());
        stats.put("expiredCertificates", count(queryWrapper));
        
        queryWrapper.clear();
        queryWrapper.eq("cert_type", CertType.LETS_ENCRYPT.getCode());
        stats.put("letsEncryptCertificates", count(queryWrapper));
        
        queryWrapper.clear();
        queryWrapper.eq("cert_type", CertType.UPLOADED.getCode());
        stats.put("uploadedCertificates", count(queryWrapper));
        
        // 即将过期的证书（30天内）
        queryWrapper.clear();
        queryWrapper.eq("status", CertStatus.ACTIVE.getCode());
        queryWrapper.le("expires_at", LocalDateTime.now().plusDays(30));
        stats.put("expiringCertificates", count(queryWrapper));
        
        return stats;
    }

    @Override
    public boolean testCertificateConfiguration(String domain) {
        try {
            URL url = new URL("https://" + domain);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            
            Certificate[] certificates = connection.getServerCertificates();
            if (certificates.length > 0 && certificates[0] instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) certificates[0];
                cert.checkValidity();
                return true;
            }
        } catch (Exception e) {
            log.error("测试证书配置失败: {}", domain, e);
        }
        
        return false;
    }

    @Override
    public boolean backupCertificates() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupPath = Paths.get(backupDirectory, "ssl_backup_" + timestamp);
            Files.createDirectories(backupPath);
            
            // 复制证书目录
            copyDirectory(Paths.get(certsDirectory), backupPath.resolve("certs"));
            
            // 备份数据库记录
            List<SslCertificate> certificates = list();
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(certificates);
            Files.write(backupPath.resolve("certificates.json"), json.getBytes());
            
            log.info("证书备份完成: {}", backupPath);
            return true;
            
        } catch (Exception e) {
            log.error("证书备份失败", e);
            return false;
        }
    }

    @Override
    public boolean restoreCertificates(String backupPath) {
        try {
            Path backup = Paths.get(backupPath);
            if (!Files.exists(backup)) {
                log.error("备份路径不存在: {}", backupPath);
                return false;
            }
            
            // 恢复证书文件
            Path certsBackup = backup.resolve("certs");
            if (Files.exists(certsBackup)) {
                copyDirectory(certsBackup, Paths.get(certsDirectory));
            }
            
            // 恢复数据库记录
            Path jsonFile = backup.resolve("certificates.json");
            if (Files.exists(jsonFile)) {
                String json = new String(Files.readAllBytes(jsonFile));
                List<SslCertificate> certificates = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<SslCertificate>>() {});
                
                for (SslCertificate cert : certificates) {
                    cert.setId(null); // 让数据库自动生成ID
                    save(cert);
                }
            }
            
            log.info("证书恢复完成: {}", backupPath);
            return true;
            
        } catch (Exception e) {
            log.error("证书恢复失败", e);
            return false;
        }
    }

    @Override
    public String generateNginxConfig(Long certificateId) {
        SslCertificate certificate = getById(certificateId);
        if (certificate == null) {
            return "";
        }
        
        StringBuilder config = new StringBuilder();
        config.append("# SSL configuration for ").append(certificate.getDomain()).append("\n");
        config.append("server {\n");
        config.append("    listen 443 ssl http2;\n");
        config.append("    server_name ").append(certificate.getDomain()).append(";\n\n");
        
        config.append("    ssl_certificate ").append(certificate.getCertPath()).append(";\n");
        config.append("    ssl_certificate_key ").append(certificate.getKeyPath()).append(";\n");
        
        if (certificate.getChainPath() != null) {
            config.append("    ssl_trusted_certificate ").append(certificate.getChainPath()).append(";\n");
        }
        
        config.append("\n");
        config.append("    # SSL Security Settings\n");
        config.append("    ssl_protocols TLSv1.2 TLSv1.3;\n");
        config.append("    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-SHA256:ECDHE-RSA-AES256-SHA384;\n");
        config.append("    ssl_prefer_server_ciphers on;\n");
        config.append("    ssl_session_cache shared:SSL:10m;\n");
        config.append("    ssl_session_timeout 10m;\n\n");
        
        config.append("    # HSTS\n");
        config.append("    add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains\" always;\n");
        config.append("    add_header X-Frame-Options DENY;\n");
        config.append("    add_header X-Content-Type-Options nosniff;\n\n");
        
        config.append("    # Your application configuration here\n");
        config.append("    location / {\n");
        config.append("        proxy_pass http://localhost:8080;\n");
        config.append("        proxy_set_header Host $host;\n");
        config.append("        proxy_set_header X-Real-IP $remote_addr;\n");
        config.append("        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n");
        config.append("        proxy_set_header X-Forwarded-Proto $scheme;\n");
        config.append("    }\n");
        config.append("}\n\n");
        
        config.append("# HTTP to HTTPS redirect\n");
        config.append("server {\n");
        config.append("    listen 80;\n");
        config.append("    server_name ").append(certificate.getDomain()).append(";\n");
        config.append("    return 301 https://$server_name$request_uri;\n");
        config.append("}\n");
        
        return config.toString();
    }

    @Override
    public boolean reloadNginxConfig() {
        try {
            // 先测试配置
            ProcessBuilder testPb = new ProcessBuilder("nginx", "-t");
            Process testProcess = testPb.start();
            int testExitCode = testProcess.waitFor();
            
            if (testExitCode != 0) {
                log.error("Nginx配置测试失败");
                return false;
            }
            
            // 重载配置
            ProcessBuilder reloadPb = new ProcessBuilder("nginx", "-s", "reload");
            Process reloadProcess = reloadPb.start();
            int reloadExitCode = reloadProcess.waitFor();
            
            if (reloadExitCode == 0) {
                log.info("Nginx配置重载成功");
                return true;
            } else {
                log.error("Nginx配置重载失败");
                return false;
            }
            
        } catch (Exception e) {
            log.error("重载Nginx配置失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> performHealthCheck() {
        Map<String, Object> healthCheck = new HashMap<>();
        
        try {
            // 检查证书目录
            boolean certsDirExists = Files.exists(Paths.get(certsDirectory));
            healthCheck.put("certificatesDirectoryExists", certsDirExists);
            
            // 检查Nginx配置目录
            boolean nginxDirExists = Files.exists(Paths.get(nginxConfigDir));
            healthCheck.put("nginxConfigDirectoryExists", nginxDirExists);
            
            // 检查certbot是否可用
            boolean certbotAvailable = checkCommandAvailable("certbot");
            healthCheck.put("certbotAvailable", certbotAvailable);
            
            // 检查openssl是否可用
            boolean opensslAvailable = checkCommandAvailable("openssl");
            healthCheck.put("opensslAvailable", opensslAvailable);
            
            // 检查nginx是否可用
            boolean nginxAvailable = checkCommandAvailable("nginx");
            healthCheck.put("nginxAvailable", nginxAvailable);
            
            // 统计证书状态
            healthCheck.putAll(getCertificateStats());
            
            boolean overall = certsDirExists && nginxDirExists && certbotAvailable && opensslAvailable;
            healthCheck.put("overallHealth", overall ? "HEALTHY" : "UNHEALTHY");
            
        } catch (Exception e) {
            log.error("健康检查失败", e);
            healthCheck.put("overallHealth", "ERROR");
            healthCheck.put("error", e.getMessage());
        }
        
        return healthCheck;
    }

    // 私有辅助方法
    private long calculateDaysUntilExpiry(X509Certificate cert) {
        Date expiry = cert.getNotAfter();
        Date now = new Date();
        long diffInMillis = expiry.getTime() - now.getTime();
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
    }

    private boolean isValidDate(X509Certificate cert) {
        try {
            cert.checkValidity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] exportToPKCS12(SslCertificate certificate) throws Exception {
        // 这里需要实现PKCS12格式导出
        // 由于复杂性，这里返回PEM格式
        return Files.readAllBytes(Paths.get(certificate.getCertPath()));
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath);
                }
            } catch (IOException e) {
                log.error("复制文件失败: {}", sourcePath, e);
            }
        });
    }

    private boolean checkCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
}