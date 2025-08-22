package com.enterprise.email.service.impl;

import com.enterprise.email.entity.RspamdConfig;
import com.enterprise.email.mapper.RspamdConfigMapper;
import com.enterprise.email.service.RspamdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rspamd反垃圾邮件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RspamdServiceImpl implements RspamdService {

    private final RspamdConfigMapper rspamdConfigMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    
    // 缓存白名单和黑名单
    private final Map<String, Set<String>> whitelistCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> blacklistCache = new ConcurrentHashMap<>();

    @Override
    public boolean createRspamdConfig(RspamdConfig config) {
        try {
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setEnabled(true);
            config.setStatus("ACTIVE");
            
            // 设置默认值
            setDefaultValues(config);
            
            int result = rspamdConfigMapper.insert(config);
            if (result > 0) {
                // 生成Rspamd配置文件
                generateRspamdConfig(config);
                log.info("Rspamd配置创建成功: {}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("创建Rspamd配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateRspamdConfig(RspamdConfig config) {
        try {
            config.setUpdatedAt(LocalDateTime.now());
            int result = rspamdConfigMapper.updateById(config);
            if (result > 0) {
                // 重新生成配置文件
                generateRspamdConfig(config);
                log.info("Rspamd配置更新成功: {}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("更新Rspamd配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteRspamdConfig(Long configId) {
        try {
            RspamdConfig config = rspamdConfigMapper.selectById(configId);
            if (config != null) {
                config.setDeleted(true);
                config.setUpdatedAt(LocalDateTime.now());
                int result = rspamdConfigMapper.updateById(config);
                if (result > 0) {
                    log.info("Rspamd配置删除成功: {}", config.getDomain());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除Rspamd配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public RspamdConfig getRspamdConfig(String domain) {
        return rspamdConfigMapper.selectByDomain(domain);
    }

    @Override
    public List<RspamdConfig> getEnabledConfigs() {
        return rspamdConfigMapper.selectEnabledConfigs();
    }

    @Override
    public Map<String, Object> checkSpam(String emailContent, String senderIp, String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            RspamdConfig config = getRspamdConfig(domain);
            if (config == null || !config.getEnabled()) {
                result.put("status", "CONFIG_NOT_FOUND");
                return result;
            }
            
            // 检查白名单
            if (isInWhitelist(senderIp, domain)) {
                result.put("isSpam", false);
                result.put("score", 0.0);
                result.put("action", "WHITELIST");
                return result;
            }
            
            // 调用Rspamd API检查垃圾邮件
            String rspamdUrl = String.format("http://%s:%d/checkv2", 
                config.getRspamdHost(), config.getRspamdPort());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.set("IP", senderIp);
            headers.set("From", extractFromAddress(emailContent));
            
            HttpEntity<String> entity = new HttpEntity<>(emailContent, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(rspamdUrl, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> rspamdResult = response.getBody();
                
                double score = (Double) rspamdResult.getOrDefault("score", 0.0);
                String action = (String) rspamdResult.getOrDefault("action", "no action");
                
                result.put("isSpam", score >= config.getSpamThreshold());
                result.put("score", score);
                result.put("action", action);
                result.put("symbols", rspamdResult.get("symbols"));
                result.put("required_score", config.getSpamThreshold());
                result.put("reject_score", config.getRejectThreshold());
                
                // 记录统计信息
                updateStatistics(domain, score >= config.getSpamThreshold());
                
            } else {
                result.put("status", "ERROR");
                result.put("message", "Rspamd服务器响应异常");
            }
            
        } catch (Exception e) {
            log.error("检查垃圾邮件失败: {}", e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public boolean learnSpam(String emailContent, boolean isSpam) {
        try {
            List<RspamdConfig> configs = getEnabledConfigs();
            boolean success = true;
            
            for (RspamdConfig config : configs) {
                if (!config.getBayesEnabled() || !config.getBayesAutolearn()) {
                    continue;
                }
                
                String endpoint = isSpam ? "/learnspam" : "/learnham";
                String rspamdUrl = String.format("http://%s:%d%s", 
                    config.getRspamdHost(), config.getRspamdPort(), endpoint);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                
                HttpEntity<String> entity = new HttpEntity<>(emailContent, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(rspamdUrl, entity, String.class);
                
                if (response.getStatusCode() != HttpStatus.OK) {
                    success = false;
                    log.error("学习{}失败: {}", isSpam ? "垃圾邮件" : "正常邮件", response.getBody());
                }
            }
            
            return success;
        } catch (Exception e) {
            log.error("学习邮件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean trainBayesFilter(List<String> spamEmails, List<String> hamEmails) {
        try {
            boolean success = true;
            
            // 训练垃圾邮件
            for (String spam : spamEmails) {
                if (!learnSpam(spam, true)) {
                    success = false;
                }
            }
            
            // 训练正常邮件
            for (String ham : hamEmails) {
                if (!learnSpam(ham, false)) {
                    success = false;
                }
            }
            
            log.info("贝叶斯过滤器训练完成: 垃圾邮件={}, 正常邮件={}", spamEmails.size(), hamEmails.size());
            return success;
        } catch (Exception e) {
            log.error("训练贝叶斯过滤器失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean addToWhitelist(String domain, String entry, String type) {
        try {
            String cacheKey = domain + "_" + type;
            Set<String> whitelist = whitelistCache.computeIfAbsent(cacheKey, k -> new HashSet<>());
            whitelist.add(entry);
            
            // 更新数据库配置
            RspamdConfig config = getRspamdConfig(domain);
            if (config != null) {
                if ("domain".equals(type)) {
                    String domains = config.getWhitelistDomains();
                    domains = domains == null ? entry : domains + "," + entry;
                    config.setWhitelistDomains(domains);
                } else if ("ip".equals(type)) {
                    String ips = config.getWhitelistIps();
                    ips = ips == null ? entry : ips + "," + entry;
                    config.setWhitelistIps(ips);
                }
                updateRspamdConfig(config);
            }
            
            log.info("添加到白名单成功: domain={}, entry={}, type={}", domain, entry, type);
            return true;
        } catch (Exception e) {
            log.error("添加到白名单失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeFromWhitelist(String domain, String entry, String type) {
        try {
            String cacheKey = domain + "_" + type;
            Set<String> whitelist = whitelistCache.get(cacheKey);
            if (whitelist != null) {
                whitelist.remove(entry);
            }
            
            // 更新数据库配置
            RspamdConfig config = getRspamdConfig(domain);
            if (config != null) {
                if ("domain".equals(type)) {
                    String domains = config.getWhitelistDomains();
                    if (domains != null) {
                        domains = domains.replace(entry, "").replace(",,", ",");
                        config.setWhitelistDomains(domains);
                    }
                } else if ("ip".equals(type)) {
                    String ips = config.getWhitelistIps();
                    if (ips != null) {
                        ips = ips.replace(entry, "").replace(",,", ",");
                        config.setWhitelistIps(ips);
                    }
                }
                updateRspamdConfig(config);
            }
            
            log.info("从白名单移除成功: domain={}, entry={}, type={}", domain, entry, type);
            return true;
        } catch (Exception e) {
            log.error("从白名单移除失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getWhitelist(String domain) {
        Map<String, Object> whitelist = new HashMap<>();
        
        try {
            RspamdConfig config = getRspamdConfig(domain);
            if (config != null) {
                whitelist.put("domains", parseListString(config.getWhitelistDomains()));
                whitelist.put("ips", parseListString(config.getWhitelistIps()));
            }
        } catch (Exception e) {
            log.error("获取白名单失败: {}", e.getMessage(), e);
        }
        
        return whitelist;
    }

    @Override
    public boolean addToBlacklist(String domain, String entry, String type) {
        try {
            String cacheKey = domain + "_" + type + "_blacklist";
            Set<String> blacklist = blacklistCache.computeIfAbsent(cacheKey, k -> new HashSet<>());
            blacklist.add(entry);
            
            log.info("添加到黑名单成功: domain={}, entry={}, type={}", domain, entry, type);
            return true;
        } catch (Exception e) {
            log.error("添加到黑名单失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeFromBlacklist(String domain, String entry, String type) {
        try {
            String cacheKey = domain + "_" + type + "_blacklist";
            Set<String> blacklist = blacklistCache.get(cacheKey);
            if (blacklist != null) {
                blacklist.remove(entry);
            }
            
            log.info("从黑名单移除成功: domain={}, entry={}, type={}", domain, entry, type);
            return true;
        } catch (Exception e) {
            log.error("从黑名单移除失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getBlacklist(String domain) {
        Map<String, Object> blacklist = new HashMap<>();
        
        try {
            Set<String> domainBlacklist = blacklistCache.get(domain + "_domain_blacklist");
            Set<String> ipBlacklist = blacklistCache.get(domain + "_ip_blacklist");
            
            blacklist.put("domains", domainBlacklist != null ? new ArrayList<>(domainBlacklist) : new ArrayList<>());
            blacklist.put("ips", ipBlacklist != null ? new ArrayList<>(ipBlacklist) : new ArrayList<>());
        } catch (Exception e) {
            log.error("获取黑名单失败: {}", e.getMessage(), e);
        }
        
        return blacklist;
    }

    @Override
    public Map<String, Object> getSpamStatistics(String domain) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            RspamdConfig config = getRspamdConfig(domain);
            if (config == null) {
                return stats;
            }
            
            String rspamdUrl = String.format("http://%s:%d/stat", 
                config.getRspamdHost(), config.getRspamdPort());
            
            ResponseEntity<Map> response = restTemplate.getForEntity(rspamdUrl, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                stats = response.getBody();
            }
            
        } catch (Exception e) {
            log.error("获取垃圾邮件统计失败: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    @Override
    public Map<String, Object> getRspamdStatus(String domain) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            RspamdConfig config = getRspamdConfig(domain);
            if (config != null) {
                status.put("domain", domain);
                status.put("enabled", config.getEnabled());
                status.put("status", config.getStatus());
                status.put("version", config.getVersion());
                status.put("lastStatsUpdate", config.getLastStatsUpdate());
                
                // 检查服务可用性
                try {
                    String rspamdUrl = String.format("http://%s:%d/ping", 
                        config.getRspamdHost(), config.getRspamdPort());
                    ResponseEntity<String> response = restTemplate.getForEntity(rspamdUrl, String.class);
                    status.put("online", response.getStatusCode() == HttpStatus.OK);
                } catch (Exception e) {
                    status.put("online", false);
                    status.put("error", e.getMessage());
                }
            } else {
                status.put("status", "NOT_CONFIGURED");
            }
        } catch (Exception e) {
            log.error("获取Rspamd状态失败: {}", e.getMessage(), e);
            status.put("status", "ERROR");
            status.put("message", e.getMessage());
        }
        
        return status;
    }

    @Override
    public boolean restartRspamdService(String domain) {
        try {
            // 这里应该实际重启Rspamd服务
            log.info("重启Rspamd服务: {}", domain);
            
            // 更新配置状态
            RspamdConfig config = getRspamdConfig(domain);
            if (config != null) {
                config.setStatus("RESTARTING");
                updateRspamdConfig(config);
                
                // 模拟重启过程
                Thread.sleep(3000);
                
                config.setStatus("ACTIVE");
                config.setLastStatsUpdate(LocalDateTime.now());
                updateRspamdConfig(config);
            }
            
            return true;
        } catch (Exception e) {
            log.error("重启Rspamd服务失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String generateRspamdConfig(RspamdConfig config) {
        try {
            StringBuilder configContent = new StringBuilder();
            
            // 生成主配置文件
            configContent.append("# Rspamd Configuration for ").append(config.getDomain()).append("\n\n");
            
            // 基本配置
            configContent.append("options {\n");
            configContent.append("    pidfile = \"/var/run/rspamd/rspamd.pid\";\n");
            configContent.append("    .include \"$CONFDIR/options.inc\"\n");
            configContent.append("    .include(try=true; priority=1,duplicate=merge) \"$LOCAL_CONFDIR/local.d/options.inc\"\n");
            configContent.append("    .include(try=true; priority=10) \"$LOCAL_CONFDIR/override.d/options.inc\"\n");
            configContent.append("}\n\n");
            
            // 日志配置
            configContent.append("logging {\n");
            configContent.append("    type = \"file\";\n");
            configContent.append("    filename = \"").append(config.getLogFile()).append("\";\n");
            configContent.append("    level = \"").append(config.getLogLevel().toLowerCase()).append("\";\n");
            configContent.append("}\n\n");
            
            // Worker配置
            configContent.append("worker \"rspamd_proxy\" {\n");
            configContent.append("    bind_socket = \"*:").append(config.getRspamdPort()).append("\";\n");
            configContent.append("    milter = true;\n");
            configContent.append("    timeout = 120s;\n");
            configContent.append("    upstream \"local\" {\n");
            configContent.append("        default = true;\n");
            configContent.append("        self_scan = true;\n");
            configContent.append("    }\n");
            configContent.append("}\n\n");
            
            // Controller配置
            configContent.append("worker \"controller\" {\n");
            configContent.append("    bind_socket = \"*:").append(config.getWebInterfacePort()).append("\";\n");
            configContent.append("    count = 1;\n");
            if (config.getWebPassword() != null) {
                configContent.append("    password = \"").append(config.getWebPassword()).append("\";\n");
            }
            configContent.append("}\n\n");
            
            // 阈值配置
            configContent.append("actions {\n");
            configContent.append("    reject = ").append(config.getRejectThreshold()).append(";\n");
            configContent.append("    add_header = ").append(config.getSpamThreshold()).append(";\n");
            configContent.append("    greylist = ").append(config.getGreylistingThreshold()).append(";\n");
            configContent.append("}\n\n");
            
            // Redis配置
            if (config.getRedisHost() != null) {
                configContent.append("redis {\n");
                configContent.append("    servers = \"").append(config.getRedisHost()).append(":").append(config.getRedisPort()).append("\";\n");
                if (config.getRedisPassword() != null) {
                    configContent.append("    password = \"").append(config.getRedisPassword()).append("\";\n");
                }
                configContent.append("    db = \"").append(config.getRedisDb()).append("\";\n");
                configContent.append("}\n\n");
            }
            
            // 写入配置文件
            String configPath = "/etc/rspamd/local.d/" + config.getDomain() + ".conf";
            File configFile = new File(configPath);
            configFile.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(configContent.toString());
            }
            
            log.info("Rspamd配置文件生成成功: {}", configPath);
            return configPath;
            
        } catch (Exception e) {
            log.error("生成Rspamd配置文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean validateRspamdConfig(RspamdConfig config) {
        try {
            // 验证必需字段
            if (config.getDomain() == null || config.getDomain().trim().isEmpty()) {
                log.error("域名不能为空");
                return false;
            }
            
            if (config.getRspamdHost() == null || config.getRspamdHost().trim().isEmpty()) {
                log.error("Rspamd服务器地址不能为空");
                return false;
            }
            
            if (config.getRspamdPort() == null || config.getRspamdPort() <= 0 || config.getRspamdPort() > 65535) {
                log.error("Rspamd端口无效");
                return false;
            }
            
            // 验证阈值配置
            if (config.getSpamThreshold() == null || config.getSpamThreshold() < 0) {
                log.error("垃圾邮件阈值无效");
                return false;
            }
            
            if (config.getRejectThreshold() == null || config.getRejectThreshold() < config.getSpamThreshold()) {
                log.error("拒绝阈值必须大于垃圾邮件阈值");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("验证Rspamd配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // 其他方法的简化实现，实际项目中需要完整实现
    @Override
    public List<Map<String, Object>> getSpamHistory(String domain, int limit) {
        return new ArrayList<>();
    }

    @Override
    public boolean cleanupHistory(String domain, int days) {
        return true;
    }

    @Override
    public boolean updateDnsblServers(String domain, List<String> servers) {
        return true;
    }

    @Override
    public Map<String, Object> testDnsblServers(String domain) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> getNeuralNetworkStatus(String domain) {
        return new HashMap<>();
    }

    @Override
    public boolean trainNeuralNetwork(String domain, boolean forceRetrain) {
        return true;
    }

    @Override
    public Map<String, Object> getFuzzyStats(String domain) {
        return new HashMap<>();
    }

    @Override
    public boolean addFuzzyRule(String domain, String hash, String flag) {
        return true;
    }

    @Override
    public boolean removeFuzzyRule(String domain, String hash) {
        return true;
    }

    @Override
    public List<Map<String, Object>> getCustomRules(String domain) {
        return new ArrayList<>();
    }

    @Override
    public boolean addCustomRule(String domain, String ruleName, String ruleContent) {
        return true;
    }

    @Override
    public boolean removeCustomRule(String domain, String ruleName) {
        return true;
    }

    @Override
    public boolean validateCustomRule(String ruleContent) {
        return true;
    }

    @Override
    public Map<String, Object> getPerformanceMetrics(String domain) {
        return new HashMap<>();
    }

    @Override
    public boolean optimizePerformance(String domain) {
        return true;
    }

    @Override
    public String exportConfiguration(String domain) {
        return "";
    }

    @Override
    public boolean importConfiguration(String domain, String configData) {
        return true;
    }

    @Override
    public List<Map<String, Object>> batchCheckSpam(List<String> emails, String domain) {
        return new ArrayList<>();
    }

    // 私有辅助方法
    private void setDefaultValues(RspamdConfig config) {
        if (config.getSpamThreshold() == null) config.setSpamThreshold(5.0);
        if (config.getRejectThreshold() == null) config.setRejectThreshold(15.0);
        if (config.getGreylistingThreshold() == null) config.setGreylistingThreshold(4.0);
        if (config.getBayesEnabled() == null) config.setBayesEnabled(true);
        if (config.getBayesAutolearn() == null) config.setBayesAutolearn(true);
        if (config.getDkimEnabled() == null) config.setDkimEnabled(true);
        if (config.getSpfEnabled() == null) config.setSpfEnabled(true);
        if (config.getDmarcEnabled() == null) config.setDmarcEnabled(true);
        if (config.getLogLevel() == null) config.setLogLevel("INFO");
        if (config.getWorkerProcesses() == null) config.setWorkerProcesses(4);
    }

    private boolean isInWhitelist(String ip, String domain) {
        Set<String> whitelist = whitelistCache.get(domain + "_ip");
        return whitelist != null && whitelist.contains(ip);
    }

    private String extractFromAddress(String emailContent) {
        // 简化的From地址提取
        if (emailContent.contains("From:")) {
            String fromLine = emailContent.substring(emailContent.indexOf("From:") + 5);
            if (fromLine.contains("\n")) {
                fromLine = fromLine.substring(0, fromLine.indexOf("\n"));
            }
            return fromLine.trim();
        }
        return "";
    }

    private void updateStatistics(String domain, boolean isSpam) {
        // 更新统计信息的实现
        log.debug("更新统计信息: domain={}, isSpam={}", domain, isSpam);
    }

    private List<String> parseListString(String listString) {
        if (listString == null || listString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(listString.split(","));
    }
}