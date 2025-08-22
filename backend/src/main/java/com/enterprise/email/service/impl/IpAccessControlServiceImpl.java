package com.enterprise.email.service.impl;

import com.enterprise.email.entity.IpAccessControl;
import com.enterprise.email.entity.Whitelist;
import com.enterprise.email.mapper.IpAccessControlMapper;
import com.enterprise.email.service.IpAccessControlService;
import com.enterprise.email.service.WhitelistService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * IP访问控制服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpAccessControlServiceImpl implements IpAccessControlService {

    private final IpAccessControlMapper ipAccessControlMapper;
    private final WhitelistService whitelistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // IP地址正则表达式
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    // 速率限制缓存
    private final Map<String, Map<String, Object>> rateLimitCache = new ConcurrentHashMap<>();

    @Override
    public boolean createAccessRule(IpAccessControl rule) {
        try {
            // 验证规则
            Map<String, Object> validation = validateAccessRule(rule);
            if (!Boolean.TRUE.equals(validation.get("valid"))) {
                log.warn("访问控制规则无效: {}", validation.get("errors"));
                return false;
            }

            // 设置默认值
            setDefaultValues(rule);

            int result = ipAccessControlMapper.insert(rule);
            if (result > 0) {
                log.info("访问控制规则创建成功: name={}, ruleType={}, ipAddress={}", 
                    rule.getName(), rule.getRuleType(), rule.getIpAddress());
                return true;
            }
        } catch (Exception e) {
            log.error("创建访问控制规则失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateAccessRule(IpAccessControl rule) {
        try {
            rule.setUpdatedAt(LocalDateTime.now());
            int result = ipAccessControlMapper.updateById(rule);
            if (result > 0) {
                log.info("访问控制规则更新成功: id={}, name={}", rule.getId(), rule.getName());
                return true;
            }
        } catch (Exception e) {
            log.error("更新访问控制规则失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteAccessRule(Long ruleId) {
        try {
            IpAccessControl rule = ipAccessControlMapper.selectById(ruleId);
            if (rule != null) {
                rule.setDeleted(true);
                rule.setUpdatedAt(LocalDateTime.now());
                int result = ipAccessControlMapper.updateById(rule);
                if (result > 0) {
                    log.info("访问控制规则删除成功: id={}, name={}", ruleId, rule.getName());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除访问控制规则失败: id={}, error={}", ruleId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public IpAccessControl getAccessRule(Long ruleId) {
        return ipAccessControlMapper.selectById(ruleId);
    }

    @Override
    public List<IpAccessControl> getActiveRules() {
        return ipAccessControlMapper.selectActiveRules();
    }

    @Override
    public List<IpAccessControl> getRulesByService(String service) {
        return ipAccessControlMapper.selectByService(service);
    }

    @Override
    public Map<String, Object> checkIpAccess(String ipAddress, String service, Integer port) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 首先检查白名单
            if (whitelistService.isIpInWhitelist(ipAddress, null)) {
                result.put("allowed", true);
                result.put("reason", "IP在白名单中");
                result.put("action", "ALLOW");
                return result;
            }

            // 获取匹配的规则
            List<IpAccessControl> matchingRules = getMatchingRules(ipAddress, service, port);
            
            if (matchingRules.isEmpty()) {
                // 没有匹配规则，使用默认策略
                result.put("allowed", true);
                result.put("reason", "没有匹配的规则，使用默认允许策略");
                result.put("action", "ALLOW");
                return result;
            }

            // 按优先级排序，处理第一个匹配的规则
            matchingRules.sort(Comparator.comparing(IpAccessControl::getPriority));
            IpAccessControl firstRule = matchingRules.get(0);

            // 记录匹配
            ipAccessControlMapper.incrementRuleCount(firstRule.getId());

            switch (firstRule.getRuleType()) {
                case "ALLOW":
                    result.put("allowed", true);
                    result.put("reason", "匹配到允许规则: " + firstRule.getName());
                    result.put("action", firstRule.getAction());
                    break;
                    
                case "DENY":
                    result.put("allowed", false);
                    result.put("reason", "匹配到拒绝规则: " + firstRule.getName());
                    result.put("action", firstRule.getAction());
                    // 记录阻止
                    ipAccessControlMapper.incrementBlockCount(firstRule.getId());
                    break;
                    
                case "RATE_LIMIT":
                    boolean rateLimitExceeded = checkRateLimitExceeded(firstRule, ipAddress);
                    if (rateLimitExceeded) {
                        result.put("allowed", false);
                        result.put("reason", "超出速率限制: " + firstRule.getName());
                        result.put("action", "RATE_LIMITED");
                        ipAccessControlMapper.incrementBlockCount(firstRule.getId());
                    } else {
                        result.put("allowed", true);
                        result.put("reason", "在速率限制范围内");
                        result.put("action", "ALLOW");
                        updateRateLimit(firstRule, ipAddress);
                    }
                    break;
                    
                case "MONITOR":
                    result.put("allowed", true);
                    result.put("reason", "监控模式，允许访问但记录日志");
                    result.put("action", "MONITOR");
                    break;
                    
                default:
                    result.put("allowed", true);
                    result.put("reason", "未知规则类型，默认允许");
                    result.put("action", "ALLOW");
                    break;
            }

            result.put("matchedRule", firstRule.getName());
            result.put("ruleId", firstRule.getId());

        } catch (Exception e) {
            log.error("检查IP访问权限失败: ip={}, service={}, error={}", ipAddress, service, e.getMessage(), e);
            result.put("allowed", true);
            result.put("reason", "检查过程中发生错误，默认允许");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public boolean validateIpAccess(String ipAddress, String service) {
        Map<String, Object> result = checkIpAccess(ipAddress, service, null);
        return Boolean.TRUE.equals(result.get("allowed"));
    }

    @Override
    public boolean blockIpAddress(String ipAddress, String reason, Integer durationMinutes) {
        try {
            IpAccessControl rule = new IpAccessControl();
            rule.setName("自动阻止 - " + ipAddress);
            rule.setRuleType("DENY");
            rule.setIpAddress(ipAddress);
            rule.setService("ALL");
            rule.setEnabled(true);
            rule.setPriority(1); // 高优先级
            rule.setAction("REJECT");
            rule.setDescription(reason);
            
            if (durationMinutes != null) {
                rule.setTemporary(true);
                rule.setTemporaryDuration(durationMinutes);
                rule.setEffectiveUntil(LocalDateTime.now().plusMinutes(durationMinutes));
            }

            boolean created = createAccessRule(rule);
            if (created) {
                log.info("IP地址已被阻止: ip={}, reason={}, duration={}分钟", ipAddress, reason, durationMinutes);
            }
            return created;

        } catch (Exception e) {
            log.error("阻止IP地址失败: ip={}, error={}", ipAddress, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean unblockIpAddress(String ipAddress) {
        try {
            List<IpAccessControl> rules = ipAccessControlMapper.selectByIpAddress(ipAddress);
            boolean unblocked = false;
            
            for (IpAccessControl rule : rules) {
                if ("DENY".equals(rule.getRuleType()) && rule.getEnabled()) {
                    rule.setEnabled(false);
                    rule.setUpdatedAt(LocalDateTime.now());
                    ipAccessControlMapper.updateById(rule);
                    unblocked = true;
                }
            }
            
            if (unblocked) {
                log.info("IP地址已解除阻止: ip={}", ipAddress);
            }
            return unblocked;

        } catch (Exception e) {
            log.error("解除IP阻止失败: ip={}, error={}", ipAddress, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean temporaryBlockIp(String ipAddress, int durationMinutes, String reason) {
        return blockIpAddress(ipAddress, reason, durationMinutes);
    }

    @Override
    public boolean addIpToWhitelist(String ipAddress, String description) {
        try {
            Whitelist whitelist = new Whitelist();
            whitelist.setName("IP白名单 - " + ipAddress);
            whitelist.setType("IP");
            whitelist.setValue(ipAddress);
            whitelist.setDescription(description);
            whitelist.setGlobal(true);
            whitelist.setEnabled(true);
            whitelist.setPriority(1);

            boolean created = whitelistService.createWhitelist(whitelist);
            if (created) {
                log.info("IP已添加到白名单: ip={}, description={}", ipAddress, description);
            }
            return created;

        } catch (Exception e) {
            log.error("添加IP到白名单失败: ip={}, error={}", ipAddress, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeIpFromWhitelist(String ipAddress) {
        try {
            List<Whitelist> whitelists = whitelistService.getWhitelistsByType("IP");
            boolean removed = false;
            
            for (Whitelist whitelist : whitelists) {
                if (ipAddress.equals(whitelist.getValue())) {
                    whitelistService.deleteWhitelist(whitelist.getId());
                    removed = true;
                }
            }
            
            if (removed) {
                log.info("IP已从白名单移除: ip={}", ipAddress);
            }
            return removed;

        } catch (Exception e) {
            log.error("从白名单移除IP失败: ip={}, error={}", ipAddress, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean setRateLimit(String ipAddress, int limit, int timeWindowSeconds) {
        try {
            IpAccessControl rule = new IpAccessControl();
            rule.setName("速率限制 - " + ipAddress);
            rule.setRuleType("RATE_LIMIT");
            rule.setIpAddress(ipAddress);
            rule.setService("ALL");
            rule.setEnabled(true);
            rule.setPriority(50);
            rule.setAction("RATE_LIMITED");
            rule.setRateLimit(limit);
            rule.setTimeWindow(timeWindowSeconds);
            rule.setCurrentCount(0);
            rule.setCountResetAt(LocalDateTime.now());

            boolean created = createAccessRule(rule);
            if (created) {
                log.info("已设置速率限制: ip={}, limit={}/{} seconds", ipAddress, limit, timeWindowSeconds);
            }
            return created;

        } catch (Exception e) {
            log.error("设置速率限制失败: ip={}, error={}", ipAddress, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean checkRateLimit(String ipAddress, String service) {
        try {
            List<IpAccessControl> rules = ipAccessControlMapper.selectByIpAddress(ipAddress);
            
            for (IpAccessControl rule : rules) {
                if ("RATE_LIMIT".equals(rule.getRuleType()) && rule.getEnabled()) {
                    if (checkRateLimitExceeded(rule, ipAddress)) {
                        return false; // 超出限制
                    }
                }
            }
            
            return true; // 在限制范围内
            
        } catch (Exception e) {
            log.error("检查速率限制失败: ip={}, service={}, error={}", ipAddress, service, e.getMessage(), e);
            return true; // 出错时默认允许
        }
    }

    @Override
    public Map<String, Object> getIpGeolocation(String ipAddress) {
        Map<String, Object> geolocation = new HashMap<>();
        
        try {
            // 简化实现 - 实际应集成IP地理位置服务API
            geolocation.put("ip", ipAddress);
            geolocation.put("country", "Unknown");
            geolocation.put("countryCode", "XX");
            geolocation.put("region", "Unknown");
            geolocation.put("city", "Unknown");
            geolocation.put("isp", "Unknown");
            geolocation.put("timezone", "UTC");
            
            // 根据IP段简单判断（示例）
            if (ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") || 
                ipAddress.startsWith("172.")) {
                geolocation.put("type", "private");
                geolocation.put("country", "Local");
                geolocation.put("countryCode", "LO");
            } else {
                geolocation.put("type", "public");
            }

        } catch (Exception e) {
            log.error("获取IP地理位置失败: ip={}, error={}", ipAddress, e.getMessage(), e);
            geolocation.put("error", e.getMessage());
        }
        
        return geolocation;
    }

    @Override
    public boolean createGeolocationRule(String countryCode, String ruleType, String action) {
        try {
            IpAccessControl rule = new IpAccessControl();
            rule.setName("地理位置规则 - " + countryCode);
            rule.setRuleType(ruleType);
            rule.setCountryCode(countryCode);
            rule.setService("ALL");
            rule.setEnabled(true);
            rule.setPriority(60);
            rule.setAction(action);
            rule.setDescription("基于国家代码的访问控制");

            boolean created = createAccessRule(rule);
            if (created) {
                log.info("地理位置规则创建成功: country={}, ruleType={}, action={}", 
                    countryCode, ruleType, action);
            }
            return created;

        } catch (Exception e) {
            log.error("创建地理位置规则失败: country={}, error={}", countryCode, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean batchBlockCountries(List<String> countryCodes, String reason) {
        try {
            for (String countryCode : countryCodes) {
                createGeolocationRule(countryCode, "DENY", "REJECT");
            }
            log.info("批量阻止国家/地区成功: countries={}, reason={}", countryCodes, reason);
            return true;
        } catch (Exception e) {
            log.error("批量阻止国家/地区失败: countries={}, error={}", countryCodes, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean batchAllowCountries(List<String> countryCodes) {
        try {
            for (String countryCode : countryCodes) {
                createGeolocationRule(countryCode, "ALLOW", "ACCEPT");
            }
            log.info("批量允许国家/地区成功: countries={}", countryCodes);
            return true;
        } catch (Exception e) {
            log.error("批量允许国家/地区失败: countries={}, error={}", countryCodes, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getAccessStatistics() {
        try {
            List<Map<String, Object>> ruleStats = ipAccessControlMapper.selectAccessControlStatistics();
            
            Map<String, Object> result = new HashMap<>();
            result.put("byRuleType", ruleStats);
            
            // 计算总数
            int totalRules = ruleStats.stream()
                .mapToInt(stat -> ((Number) stat.get("rule_count")).intValue())
                .sum();
            
            int enabledRules = ruleStats.stream()
                .mapToInt(stat -> ((Number) stat.get("enabled_count")).intValue())
                .sum();
            
            long totalMatches = ruleStats.stream()
                .mapToLong(stat -> ((Number) stat.getOrDefault("total_matches", 0)).longValue())
                .sum();
            
            long totalBlocks = ruleStats.stream()
                .mapToLong(stat -> ((Number) stat.getOrDefault("total_blocks", 0)).longValue())
                .sum();
            
            result.put("totalRules", totalRules);
            result.put("enabledRules", enabledRules);
            result.put("disabledRules", totalRules - enabledRules);
            result.put("totalMatches", totalMatches);
            result.put("totalBlocks", totalBlocks);
            
            if (totalMatches > 0) {
                double blockRate = (double) totalBlocks / totalMatches * 100;
                result.put("blockRate", Math.round(blockRate * 100.0) / 100.0);
            } else {
                result.put("blockRate", 0.0);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("获取访问统计失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getBlockedIpStatistics() {
        try {
            List<IpAccessControl> deniedRules = ipAccessControlMapper.selectByRuleType("DENY");
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalBlockedIps", deniedRules.size());
            
            long temporaryBlocks = deniedRules.stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getTemporary()))
                .count();
            
            result.put("temporaryBlocks", temporaryBlocks);
            result.put("permanentBlocks", deniedRules.size() - temporaryBlocks);
            
            // 按国家统计
            Map<String, Long> byCountry = deniedRules.stream()
                .filter(rule -> rule.getCountryCode() != null)
                .collect(HashMap::new,
                    (map, rule) -> map.merge(rule.getCountryCode(), 1L, Long::sum),
                    (map1, map2) -> { map1.putAll(map2); return map1; });
            
            result.put("byCountry", byCountry);
            
            return result;
            
        } catch (Exception e) {
            log.error("获取阻止IP统计失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public List<Map<String, Object>> getMostActiveIps(int limit) {
        return ipAccessControlMapper.selectMostActiveIps(limit);
    }

    @Override
    public List<IpAccessControl> getRecentlyBlockedIps(int hours, int limit) {
        return ipAccessControlMapper.selectRecentlyBlockedIps(hours, limit);
    }

    // 简化实现的其他方法
    @Override
    public Map<String, Object> analyzeIpAccessPatterns() { return new HashMap<>(); }

    @Override
    public List<Map<String, Object>> detectAnomalousIpActivity() { return new ArrayList<>(); }

    @Override
    public boolean autoBlockMaliciousIps(Map<String, Object> criteria) { return true; }

    @Override
    public Map<String, Object> getIpReputation(String ipAddress) { 
        Map<String, Object> reputation = new HashMap<>();
        reputation.put("ip", ipAddress);
        reputation.put("reputation", "unknown");
        reputation.put("riskScore", 50);
        return reputation;
    }

    @Override
    public Map<String, Object> batchImportRules(List<Map<String, Object>> rules) {
        Map<String, Object> result = new HashMap<>();
        List<String> successItems = new ArrayList<>();
        List<String> failedItems = new ArrayList<>();

        for (Map<String, Object> ruleMap : rules) {
            try {
                IpAccessControl rule = convertMapToRule(ruleMap);
                if (createAccessRule(rule)) {
                    successItems.add(rule.getName());
                } else {
                    failedItems.add(rule.getName());
                }
            } catch (Exception e) {
                String name = (String) ruleMap.getOrDefault("name", "Unknown");
                log.error("批量导入规则失败: name={}, error={}", name, e.getMessage(), e);
                failedItems.add(name);
            }
        }

        result.put("total", rules.size());
        result.put("success", successItems.size());
        result.put("failed", failedItems.size());
        result.put("successItems", successItems);
        result.put("failedItems", failedItems);

        return result;
    }

    @Override
    public List<Map<String, Object>> exportAccessRules(String format) {
        try {
            List<IpAccessControl> rules = getActiveRules();
            return rules.stream()
                .map(this::convertRuleToMap)
                .toList();
        } catch (Exception e) {
            log.error("导出访问规则失败: format={}, error={}", format, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> validateAccessRule(IpAccessControl rule) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try {
            // 检查必需字段
            if (rule.getName() == null || rule.getName().trim().isEmpty()) {
                errors.add("规则名称不能为空");
            }

            if (rule.getRuleType() == null || rule.getRuleType().trim().isEmpty()) {
                errors.add("规则类型不能为空");
            }

            // 验证IP地址或国家代码
            if (rule.getIpAddress() != null && !rule.getIpAddress().trim().isEmpty()) {
                if (!isValidIpOrCidr(rule.getIpAddress())) {
                    errors.add("IP地址或CIDR格式无效");
                }
            } else if (rule.getCountryCode() == null || rule.getCountryCode().trim().isEmpty()) {
                errors.add("必须指定IP地址或国家代码");
            }

            // 验证速率限制配置
            if ("RATE_LIMIT".equals(rule.getRuleType())) {
                if (rule.getRateLimit() == null || rule.getRateLimit() <= 0) {
                    errors.add("速率限制必须大于0");
                }
                if (rule.getTimeWindow() == null || rule.getTimeWindow() <= 0) {
                    errors.add("时间窗口必须大于0");
                }
            }

            // 检查优先级
            if (rule.getPriority() != null && rule.getPriority() < 0) {
                errors.add("优先级不能为负数");
            }

            if (errors.isEmpty()) {
                result.put("valid", true);
                result.put("message", "验证通过");
            } else {
                result.put("valid", false);
                result.put("errors", errors);
            }

        } catch (Exception e) {
            log.error("验证访问规则失败: {}", e.getMessage(), e);
            result.put("valid", false);
            result.put("errors", Arrays.asList("验证过程中发生错误: " + e.getMessage()));
        }

        return result;
    }

    @Override
    public Map<String, Object> testAccessRule(Long ruleId, String testIp) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            IpAccessControl rule = getAccessRule(ruleId);
            if (rule == null) {
                result.put("success", false);
                result.put("error", "规则不存在");
                return result;
            }

            boolean matches = doesRuleMatchIp(rule, testIp);
            result.put("success", true);
            result.put("matches", matches);
            result.put("ruleName", rule.getName());
            result.put("ruleType", rule.getRuleType());
            result.put("action", rule.getAction());

            if (matches) {
                result.put("message", "IP匹配此规则");
            } else {
                result.put("message", "IP不匹配此规则");
            }

        } catch (Exception e) {
            log.error("测试访问规则失败: ruleId={}, testIp={}, error={}", ruleId, testIp, e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    // 其他简化实现的方法
    @Override
    public boolean toggleRule(Long ruleId, boolean enabled) {
        try {
            IpAccessControl rule = getAccessRule(ruleId);
            if (rule != null) {
                rule.setEnabled(enabled);
                return updateAccessRule(rule);
            }
        } catch (Exception e) {
            log.error("切换规则状态失败: ruleId={}, enabled={}, error={}", ruleId, enabled, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean batchToggleRules(List<Long> ruleIds, boolean enabled) {
        try {
            for (Long ruleId : ruleIds) {
                toggleRule(ruleId, enabled);
            }
            return true;
        } catch (Exception e) {
            log.error("批量切换规则状态失败: enabled={}, error={}", enabled, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean cleanupExpiredTemporaryRules() {
        try {
            List<IpAccessControl> expiredRules = ipAccessControlMapper.selectExpiredTemporaryRules();
            for (IpAccessControl rule : expiredRules) {
                rule.setEnabled(false);
                rule.setUpdatedAt(LocalDateTime.now());
                ipAccessControlMapper.updateById(rule);
            }
            log.info("清理过期临时规则完成: count={}", expiredRules.size());
            return true;
        } catch (Exception e) {
            log.error("清理过期临时规则失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean resetRateLimitCounts() {
        try {
            List<IpAccessControl> rules = ipAccessControlMapper.selectRulesForCountReset();
            for (IpAccessControl rule : rules) {
                ipAccessControlMapper.resetRuleCount(rule.getId());
            }
            log.info("重置速率限制计数完成: count={}", rules.size());
            return true;
        } catch (Exception e) {
            log.error("重置速率限制计数失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // 其他简化实现的方法
    @Override
    public List<Map<String, Object>> getRuleConflicts() { return new ArrayList<>(); }
    @Override
    public boolean resolveRuleConflicts(List<Map<String, Object>> resolutions) { return true; }
    @Override
    public List<Map<String, Object>> optimizeAccessRules() { return new ArrayList<>(); }
    @Override
    public boolean syncToFirewall(String firewallType, Map<String, Object> config) { return true; }
    @Override
    public boolean syncFromFirewall(String firewallType, Map<String, Object> config) { return true; }
    @Override
    public String generateFirewallRules(String firewallType) { return ""; }
    @Override
    public List<Map<String, Object>> getSecurityRecommendations() { return new ArrayList<>(); }
    @Override
    public Map<String, Object> performSecurityScan() { return new HashMap<>(); }
    @Override
    public String generateSecurityReport(String startDate, String endDate) { return ""; }

    @Override
    public String backupAccessControlConfiguration() {
        try {
            List<IpAccessControl> rules = getActiveRules();
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            log.error("备份访问控制配置失败: {}", e.getMessage(), e);
            return "";
        }
    }

    @Override
    public boolean restoreAccessControlConfiguration(String backupData) {
        try {
            List<IpAccessControl> rules = objectMapper.readValue(backupData, new TypeReference<List<IpAccessControl>>() {});
            for (IpAccessControl rule : rules) {
                rule.setId(null);
                createAccessRule(rule);
            }
            return true;
        } catch (Exception e) {
            log.error("恢复访问控制配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // ========== 私有辅助方法 ==========

    private void setDefaultValues(IpAccessControl rule) {
        if (rule.getEnabled() == null) rule.setEnabled(true);
        if (rule.getPriority() == null) rule.setPriority(100);
        if (rule.getAction() == null) {
            switch (rule.getRuleType()) {
                case "ALLOW": rule.setAction("ACCEPT"); break;
                case "DENY": rule.setAction("REJECT"); break;
                case "RATE_LIMIT": rule.setAction("RATE_LIMITED"); break;
                case "MONITOR": rule.setAction("LOG"); break;
                default: rule.setAction("ACCEPT"); break;
            }
        }
        if (rule.getService() == null) rule.setService("ALL");
        if (rule.getTemporary() == null) rule.setTemporary(false);
        if (rule.getMatchCount() == null) rule.setMatchCount(0L);
        if (rule.getBlockCount() == null) rule.setBlockCount(0L);
        if (rule.getCurrentCount() == null) rule.setCurrentCount(0);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
    }

    private List<IpAccessControl> getMatchingRules(String ipAddress, String service, Integer port) {
        List<IpAccessControl> allRules = getActiveRules();
        List<IpAccessControl> matchingRules = new ArrayList<>();

        for (IpAccessControl rule : allRules) {
            if (doesRuleMatchIp(rule, ipAddress) && doesRuleMatchService(rule, service, port)) {
                matchingRules.add(rule);
            }
        }

        return matchingRules;
    }

    private boolean doesRuleMatchIp(IpAccessControl rule, String ipAddress) {
        // IP地址匹配
        if (rule.getIpAddress() != null) {
            if (rule.getCidrMask() != null) {
                return isIpInCidr(ipAddress, rule.getIpAddress(), rule.getCidrMask());
            } else {
                return ipAddress.equals(rule.getIpAddress());
            }
        }

        // 国家代码匹配
        if (rule.getCountryCode() != null) {
            Map<String, Object> geolocation = getIpGeolocation(ipAddress);
            String ipCountryCode = (String) geolocation.get("countryCode");
            return rule.getCountryCode().equals(ipCountryCode);
        }

        return false;
    }

    private boolean doesRuleMatchService(IpAccessControl rule, String service, Integer port) {
        // 服务匹配
        if (!"ALL".equals(rule.getService()) && service != null && !service.equals(rule.getService())) {
            return false;
        }

        // 端口匹配
        if (rule.getPort() != null && port != null && !rule.getPort().equals(port)) {
            return false;
        }

        return true;
    }

    private boolean checkRateLimitExceeded(IpAccessControl rule, String ipAddress) {
        if (rule.getRateLimit() == null || rule.getTimeWindow() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resetTime = rule.getCountResetAt();

        // 检查是否需要重置计数器
        if (resetTime == null || now.isAfter(resetTime.plusSeconds(rule.getTimeWindow()))) {
            rule.setCurrentCount(0);
            rule.setCountResetAt(now);
            ipAccessControlMapper.resetRuleCount(rule.getId());
            return false;
        }

        return rule.getCurrentCount() >= rule.getRateLimit();
    }

    private void updateRateLimit(IpAccessControl rule, String ipAddress) {
        ipAccessControlMapper.incrementRuleCount(rule.getId());
    }

    private boolean isValidIpOrCidr(String value) {
        if (IP_PATTERN.matcher(value).matches()) {
            return true;
        }

        // 检查CIDR格式
        if (value.contains("/")) {
            String[] parts = value.split("/");
            if (parts.length == 2) {
                try {
                    String ip = parts[0];
                    int mask = Integer.parseInt(parts[1]);
                    return IP_PATTERN.matcher(ip).matches() && mask >= 0 && mask <= 32;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        return false;
    }

    private boolean isIpInCidr(String ip, String cidrIp, int mask) {
        // 简化实现 - 实际应使用专业的网络库
        try {
            long ipLong = ipToLong(ip);
            long cidrLong = ipToLong(cidrIp);
            long maskLong = (0xFFFFFFFFL << (32 - mask)) & 0xFFFFFFFFL;
            
            return (ipLong & maskLong) == (cidrLong & maskLong);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (Long.parseLong(parts[i]) << (8 * (3 - i)));
        }
        return result;
    }

    private IpAccessControl convertMapToRule(Map<String, Object> ruleMap) {
        IpAccessControl rule = new IpAccessControl();
        rule.setName((String) ruleMap.get("name"));
        rule.setRuleType((String) ruleMap.get("ruleType"));
        rule.setIpAddress((String) ruleMap.get("ipAddress"));
        rule.setService((String) ruleMap.getOrDefault("service", "ALL"));
        rule.setAction((String) ruleMap.get("action"));
        rule.setEnabled((Boolean) ruleMap.getOrDefault("enabled", true));
        rule.setPriority((Integer) ruleMap.getOrDefault("priority", 100));
        rule.setDescription((String) ruleMap.get("description"));
        return rule;
    }

    private Map<String, Object> convertRuleToMap(IpAccessControl rule) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", rule.getName());
        map.put("ruleType", rule.getRuleType());
        map.put("ipAddress", rule.getIpAddress());
        map.put("service", rule.getService());
        map.put("action", rule.getAction());
        map.put("enabled", rule.getEnabled());
        map.put("priority", rule.getPriority());
        map.put("description", rule.getDescription());
        return map;
    }
}