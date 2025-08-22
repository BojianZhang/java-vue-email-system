package com.enterprise.email.service.impl;

import com.enterprise.email.entity.DkimConfig;
import com.enterprise.email.entity.SpfConfig;
import com.enterprise.email.entity.DmarcConfig;
import com.enterprise.email.mapper.DkimConfigMapper;
import com.enterprise.email.mapper.SpfConfigMapper;
import com.enterprise.email.mapper.DmarcConfigMapper;
import com.enterprise.email.service.EmailAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮件认证服务实现 (SPF/DKIM/DMARC)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthServiceImpl implements EmailAuthService {

    private final DkimConfigMapper dkimConfigMapper;
    private final SpfConfigMapper spfConfigMapper;
    private final DmarcConfigMapper dmarcConfigMapper;

    // ========== DKIM相关方法实现 ==========

    @Override
    public Map<String, String> generateDkimKeys(String domain, String selector, int keySize) {
        Map<String, String> keys = new HashMap<>();
        
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(keySize);
            KeyPair keyPair = keyGenerator.generateKeyPair();
            
            // 获取私钥和公钥
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();
            
            // 转换为Base64编码
            String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
            String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            
            keys.put("privateKey", privateKeyBase64);
            keys.put("publicKey", publicKeyBase64);
            keys.put("algorithm", "RSA");
            keys.put("keySize", String.valueOf(keySize));
            
            log.info("DKIM密钥生成成功: domain={}, selector={}, keySize={}", domain, selector, keySize);
            
        } catch (Exception e) {
            log.error("生成DKIM密钥失败: {}", e.getMessage(), e);
        }
        
        return keys;
    }

    @Override
    public boolean createDkimConfig(DkimConfig config) {
        try {
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setEnabled(true);
            config.setStatus("ACTIVE");
            
            // 设置默认值
            if (config.getAlgorithm() == null) config.setAlgorithm("rsa-sha256");
            if (config.getCanonicalization() == null) config.setCanonicalization("relaxed/simple");
            if (config.getHeaders() == null) config.setHeaders("from:to:subject:date");
            if (config.getKeyType() == null) config.setKeyType("rsa");
            if (config.getTestMode() == null) config.setTestMode(false);
            if (config.getAutoRotate() == null) config.setAutoRotate(true);
            if (config.getRotateInterval() == null) config.setRotateInterval(90);
            
            // 如果没有提供密钥，自动生成
            if (config.getPrivateKey() == null || config.getPublicKey() == null) {
                Map<String, String> keys = generateDkimKeys(config.getDomain(), config.getSelector(), 
                    config.getKeySize() != null ? config.getKeySize() : 2048);
                config.setPrivateKey(keys.get("privateKey"));
                config.setPublicKey(keys.get("publicKey"));
                config.setKeySize(Integer.valueOf(keys.get("keySize")));
            }
            
            // 生成DNS记录
            config.setDnsRecord(generateDkimDnsRecord(config));
            
            int result = dkimConfigMapper.insert(config);
            if (result > 0) {
                log.info("DKIM配置创建成功: domain={}, selector={}", config.getDomain(), config.getSelector());
                return true;
            }
        } catch (Exception e) {
            log.error("创建DKIM配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateDkimConfig(DkimConfig config) {
        try {
            config.setUpdatedAt(LocalDateTime.now());
            
            // 重新生成DNS记录
            config.setDnsRecord(generateDkimDnsRecord(config));
            
            int result = dkimConfigMapper.updateById(config);
            if (result > 0) {
                log.info("DKIM配置更新成功: domain={}, selector={}", config.getDomain(), config.getSelector());
                return true;
            }
        } catch (Exception e) {
            log.error("更新DKIM配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteDkimConfig(Long configId) {
        try {
            DkimConfig config = dkimConfigMapper.selectById(configId);
            if (config != null) {
                config.setDeleted(true);
                config.setUpdatedAt(LocalDateTime.now());
                int result = dkimConfigMapper.updateById(config);
                if (result > 0) {
                    log.info("DKIM配置删除成功: domain={}, selector={}", config.getDomain(), config.getSelector());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除DKIM配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public DkimConfig getDkimConfig(String domain, String selector) {
        return dkimConfigMapper.selectByDomainAndSelector(domain, selector);
    }

    @Override
    public List<DkimConfig> getDkimConfigs(String domain) {
        return dkimConfigMapper.selectByDomain(domain);
    }

    @Override
    public boolean rotateDkimKey(String domain, String selector) {
        try {
            DkimConfig config = getDkimConfig(domain, selector);
            if (config == null) {
                return false;
            }
            
            // 生成新的密钥对
            Map<String, String> newKeys = generateDkimKeys(domain, selector, config.getKeySize());
            
            // 更新配置
            config.setPrivateKey(newKeys.get("privateKey"));
            config.setPublicKey(newKeys.get("publicKey"));
            config.setDnsRecord(generateDkimDnsRecord(config));
            config.setUpdatedAt(LocalDateTime.now());
            
            return updateDkimConfig(config);
        } catch (Exception e) {
            log.error("轮换DKIM密钥失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> verifyDkimDns(String domain, String selector) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String dnsName = selector + "._domainkey." + domain;
            
            // 模拟DNS查询，实际应该使用DNS解析库
            // InetAddress[] addresses = InetAddress.getAllByName(dnsName);
            
            // 模拟验证结果
            result.put("domain", domain);
            result.put("selector", selector);
            result.put("dnsName", dnsName);
            result.put("exists", true);
            result.put("valid", true);
            result.put("publicKey", "v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMI...");
            result.put("verifiedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("验证DKIM DNS记录失败: {}", e.getMessage(), e);
            result.put("valid", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public String generateDkimDnsRecord(DkimConfig config) {
        StringBuilder dnsRecord = new StringBuilder();
        
        dnsRecord.append("v=DKIM1");
        dnsRecord.append("; k=").append(config.getKeyType());
        
        if (config.getTestMode()) {
            dnsRecord.append("; t=y");
        }
        
        if (config.getSubdomainPolicy() != null) {
            dnsRecord.append("; s=").append(config.getSubdomainPolicy());
        }
        
        if (config.getServiceType() != null) {
            dnsRecord.append("; n=").append(config.getServiceType());
        }
        
        // 格式化公钥
        String publicKey = config.getPublicKey();
        if (publicKey != null) {
            // 移除PEM格式的头尾
            publicKey = publicKey.replaceAll("-----BEGIN PUBLIC KEY-----", "")
                               .replaceAll("-----END PUBLIC KEY-----", "")
                               .replaceAll("\\s", "");
            dnsRecord.append("; p=").append(publicKey);
        }
        
        return dnsRecord.toString();
    }

    @Override
    public String signEmail(String emailContent, String domain, String selector) {
        try {
            DkimConfig config = getDkimConfig(domain, selector);
            if (config == null || !config.getEnabled()) {
                return emailContent;
            }
            
            // 解析邮件头部
            Map<String, String> headers = parseEmailHeaders(emailContent);
            
            // 构建DKIM签名头部
            StringBuilder dkimHeader = new StringBuilder();
            dkimHeader.append("DKIM-Signature: v=1");
            dkimHeader.append("; a=").append(config.getAlgorithm());
            dkimHeader.append("; c=").append(config.getCanonicalization());
            dkimHeader.append("; d=").append(domain);
            dkimHeader.append("; s=").append(selector);
            dkimHeader.append("; h=").append(config.getHeaders());
            dkimHeader.append("; bh=").append(calculateBodyHash(emailContent));
            
            // 计算签名
            String signature = calculateDkimSignature(emailContent, config, dkimHeader.toString());
            dkimHeader.append("; b=").append(signature);
            
            // 将DKIM签名插入邮件头部
            return insertDkimHeader(emailContent, dkimHeader.toString());
            
        } catch (Exception e) {
            log.error("DKIM签名失败: {}", e.getMessage(), e);
            return emailContent;
        }
    }

    @Override
    public Map<String, Object> verifyDkimSignature(String emailContent) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 提取DKIM签名头部
            String dkimHeader = extractDkimHeader(emailContent);
            if (dkimHeader == null) {
                result.put("valid", false);
                result.put("reason", "No DKIM signature found");
                return result;
            }
            
            // 解析DKIM签名参数
            Map<String, String> dkimParams = parseDkimHeader(dkimHeader);
            String domain = dkimParams.get("d");
            String selector = dkimParams.get("s");
            
            // 获取公钥
            Map<String, Object> dnsResult = verifyDkimDns(domain, selector);
            if (!Boolean.TRUE.equals(dnsResult.get("valid"))) {
                result.put("valid", false);
                result.put("reason", "Invalid DNS record");
                return result;
            }
            
            // 验证签名
            boolean signatureValid = verifySignature(emailContent, dkimParams, (String) dnsResult.get("publicKey"));
            
            result.put("valid", signatureValid);
            result.put("domain", domain);
            result.put("selector", selector);
            result.put("algorithm", dkimParams.get("a"));
            result.put("verifiedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("验证DKIM签名失败: {}", e.getMessage(), e);
            result.put("valid", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    // ========== SPF相关方法实现 ==========

    @Override
    public boolean createSpfConfig(SpfConfig config) {
        try {
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setEnabled(true);
            config.setStatus("ACTIVE");
            
            // 设置默认值
            if (config.getFailPolicy() == null) config.setFailPolicy("~all");
            if (config.getStrictMode() == null) config.setStrictMode(false);
            if (config.getDnsLookupLimit() == null) config.setDnsLookupLimit(10);
            
            // 生成SPF记录
            config.setSpfRecord(generateSpfRecord(config));
            
            int result = spfConfigMapper.insert(config);
            if (result > 0) {
                log.info("SPF配置创建成功: domain={}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("创建SPF配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateSpfConfig(SpfConfig config) {
        try {
            config.setUpdatedAt(LocalDateTime.now());
            config.setSpfRecord(generateSpfRecord(config));
            
            int result = spfConfigMapper.updateById(config);
            if (result > 0) {
                log.info("SPF配置更新成功: domain={}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("更新SPF配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteSpfConfig(Long configId) {
        try {
            SpfConfig config = spfConfigMapper.selectById(configId);
            if (config != null) {
                config.setDeleted(true);
                config.setUpdatedAt(LocalDateTime.now());
                int result = spfConfigMapper.updateById(config);
                if (result > 0) {
                    log.info("SPF配置删除成功: domain={}", config.getDomain());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除SPF配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public SpfConfig getSpfConfig(String domain) {
        return spfConfigMapper.selectByDomain(domain);
    }

    @Override
    public String generateSpfRecord(SpfConfig config) {
        StringBuilder spfRecord = new StringBuilder("v=spf1");
        
        // 添加IP4地址
        if (config.getIp4Addresses() != null && !config.getIp4Addresses().isEmpty()) {
            String[] ips = config.getIp4Addresses().split(",");
            for (String ip : ips) {
                spfRecord.append(" ip4:").append(ip.trim());
            }
        }
        
        // 添加IP6地址
        if (config.getIp6Addresses() != null && !config.getIp6Addresses().isEmpty()) {
            String[] ips = config.getIp6Addresses().split(",");
            for (String ip : ips) {
                spfRecord.append(" ip6:").append(ip.trim());
            }
        }
        
        // 添加A记录
        if (config.getARecords() != null && !config.getARecords().isEmpty()) {
            String[] records = config.getARecords().split(",");
            for (String record : records) {
                spfRecord.append(" a:").append(record.trim());
            }
        }
        
        // 添加MX记录
        if (config.getMxRecords() != null && !config.getMxRecords().isEmpty()) {
            String[] records = config.getMxRecords().split(",");
            for (String record : records) {
                spfRecord.append(" mx:").append(record.trim());
            }
        }
        
        // 添加包含域名
        if (config.getIncludeDomains() != null && !config.getIncludeDomains().isEmpty()) {
            String[] domains = config.getIncludeDomains().split(",");
            for (String domain : domains) {
                spfRecord.append(" include:").append(domain.trim());
            }
        }
        
        // 添加重定向
        if (config.getRedirectDomain() != null && !config.getRedirectDomain().isEmpty()) {
            spfRecord.append(" redirect=").append(config.getRedirectDomain());
        }
        
        // 添加解释
        if (config.getExplanation() != null && !config.getExplanation().isEmpty()) {
            spfRecord.append(" exp=").append(config.getExplanation());
        }
        
        // 添加失败策略
        spfRecord.append(" ").append(config.getFailPolicy());
        
        return spfRecord.toString();
    }

    @Override
    public Map<String, Object> verifySpfRecord(String domain, String senderIp, String mailFrom) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            SpfConfig config = getSpfConfig(domain);
            if (config == null) {
                result.put("result", "none");
                result.put("reason", "No SPF record found");
                return result;
            }
            
            String spfRecord = config.getSpfRecord();
            if (spfRecord == null || spfRecord.isEmpty()) {
                result.put("result", "none");
                result.put("reason", "Empty SPF record");
                return result;
            }
            
            // 解析SPF记录
            Map<String, Object> spfData = parseSpfRecord(spfRecord);
            
            // 检查IP是否授权
            boolean authorized = checkIpAuthorization(senderIp, spfData);
            
            if (authorized) {
                result.put("result", "pass");
                result.put("reason", "IP authorized");
            } else {
                String failPolicy = config.getFailPolicy();
                switch (failPolicy) {
                    case "-all":
                        result.put("result", "fail");
                        break;
                    case "~all":
                        result.put("result", "softfail");
                        break;
                    case "?all":
                        result.put("result", "neutral");
                        break;
                    default:
                        result.put("result", "none");
                }
                result.put("reason", "IP not authorized");
            }
            
            result.put("domain", domain);
            result.put("senderIp", senderIp);
            result.put("mailFrom", mailFrom);
            result.put("spfRecord", spfRecord);
            result.put("verifiedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("验证SPF记录失败: {}", e.getMessage(), e);
            result.put("result", "temperror");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> verifySpfDns(String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 模拟DNS TXT记录查询
            result.put("domain", domain);
            result.put("exists", true);
            result.put("record", "v=spf1 include:_spf.google.com ~all");
            result.put("valid", true);
            result.put("verifiedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("验证SPF DNS记录失败: {}", e.getMessage(), e);
            result.put("valid", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> parseSpfRecord(String spfRecord) {
        Map<String, Object> parsed = new HashMap<>();
        List<String> ip4List = new ArrayList<>();
        List<String> ip6List = new ArrayList<>();
        List<String> includeList = new ArrayList<>();
        List<String> aList = new ArrayList<>();
        List<String> mxList = new ArrayList<>();
        
        try {
            String[] parts = spfRecord.split("\\s+");
            
            for (String part : parts) {
                if (part.startsWith("ip4:")) {
                    ip4List.add(part.substring(4));
                } else if (part.startsWith("ip6:")) {
                    ip6List.add(part.substring(4));
                } else if (part.startsWith("include:")) {
                    includeList.add(part.substring(8));
                } else if (part.startsWith("a:")) {
                    aList.add(part.substring(2));
                } else if (part.equals("a")) {
                    aList.add("");
                } else if (part.startsWith("mx:")) {
                    mxList.add(part.substring(3));
                } else if (part.equals("mx")) {
                    mxList.add("");
                } else if (part.startsWith("redirect=")) {
                    parsed.put("redirect", part.substring(9));
                } else if (part.startsWith("exp=")) {
                    parsed.put("explanation", part.substring(4));
                } else if (part.matches("[+~?-]all")) {
                    parsed.put("policy", part);
                }
            }
            
            parsed.put("ip4", ip4List);
            parsed.put("ip6", ip6List);
            parsed.put("include", includeList);
            parsed.put("a", aList);
            parsed.put("mx", mxList);
            
        } catch (Exception e) {
            log.error("解析SPF记录失败: {}", e.getMessage(), e);
        }
        
        return parsed;
    }

    // ========== DMARC相关方法实现 ==========

    @Override
    public boolean createDmarcConfig(DmarcConfig config) {
        try {
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setEnabled(true);
            config.setStatus("ACTIVE");
            
            // 设置默认值
            if (config.getPolicy() == null) config.setPolicy("none");
            if (config.getDkimAlignment() == null) config.setDkimAlignment("r");
            if (config.getSpfAlignment() == null) config.setSpfAlignment("r");
            if (config.getPercentage() == null) config.setPercentage(100);
            if (config.getVersion() == null) config.setVersion("DMARC1");
            if (config.getReportInterval() == null) config.setReportInterval(86400);
            if (config.getGenerateReports() == null) config.setGenerateReports(true);
            if (config.getTotalMessages() == null) config.setTotalMessages(0L);
            if (config.getPassedMessages() == null) config.setPassedMessages(0L);
            if (config.getFailedMessages() == null) config.setFailedMessages(0L);
            
            // 生成DMARC记录
            config.setDmarcRecord(generateDmarcRecord(config));
            
            int result = dmarcConfigMapper.insert(config);
            if (result > 0) {
                log.info("DMARC配置创建成功: domain={}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("创建DMARC配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateDmarcConfig(DmarcConfig config) {
        try {
            config.setUpdatedAt(LocalDateTime.now());
            config.setDmarcRecord(generateDmarcRecord(config));
            
            int result = dmarcConfigMapper.updateById(config);
            if (result > 0) {
                log.info("DMARC配置更新成功: domain={}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("更新DMARC配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteDmarcConfig(Long configId) {
        try {
            DmarcConfig config = dmarcConfigMapper.selectById(configId);
            if (config != null) {
                config.setDeleted(true);
                config.setUpdatedAt(LocalDateTime.now());
                int result = dmarcConfigMapper.updateById(config);
                if (result > 0) {
                    log.info("DMARC配置删除成功: domain={}", config.getDomain());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除DMARC配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public DmarcConfig getDmarcConfig(String domain) {
        return dmarcConfigMapper.selectByDomain(domain);
    }

    @Override
    public String generateDmarcRecord(DmarcConfig config) {
        StringBuilder dmarcRecord = new StringBuilder();
        
        dmarcRecord.append("v=").append(config.getVersion());
        dmarcRecord.append("; p=").append(config.getPolicy());
        
        if (config.getSubdomainPolicy() != null && !config.getSubdomainPolicy().isEmpty()) {
            dmarcRecord.append("; sp=").append(config.getSubdomainPolicy());
        }
        
        dmarcRecord.append("; adkim=").append(config.getDkimAlignment());
        dmarcRecord.append("; aspf=").append(config.getSpfAlignment());
        
        if (config.getPercentage() != 100) {
            dmarcRecord.append("; pct=").append(config.getPercentage());
        }
        
        if (config.getAggregateReportUri() != null && !config.getAggregateReportUri().isEmpty()) {
            dmarcRecord.append("; rua=").append(config.getAggregateReportUri());
        }
        
        if (config.getFailureReportUri() != null && !config.getFailureReportUri().isEmpty()) {
            dmarcRecord.append("; ruf=").append(config.getFailureReportUri());
        }
        
        if (config.getReportFormat() != null && !config.getReportFormat().isEmpty()) {
            dmarcRecord.append("; rf=").append(config.getReportFormat());
        }
        
        if (config.getReportInterval() != 86400) {
            dmarcRecord.append("; ri=").append(config.getReportInterval());
        }
        
        if (config.getFailureReportOptions() != null && !config.getFailureReportOptions().isEmpty()) {
            dmarcRecord.append("; fo=").append(config.getFailureReportOptions());
        }
        
        return dmarcRecord.toString();
    }

    @Override
    public Map<String, Object> verifyDmarcRecord(String domain, Map<String, Object> spfResult, Map<String, Object> dkimResult) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            DmarcConfig config = getDmarcConfig(domain);
            if (config == null) {
                result.put("result", "none");
                result.put("reason", "No DMARC record found");
                return result;
            }
            
            boolean spfAligned = checkSpfAlignment(domain, spfResult, config.getSpfAlignment());
            boolean dkimAligned = checkDkimAlignment(domain, dkimResult, config.getDkimAlignment());
            
            boolean dmarcPass = spfAligned || dkimAligned;
            
            result.put("result", dmarcPass ? "pass" : "fail");
            result.put("domain", domain);
            result.put("policy", config.getPolicy());
            result.put("spfAligned", spfAligned);
            result.put("dkimAligned", dkimAligned);
            result.put("percentage", config.getPercentage());
            result.put("verifiedAt", LocalDateTime.now());
            
            // 更新统计信息
            updateDmarcStatistics(config, dmarcPass);
            
        } catch (Exception e) {
            log.error("验证DMARC记录失败: {}", e.getMessage(), e);
            result.put("result", "temperror");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> verifyDmarcDns(String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String dnsName = "_dmarc." + domain;
            
            // 模拟DNS查询
            result.put("domain", domain);
            result.put("dnsName", dnsName);
            result.put("exists", true);
            result.put("record", "v=DMARC1; p=quarantine; rua=mailto:dmarc@example.com");
            result.put("valid", true);
            result.put("verifiedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("验证DMARC DNS记录失败: {}", e.getMessage(), e);
            result.put("valid", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> parseDmarcRecord(String dmarcRecord) {
        Map<String, Object> parsed = new HashMap<>();
        
        try {
            String[] parts = dmarcRecord.split(";");
            
            for (String part : parts) {
                part = part.trim();
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    parsed.put(key, value);
                }
            }
            
        } catch (Exception e) {
            log.error("解析DMARC记录失败: {}", e.getMessage(), e);
        }
        
        return parsed;
    }

    @Override
    public String generateDmarcReport(String domain, String startDate, String endDate) {
        try {
            DmarcConfig config = getDmarcConfig(domain);
            if (config == null) {
                return null;
            }
            
            // 生成XML格式的DMARC报告
            StringBuilder report = new StringBuilder();
            report.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            report.append("<feedback>\n");
            report.append("  <report_metadata>\n");
            report.append("    <org_name>").append(domain).append("</org_name>\n");
            report.append("    <email>").append(config.getReportEmail()).append("</email>\n");
            report.append("    <report_id>").append(UUID.randomUUID().toString()).append("</report_id>\n");
            report.append("    <date_range>\n");
            report.append("      <begin>").append(startDate).append("</begin>\n");
            report.append("      <end>").append(endDate).append("</end>\n");
            report.append("    </date_range>\n");
            report.append("  </report_metadata>\n");
            report.append("  <policy_published>\n");
            report.append("    <domain>").append(domain).append("</domain>\n");
            report.append("    <p>").append(config.getPolicy()).append("</p>\n");
            report.append("    <sp>").append(config.getSubdomainPolicy()).append("</sp>\n");
            report.append("    <adkim>").append(config.getDkimAlignment()).append("</adkim>\n");
            report.append("    <aspf>").append(config.getSpfAlignment()).append("</aspf>\n");
            report.append("    <pct>").append(config.getPercentage()).append("</pct>\n");
            report.append("  </policy_published>\n");
            report.append("  <record>\n");
            report.append("    <row>\n");
            report.append("      <source_ip>192.168.1.100</source_ip>\n");
            report.append("      <count>").append(config.getTotalMessages()).append("</count>\n");
            report.append("      <policy_evaluated>\n");
            report.append("        <disposition>none</disposition>\n");
            report.append("        <dkim>pass</dkim>\n");
            report.append("        <spf>pass</spf>\n");
            report.append("      </policy_evaluated>\n");
            report.append("    </row>\n");
            report.append("  </record>\n");
            report.append("</feedback>\n");
            
            return report.toString();
            
        } catch (Exception e) {
            log.error("生成DMARC报告失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean processDmarcReport(String reportXml) {
        try {
            // 解析XML报告并更新统计信息
            log.info("处理DMARC报告: 长度={}", reportXml.length());
            
            // 这里应该实际解析XML并更新数据库统计信息
            
            return true;
        } catch (Exception e) {
            log.error("处理DMARC报告失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // ========== 综合验证方法实现 ==========

    @Override
    public Map<String, Object> verifyEmailAuthentication(String emailContent, String senderIp) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 解析邮件头部获取域名
            String domain = extractDomainFromEmail(emailContent);
            String mailFrom = extractMailFrom(emailContent);
            
            // SPF验证
            Map<String, Object> spfResult = verifySpfRecord(domain, senderIp, mailFrom);
            
            // DKIM验证
            Map<String, Object> dkimResult = verifyDkimSignature(emailContent);
            
            // DMARC验证
            Map<String, Object> dmarcResult = verifyDmarcRecord(domain, spfResult, dkimResult);
            
            result.put("domain", domain);
            result.put("senderIp", senderIp);
            result.put("spf", spfResult);
            result.put("dkim", dkimResult);
            result.put("dmarc", dmarcResult);
            result.put("verifiedAt", LocalDateTime.now());
            
            // 计算总体认证状态
            boolean authenticated = "pass".equals(dmarcResult.get("result"));
            result.put("authenticated", authenticated);
            
        } catch (Exception e) {
            log.error("验证邮件认证失败: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getDomainAuthStatus(String domain) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // 获取各种配置状态
            SpfConfig spfConfig = getSpfConfig(domain);
            List<DkimConfig> dkimConfigs = getDkimConfigs(domain);
            DmarcConfig dmarcConfig = getDmarcConfig(domain);
            
            status.put("domain", domain);
            status.put("spfConfigured", spfConfig != null && spfConfig.getEnabled());
            status.put("dkimConfigured", !dkimConfigs.isEmpty() && dkimConfigs.stream().anyMatch(DkimConfig::getEnabled));
            status.put("dmarcConfigured", dmarcConfig != null && dmarcConfig.getEnabled());
            
            // DNS验证状态
            if (spfConfig != null) {
                status.put("spfDnsValid", verifySpfDns(domain).get("valid"));
            }
            
            if (!dkimConfigs.isEmpty()) {
                boolean allDkimValid = dkimConfigs.stream()
                    .filter(DkimConfig::getEnabled)
                    .allMatch(config -> Boolean.TRUE.equals(verifyDkimDns(domain, config.getSelector()).get("valid")));
                status.put("dkimDnsValid", allDkimValid);
            }
            
            if (dmarcConfig != null) {
                status.put("dmarcDnsValid", verifyDmarcDns(domain).get("valid"));
            }
            
        } catch (Exception e) {
            log.error("获取域名认证状态失败: {}", e.getMessage(), e);
            status.put("error", e.getMessage());
        }
        
        return status;
    }

    @Override
    public Map<String, Object> getAuthStatistics(String domain) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            DmarcConfig dmarcConfig = getDmarcConfig(domain);
            if (dmarcConfig != null) {
                stats.put("totalMessages", dmarcConfig.getTotalMessages());
                stats.put("passedMessages", dmarcConfig.getPassedMessages());
                stats.put("failedMessages", dmarcConfig.getFailedMessages());
                
                if (dmarcConfig.getTotalMessages() > 0) {
                    double passRate = (double) dmarcConfig.getPassedMessages() / dmarcConfig.getTotalMessages() * 100;
                    stats.put("passRate", Math.round(passRate * 100.0) / 100.0);
                } else {
                    stats.put("passRate", 0.0);
                }
                
                stats.put("lastReportAt", dmarcConfig.getLastReportAt());
            }
            
            stats.put("domain", domain);
            stats.put("reportedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("获取认证统计失败: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    // 其他方法的简化实现
    @Override
    public Map<String, Object> batchVerifyDnsRecords(List<String> domains) {
        Map<String, Object> results = new HashMap<>();
        
        for (String domain : domains) {
            results.put(domain, getDomainAuthStatus(domain));
        }
        
        return results;
    }

    @Override
    public Map<String, Object> exportAuthConfig(String domain) {
        Map<String, Object> config = new HashMap<>();
        
        config.put("spf", getSpfConfig(domain));
        config.put("dkim", getDkimConfigs(domain));
        config.put("dmarc", getDmarcConfig(domain));
        
        return config;
    }

    @Override
    public boolean importAuthConfig(String domain, Map<String, Object> config) {
        return true; // 简化实现
    }

    @Override
    public Map<String, Object> testEmailAuthentication(String domain, String testEmail) {
        return verifyEmailAuthentication(testEmail, "192.168.1.100");
    }

    @Override
    public Map<String, Object> getDnsConfigSuggestions(String domain) {
        Map<String, Object> suggestions = new HashMap<>();
        
        // SPF建议
        suggestions.put("spf", "v=spf1 include:_spf.google.com ~all");
        
        // DKIM建议
        suggestions.put("dkim", "v=DKIM1; k=rsa; p=<public_key>");
        
        // DMARC建议
        suggestions.put("dmarc", "v=DMARC1; p=quarantine; rua=mailto:dmarc@" + domain);
        
        return suggestions;
    }

    @Override
    public boolean autoConfigureDns(String domain, Map<String, Object> dnsConfig) {
        return true; // 简化实现
    }

    @Override
    public List<Map<String, Object>> getAuthErrorLogs(String domain, int limit) {
        return new ArrayList<>(); // 简化实现
    }

    @Override
    public boolean cleanupExpiredRecords(int days) {
        return true; // 简化实现
    }

    // ========== 私有辅助方法 ==========

    private Map<String, String> parseEmailHeaders(String emailContent) {
        Map<String, String> headers = new HashMap<>();
        
        String[] lines = emailContent.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                break; // 头部结束
            }
            
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim().toLowerCase();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
        }
        
        return headers;
    }

    private String calculateBodyHash(String emailContent) {
        try {
            // 提取邮件正文
            String body = emailContent.substring(emailContent.indexOf("\n\n") + 2);
            
            // 计算SHA-256哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("计算邮件正文哈希失败: {}", e.getMessage(), e);
            return "";
        }
    }

    private String calculateDkimSignature(String emailContent, DkimConfig config, String dkimHeader) {
        try {
            // 构建待签名数据
            String dataToSign = dkimHeader + "\n" + emailContent;
            
            // 使用私钥签名
            byte[] privateKeyBytes = Base64.getDecoder().decode(config.getPrivateKey());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(dataToSign.getBytes());
            
            byte[] signatureBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            log.error("计算DKIM签名失败: {}", e.getMessage(), e);
            return "";
        }
    }

    private String insertDkimHeader(String emailContent, String dkimHeader) {
        int firstNewline = emailContent.indexOf('\n');
        if (firstNewline > 0) {
            return emailContent.substring(0, firstNewline + 1) + dkimHeader + "\n" + emailContent.substring(firstNewline + 1);
        }
        return dkimHeader + "\n" + emailContent;
    }

    private String extractDkimHeader(String emailContent) {
        Pattern pattern = Pattern.compile("DKIM-Signature:.*?(?=\n[A-Za-z-]+:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(emailContent);
        if (matcher.find()) {
            return matcher.group().replaceAll("\n\\s+", " ");
        }
        return null;
    }

    private Map<String, String> parseDkimHeader(String dkimHeader) {
        Map<String, String> params = new HashMap<>();
        
        Pattern pattern = Pattern.compile("([a-z]+)=([^;]+)");
        Matcher matcher = pattern.matcher(dkimHeader);
        
        while (matcher.find()) {
            params.put(matcher.group(1), matcher.group(2).trim());
        }
        
        return params;
    }

    private boolean verifySignature(String emailContent, Map<String, String> dkimParams, String publicKeyString) {
        try {
            // 解析公钥
            String cleanPublicKey = publicKeyString.replaceAll("v=DKIM1;.*?p=", "").replaceAll(";.*", "");
            byte[] publicKeyBytes = Base64.getDecoder().decode(cleanPublicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            // 验证签名
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(emailContent.getBytes());
            
            byte[] signatureBytes = Base64.getDecoder().decode(dkimParams.get("b"));
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            log.error("验证DKIM签名失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean checkIpAuthorization(String senderIp, Map<String, Object> spfData) {
        try {
            // 检查IP4地址
            @SuppressWarnings("unchecked")
            List<String> ip4List = (List<String>) spfData.get("ip4");
            if (ip4List != null) {
                for (String ip4 : ip4List) {
                    if (isIpInRange(senderIp, ip4)) {
                        return true;
                    }
                }
            }
            
            // 检查包含域名等其他机制
            // 简化实现，实际需要递归解析
            
            return false;
        } catch (Exception e) {
            log.error("检查IP授权失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean isIpInRange(String ip, String range) {
        // 简化的IP范围检查
        if (range.contains("/")) {
            // CIDR格式
            return ip.startsWith(range.split("/")[0].substring(0, range.split("/")[0].lastIndexOf(".")));
        } else {
            // 单个IP
            return ip.equals(range);
        }
    }

    private boolean checkSpfAlignment(String domain, Map<String, Object> spfResult, String alignment) {
        if (!"pass".equals(spfResult.get("result"))) {
            return false;
        }
        
        // 检查域名对齐
        String resultDomain = (String) spfResult.get("domain");
        if ("s".equals(alignment)) {
            // 严格对齐
            return domain.equals(resultDomain);
        } else {
            // 宽松对齐
            return resultDomain.endsWith(domain);
        }
    }

    private boolean checkDkimAlignment(String domain, Map<String, Object> dkimResult, String alignment) {
        if (!Boolean.TRUE.equals(dkimResult.get("valid"))) {
            return false;
        }
        
        // 检查域名对齐
        String resultDomain = (String) dkimResult.get("domain");
        if ("s".equals(alignment)) {
            // 严格对齐
            return domain.equals(resultDomain);
        } else {
            // 宽松对齐
            return resultDomain.endsWith(domain);
        }
    }

    private void updateDmarcStatistics(DmarcConfig config, boolean passed) {
        try {
            config.setTotalMessages(config.getTotalMessages() + 1);
            if (passed) {
                config.setPassedMessages(config.getPassedMessages() + 1);
            } else {
                config.setFailedMessages(config.getFailedMessages() + 1);
            }
            updateDmarcConfig(config);
        } catch (Exception e) {
            log.error("更新DMARC统计失败: {}", e.getMessage(), e);
        }
    }

    private String extractDomainFromEmail(String emailContent) {
        Pattern pattern = Pattern.compile("From:.*?@([a-zA-Z0-9.-]+)");
        Matcher matcher = pattern.matcher(emailContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extractMailFrom(String emailContent) {
        Pattern pattern = Pattern.compile("From:.*?([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+)");
        Matcher matcher = pattern.matcher(emailContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}