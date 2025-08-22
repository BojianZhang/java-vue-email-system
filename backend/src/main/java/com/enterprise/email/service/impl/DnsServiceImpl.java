package com.enterprise.email.service.impl;

import com.enterprise.email.entity.DnsRecord;
import com.enterprise.email.mapper.DnsRecordMapper;
import com.enterprise.email.service.DnsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 * DNS管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DnsServiceImpl implements DnsService {

    private final DnsRecordMapper dnsRecordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override
    public boolean createDnsRecord(DnsRecord record) {
        try {
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            record.setStatus("ACTIVE");
            record.setDnsVerified(false);

            // 设置默认值
            if (record.getTtl() == null) {
                record.setTtl(3600);
            }
            if (record.getSource() == null) {
                record.setSource("MANUAL");
            }
            if (record.getAutoManaged() == null) {
                record.setAutoManaged(false);
            }

            int result = dnsRecordMapper.insert(record);
            if (result > 0) {
                log.info("DNS记录创建成功: name={}, type={}, value={}", 
                    record.getName(), record.getType(), record.getValue());
                
                // 异步验证DNS记录
                CompletableFuture.runAsync(() -> verifyDnsRecord(record.getId()), executor);
                
                return true;
            }
        } catch (Exception e) {
            log.error("创建DNS记录失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateDnsRecord(DnsRecord record) {
        try {
            record.setUpdatedAt(LocalDateTime.now());
            int result = dnsRecordMapper.updateById(record);
            if (result > 0) {
                log.info("DNS记录更新成功: id={}, name={}, type={}", 
                    record.getId(), record.getName(), record.getType());
                
                // 异步重新验证DNS记录
                CompletableFuture.runAsync(() -> verifyDnsRecord(record.getId()), executor);
                
                return true;
            }
        } catch (Exception e) {
            log.error("更新DNS记录失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteDnsRecord(Long recordId) {
        try {
            DnsRecord record = dnsRecordMapper.selectById(recordId);
            if (record != null) {
                record.setDeleted(true);
                record.setUpdatedAt(LocalDateTime.now());
                int result = dnsRecordMapper.updateById(record);
                if (result > 0) {
                    log.info("DNS记录删除成功: id={}, name={}, type={}", 
                        recordId, record.getName(), record.getType());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除DNS记录失败: id={}, error={}", recordId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public DnsRecord getDnsRecord(Long recordId) {
        return dnsRecordMapper.selectById(recordId);
    }

    @Override
    public List<DnsRecord> getDomainDnsRecords(String domain) {
        return dnsRecordMapper.selectByDomain(domain);
    }

    @Override
    public List<DnsRecord> getDnsRecordsByType(String domain, String type) {
        return dnsRecordMapper.selectByDomainAndType(domain, type);
    }

    @Override
    public Map<String, Object> verifyDnsRecord(Long recordId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            DnsRecord record = dnsRecordMapper.selectById(recordId);
            if (record == null) {
                result.put("verified", false);
                result.put("error", "DNS记录不存在");
                return result;
            }

            Map<String, Object> queryResult = queryDnsRecord(record.getName(), record.getType());
            boolean verified = false;
            String error = null;

            if (Boolean.TRUE.equals(queryResult.get("found"))) {
                String actualValue = (String) queryResult.get("value");
                verified = record.getValue().equals(actualValue);
                if (!verified) {
                    error = String.format("DNS值不匹配，期望: %s, 实际: %s", record.getValue(), actualValue);
                }
            } else {
                error = "DNS记录未找到";
            }

            // 更新验证状态
            record.setDnsVerified(verified);
            record.setLastVerifiedAt(LocalDateTime.now());
            record.setDnsError(error);
            if (queryResult.containsKey("resolvedIps")) {
                record.setResolvedIps((String) queryResult.get("resolvedIps"));
            }
            dnsRecordMapper.updateById(record);

            result.put("verified", verified);
            result.put("actualValue", queryResult.get("value"));
            result.put("expectedValue", record.getValue());
            result.put("error", error);
            result.put("resolvedIps", queryResult.get("resolvedIps"));

        } catch (Exception e) {
            log.error("验证DNS记录失败: recordId={}, error={}", recordId, e.getMessage(), e);
            result.put("verified", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> batchVerifyDnsRecords(List<Long> recordIds) {
        Map<String, Object> result = new HashMap<>();
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();

        for (Long recordId : recordIds) {
            CompletableFuture<Map<String, Object>> future = CompletableFuture
                .supplyAsync(() -> verifyDnsRecord(recordId), executor);
            futures.add(future);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int verified = 0;
        int failed = 0;

        for (CompletableFuture<Map<String, Object>> future : futures) {
            try {
                Map<String, Object> verifyResult = future.get();
                results.add(verifyResult);
                if (Boolean.TRUE.equals(verifyResult.get("verified"))) {
                    verified++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.error("批量验证DNS记录失败: {}", e.getMessage(), e);
                failed++;
            }
        }

        result.put("total", recordIds.size());
        result.put("verified", verified);
        result.put("failed", failed);
        result.put("results", results);

        return result;
    }

    @Override
    public Map<String, Object> queryDnsRecord(String name, String type) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);

            Attributes attrs = ctx.getAttributes(name, new String[]{type});
            Attribute attr = attrs.get(type);

            if (attr != null) {
                result.put("found", true);
                result.put("value", attr.get().toString());
                
                // 如果是A记录，解析IP地址
                if ("A".equals(type) || "AAAA".equals(type)) {
                    List<String> ips = new ArrayList<>();
                    for (int i = 0; i < attr.size(); i++) {
                        ips.add(attr.get(i).toString());
                    }
                    result.put("resolvedIps", String.join(",", ips));
                }
            } else {
                result.put("found", false);
            }

            ctx.close();

        } catch (NamingException e) {
            log.error("查询DNS记录失败: name={}, type={}, error={}", name, type, e.getMessage());
            result.put("found", false);
            result.put("error", e.getMessage());
        } catch (Exception e) {
            log.error("查询DNS记录异常: name={}, type={}, error={}", name, type, e.getMessage(), e);
            result.put("found", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getDnsStatistics() {
        return dnsRecordMapper.selectDnsStatistics();
    }

    @Override
    public boolean autoConfigureEmailDns(String domain, Map<String, Object> config) {
        try {
            List<DnsRecord> records = generateStandardEmailDnsRecords(domain, config);
            
            for (DnsRecord record : records) {
                // 检查是否已存在相同记录
                DnsRecord existing = dnsRecordMapper.selectByNameAndType(record.getName(), record.getType());
                if (existing == null) {
                    createDnsRecord(record);
                } else {
                    log.info("DNS记录已存在，跳过创建: name={}, type={}", record.getName(), record.getType());
                }
            }
            
            log.info("邮件DNS自动配置完成: domain={}, records={}", domain, records.size());
            return true;
            
        } catch (Exception e) {
            log.error("自动配置邮件DNS失败: domain={}, error={}", domain, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<DnsRecord> generateStandardEmailDnsRecords(String domain, Map<String, Object> config) {
        List<DnsRecord> records = new ArrayList<>();
        
        String mailServer = (String) config.getOrDefault("mailServer", "mail." + domain);
        String ipAddress = (String) config.get("ipAddress");
        
        if (ipAddress != null) {
            // MX记录
            DnsRecord mxRecord = new DnsRecord();
            mxRecord.setDomain(domain);
            mxRecord.setName(domain);
            mxRecord.setType("MX");
            mxRecord.setValue(mailServer);
            mxRecord.setPriority(10);
            mxRecord.setTtl(3600);
            mxRecord.setSource("AUTO_GENERATED");
            mxRecord.setServiceType("MX");
            mxRecord.setAutoManaged(true);
            records.add(mxRecord);

            // A记录（邮件服务器）
            DnsRecord aRecord = new DnsRecord();
            aRecord.setDomain(domain);
            aRecord.setName(mailServer);
            aRecord.setType("A");
            aRecord.setValue(ipAddress);
            aRecord.setTtl(3600);
            aRecord.setSource("AUTO_GENERATED");
            aRecord.setServiceType("MX");
            aRecord.setAutoManaged(true);
            records.add(aRecord);

            // SPF记录
            DnsRecord spfRecord = new DnsRecord();
            spfRecord.setDomain(domain);
            spfRecord.setName(domain);
            spfRecord.setType("TXT");
            spfRecord.setValue("v=spf1 a mx ip4:" + ipAddress + " ~all");
            spfRecord.setTtl(3600);
            spfRecord.setSource("AUTO_GENERATED");
            spfRecord.setServiceType("SPF");
            spfRecord.setAutoManaged(true);
            records.add(spfRecord);

            // DMARC记录
            DnsRecord dmarcRecord = new DnsRecord();
            dmarcRecord.setDomain(domain);
            dmarcRecord.setName("_dmarc." + domain);
            dmarcRecord.setType("TXT");
            dmarcRecord.setValue("v=DMARC1; p=quarantine; rua=mailto:dmarc@" + domain);
            dmarcRecord.setTtl(3600);
            dmarcRecord.setSource("AUTO_GENERATED");
            dmarcRecord.setServiceType("DMARC");
            dmarcRecord.setAutoManaged(true);
            records.add(dmarcRecord);
        }
        
        return records;
    }

    @Override
    public Map<String, Object> verifyEmailDnsConfiguration(String domain) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> issues = new ArrayList<>();
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        try {
            // 检查MX记录
            Map<String, Object> mxCheck = queryDnsRecord(domain, "MX");
            if (!Boolean.TRUE.equals(mxCheck.get("found"))) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "MX");
                issue.put("severity", "critical");
                issue.put("message", "缺少MX记录");
                issues.add(issue);
            }

            // 检查SPF记录
            Map<String, Object> spfCheck = queryDnsRecord(domain, "TXT");
            if (Boolean.TRUE.equals(spfCheck.get("found"))) {
                String txtValue = (String) spfCheck.get("value");
                if (!txtValue.contains("v=spf1")) {
                    Map<String, Object> recommendation = new HashMap<>();
                    recommendation.put("type", "SPF");
                    recommendation.put("message", "建议添加SPF记录");
                    recommendations.add(recommendation);
                }
            }

            // 检查DMARC记录
            Map<String, Object> dmarcCheck = queryDnsRecord("_dmarc." + domain, "TXT");
            if (!Boolean.TRUE.equals(dmarcCheck.get("found"))) {
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("type", "DMARC");
                recommendation.put("message", "建议添加DMARC记录");
                recommendations.add(recommendation);
            }

            result.put("domain", domain);
            result.put("issues", issues);
            result.put("recommendations", recommendations);
            result.put("score", calculateDnsScore(issues, recommendations));

        } catch (Exception e) {
            log.error("验证邮件DNS配置失败: domain={}, error={}", domain, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getDnsConfigRecommendations(String domain) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        Map<String, Object> ttl = new HashMap<>();
        ttl.put("title", "TTL设置");
        ttl.put("description", "建议邮件相关记录TTL设置为3600秒");
        ttl.put("priority", "medium");
        recommendations.add(ttl);
        
        Map<String, Object> redundancy = new HashMap<>();
        redundancy.put("title", "冗余配置");
        redundancy.put("description", "建议设置多个MX记录以提高可用性");
        redundancy.put("priority", "high");
        recommendations.add(redundancy);
        
        return recommendations;
    }

    @Override
    public Map<String, Object> detectDnsConfigurationIssues(String domain) {
        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        
        try {
            List<DnsRecord> records = getDomainDnsRecords(domain);
            
            // 检查重复记录
            Set<String> seen = new HashSet<>();
            for (DnsRecord record : records) {
                String key = record.getName() + ":" + record.getType();
                if (seen.contains(key)) {
                    issues.add("发现重复的DNS记录: " + key);
                }
                seen.add(key);
            }
            
            // 检查验证状态
            long unverified = records.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getDnsVerified()))
                .count();
            
            if (unverified > 0) {
                issues.add(String.format("有 %d 条DNS记录未验证", unverified));
            }
            
            result.put("domain", domain);
            result.put("totalRecords", records.size());
            result.put("issues", issues);
            result.put("hasIssues", !issues.isEmpty());
            
        } catch (Exception e) {
            log.error("检测DNS配置问题失败: domain={}, error={}", domain, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    // 简化实现的其他方法
    @Override
    public boolean importDnsRecords(String domain, List<Map<String, Object>> records) {
        try {
            for (Map<String, Object> recordMap : records) {
                DnsRecord record = convertMapToRecord(domain, recordMap);
                createDnsRecord(record);
            }
            return true;
        } catch (Exception e) {
            log.error("导入DNS记录失败: domain={}, error={}", domain, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> exportDnsRecords(String domain) {
        try {
            List<DnsRecord> records = getDomainDnsRecords(domain);
            return records.stream()
                .map(this::convertRecordToMap)
                .toList();
        } catch (Exception e) {
            log.error("导出DNS记录失败: domain={}, error={}", domain, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean syncToExternalDns(String domain, String provider, Map<String, Object> credentials) {
        // 简化实现 - 实际应集成各DNS服务商API
        log.info("同步DNS记录到外部服务商: domain={}, provider={}", domain, provider);
        return true;
    }

    @Override
    public boolean syncFromExternalDns(String domain, String provider, Map<String, Object> credentials) {
        // 简化实现 - 实际应集成各DNS服务商API
        log.info("从外部服务商同步DNS记录: domain={}, provider={}", domain, provider);
        return true;
    }

    @Override
    public List<Map<String, Object>> getSupportedDnsProviders() {
        List<Map<String, Object>> providers = new ArrayList<>();
        
        Map<String, Object> cloudflare = new HashMap<>();
        cloudflare.put("name", "Cloudflare");
        cloudflare.put("code", "cloudflare");
        cloudflare.put("apiSupport", true);
        providers.add(cloudflare);
        
        Map<String, Object> route53 = new HashMap<>();
        route53.put("name", "Amazon Route 53");
        route53.put("code", "route53");
        route53.put("apiSupport", true);
        providers.add(route53);
        
        return providers;
    }

    @Override
    public Map<String, Object> testDnsResolution(String name, String type, String dnsServer) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            if (dnsServer != null) {
                env.put(Context.PROVIDER_URL, "dns://" + dnsServer);
            }
            
            DirContext ctx = new InitialDirContext(env);
            long startTime = System.currentTimeMillis();
            
            Attributes attrs = ctx.getAttributes(name, new String[]{type});
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            Attribute attr = attrs.get(type);
            if (attr != null) {
                result.put("success", true);
                result.put("value", attr.get().toString());
                result.put("responseTime", responseTime);
            } else {
                result.put("success", false);
                result.put("error", "记录不存在");
            }
            
            ctx.close();
            
        } catch (Exception e) {
            log.error("测试DNS解析失败: name={}, type={}, server={}, error={}", 
                name, type, dnsServer, e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    // 其他简化实现的方法
    @Override
    public Map<String, Object> getDnsPropagationStatus(String name, String type) { return new HashMap<>(); }

    @Override
    public boolean cleanupExpiredDnsRecords() { return true; }

    @Override
    public String backupDnsRecords(String domain) {
        try {
            List<DnsRecord> records = getDomainDnsRecords(domain);
            return objectMapper.writeValueAsString(records);
        } catch (Exception e) {
            log.error("备份DNS记录失败: domain={}, error={}", domain, e.getMessage(), e);
            return "";
        }
    }

    @Override
    public boolean restoreDnsRecords(String domain, String backupData) {
        try {
            List<DnsRecord> records = objectMapper.readValue(backupData, new TypeReference<List<DnsRecord>>() {});
            for (DnsRecord record : records) {
                record.setId(null);
                record.setDomain(domain);
                createDnsRecord(record);
            }
            return true;
        } catch (Exception e) {
            log.error("恢复DNS记录失败: domain={}, error={}", domain, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> monitorDnsChanges(String domain) { return new HashMap<>(); }

    @Override
    public Map<String, Object> getDnsHealthStatus(String domain) { return new HashMap<>(); }

    @Override
    public Map<String, Object> performDnsMaintenance() { return new HashMap<>(); }

    @Override
    public Map<String, Object> verifyDnsServerConfiguration(String dnsServer) { return new HashMap<>(); }

    @Override
    public List<Map<String, Object>> getDnsQueryHistory(String name, String type, int limit) { return new ArrayList<>(); }

    @Override
    public Map<String, Object> analyzeDnsPerformance(String domain) { return new HashMap<>(); }

    @Override
    public boolean optimizeDnsConfiguration(String domain, Map<String, Object> options) { return true; }

    @Override
    public boolean setupDnsMonitoring(String domain, Map<String, Object> monitorConfig) { return true; }

    @Override
    public Map<String, Object> getDnsMonitoringReport(String domain) { return new HashMap<>(); }

    // ========== 私有辅助方法 ==========

    private int calculateDnsScore(List<Map<String, Object>> issues, List<Map<String, Object>> recommendations) {
        int score = 100;
        for (Map<String, Object> issue : issues) {
            String severity = (String) issue.get("severity");
            if ("critical".equals(severity)) {
                score -= 30;
            } else if ("high".equals(severity)) {
                score -= 20;
            } else {
                score -= 10;
            }
        }
        score -= recommendations.size() * 5;
        return Math.max(0, score);
    }

    private DnsRecord convertMapToRecord(String domain, Map<String, Object> recordMap) {
        DnsRecord record = new DnsRecord();
        record.setDomain(domain);
        record.setName((String) recordMap.get("name"));
        record.setType((String) recordMap.get("type"));
        record.setValue((String) recordMap.get("value"));
        record.setTtl((Integer) recordMap.getOrDefault("ttl", 3600));
        record.setPriority((Integer) recordMap.get("priority"));
        record.setSource("IMPORTED");
        return record;
    }

    private Map<String, Object> convertRecordToMap(DnsRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", record.getName());
        map.put("type", record.getType());
        map.put("value", record.getValue());
        map.put("ttl", record.getTtl());
        map.put("priority", record.getPriority());
        map.put("status", record.getStatus());
        return map;
    }
}