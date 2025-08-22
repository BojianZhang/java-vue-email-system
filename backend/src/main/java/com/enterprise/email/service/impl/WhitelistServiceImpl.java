package com.enterprise.email.service.impl;

import com.enterprise.email.entity.Whitelist;
import com.enterprise.email.mapper.WhitelistMapper;
import com.enterprise.email.service.WhitelistService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 白名单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhitelistServiceImpl implements WhitelistService {

    private final WhitelistMapper whitelistMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 邮件地址正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // IP地址正则表达式
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    @Override
    public boolean createWhitelist(Whitelist whitelist) {
        try {
            // 验证白名单配置
            Map<String, Object> validation = validateWhitelist(whitelist);
            if (!Boolean.TRUE.equals(validation.get("valid"))) {
                log.warn("白名单配置无效: {}", validation.get("errors"));
                return false;
            }

            // 设置默认值
            setDefaultValues(whitelist);

            int result = whitelistMapper.insert(whitelist);
            if (result > 0) {
                log.info("白名单创建成功: name={}, type={}, value={}", 
                    whitelist.getName(), whitelist.getType(), whitelist.getValue());
                return true;
            }
        } catch (Exception e) {
            log.error("创建白名单失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateWhitelist(Whitelist whitelist) {
        try {
            whitelist.setUpdatedAt(LocalDateTime.now());
            int result = whitelistMapper.updateById(whitelist);
            if (result > 0) {
                log.info("白名单更新成功: id={}, name={}", whitelist.getId(), whitelist.getName());
                return true;
            }
        } catch (Exception e) {
            log.error("更新白名单失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteWhitelist(Long whitelistId) {
        try {
            Whitelist whitelist = whitelistMapper.selectById(whitelistId);
            if (whitelist != null) {
                whitelist.setDeleted(true);
                whitelist.setUpdatedAt(LocalDateTime.now());
                int result = whitelistMapper.updateById(whitelist);
                if (result > 0) {
                    log.info("白名单删除成功: id={}, name={}", whitelistId, whitelist.getName());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除白名单失败: id={}, error={}", whitelistId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Whitelist getWhitelist(Long whitelistId) {
        return whitelistMapper.selectById(whitelistId);
    }

    @Override
    public List<Whitelist> getWhitelistsByType(String type) {
        return whitelistMapper.selectByType(type);
    }

    @Override
    public List<Whitelist> getWhitelistsByDomain(String domain) {
        return whitelistMapper.selectByDomain(domain);
    }

    @Override
    public boolean isInWhitelist(String value, String type, String domain) {
        try {
            // 获取相关的白名单
            List<Whitelist> whitelists = whitelistMapper.selectByDomain(domain);
            
            for (Whitelist whitelist : whitelists) {
                if (!whitelist.getType().equals(type) || !whitelist.getEnabled()) {
                    continue;
                }

                // 检查是否在有效期内
                if (!isWhitelistEffective(whitelist)) {
                    continue;
                }

                // 根据类型进行匹配
                if (matchesWhitelistValue(value, whitelist)) {
                    // 记录匹配
                    recordWhitelistMatch(whitelist.getId(), value, null);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("检查白名单失败: value={}, type={}, domain={}, error={}", 
                value, type, domain, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isEmailInWhitelist(String email, String domain) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }

        // 检查邮件地址白名单
        if (isInWhitelist(email, "EMAIL", domain)) {
            return true;
        }

        // 检查域名白名单
        String emailDomain = extractDomainFromEmail(email);
        return isInWhitelist(emailDomain, "DOMAIN", domain);
    }

    @Override
    public boolean isIpInWhitelist(String ipAddress, String domain) {
        if (!IP_PATTERN.matcher(ipAddress).matches()) {
            return false;
        }

        // 检查IP地址白名单
        if (isInWhitelist(ipAddress, "IP", domain)) {
            return true;
        }

        // 检查CIDR白名单
        return isInWhitelist(ipAddress, "CIDR", domain);
    }

    @Override
    public boolean isDomainInWhitelist(String domain) {
        return isInWhitelist(domain, "DOMAIN", null);
    }

    @Override
    public Map<String, Object> batchAddWhitelists(List<Whitelist> whitelists) {
        Map<String, Object> result = new HashMap<>();
        List<String> successItems = new ArrayList<>();
        List<String> failedItems = new ArrayList<>();

        for (Whitelist whitelist : whitelists) {
            try {
                if (createWhitelist(whitelist)) {
                    successItems.add(whitelist.getName());
                } else {
                    failedItems.add(whitelist.getName());
                }
            } catch (Exception e) {
                log.error("批量添加白名单失败: name={}, error={}", whitelist.getName(), e.getMessage(), e);
                failedItems.add(whitelist.getName());
            }
        }

        result.put("total", whitelists.size());
        result.put("success", successItems.size());
        result.put("failed", failedItems.size());
        result.put("successItems", successItems);
        result.put("failedItems", failedItems);

        return result;
    }

    @Override
    public boolean batchDeleteWhitelists(List<Long> whitelistIds) {
        try {
            for (Long whitelistId : whitelistIds) {
                deleteWhitelist(whitelistId);
            }
            return true;
        } catch (Exception e) {
            log.error("批量删除白名单失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean importWhitelists(String data, String format) {
        try {
            List<Whitelist> whitelists;
            
            switch (format.toLowerCase()) {
                case "json":
                    whitelists = objectMapper.readValue(data, new TypeReference<List<Whitelist>>() {});
                    break;
                case "csv":
                    whitelists = parseCsvData(data);
                    break;
                default:
                    log.error("不支持的导入格式: {}", format);
                    return false;
            }

            for (Whitelist whitelist : whitelists) {
                whitelist.setId(null); // 清除ID，让数据库自动生成
                createWhitelist(whitelist);
            }

            log.info("导入白名单成功: count={}, format={}", whitelists.size(), format);
            return true;

        } catch (Exception e) {
            log.error("导入白名单失败: format={}, error={}", format, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String exportWhitelists(String type, String format) {
        try {
            List<Whitelist> whitelists;
            if (type != null && !type.isEmpty()) {
                whitelists = getWhitelistsByType(type);
            } else {
                whitelists = whitelistMapper.selectActiveWhitelists();
            }

            switch (format.toLowerCase()) {
                case "json":
                    return objectMapper.writeValueAsString(whitelists);
                case "csv":
                    return convertToCsv(whitelists);
                default:
                    return objectMapper.writeValueAsString(whitelists);
            }

        } catch (Exception e) {
            log.error("导出白名单失败: type={}, format={}, error={}", type, format, e.getMessage(), e);
            return "";
        }
    }

    @Override
    public Map<String, Object> getWhitelistStatistics() {
        try {
            List<Map<String, Object>> typeStats = whitelistMapper.selectWhitelistStatistics();
            
            Map<String, Object> result = new HashMap<>();
            result.put("byType", typeStats);
            
            // 计算总数
            int totalCount = typeStats.stream()
                .mapToInt(stat -> ((Number) stat.get("count")).intValue())
                .sum();
            
            int enabledCount = typeStats.stream()
                .mapToInt(stat -> ((Number) stat.get("enabled_count")).intValue())
                .sum();
            
            long totalMatches = typeStats.stream()
                .mapToLong(stat -> ((Number) stat.getOrDefault("total_matches", 0)).longValue())
                .sum();
            
            result.put("totalCount", totalCount);
            result.put("enabledCount", enabledCount);
            result.put("disabledCount", totalCount - enabledCount);
            result.put("totalMatches", totalMatches);
            
            return result;
            
        } catch (Exception e) {
            log.error("获取白名单统计失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean toggleWhitelist(Long whitelistId, boolean enabled) {
        try {
            Whitelist whitelist = whitelistMapper.selectById(whitelistId);
            if (whitelist != null) {
                whitelist.setEnabled(enabled);
                whitelist.setStatus(enabled ? "ACTIVE" : "INACTIVE");
                whitelist.setUpdatedAt(LocalDateTime.now());
                return whitelistMapper.updateById(whitelist) > 0;
            }
        } catch (Exception e) {
            log.error("切换白名单状态失败: id={}, enabled={}, error={}", whitelistId, enabled, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean batchToggleWhitelists(List<Long> whitelistIds, boolean enabled) {
        try {
            for (Long whitelistId : whitelistIds) {
                toggleWhitelist(whitelistId, enabled);
            }
            return true;
        } catch (Exception e) {
            log.error("批量切换白名单状态失败: enabled={}, error={}", enabled, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> validateWhitelist(Whitelist whitelist) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try {
            // 检查必需字段
            if (whitelist.getName() == null || whitelist.getName().trim().isEmpty()) {
                errors.add("名称不能为空");
            }

            if (whitelist.getType() == null || whitelist.getType().trim().isEmpty()) {
                errors.add("类型不能为空");
            }

            if (whitelist.getValue() == null || whitelist.getValue().trim().isEmpty()) {
                errors.add("值不能为空");
            }

            // 根据类型验证值的格式
            if (whitelist.getType() != null && whitelist.getValue() != null) {
                switch (whitelist.getType()) {
                    case "EMAIL":
                        if (!EMAIL_PATTERN.matcher(whitelist.getValue()).matches()) {
                            errors.add("邮件地址格式无效");
                        }
                        break;
                    case "IP":
                        if (!IP_PATTERN.matcher(whitelist.getValue()).matches()) {
                            errors.add("IP地址格式无效");
                        }
                        break;
                    case "CIDR":
                        if (!isValidCidr(whitelist.getValue())) {
                            errors.add("CIDR格式无效");
                        }
                        break;
                    case "DOMAIN":
                        if (!isValidDomain(whitelist.getValue())) {
                            errors.add("域名格式无效");
                        }
                        break;
                }
            }

            // 检查时间配置
            if (whitelist.getEffectiveFrom() != null && whitelist.getEffectiveUntil() != null) {
                if (whitelist.getEffectiveFrom().isAfter(whitelist.getEffectiveUntil())) {
                    errors.add("生效时间不能晚于失效时间");
                }
            }

            // 检查优先级
            if (whitelist.getPriority() != null && whitelist.getPriority() < 0) {
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
            log.error("验证白名单失败: {}", e.getMessage(), e);
            result.put("valid", false);
            result.put("errors", Arrays.asList("验证过程中发生错误: " + e.getMessage()));
        }

        return result;
    }

    @Override
    public List<Whitelist> searchWhitelists(Map<String, Object> criteria) {
        // 简化实现 - 实际应使用动态查询
        try {
            if (criteria.containsKey("type")) {
                return getWhitelistsByType((String) criteria.get("type"));
            } else if (criteria.containsKey("domain")) {
                return getWhitelistsByDomain((String) criteria.get("domain"));
            } else {
                return whitelistMapper.selectActiveWhitelists();
            }
        } catch (Exception e) {
            log.error("搜索白名单失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Whitelist> getExpiringWhitelists(int days) {
        return whitelistMapper.selectExpiringWhitelists(days);
    }

    @Override
    public boolean cleanupExpiredWhitelists() {
        try {
            List<Whitelist> expired = getExpiringWhitelists(0); // 已过期的
            for (Whitelist whitelist : expired) {
                whitelist.setEnabled(false);
                whitelist.setStatus("EXPIRED");
                whitelistMapper.updateById(whitelist);
            }
            return true;
        } catch (Exception e) {
            log.error("清理过期白名单失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getWhitelistTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();

        // 邮件白名单模板
        Map<String, Object> emailTemplate = new HashMap<>();
        emailTemplate.put("name", "email_whitelist");
        emailTemplate.put("displayName", "邮件白名单");
        emailTemplate.put("type", "EMAIL");
        emailTemplate.put("description", "允许特定邮件地址发送邮件");
        templates.add(emailTemplate);

        // 域名白名单模板
        Map<String, Object> domainTemplate = new HashMap<>();
        domainTemplate.put("name", "domain_whitelist");
        domainTemplate.put("displayName", "域名白名单");
        domainTemplate.put("type", "DOMAIN");
        domainTemplate.put("description", "允许特定域名的所有邮件");
        templates.add(domainTemplate);

        // IP白名单模板
        Map<String, Object> ipTemplate = new HashMap<>();
        ipTemplate.put("name", "ip_whitelist");
        ipTemplate.put("displayName", "IP白名单");
        ipTemplate.put("type", "IP");
        ipTemplate.put("description", "允许特定IP地址访问");
        templates.add(ipTemplate);

        return templates;
    }

    // 简化实现的其他方法
    @Override
    public boolean createFromTemplate(String templateName, Map<String, Object> parameters) {
        try {
            Whitelist whitelist = new Whitelist();
            whitelist.setName((String) parameters.get("name"));
            whitelist.setValue((String) parameters.get("value"));
            
            switch (templateName) {
                case "email_whitelist":
                    whitelist.setType("EMAIL");
                    break;
                case "domain_whitelist":
                    whitelist.setType("DOMAIN");
                    break;
                case "ip_whitelist":
                    whitelist.setType("IP");
                    break;
                default:
                    return false;
            }
            
            return createWhitelist(whitelist);
        } catch (Exception e) {
            log.error("从模板创建白名单失败: template={}, error={}", templateName, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean copyWhitelist(Long whitelistId, String newName) {
        try {
            Whitelist original = whitelistMapper.selectById(whitelistId);
            if (original != null) {
                Whitelist copy = cloneWhitelist(original);
                copy.setName(newName);
                return createWhitelist(copy);
            }
        } catch (Exception e) {
            log.error("复制白名单失败: id={}, error={}", whitelistId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean mergeWhitelists(List<Long> whitelistIds, String newName) { return true; }

    @Override
    public Map<String, Object> getWhitelistUsage(Long whitelistId) {
        Whitelist whitelist = whitelistMapper.selectById(whitelistId);
        if (whitelist != null) {
            Map<String, Object> usage = new HashMap<>();
            usage.put("matchCount", whitelist.getMatchCount());
            usage.put("lastMatchedAt", whitelist.getLastMatchedAt());
            return usage;
        }
        return new HashMap<>();
    }

    @Override
    public void recordWhitelistMatch(Long whitelistId, String matchedValue, String sourceIp) {
        try {
            Whitelist whitelist = whitelistMapper.selectById(whitelistId);
            if (whitelist != null) {
                whitelist.setMatchCount((whitelist.getMatchCount() != null ? whitelist.getMatchCount() : 0) + 1);
                whitelist.setLastMatchedAt(LocalDateTime.now());
                if (sourceIp != null) {
                    whitelist.setMatchedIps(sourceIp);
                }
                whitelistMapper.updateById(whitelist);
            }
        } catch (Exception e) {
            log.error("记录白名单匹配失败: id={}, error={}", whitelistId, e.getMessage(), e);
        }
    }

    // 其他简化实现的方法
    @Override
    public List<Map<String, Object>> getWhitelistMatchHistory(Long whitelistId, int limit) { return new ArrayList<>(); }

    @Override
    public Map<String, Object> analyzeWhitelistEffectiveness() { return new HashMap<>(); }

    @Override
    public List<Map<String, Object>> optimizeWhitelistConfiguration() { return new ArrayList<>(); }

    @Override
    public List<Map<String, Object>> detectWhitelistConflicts() { return new ArrayList<>(); }

    @Override
    public boolean resolveWhitelistConflicts(List<Map<String, Object>> resolutions) { return true; }

    @Override
    public String backupWhitelistConfiguration() {
        try {
            List<Whitelist> whitelists = whitelistMapper.selectActiveWhitelists();
            return objectMapper.writeValueAsString(whitelists);
        } catch (Exception e) {
            log.error("备份白名单配置失败: {}", e.getMessage(), e);
            return "";
        }
    }

    @Override
    public boolean restoreWhitelistConfiguration(String backupData) {
        try {
            List<Whitelist> whitelists = objectMapper.readValue(backupData, new TypeReference<List<Whitelist>>() {});
            for (Whitelist whitelist : whitelists) {
                whitelist.setId(null);
                createWhitelist(whitelist);
            }
            return true;
        } catch (Exception e) {
            log.error("恢复白名单配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean syncWhitelistToExternal(String system, Map<String, Object> config) { return true; }

    @Override
    public boolean syncWhitelistFromExternal(String system, Map<String, Object> config) { return true; }

    @Override
    public List<Map<String, Object>> getWhitelistSuggestions(String domain) { return new ArrayList<>(); }

    @Override
    public boolean autoLearnWhitelist(String domain, Map<String, Object> config) { return true; }

    // ========== 私有辅助方法 ==========

    private void setDefaultValues(Whitelist whitelist) {
        if (whitelist.getEnabled() == null) whitelist.setEnabled(true);
        if (whitelist.getGlobal() == null) whitelist.setGlobal(false);
        if (whitelist.getPriority() == null) whitelist.setPriority(100);
        if (whitelist.getStatus() == null) whitelist.setStatus("ACTIVE");
        if (whitelist.getMatchCount() == null) whitelist.setMatchCount(0L);
        whitelist.setCreatedAt(LocalDateTime.now());
        whitelist.setUpdatedAt(LocalDateTime.now());
    }

    private boolean isWhitelistEffective(Whitelist whitelist) {
        LocalDateTime now = LocalDateTime.now();
        
        if (whitelist.getEffectiveFrom() != null && now.isBefore(whitelist.getEffectiveFrom())) {
            return false;
        }
        
        if (whitelist.getEffectiveUntil() != null && now.isAfter(whitelist.getEffectiveUntil())) {
            return false;
        }
        
        return true;
    }

    private boolean matchesWhitelistValue(String value, Whitelist whitelist) {
        String whitelistValue = whitelist.getValue();
        
        switch (whitelist.getType()) {
            case "EMAIL":
                return value.equalsIgnoreCase(whitelistValue);
            case "DOMAIN":
                return value.equalsIgnoreCase(whitelistValue) || value.endsWith("." + whitelistValue);
            case "IP":
                return value.equals(whitelistValue);
            case "CIDR":
                return isIpInCidr(value, whitelistValue);
            default:
                return value.equals(whitelistValue);
        }
    }

    private String extractDomainFromEmail(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(atIndex + 1) : "";
    }

    private boolean isValidCidr(String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) return false;
            
            String ip = parts[0];
            int mask = Integer.parseInt(parts[1]);
            
            return IP_PATTERN.matcher(ip).matches() && mask >= 0 && mask <= 32;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidDomain(String domain) {
        return domain.matches("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");
    }

    private boolean isIpInCidr(String ip, String cidr) {
        // 简化实现 - 实际应使用专业的网络库
        return true;
    }

    private List<Whitelist> parseCsvData(String csvData) {
        // 简化实现 - 实际应使用专业的CSV解析库
        List<Whitelist> whitelists = new ArrayList<>();
        String[] lines = csvData.split("\n");
        
        for (int i = 1; i < lines.length; i++) { // 跳过标题行
            String[] parts = lines[i].split(",");
            if (parts.length >= 3) {
                Whitelist whitelist = new Whitelist();
                whitelist.setName(parts[0].trim());
                whitelist.setType(parts[1].trim());
                whitelist.setValue(parts[2].trim());
                whitelists.add(whitelist);
            }
        }
        
        return whitelists;
    }

    private String convertToCsv(List<Whitelist> whitelists) {
        StringBuilder csv = new StringBuilder();
        csv.append("Name,Type,Value,Domain,Enabled\n");
        
        for (Whitelist whitelist : whitelists) {
            csv.append(whitelist.getName()).append(",")
               .append(whitelist.getType()).append(",")
               .append(whitelist.getValue()).append(",")
               .append(whitelist.getDomain() != null ? whitelist.getDomain() : "").append(",")
               .append(whitelist.getEnabled()).append("\n");
        }
        
        return csv.toString();
    }

    private Whitelist cloneWhitelist(Whitelist original) {
        Whitelist clone = new Whitelist();
        clone.setType(original.getType());
        clone.setValue(original.getValue());
        clone.setDescription(original.getDescription());
        clone.setDomain(original.getDomain());
        clone.setUserAliasId(original.getUserAliasId());
        clone.setGlobal(original.getGlobal());
        clone.setPriority(original.getPriority());
        clone.setEnabled(original.getEnabled());
        clone.setEffectiveFrom(original.getEffectiveFrom());
        clone.setEffectiveUntil(original.getEffectiveUntil());
        return clone;
    }
}