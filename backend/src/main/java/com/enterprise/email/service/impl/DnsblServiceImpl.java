package com.enterprise.email.service.impl;

import com.enterprise.email.entity.DnsblConfig;
import com.enterprise.email.mapper.DnsblConfigMapper;
import com.enterprise.email.service.DnsblService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * DNSBL黑名单检查服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DnsblServiceImpl implements DnsblService {

    private final DnsblConfigMapper dnsblConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 查询结果缓存
    private final Map<String, Map<String, Object>> queryCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    // 查询历史记录
    private final Map<String, List<Map<String, Object>>> queryHistory = new ConcurrentHashMap<>();

    @Override
    public boolean createDnsblConfig(DnsblConfig config) {
        try {
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setEnabled(true);
            config.setStatus("ACTIVE");
            
            // 设置默认值
            setDefaultValues(config);
            
            int result = dnsblConfigMapper.insert(config);
            if (result > 0) {
                log.info("DNSBL配置创建成功: {}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("创建DNSBL配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateDnsblConfig(DnsblConfig config) {
        try {
            config.setUpdatedAt(LocalDateTime.now());
            int result = dnsblConfigMapper.updateById(config);
            if (result > 0) {
                log.info("DNSBL配置更新成功: {}", config.getDomain());
                return true;
            }
        } catch (Exception e) {
            log.error("更新DNSBL配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteDnsblConfig(Long configId) {
        try {
            DnsblConfig config = dnsblConfigMapper.selectById(configId);
            if (config != null) {
                config.setDeleted(true);
                config.setUpdatedAt(LocalDateTime.now());
                int result = dnsblConfigMapper.updateById(config);
                if (result > 0) {
                    log.info("DNSBL配置删除成功: {}", config.getDomain());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除DNSBL配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public DnsblConfig getDnsblConfig(String domain) {
        return dnsblConfigMapper.selectByDomain(domain);
    }

    @Override
    public List<DnsblConfig> getEnabledConfigs() {
        return dnsblConfigMapper.selectEnabledConfigs();
    }

    @Override
    public Map<String, Object> checkIpBlacklist(String ip, String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查缓存
            String cacheKey = domain + "_ip_" + ip;
            if (isCacheValid(cacheKey)) {
                result = queryCache.get(cacheKey);
                result.put("fromCache", true);
                return result;
            }
            
            DnsblConfig config = getDnsblConfig(domain);
            if (config == null || !config.getEnabled() || !config.getCheckIpBlacklist()) {
                result.put("checked", false);
                result.put("reason", "DNSBL检查未启用");
                return result;
            }
            
            // 检查白名单
            if (isInWhitelist(ip, config.getWhitelistIps())) {
                result.put("blacklisted", false);
                result.put("whitelisted", true);
                result.put("reason", "IP在白名单中");
                updateStatistics(config, false, true);
                return result;
            }
            
            // 检查自定义黑名单
            if (isInCustomBlacklist(ip, config.getCustomBlacklistIps())) {
                result.put("blacklisted", true);
                result.put("source", "CUSTOM");
                result.put("reason", "IP在自定义黑名单中");
                updateStatistics(config, true, false);
                return result;
            }
            
            // 解析DNSBL服务器列表
            List<Map<String, Object>> dnsblServers = parseDnsblServers(config.getDnsblServers());
            if (dnsblServers.isEmpty()) {
                result.put("checked", false);
                result.put("reason", "没有配置DNSBL服务器");
                return result;
            }
            
            // 并发查询多个DNSBL服务器
            List<Map<String, Object>> queryResults = queryDnsblServers(ip, dnsblServers, config);
            
            // 分析查询结果
            boolean blacklisted = false;
            List<String> hitServers = new ArrayList<>();
            List<String> reasons = new ArrayList<>();
            
            for (Map<String, Object> queryResult : queryResults) {
                if (Boolean.TRUE.equals(queryResult.get("blacklisted"))) {
                    blacklisted = true;
                    hitServers.add((String) queryResult.get("server"));
                    reasons.add((String) queryResult.get("reason"));
                }
            }
            
            result.put("ip", ip);
            result.put("blacklisted", blacklisted);
            result.put("hitServers", hitServers);
            result.put("reasons", reasons);
            result.put("queriedServers", dnsblServers.size());
            result.put("responseTime", calculateAverageResponseTime(queryResults));
            result.put("checkedAt", LocalDateTime.now());
            
            // 更新统计信息
            updateStatistics(config, blacklisted, false);
            
            // 缓存结果
            cacheResult(cacheKey, result, config.getCacheTtl());
            
            // 记录查询历史
            recordQueryHistory(domain, "IP", ip, result);
            
        } catch (Exception e) {
            log.error("检查IP黑名单失败: ip={}, domain={}, error={}", ip, domain, e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> checkDomainBlacklist(String domainToCheck, String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查缓存
            String cacheKey = domain + "_domain_" + domainToCheck;
            if (isCacheValid(cacheKey)) {
                result = queryCache.get(cacheKey);
                result.put("fromCache", true);
                return result;
            }
            
            DnsblConfig config = getDnsblConfig(domain);
            if (config == null || !config.getEnabled() || !config.getCheckDomainBlacklist()) {
                result.put("checked", false);
                result.put("reason", "域名黑名单检查未启用");
                return result;
            }
            
            // 检查白名单
            if (isInWhitelist(domainToCheck, config.getWhitelistDomains())) {
                result.put("blacklisted", false);
                result.put("whitelisted", true);
                result.put("reason", "域名在白名单中");
                return result;
            }
            
            // 检查自定义黑名单
            if (isInCustomBlacklist(domainToCheck, config.getCustomBlacklistDomains())) {
                result.put("blacklisted", true);
                result.put("source", "CUSTOM");
                result.put("reason", "域名在自定义黑名单中");
                return result;
            }
            
            // 解析SURBL服务器列表
            List<Map<String, Object>> surblServers = parseDnsblServers(config.getSurblServers());
            if (surblServers.isEmpty()) {
                result.put("checked", false);
                result.put("reason", "没有配置SURBL服务器");
                return result;
            }
            
            // 查询SURBL服务器
            List<Map<String, Object>> queryResults = querySurblServers(domainToCheck, surblServers, config);
            
            // 分析查询结果
            boolean blacklisted = false;
            List<String> hitServers = new ArrayList<>();
            List<String> reasons = new ArrayList<>();
            
            for (Map<String, Object> queryResult : queryResults) {
                if (Boolean.TRUE.equals(queryResult.get("blacklisted"))) {
                    blacklisted = true;
                    hitServers.add((String) queryResult.get("server"));
                    reasons.add((String) queryResult.get("reason"));
                }
            }
            
            result.put("domain", domainToCheck);
            result.put("blacklisted", blacklisted);
            result.put("hitServers", hitServers);
            result.put("reasons", reasons);
            result.put("queriedServers", surblServers.size());
            result.put("responseTime", calculateAverageResponseTime(queryResults));
            result.put("checkedAt", LocalDateTime.now());
            
            // 缓存结果
            cacheResult(cacheKey, result, config.getCacheTtl());
            
            // 记录查询历史
            recordQueryHistory(domain, "DOMAIN", domainToCheck, result);
            
        } catch (Exception e) {
            log.error("检查域名黑名单失败: domain={}, checkDomain={}, error={}", domainToCheck, domain, e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> checkUrlBlacklist(String url, String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查缓存
            String cacheKey = domain + "_url_" + url.hashCode();
            if (isCacheValid(cacheKey)) {
                result = queryCache.get(cacheKey);
                result.put("fromCache", true);
                return result;
            }
            
            DnsblConfig config = getDnsblConfig(domain);
            if (config == null || !config.getEnabled() || !config.getCheckUrlBlacklist()) {
                result.put("checked", false);
                result.put("reason", "URL黑名单检查未启用");
                return result;
            }
            
            // 解析URIBL服务器列表
            List<Map<String, Object>> uriblServers = parseDnsblServers(config.getUriblServers());
            if (uriblServers.isEmpty()) {
                result.put("checked", false);
                result.put("reason", "没有配置URIBL服务器");
                return result;
            }
            
            // 提取URL中的域名
            String urlDomain = extractDomainFromUrl(url);
            
            // 查询URIBL服务器
            List<Map<String, Object>> queryResults = queryUriblServers(urlDomain, uriblServers, config);
            
            // 分析查询结果
            boolean blacklisted = false;
            List<String> hitServers = new ArrayList<>();
            List<String> reasons = new ArrayList<>();
            
            for (Map<String, Object> queryResult : queryResults) {
                if (Boolean.TRUE.equals(queryResult.get("blacklisted"))) {
                    blacklisted = true;
                    hitServers.add((String) queryResult.get("server"));
                    reasons.add((String) queryResult.get("reason"));
                }
            }
            
            result.put("url", url);
            result.put("urlDomain", urlDomain);
            result.put("blacklisted", blacklisted);
            result.put("hitServers", hitServers);
            result.put("reasons", reasons);
            result.put("queriedServers", uriblServers.size());
            result.put("responseTime", calculateAverageResponseTime(queryResults));
            result.put("checkedAt", LocalDateTime.now());
            
            // 缓存结果
            cacheResult(cacheKey, result, config.getCacheTtl());
            
            // 记录查询历史
            recordQueryHistory(domain, "URL", url, result);
            
        } catch (Exception e) {
            log.error("检查URL黑名单失败: url={}, domain={}, error={}", url, domain, e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> batchCheckIpBlacklist(List<String> ips, String domain) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            for (String ip : ips) {
                Map<String, Object> ipResult = checkIpBlacklist(ip, domain);
                results.add(ipResult);
            }
            
            // 统计结果
            long blacklistedCount = results.stream()
                .mapToLong(r -> Boolean.TRUE.equals(r.get("blacklisted")) ? 1 : 0)
                .sum();
            
            result.put("totalIps", ips.size());
            result.put("blacklistedIps", blacklistedCount);
            result.put("cleanIps", ips.size() - blacklistedCount);
            result.put("results", results);
            result.put("checkedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("批量检查IP黑名单失败: domain={}, error={}", domain, e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> batchCheckDomainBlacklist(List<String> domains, String checkDomain) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            for (String domain : domains) {
                Map<String, Object> domainResult = checkDomainBlacklist(domain, checkDomain);
                results.add(domainResult);
            }
            
            // 统计结果
            long blacklistedCount = results.stream()
                .mapToLong(r -> Boolean.TRUE.equals(r.get("blacklisted")) ? 1 : 0)
                .sum();
            
            result.put("totalDomains", domains.size());
            result.put("blacklistedDomains", blacklistedCount);
            result.put("cleanDomains", domains.size() - blacklistedCount);
            result.put("results", results);
            result.put("checkedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("批量检查域名黑名单失败: checkDomain={}, error={}", checkDomain, e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> comprehensiveBlacklistCheck(String ip, String fromDomain, List<String> urls, String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查IP黑名单
            Map<String, Object> ipResult = checkIpBlacklist(ip, domain);
            
            // 检查域名黑名单
            Map<String, Object> domainResult = checkDomainBlacklist(fromDomain, domain);
            
            // 检查URL黑名单
            List<Map<String, Object>> urlResults = new ArrayList<>();
            for (String url : urls) {
                urlResults.add(checkUrlBlacklist(url, domain));
            }
            
            // 综合评估
            boolean anyBlacklisted = Boolean.TRUE.equals(ipResult.get("blacklisted")) ||
                                   Boolean.TRUE.equals(domainResult.get("blacklisted")) ||
                                   urlResults.stream().anyMatch(r -> Boolean.TRUE.equals(r.get("blacklisted")));
            
            double riskScore = calculateRiskScore(ipResult, domainResult, urlResults);
            
            result.put("ip", ip);
            result.put("fromDomain", fromDomain);
            result.put("urls", urls);
            result.put("anyBlacklisted", anyBlacklisted);
            result.put("riskScore", riskScore);
            result.put("ipResult", ipResult);
            result.put("domainResult", domainResult);
            result.put("urlResults", urlResults);
            result.put("checkedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("综合黑名单检查失败: domain={}, error={}", domain, e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public boolean addToCustomBlacklist(String domain, String entry, String type) {
        try {
            DnsblConfig config = getDnsblConfig(domain);
            if (config == null) {
                return false;
            }
            
            Set<String> entries = new HashSet<>();
            if ("ip".equals(type)) {
                if (config.getCustomBlacklistIps() != null) {
                    entries.addAll(Arrays.asList(config.getCustomBlacklistIps().split(",")));
                }
                entries.add(entry);
                config.setCustomBlacklistIps(String.join(",", entries));
            } else if ("domain".equals(type)) {
                if (config.getCustomBlacklistDomains() != null) {
                    entries.addAll(Arrays.asList(config.getCustomBlacklistDomains().split(",")));
                }
                entries.add(entry);
                config.setCustomBlacklistDomains(String.join(",", entries));
            }
            
            return updateDnsblConfig(config);
        } catch (Exception e) {
            log.error("添加到自定义黑名单失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeFromCustomBlacklist(String domain, String entry, String type) {
        try {
            DnsblConfig config = getDnsblConfig(domain);
            if (config == null) {
                return false;
            }
            
            if ("ip".equals(type)) {
                if (config.getCustomBlacklistIps() != null) {
                    String updated = config.getCustomBlacklistIps().replace(entry, "").replace(",,", ",");
                    config.setCustomBlacklistIps(updated.startsWith(",") ? updated.substring(1) : updated);
                }
            } else if ("domain".equals(type)) {
                if (config.getCustomBlacklistDomains() != null) {
                    String updated = config.getCustomBlacklistDomains().replace(entry, "").replace(",,", ",");
                    config.setCustomBlacklistDomains(updated.startsWith(",") ? updated.substring(1) : updated);
                }
            }
            
            return updateDnsblConfig(config);
        } catch (Exception e) {
            log.error("从自定义黑名单移除失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getCustomBlacklist(String domain) {
        Map<String, Object> blacklist = new HashMap<>();
        
        try {
            DnsblConfig config = getDnsblConfig(domain);
            if (config != null) {
                blacklist.put("ips", parseListString(config.getCustomBlacklistIps()));
                blacklist.put("domains", parseListString(config.getCustomBlacklistDomains()));
            }
        } catch (Exception e) {
            log.error("获取自定义黑名单失败: {}", e.getMessage(), e);
        }
        
        return blacklist;
    }

    @Override
    public boolean addToWhitelist(String domain, String entry, String type) {
        try {
            DnsblConfig config = getDnsblConfig(domain);
            if (config == null) {
                return false;
            }
            
            Set<String> entries = new HashSet<>();
            if ("ip".equals(type)) {
                if (config.getWhitelistIps() != null) {
                    entries.addAll(Arrays.asList(config.getWhitelistIps().split(",")));
                }
                entries.add(entry);
                config.setWhitelistIps(String.join(",", entries));
            } else if ("domain".equals(type)) {
                if (config.getWhitelistDomains() != null) {
                    entries.addAll(Arrays.asList(config.getWhitelistDomains().split(",")));
                }
                entries.add(entry);
                config.setWhitelistDomains(String.join(",", entries));
            }
            
            return updateDnsblConfig(config);
        } catch (Exception e) {
            log.error("添加到白名单失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeFromWhitelist(String domain, String entry, String type) {
        try {
            DnsblConfig config = getDnsblConfig(domain);
            if (config == null) {
                return false;
            }
            
            if ("ip".equals(type)) {
                if (config.getWhitelistIps() != null) {
                    String updated = config.getWhitelistIps().replace(entry, "").replace(",,", ",");
                    config.setWhitelistIps(updated.startsWith(",") ? updated.substring(1) : updated);
                }
            } else if ("domain".equals(type)) {
                if (config.getWhitelistDomains() != null) {
                    String updated = config.getWhitelistDomains().replace(entry, "").replace(",,", ",");
                    config.setWhitelistDomains(updated.startsWith(",") ? updated.substring(1) : updated);
                }
            }
            
            return updateDnsblConfig(config);
        } catch (Exception e) {
            log.error("从白名单移除失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getWhitelist(String domain) {
        Map<String, Object> whitelist = new HashMap<>();
        
        try {
            DnsblConfig config = getDnsblConfig(domain);
            if (config != null) {
                whitelist.put("ips", parseListString(config.getWhitelistIps()));
                whitelist.put("domains", parseListString(config.getWhitelistDomains()));
            }
        } catch (Exception e) {
            log.error("获取白名单失败: {}", e.getMessage(), e);
        }
        
        return whitelist;
    }

    @Override
    public Map<String, Object> testDnsblServers(String domain) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            DnsblConfig config = getDnsblConfig(domain);
            if (config == null) {
                result.put("error", "配置不存在");
                return result;
            }
            
            List<Map<String, Object>> dnsblServers = parseDnsblServers(config.getDnsblServers());
            List<Map<String, Object>> testResults = new ArrayList<>();
            
            // 使用测试IP地址 (127.0.0.2 通常被DNSBL服务器用作测试)
            String testIp = "127.0.0.2";
            
            for (Map<String, Object> server : dnsblServers) {
                Map<String, Object> testResult = testSingleDnsblServer(testIp, server, config);
                testResults.add(testResult);
            }
            
            long healthyCount = testResults.stream()
                .mapToLong(r -> Boolean.TRUE.equals(r.get("healthy")) ? 1 : 0)
                .sum();
            
            result.put("totalServers", dnsblServers.size());
            result.put("healthyServers", healthyCount);
            result.put("unhealthyServers", dnsblServers.size() - healthyCount);
            result.put("testResults", testResults);
            result.put("testedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("测试DNSBL服务器失败: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> healthCheckServers(String domain) {
        Map<String, Object> result = testDnsblServers(domain);
        
        try {
            // 更新配置中的健康状态
            DnsblConfig config = getDnsblConfig(domain);
            if (config != null) {
                config.setLastHealthCheck(LocalDateTime.now());
                config.setHealthyServers((Integer) result.get("healthyServers"));
                config.setTotalServers((Integer) result.get("totalServers"));
                updateDnsblConfig(config);
            }
        } catch (Exception e) {
            log.error("更新服务器健康状态失败: {}", e.getMessage(), e);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getDnsblStatistics(String domain) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            DnsblConfig config = getDnsblConfig(domain);
            if (config != null) {
                stats.put("totalQueries", config.getTotalQueries());
                stats.put("blacklistHits", config.getBlacklistHits());
                stats.put("whitelistHits", config.getWhitelistHits());
                stats.put("avgResponseTime", config.getAvgResponseTime());
                stats.put("maxResponseTime", config.getMaxResponseTime());
                stats.put("minResponseTime", config.getMinResponseTime());
                stats.put("timeoutErrors", config.getTimeoutErrors());
                stats.put("dnsErrors", config.getDnsErrors());
                stats.put("connectionErrors", config.getConnectionErrors());
                stats.put("lastQueryAt", config.getLastQueryAt());
                stats.put("lastHealthCheck", config.getLastHealthCheck());
                stats.put("healthyServers", config.getHealthyServers());
                stats.put("totalServers", config.getTotalServers());
                
                // 计算命中率
                if (config.getTotalQueries() > 0) {
                    double hitRate = (double) config.getBlacklistHits() / config.getTotalQueries() * 100;
                    stats.put("hitRate", Math.round(hitRate * 100.0) / 100.0);
                } else {
                    stats.put("hitRate", 0.0);
                }
            }
        } catch (Exception e) {
            log.error("获取DNSBL统计失败: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    @Override
    public List<Map<String, Object>> getQueryHistory(String domain, int limit) {
        List<Map<String, Object>> history = queryHistory.get(domain);
        if (history == null) {
            return new ArrayList<>();
        }
        
        // 返回最近的记录
        int start = Math.max(0, history.size() - limit);
        return new ArrayList<>(history.subList(start, history.size()));
    }

    @Override
    public boolean cleanupQueryHistory(String domain, int days) {
        try {
            List<Map<String, Object>> history = queryHistory.get(domain);
            if (history != null) {
                LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
                history.removeIf(record -> {
                    LocalDateTime recordTime = (LocalDateTime) record.get("checkedAt");
                    return recordTime != null && recordTime.isBefore(cutoff);
                });
            }
            
            log.info("清理查询历史完成: domain={}, days={}", domain, days);
            return true;
        } catch (Exception e) {
            log.error("清理查询历史失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // 其他方法的简化实现
    @Override
    public boolean updateDnsblServers(String domain, List<Map<String, Object>> servers) {
        try {
            DnsblConfig config = getDnsblConfig(domain);
            if (config != null) {
                config.setDnsblServers(objectMapper.writeValueAsString(servers));
                return updateDnsblConfig(config);
            }
        } catch (Exception e) {
            log.error("更新DNSBL服务器失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<Map<String, Object>> getRecommendedDnsblServers() {
        List<Map<String, Object>> servers = new ArrayList<>();
        
        // 推荐的DNSBL服务器
        servers.add(createServerConfig("zen.spamhaus.org", "Spamhaus", true));
        servers.add(createServerConfig("bl.spamcop.net", "SpamCop", true));
        servers.add(createServerConfig("dnsbl.sorbs.net", "SORBS", true));
        servers.add(createServerConfig("rbl.interserver.net", "InterServer", true));
        servers.add(createServerConfig("dnsbl-1.uceprotect.net", "UCEPROTECT", true));
        
        return servers;
    }

    @Override
    public boolean autoConfigureDnsblServers(String domain) {
        List<Map<String, Object>> recommendedServers = getRecommendedDnsblServers();
        return updateDnsblServers(domain, recommendedServers);
    }

    @Override
    public boolean validateDnsblConfig(DnsblConfig config) {
        try {
            if (config.getDomain() == null || config.getDomain().trim().isEmpty()) {
                log.error("域名不能为空");
                return false;
            }
            
            if (config.getTimeout() != null && config.getTimeout() <= 0) {
                log.error("超时时间必须大于0");
                return false;
            }
            
            if (config.getCacheTtl() != null && config.getCacheTtl() <= 0) {
                log.error("缓存TTL必须大于0");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("验证DNSBL配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // 简化实现的其他方法
    @Override
    public String exportDnsblConfig(String domain) { return ""; }

    @Override
    public boolean importDnsblConfig(String domain, String configData) { return true; }

    @Override
    public Map<String, Object> getRealTimeBlacklistStatus(String domain) { return new HashMap<>(); }

    @Override
    public boolean optimizeDnsblPerformance(String domain) { return true; }

    @Override
    public Map<String, Object> analyzeBlacklistTrends(String domain, String period) { return new HashMap<>(); }

    @Override
    public String generateBlacklistReport(String domain, String startDate, String endDate) { return ""; }

    @Override
    public boolean clearDnsblCache(String domain) {
        queryCache.entrySet().removeIf(entry -> entry.getKey().startsWith(domain + "_"));
        cacheTimestamps.entrySet().removeIf(entry -> entry.getKey().startsWith(domain + "_"));
        return true;
    }

    @Override
    public Map<String, Object> getCacheStatistics(String domain) {
        Map<String, Object> stats = new HashMap<>();
        long cacheSize = queryCache.entrySet().stream()
            .mapToLong(entry -> entry.getKey().startsWith(domain + "_") ? 1 : 0)
            .sum();
        stats.put("cacheSize", cacheSize);
        return stats;
    }

    @Override
    public boolean setBlacklistThreshold(String domain, Map<String, Object> thresholds) { return true; }

    @Override
    public Map<String, Object> getBlacklistThreshold(String domain) { return new HashMap<>(); }

    @Override
    public boolean autoLearnBlacklist(String domain, boolean enabled) { return true; }

    @Override
    public Map<String, Object> getBlacklistLearningStatus(String domain) { return new HashMap<>(); }

    // ========== 私有辅助方法 ==========

    private void setDefaultValues(DnsblConfig config) {
        if (config.getTimeout() == null) config.setTimeout(5000);
        if (config.getMaxConcurrentQueries() == null) config.setMaxConcurrentQueries(10);
        if (config.getCacheTtl() == null) config.setCacheTtl(3600);
        if (config.getCheckIpBlacklist() == null) config.setCheckIpBlacklist(true);
        if (config.getCheckDomainBlacklist() == null) config.setCheckDomainBlacklist(true);
        if (config.getCheckUrlBlacklist() == null) config.setCheckUrlBlacklist(true);
        if (config.getBlacklistAction() == null) config.setBlacklistAction("REJECT");
        if (config.getBlacklistScore() == null) config.setBlacklistScore(10.0);
        if (config.getLogQueries() == null) config.setLogQueries(true);
        if (config.getEnableStatistics() == null) config.setEnableStatistics(true);
        if (config.getTotalQueries() == null) config.setTotalQueries(0L);
        if (config.getBlacklistHits() == null) config.setBlacklistHits(0L);
        if (config.getWhitelistHits() == null) config.setWhitelistHits(0L);
        if (config.getTimeoutErrors() == null) config.setTimeoutErrors(0L);
        if (config.getDnsErrors() == null) config.setDnsErrors(0L);
        if (config.getConnectionErrors() == null) config.setConnectionErrors(0L);
        
        // 设置默认DNSBL服务器
        if (config.getDnsblServers() == null || config.getDnsblServers().isEmpty()) {
            config.setDnsblServers(objectMapper.writeValueAsString(getRecommendedDnsblServers()));
        }
    }

    private List<Map<String, Object>> parseDnsblServers(String serversJson) {
        try {
            if (serversJson == null || serversJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(serversJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析DNSBL服务器配置失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private boolean isInWhitelist(String entry, String whitelist) {
        if (whitelist == null || whitelist.trim().isEmpty()) {
            return false;
        }
        return Arrays.asList(whitelist.split(",")).contains(entry.trim());
    }

    private boolean isInCustomBlacklist(String entry, String blacklist) {
        if (blacklist == null || blacklist.trim().isEmpty()) {
            return false;
        }
        return Arrays.asList(blacklist.split(",")).contains(entry.trim());
    }

    private List<Map<String, Object>> queryDnsblServers(String ip, List<Map<String, Object>> servers, DnsblConfig config) {
        List<Map<String, Object>> results = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(config.getMaxConcurrentQueries());
        List<Future<Map<String, Object>>> futures = new ArrayList<>();
        
        for (Map<String, Object> server : servers) {
            Future<Map<String, Object>> future = executor.submit(() -> queryDnsblServer(ip, server, config));
            futures.add(future);
        }
        
        for (Future<Map<String, Object>> future : futures) {
            try {
                Map<String, Object> result = future.get(config.getTimeout(), TimeUnit.MILLISECONDS);
                results.add(result);
            } catch (Exception e) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", true);
                errorResult.put("message", e.getMessage());
                results.add(errorResult);
            }
        }
        
        executor.shutdown();
        return results;
    }

    private Map<String, Object> queryDnsblServer(String ip, Map<String, Object> server, DnsblConfig config) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            String serverName = (String) server.get("hostname");
            String[] ipParts = ip.split("\\.");
            String reversedIp = ipParts[3] + "." + ipParts[2] + "." + ipParts[1] + "." + ipParts[0];
            String queryDomain = reversedIp + "." + serverName;
            
            // 模拟DNS查询
            // InetAddress.getByName(queryDomain);
            
            // 模拟查询结果
            boolean blacklisted = Math.random() < 0.1; // 10%概率命中黑名单
            
            result.put("server", serverName);
            result.put("blacklisted", blacklisted);
            result.put("responseTime", System.currentTimeMillis() - startTime);
            
            if (blacklisted) {
                result.put("reason", "Listed in " + serverName);
            }
            
        } catch (Exception e) {
            result.put("error", true);
            result.put("message", e.getMessage());
            result.put("responseTime", System.currentTimeMillis() - startTime);
        }
        
        return result;
    }

    private List<Map<String, Object>> querySurblServers(String domain, List<Map<String, Object>> servers, DnsblConfig config) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Map<String, Object> server : servers) {
            Map<String, Object> result = querySurblServer(domain, server, config);
            results.add(result);
        }
        
        return results;
    }

    private Map<String, Object> querySurblServer(String domain, Map<String, Object> server, DnsblConfig config) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            String serverName = (String) server.get("hostname");
            String queryDomain = domain + "." + serverName;
            
            // 模拟DNS查询
            boolean blacklisted = Math.random() < 0.05; // 5%概率命中黑名单
            
            result.put("server", serverName);
            result.put("blacklisted", blacklisted);
            result.put("responseTime", System.currentTimeMillis() - startTime);
            
            if (blacklisted) {
                result.put("reason", "Listed in " + serverName);
            }
            
        } catch (Exception e) {
            result.put("error", true);
            result.put("message", e.getMessage());
            result.put("responseTime", System.currentTimeMillis() - startTime);
        }
        
        return result;
    }

    private List<Map<String, Object>> queryUriblServers(String domain, List<Map<String, Object>> servers, DnsblConfig config) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Map<String, Object> server : servers) {
            Map<String, Object> result = queryUriblServer(domain, server, config);
            results.add(result);
        }
        
        return results;
    }

    private Map<String, Object> queryUriblServer(String domain, Map<String, Object> server, DnsblConfig config) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            String serverName = (String) server.get("hostname");
            String queryDomain = domain + "." + serverName;
            
            // 模拟DNS查询
            boolean blacklisted = Math.random() < 0.03; // 3%概率命中黑名单
            
            result.put("server", serverName);
            result.put("blacklisted", blacklisted);
            result.put("responseTime", System.currentTimeMillis() - startTime);
            
            if (blacklisted) {
                result.put("reason", "Listed in " + serverName);
            }
            
        } catch (Exception e) {
            result.put("error", true);
            result.put("message", e.getMessage());
            result.put("responseTime", System.currentTimeMillis() - startTime);
        }
        
        return result;
    }

    private String extractDomainFromUrl(String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                url = url.substring(url.indexOf("://") + 3);
            }
            if (url.contains("/")) {
                url = url.substring(0, url.indexOf("/"));
            }
            if (url.contains(":")) {
                url = url.substring(0, url.indexOf(":"));
            }
            return url;
        } catch (Exception e) {
            return url;
        }
    }

    private double calculateAverageResponseTime(List<Map<String, Object>> results) {
        return results.stream()
            .filter(r -> r.containsKey("responseTime"))
            .mapToLong(r -> ((Number) r.get("responseTime")).longValue())
            .average()
            .orElse(0.0);
    }

    private double calculateRiskScore(Map<String, Object> ipResult, Map<String, Object> domainResult, List<Map<String, Object>> urlResults) {
        double score = 0.0;
        
        if (Boolean.TRUE.equals(ipResult.get("blacklisted"))) {
            score += 40.0;
        }
        if (Boolean.TRUE.equals(domainResult.get("blacklisted"))) {
            score += 30.0;
        }
        
        long blacklistedUrls = urlResults.stream()
            .mapToLong(r -> Boolean.TRUE.equals(r.get("blacklisted")) ? 1 : 0)
            .sum();
        score += blacklistedUrls * 10.0;
        
        return Math.min(score, 100.0);
    }

    private void updateStatistics(DnsblConfig config, boolean blacklisted, boolean whitelisted) {
        try {
            config.setTotalQueries(config.getTotalQueries() + 1);
            if (blacklisted) {
                config.setBlacklistHits(config.getBlacklistHits() + 1);
            }
            if (whitelisted) {
                config.setWhitelistHits(config.getWhitelistHits() + 1);
            }
            config.setLastQueryAt(LocalDateTime.now());
            updateDnsblConfig(config);
        } catch (Exception e) {
            log.error("更新统计信息失败: {}", e.getMessage(), e);
        }
    }

    private boolean isCacheValid(String cacheKey) {
        if (!queryCache.containsKey(cacheKey)) {
            return false;
        }
        
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp == null) {
            return false;
        }
        
        DnsblConfig config = getDnsblConfig(cacheKey.split("_")[0]);
        if (config == null) {
            return false;
        }
        
        return System.currentTimeMillis() - timestamp < config.getCacheTtl() * 1000L;
    }

    private void cacheResult(String cacheKey, Map<String, Object> result, int ttl) {
        queryCache.put(cacheKey, new HashMap<>(result));
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
    }

    private void recordQueryHistory(String domain, String type, String query, Map<String, Object> result) {
        List<Map<String, Object>> history = queryHistory.computeIfAbsent(domain, k -> new ArrayList<>());
        
        Map<String, Object> record = new HashMap<>();
        record.put("type", type);
        record.put("query", query);
        record.put("blacklisted", result.get("blacklisted"));
        record.put("checkedAt", LocalDateTime.now());
        
        history.add(record);
        
        // 保持历史记录在合理范围内
        if (history.size() > 1000) {
            history.subList(0, history.size() - 1000).clear();
        }
    }

    private Map<String, Object> testSingleDnsblServer(String testIp, Map<String, Object> server, DnsblConfig config) {
        Map<String, Object> result = queryDnsblServer(testIp, server, config);
        result.put("healthy", !Boolean.TRUE.equals(result.get("error")));
        return result;
    }

    private Map<String, Object> createServerConfig(String hostname, String name, boolean enabled) {
        Map<String, Object> server = new HashMap<>();
        server.put("hostname", hostname);
        server.put("name", name);
        server.put("enabled", enabled);
        return server;
    }

    private List<String> parseListString(String listString) {
        if (listString == null || listString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(listString.split(","));
    }
}