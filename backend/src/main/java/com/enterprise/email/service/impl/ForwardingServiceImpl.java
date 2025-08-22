package com.enterprise.email.service.impl;

import com.enterprise.email.entity.ForwardingConfig;
import com.enterprise.email.mapper.ForwardingConfigMapper;
import com.enterprise.email.service.ForwardingService;
import com.enterprise.email.service.SmtpService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 邮件转发服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardingServiceImpl implements ForwardingService {

    private final ForwardingConfigMapper forwardingConfigMapper;
    private final SmtpService smtpService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 转发历史缓存
    private final Map<Long, List<Map<String, Object>>> forwardingHistory = new HashMap<>();
    // 转发队列
    private final List<Map<String, Object>> forwardingQueue = new ArrayList<>();

    @Override
    public boolean createForwardingConfig(ForwardingConfig config) {
        try {
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setEnabled(true);
            config.setStatus("ACTIVE");
            
            // 设置默认值
            setDefaultValues(config);
            
            int result = forwardingConfigMapper.insert(config);
            if (result > 0) {
                log.info("转发配置创建成功: {}", config.getName());
                return true;
            }
        } catch (Exception e) {
            log.error("创建转发配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateForwardingConfig(ForwardingConfig config) {
        try {
            config.setUpdatedAt(LocalDateTime.now());
            int result = forwardingConfigMapper.updateById(config);
            if (result > 0) {
                log.info("转发配置更新成功: {}", config.getName());
                return true;
            }
        } catch (Exception e) {
            log.error("更新转发配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteForwardingConfig(Long configId) {
        try {
            ForwardingConfig config = forwardingConfigMapper.selectById(configId);
            if (config != null) {
                config.setDeleted(true);
                config.setUpdatedAt(LocalDateTime.now());
                int result = forwardingConfigMapper.updateById(config);
                if (result > 0) {
                    log.info("转发配置删除成功: {}", config.getName());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除转发配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public ForwardingConfig getForwardingConfig(Long configId) {
        return forwardingConfigMapper.selectById(configId);
    }

    @Override
    public List<ForwardingConfig> getUserForwardingConfigs(Long userAliasId) {
        return forwardingConfigMapper.selectByUserAliasId(userAliasId);
    }

    @Override
    public List<ForwardingConfig> getUserActiveConfigs(Long userAliasId) {
        return forwardingConfigMapper.selectActiveByUserAliasId(userAliasId);
    }

    @Override
    public Map<String, Object> processEmailForwarding(String emailContent, Long userAliasId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> forwardedEmails = new ArrayList<>();
        
        try {
            List<ForwardingConfig> configs = getUserActiveConfigs(userAliasId);
            
            // 解析邮件内容
            Map<String, Object> emailData = parseEmailContent(emailContent);
            
            boolean emailForwarded = false;
            
            for (ForwardingConfig config : configs) {
                try {
                    // 检查是否在有效期内
                    if (!isConfigEffective(config)) {
                        continue;
                    }
                    
                    // 检查是否应该转发
                    Map<String, Object> shouldForward = shouldForwardEmail(config, emailData);
                    
                    if (Boolean.TRUE.equals(shouldForward.get("shouldForward"))) {
                        // 检查频率限制
                        if (!checkRateLimit(config)) {
                            updateSkipStatistics(config, "超出频率限制");
                            continue;
                        }
                        
                        // 转发邮件
                        boolean forwarded = forwardEmail(config, emailData);
                        
                        if (forwarded) {
                            Map<String, Object> forwardInfo = new HashMap<>();
                            forwardInfo.put("configId", config.getId());
                            forwardInfo.put("configName", config.getName());
                            forwardInfo.put("targetAddresses", parseTargetAddresses(config.getTargetAddresses()));
                            forwardInfo.put("forwardedAt", LocalDateTime.now());
                            forwardedEmails.add(forwardInfo);
                            
                            emailForwarded = true;
                            
                            // 更新统计信息
                            updateForwardingStatistics(config, true, null);
                            
                            // 记录转发历史
                            recordForwardingHistory(userAliasId, forwardInfo);
                        } else {
                            updateForwardingStatistics(config, false, "转发失败");
                        }
                    } else {
                        // 记录跳过原因
                        updateSkipStatistics(config, (String) shouldForward.get("reason"));
                    }
                } catch (Exception e) {
                    log.error("处理邮件转发失败: configId={}, error={}", config.getId(), e.getMessage(), e);
                    updateForwardingStatistics(config, false, e.getMessage());
                }
            }
            
            result.put("emailForwarded", emailForwarded);
            result.put("forwardedEmails", forwardedEmails);
            result.put("totalConfigs", configs.size());
            result.put("processedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("处理邮件转发失败: userAliasId={}, error={}", userAliasId, e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> testForwardingConfig(ForwardingConfig config, String emailContent) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证配置
            Map<String, Object> validation = validateForwardingConfig(config);
            if (!Boolean.TRUE.equals(validation.get("valid"))) {
                result.put("valid", false);
                result.put("errors", validation.get("errors"));
                return result;
            }
            
            // 解析邮件内容
            Map<String, Object> emailData = parseEmailContent(emailContent);
            
            // 检查是否应该转发
            Map<String, Object> shouldForward = shouldForwardEmail(config, emailData);
            result.put("shouldForward", shouldForward.get("shouldForward"));
            result.put("reason", shouldForward.get("reason"));
            
            if (Boolean.TRUE.equals(shouldForward.get("shouldForward"))) {
                // 生成预览内容
                Map<String, Object> preview = previewForwardedEmail(config, emailData);
                result.put("preview", preview);
                
                // 检测循环
                List<String> targetAddresses = parseTargetAddresses(config.getTargetAddresses());
                Map<String, Object> loopDetection = detectForwardingLoop(emailContent, targetAddresses);
                result.put("loopDetection", loopDetection);
            }
            
            result.put("emailData", emailData);
            result.put("configEffective", isConfigEffective(config));
            result.put("rateLimit", checkRateLimit(config));
            result.put("testedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("测试转发配置失败: {}", e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public boolean forwardEmail(ForwardingConfig config, Map<String, Object> emailData) {
        try {
            List<String> targetAddresses = parseTargetAddresses(config.getTargetAddresses());
            
            if (targetAddresses.isEmpty()) {
                log.warn("没有有效的转发地址: configId={}", config.getId());
                return false;
            }
            
            // 检测转发循环
            String originalContent = (String) emailData.get("content");
            Map<String, Object> loopCheck = detectForwardingLoop(originalContent, targetAddresses);
            if (Boolean.TRUE.equals(loopCheck.get("loopDetected"))) {
                log.warn("检测到转发循环: configId={}", config.getId());
                return false;
            }
            
            boolean allSuccess = true;
            
            for (String targetAddress : targetAddresses) {
                try {
                    // 构建转发邮件
                    Map<String, Object> forwardedEmail = buildForwardedEmail(config, emailData, targetAddress);
                    
                    // 发送邮件
                    boolean sent = smtpService.sendEmail(forwardedEmail);
                    
                    if (!sent) {
                        allSuccess = false;
                        log.error("转发邮件失败: to={}, configId={}", targetAddress, config.getId());
                    } else {
                        log.info("转发邮件成功: to={}, configId={}", targetAddress, config.getId());
                    }
                } catch (Exception e) {
                    allSuccess = false;
                    log.error("转发邮件到{}失败: {}", targetAddress, e.getMessage(), e);
                }
            }
            
            // 更新转发计数
            updateRateLimit(config);
            config.setLastForwardedAt(LocalDateTime.now());
            updateForwardingConfig(config);
            
            return allSuccess;
            
        } catch (Exception e) {
            log.error("转发邮件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> shouldForwardEmail(ForwardingConfig config, Map<String, Object> emailData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String fromEmail = (String) emailData.get("from");
            String subject = (String) emailData.get("subject");
            
            // 检查黑名单
            if (isInBlacklist(fromEmail, config)) {
                result.put("shouldForward", false);
                result.put("reason", "发件人在黑名单中");
                return result;
            }
            
            // 检查白名单（如果设置了白名单，只转发白名单中的邮件）
            if (hasWhitelist(config) && !isInWhitelist(fromEmail, config)) {
                result.put("shouldForward", false);
                result.put("reason", "发件人不在白名单中");
                return result;
            }
            
            // 检查时间段限制
            if (!isInActiveTimeRange(config)) {
                result.put("shouldForward", false);
                result.put("reason", "不在活跃时间段内");
                return result;
            }
            
            // 检查星期限制
            if (!isInActiveDays(config)) {
                result.put("shouldForward", false);
                result.put("reason", "不在活跃星期内");
                return result;
            }
            
            // 检查垃圾邮件和病毒邮件
            if (!config.getForwardSpam() && isSpamEmail(emailData)) {
                result.put("shouldForward", false);
                result.put("reason", "垃圾邮件不转发");
                return result;
            }
            
            if (!config.getForwardVirus() && isVirusEmail(emailData)) {
                result.put("shouldForward", false);
                result.put("reason", "病毒邮件不转发");
                return result;
            }
            
            // 检查邮件大小限制
            if (!checkSizeLimit(config, emailData)) {
                result.put("shouldForward", false);
                result.put("reason", "邮件大小超出限制");
                return result;
            }
            
            // 检查转发条件
            if (!checkForwardingConditions(config, emailData)) {
                result.put("shouldForward", false);
                result.put("reason", "不满足转发条件");
                return result;
            }
            
            result.put("shouldForward", true);
            result.put("reason", "满足所有条件");
            
        } catch (Exception e) {
            log.error("检查转发条件失败: {}", e.getMessage(), e);
            result.put("shouldForward", false);
            result.put("reason", "检查条件时发生错误: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> validateForwardingConfig(ForwardingConfig config) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        try {
            // 检查必需字段
            if (config.getName() == null || config.getName().trim().isEmpty()) {
                errors.add("名称不能为空");
            }
            
            if (config.getTargetAddresses() == null || config.getTargetAddresses().trim().isEmpty()) {
                errors.add("转发地址不能为空");
            } else {
                // 验证转发地址格式
                List<String> addresses = parseTargetAddresses(config.getTargetAddresses());
                Map<String, Object> addressValidation = validateForwardingAddresses(addresses);
                if (!Boolean.TRUE.equals(addressValidation.get("valid"))) {
                    errors.addAll((List<String>) addressValidation.get("errors"));
                }
            }
            
            // 检查时间配置
            if (config.getEffectiveFrom() != null && config.getEffectiveUntil() != null) {
                if (config.getEffectiveFrom().isAfter(config.getEffectiveUntil())) {
                    errors.add("开始时间不能晚于结束时间");
                }
            }
            
            // 检查频率限制
            if (config.getRateLimit() != null && config.getRateLimit() <= 0) {
                errors.add("频率限制必须大于0");
            }
            
            // 检查最大转发大小
            if (config.getMaxForwardSize() != null && config.getMaxForwardSize() <= 0) {
                errors.add("最大转发大小必须大于0");
            }
            
            // 检查最大跳数
            if (config.getMaxHops() != null && config.getMaxHops() <= 0) {
                errors.add("最大跳数必须大于0");
            }
            
            if (errors.isEmpty()) {
                result.put("valid", true);
                result.put("message", "配置验证通过");
            } else {
                result.put("valid", false);
                result.put("errors", errors);
            }
            
        } catch (Exception e) {
            log.error("验证转发配置失败: {}", e.getMessage(), e);
            result.put("valid", false);
            result.put("errors", Arrays.asList("验证过程中发生错误: " + e.getMessage()));
        }
        
        return result;
    }

    @Override
    public boolean toggleForwarding(Long configId, boolean enabled) {
        try {
            ForwardingConfig config = getForwardingConfig(configId);
            if (config != null) {
                config.setEnabled(enabled);
                config.setStatus(enabled ? "ACTIVE" : "INACTIVE");
                return updateForwardingConfig(config);
            }
        } catch (Exception e) {
            log.error("切换转发状态失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean batchToggleForwarding(List<Long> configIds, boolean enabled) {
        try {
            for (Long configId : configIds) {
                toggleForwarding(configId, enabled);
            }
            return true;
        } catch (Exception e) {
            log.error("批量切换转发状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getForwardingTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();
        
        // 简单转发模板
        Map<String, Object> simple = new HashMap<>();
        simple.put("name", "simple_forward");
        simple.put("displayName", "简单转发");
        simple.put("category", "basic");
        simple.put("description", "将所有邮件转发到指定地址");
        simple.put("parameters", Arrays.asList("target_address", "keep_copy"));
        templates.add(simple);
        
        // 条件转发模板
        Map<String, Object> conditional = new HashMap<>();
        conditional.put("name", "conditional_forward");
        conditional.put("displayName", "条件转发");
        conditional.put("category", "advanced");
        conditional.put("description", "根据条件转发邮件");
        conditional.put("parameters", Arrays.asList("target_address", "condition_type", "condition_value", "keep_copy"));
        templates.add(conditional);
        
        // 备份转发模板
        Map<String, Object> backup = new HashMap<>();
        backup.put("name", "backup_forward");
        backup.put("displayName", "备份转发");
        backup.put("category", "backup");
        backup.put("description", "将邮件副本发送到备份地址");
        backup.put("parameters", Arrays.asList("backup_address"));
        templates.add(backup);
        
        // 分发转发模板
        Map<String, Object> distribution = new HashMap<>();
        distribution.put("name", "distribution_forward");
        distribution.put("displayName", "分发转发");
        distribution.put("category", "distribution");
        distribution.put("description", "将邮件转发到多个地址");
        distribution.put("parameters", Arrays.asList("target_addresses", "subject_prefix"));
        templates.add(distribution);
        
        return templates;
    }

    @Override
    public boolean createForwardingFromTemplate(Long userAliasId, String templateName, Map<String, Object> parameters) {
        try {
            ForwardingConfig config = new ForwardingConfig();
            config.setUserAliasId(userAliasId);
            
            switch (templateName) {
                case "simple_forward":
                    createSimpleForwardTemplate(config, parameters);
                    break;
                case "conditional_forward":
                    createConditionalForwardTemplate(config, parameters);
                    break;
                case "backup_forward":
                    createBackupForwardTemplate(config, parameters);
                    break;
                case "distribution_forward":
                    createDistributionForwardTemplate(config, parameters);
                    break;
                default:
                    log.error("未知的转发模板: {}", templateName);
                    return false;
            }
            
            return createForwardingConfig(config);
            
        } catch (Exception e) {
            log.error("从模板创建转发配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean copyForwardingConfig(Long configId, Long targetUserAliasId) {
        try {
            ForwardingConfig originalConfig = getForwardingConfig(configId);
            if (originalConfig != null) {
                ForwardingConfig newConfig = cloneConfig(originalConfig);
                newConfig.setUserAliasId(targetUserAliasId);
                newConfig.setName(newConfig.getName() + " (复制)");
                return createForwardingConfig(newConfig);
            }
        } catch (Exception e) {
            log.error("复制转发配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean importForwardingConfigs(Long userAliasId, String configData) {
        try {
            List<Map<String, Object>> configs = objectMapper.readValue(configData, new TypeReference<List<Map<String, Object>>>() {});
            
            for (Map<String, Object> configMap : configs) {
                ForwardingConfig config = convertToConfig(userAliasId, configMap);
                createForwardingConfig(config);
            }
            
            log.info("导入转发配置成功: userAliasId={}, count={}", userAliasId, configs.size());
            return true;
            
        } catch (Exception e) {
            log.error("导入转发配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String exportForwardingConfigs(Long userAliasId) {
        try {
            List<ForwardingConfig> configs = getUserForwardingConfigs(userAliasId);
            List<Map<String, Object>> exportData = new ArrayList<>();
            
            for (ForwardingConfig config : configs) {
                Map<String, Object> configMap = convertToMap(config);
                exportData.add(configMap);
            }
            
            return objectMapper.writeValueAsString(exportData);
            
        } catch (Exception e) {
            log.error("导出转发配置失败: {}", e.getMessage(), e);
            return "";
        }
    }

    @Override
    public Map<String, Object> getForwardingStatistics(Long userAliasId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<ForwardingConfig> configs = getUserForwardingConfigs(userAliasId);
            
            long totalConfigs = configs.size();
            long activeConfigs = configs.stream().mapToLong(c -> c.getEnabled() ? 1 : 0).sum();
            long totalForwarded = configs.stream().mapToLong(c -> c.getTotalForwarded() != null ? c.getTotalForwarded() : 0).sum();
            long successfulForwards = configs.stream().mapToLong(c -> c.getSuccessfulForwards() != null ? c.getSuccessfulForwards() : 0).sum();
            long failedForwards = configs.stream().mapToLong(c -> c.getFailedForwards() != null ? c.getFailedForwards() : 0).sum();
            long skippedCount = configs.stream().mapToLong(c -> c.getSkippedCount() != null ? c.getSkippedCount() : 0).sum();
            
            stats.put("totalConfigs", totalConfigs);
            stats.put("activeConfigs", activeConfigs);
            stats.put("inactiveConfigs", totalConfigs - activeConfigs);
            stats.put("totalForwarded", totalForwarded);
            stats.put("successfulForwards", successfulForwards);
            stats.put("failedForwards", failedForwards);
            stats.put("skippedCount", skippedCount);
            
            if (totalForwarded > 0) {
                double successRate = (double) successfulForwards / totalForwarded * 100;
                stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
            } else {
                stats.put("successRate", 0.0);
            }
            
        } catch (Exception e) {
            log.error("获取转发统计失败: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    @Override
    public List<Map<String, Object>> getForwardingHistory(Long userAliasId, int limit) {
        List<Map<String, Object>> history = forwardingHistory.get(userAliasId);
        if (history == null) {
            return new ArrayList<>();
        }
        
        int start = Math.max(0, history.size() - limit);
        return new ArrayList<>(history.subList(start, history.size()));
    }

    @Override
    public boolean cleanupForwardingHistory(Long userAliasId, int days) {
        try {
            List<Map<String, Object>> history = forwardingHistory.get(userAliasId);
            if (history != null) {
                LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
                history.removeIf(record -> {
                    LocalDateTime recordTime = (LocalDateTime) record.get("forwardedAt");
                    return recordTime != null && recordTime.isBefore(cutoff);
                });
            }
            return true;
        } catch (Exception e) {
            log.error("清理转发历史失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean resetForwardingCounters(Long configId) {
        try {
            ForwardingConfig config = getForwardingConfig(configId);
            if (config != null) {
                config.setTotalForwarded(0L);
                config.setSuccessfulForwards(0L);
                config.setFailedForwards(0L);
                config.setSkippedCount(0L);
                config.setCurrentHourCount(0);
                return updateForwardingConfig(config);
            }
        } catch (Exception e) {
            log.error("重置转发计数器失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean batchResetCounters(List<Long> configIds) {
        try {
            for (Long configId : configIds) {
                resetForwardingCounters(configId);
            }
            return true;
        } catch (Exception e) {
            log.error("批量重置转发计数器失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> detectForwardingLoop(String emailContent, List<String> targetAddresses) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查X-Forwarded-For头部
            String forwardedHeader = extractHeader(emailContent, "X-Forwarded-For");
            if (forwardedHeader != null) {
                String[] forwardedAddresses = forwardedHeader.split(",");
                
                for (String targetAddress : targetAddresses) {
                    for (String forwarded : forwardedAddresses) {
                        if (targetAddress.trim().equalsIgnoreCase(forwarded.trim())) {
                            result.put("loopDetected", true);
                            result.put("loopAddress", targetAddress);
                            result.put("reason", "目标地址已在转发链中");
                            return result;
                        }
                    }
                }
                
                // 检查跳数
                if (forwardedAddresses.length > 10) { // 最大跳数限制
                    result.put("loopDetected", true);
                    result.put("reason", "转发跳数超过限制");
                    return result;
                }
            }
            
            result.put("loopDetected", false);
            result.put("reason", "未检测到循环");
            
        } catch (Exception e) {
            log.error("检测转发循环失败: {}", e.getMessage(), e);
            result.put("loopDetected", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> validateForwardingAddresses(List<String> addresses) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> validAddresses = new ArrayList<>();
        List<String> invalidAddresses = new ArrayList<>();
        
        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        
        for (String address : addresses) {
            if (address == null || address.trim().isEmpty()) {
                errors.add("邮件地址不能为空");
                continue;
            }
            
            String trimmedAddress = address.trim();
            if (emailPattern.matcher(trimmedAddress).matches()) {
                validAddresses.add(trimmedAddress);
            } else {
                invalidAddresses.add(trimmedAddress);
                errors.add("无效的邮件地址: " + trimmedAddress);
            }
        }
        
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("validAddresses", validAddresses);
        result.put("invalidAddresses", invalidAddresses);
        result.put("totalAddresses", addresses.size());
        result.put("validCount", validAddresses.size());
        result.put("invalidCount", invalidAddresses.size());
        
        return result;
    }

    // 简化实现的其他方法
    @Override
    public Map<String, Object> getForwardingPerformanceMetrics(Long userAliasId) { return new HashMap<>(); }

    @Override
    public boolean optimizeForwardingPerformance(Long userAliasId) { return true; }

    @Override
    public List<Map<String, Object>> detectForwardingConflicts(Long userAliasId) { return new ArrayList<>(); }

    @Override
    public boolean resolveForwardingConflicts(Long userAliasId, List<Map<String, Object>> resolutions) { return true; }

    @Override
    public Map<String, Object> previewForwardedEmail(ForwardingConfig config, Map<String, Object> originalEmail) {
        Map<String, Object> preview = new HashMap<>();
        
        try {
            List<String> targetAddresses = parseTargetAddresses(config.getTargetAddresses());
            
            preview.put("targetAddresses", targetAddresses);
            preview.put("subject", generateForwardedSubject(config, (String) originalEmail.get("subject")));
            preview.put("content", generateForwardedContent(config, originalEmail));
            preview.put("keepLocalCopy", config.getKeepLocalCopy());
            preview.put("forwardAttachments", config.getForwardAttachments());
            
        } catch (Exception e) {
            log.error("预览转发邮件失败: {}", e.getMessage(), e);
            preview.put("error", e.getMessage());
        }
        
        return preview;
    }

    @Override
    public Map<String, Object> batchProcessForwarding(List<String> emails, Long userAliasId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (String email : emails) {
            Map<String, Object> emailResult = processEmailForwarding(email, userAliasId);
            results.add(emailResult);
        }
        
        result.put("totalEmails", emails.size());
        result.put("results", results);
        return result;
    }

    // 其他简化实现的方法
    @Override
    public boolean setGlobalForwarding(Long userAliasId, Map<String, Object> globalConfig) { return true; }

    @Override
    public Map<String, Object> getGlobalForwardingConfig(Long userAliasId) { return new HashMap<>(); }

    @Override
    public boolean stopGlobalForwarding(Long userAliasId) { return true; }

    @Override
    public boolean setTemporaryForwarding(Long userAliasId, Map<String, Object> tempConfig) { return true; }

    @Override
    public Map<String, Object> getTemporaryForwardingConfig(Long userAliasId) { return new HashMap<>(); }

    @Override
    public boolean stopTemporaryForwarding(Long userAliasId) { return true; }

    @Override
    public List<Map<String, Object>> getForwardingSuggestions(Long userAliasId, List<String> sampleEmails) { return new ArrayList<>(); }

    @Override
    public boolean autoConfigureForwarding(Long userAliasId, Map<String, Object> preferences) { return true; }

    @Override
    public Map<String, Object> getForwardingDebugInfo(Long configId, String emailContent) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            ForwardingConfig config = getForwardingConfig(configId);
            if (config != null) {
                debugInfo = testForwardingConfig(config, emailContent);
                debugInfo.put("debugMode", true);
            }
        } catch (Exception e) {
            log.error("获取转发调试信息失败: {}", e.getMessage(), e);
            debugInfo.put("error", e.getMessage());
        }
        
        return debugInfo;
    }

    @Override
    public boolean toggleForwardingDebugMode(Long configId, boolean debugMode) { return true; }

    @Override
    public String generateForwardingReport(Long userAliasId, String startDate, String endDate) { return ""; }

    @Override
    public Map<String, Object> getRealTimeForwardingStatus(Long userAliasId) { return new HashMap<>(); }

    @Override
    public Map<String, Object> testForwardingConnectivity(List<String> targetAddresses) { return new HashMap<>(); }

    @Override
    public Map<String, Object> getForwardingQueueStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("queueSize", forwardingQueue.size());
        status.put("queuedAt", LocalDateTime.now());
        return status;
    }

    @Override
    public boolean cleanupForwardingQueue() {
        forwardingQueue.clear();
        return true;
    }

    @Override
    public boolean retryFailedForwards(Long userAliasId) { return true; }

    @Override
    public String backupForwardingConfigs(Long userAliasId) { return exportForwardingConfigs(userAliasId); }

    @Override
    public boolean restoreForwardingConfigs(Long userAliasId, String backupData) { return importForwardingConfigs(userAliasId, backupData); }

    // ========== 私有辅助方法 ==========

    private void setDefaultValues(ForwardingConfig config) {
        if (config.getForwardingType() == null) config.setForwardingType("SIMPLE");
        if (config.getKeepLocalCopy() == null) config.setKeepLocalCopy(true);
        if (config.getConditionType() == null) config.setConditionType("ALL");
        if (config.getForwardAttachments() == null) config.setForwardAttachments(true);
        if (config.getMaxForwardSize() == null) config.setMaxForwardSize(25); // 25MB
        if (config.getHeaderHandling() == null) config.setHeaderHandling("PRESERVE");
        if (config.getRateLimit() == null) config.setRateLimit(100); // 每小时100次
        if (config.getCurrentHourCount() == null) config.setCurrentHourCount(0);
        if (config.getForwardSpam() == null) config.setForwardSpam(false);
        if (config.getForwardVirus() == null) config.setForwardVirus(false);
        if (config.getLoopDetection() == null) config.setLoopDetection(true);
        if (config.getMaxHops() == null) config.setMaxHops(10);
        if (config.getTotalForwarded() == null) config.setTotalForwarded(0L);
        if (config.getSuccessfulForwards() == null) config.setSuccessfulForwards(0L);
        if (config.getFailedForwards() == null) config.setFailedForwards(0L);
        if (config.getSkippedCount() == null) config.setSkippedCount(0L);
        if (config.getPriority() == null) config.setPriority(100);
    }

    private Map<String, Object> parseEmailContent(String emailContent) {
        Map<String, Object> emailData = new HashMap<>();
        
        try {
            String[] lines = emailContent.split("\n");
            Map<String, String> headers = new HashMap<>();
            StringBuilder body = new StringBuilder();
            boolean inBody = false;
            
            for (String line : lines) {
                if (!inBody && line.trim().isEmpty()) {
                    inBody = true;
                    continue;
                }
                
                if (!inBody) {
                    int colonIndex = line.indexOf(':');
                    if (colonIndex > 0) {
                        String headerName = line.substring(0, colonIndex).trim().toLowerCase();
                        String headerValue = line.substring(colonIndex + 1).trim();
                        headers.put(headerName, headerValue);
                    }
                } else {
                    body.append(line).append("\n");
                }
            }
            
            emailData.put("headers", headers);
            emailData.put("content", emailContent);
            emailData.put("body", body.toString());
            emailData.put("from", headers.get("from"));
            emailData.put("to", headers.get("to"));
            emailData.put("subject", headers.get("subject"));
            emailData.put("date", headers.get("date"));
            emailData.put("size", emailContent.length());
            
        } catch (Exception e) {
            log.error("解析邮件内容失败: {}", e.getMessage(), e);
        }
        
        return emailData;
    }

    private boolean isConfigEffective(ForwardingConfig config) {
        LocalDateTime now = LocalDateTime.now();
        
        if (config.getEffectiveFrom() != null && now.isBefore(config.getEffectiveFrom())) {
            return false;
        }
        
        if (config.getEffectiveUntil() != null && now.isAfter(config.getEffectiveUntil())) {
            return false;
        }
        
        return true;
    }

    private boolean checkRateLimit(ForwardingConfig config) {
        if (config.getRateLimit() == null) {
            return true;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentHourStart = config.getCurrentHourStart();
        
        // 检查是否需要重置计数器（新的小时开始）
        if (currentHourStart == null || now.getHour() != currentHourStart.getHour()) {
            config.setCurrentHourCount(0);
            config.setCurrentHourStart(now.withMinute(0).withSecond(0).withNano(0));
        }
        
        return config.getCurrentHourCount() < config.getRateLimit();
    }

    private void updateRateLimit(ForwardingConfig config) {
        config.setCurrentHourCount(config.getCurrentHourCount() + 1);
    }

    private List<String> parseTargetAddresses(String targetAddressesJson) {
        try {
            if (targetAddressesJson == null || targetAddressesJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            // 支持JSON数组格式或逗号分隔格式
            if (targetAddressesJson.trim().startsWith("[")) {
                return objectMapper.readValue(targetAddressesJson, new TypeReference<List<String>>() {});
            } else {
                return Arrays.asList(targetAddressesJson.split(","));
            }
        } catch (Exception e) {
            log.error("解析转发地址失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private boolean isInBlacklist(String email, ForwardingConfig config) {
        if (config.getBlacklistSenders() != null) {
            List<String> blacklistSenders = Arrays.asList(config.getBlacklistSenders().split(","));
            if (blacklistSenders.contains(email.trim())) {
                return true;
            }
        }
        
        if (config.getBlacklistDomains() != null) {
            String domain = extractDomain(email);
            List<String> blacklistDomains = Arrays.asList(config.getBlacklistDomains().split(","));
            return blacklistDomains.contains(domain);
        }
        
        return false;
    }

    private boolean hasWhitelist(ForwardingConfig config) {
        return (config.getWhitelistSenders() != null && !config.getWhitelistSenders().trim().isEmpty()) ||
               (config.getWhitelistDomains() != null && !config.getWhitelistDomains().trim().isEmpty());
    }

    private boolean isInWhitelist(String email, ForwardingConfig config) {
        if (config.getWhitelistSenders() != null) {
            List<String> whitelistSenders = Arrays.asList(config.getWhitelistSenders().split(","));
            if (whitelistSenders.contains(email.trim())) {
                return true;
            }
        }
        
        if (config.getWhitelistDomains() != null) {
            String domain = extractDomain(email);
            List<String> whitelistDomains = Arrays.asList(config.getWhitelistDomains().split(","));
            return whitelistDomains.contains(domain);
        }
        
        return false;
    }

    private boolean isInActiveTimeRange(ForwardingConfig config) {
        if (config.getActiveHours() == null || config.getActiveHours().trim().isEmpty()) {
            return true;
        }
        
        try {
            Map<String, Object> activeHours = objectMapper.readValue(config.getActiveHours(), Map.class);
            String startTime = (String) activeHours.get("start");
            String endTime = (String) activeHours.get("end");
            
            LocalTime now = LocalTime.now();
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            
            return now.isAfter(start) && now.isBefore(end);
        } catch (Exception e) {
            log.error("检查活跃时间段失败: {}", e.getMessage(), e);
            return true;
        }
    }

    private boolean isInActiveDays(ForwardingConfig config) {
        if (config.getActiveDays() == null || config.getActiveDays().trim().isEmpty()) {
            return true;
        }
        
        try {
            List<Integer> activeDays = objectMapper.readValue(config.getActiveDays(), List.class);
            int currentDay = LocalDateTime.now().getDayOfWeek().getValue();
            return activeDays.contains(currentDay);
        } catch (Exception e) {
            log.error("检查活跃星期失败: {}", e.getMessage(), e);
            return true;
        }
    }

    private boolean isSpamEmail(Map<String, Object> emailData) {
        // 简化实现，实际应该集成垃圾邮件检测
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) emailData.get("headers");
        String spamScore = headers.get("x-spam-score");
        return spamScore != null && Double.parseDouble(spamScore) > 5.0;
    }

    private boolean isVirusEmail(Map<String, Object> emailData) {
        // 简化实现，实际应该集成病毒检测
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) emailData.get("headers");
        String virusStatus = headers.get("x-virus-status");
        return "INFECTED".equals(virusStatus);
    }

    private boolean checkSizeLimit(ForwardingConfig config, Map<String, Object> emailData) {
        if (config.getMaxForwardSize() == null) {
            return true;
        }
        
        int emailSize = (Integer) emailData.get("size");
        int maxSizeBytes = config.getMaxForwardSize() * 1024 * 1024; // 转换为字节
        
        return emailSize <= maxSizeBytes;
    }

    private boolean checkForwardingConditions(ForwardingConfig config, Map<String, Object> emailData) {
        if ("ALL".equals(config.getConditionType())) {
            return true;
        }
        
        try {
            List<Map<String, Object>> conditions = objectMapper.readValue(
                config.getConditions(), new TypeReference<List<Map<String, Object>>>() {}
            );
            
            for (Map<String, Object> condition : conditions) {
                if (!evaluateForwardingCondition(condition, emailData)) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("检查转发条件失败: {}", e.getMessage(), e);
            return true;
        }
    }

    private boolean evaluateForwardingCondition(Map<String, Object> condition, Map<String, Object> emailData) {
        String type = (String) condition.get("type");
        String operator = (String) condition.get("operator");
        String value = (String) condition.get("value");
        
        switch (type) {
            case "subject":
                String subject = (String) emailData.get("subject");
                return evaluateStringCondition(subject, value, operator);
            case "sender":
                String sender = (String) emailData.get("from");
                return evaluateStringCondition(sender, value, operator);
            case "header":
                String headerName = (String) condition.get("header");
                @SuppressWarnings("unchecked")
                Map<String, String> headers = (Map<String, String>) emailData.get("headers");
                String headerValue = headers.get(headerName.toLowerCase());
                return evaluateStringCondition(headerValue, value, operator);
            case "body":
                String body = (String) emailData.get("body");
                return evaluateStringCondition(body, value, operator);
            case "size":
                Integer size = (Integer) emailData.get("size");
                Integer targetSize = Integer.valueOf(value);
                return compareSizes(size, targetSize, operator);
            default:
                return true;
        }
    }

    private boolean evaluateStringCondition(String actual, String expected, String operator) {
        if (actual == null) {
            return false;
        }
        
        switch (operator) {
            case "equals":
                return actual.equals(expected);
            case "contains":
                return actual.contains(expected);
            case "startsWith":
                return actual.startsWith(expected);
            case "endsWith":
                return actual.endsWith(expected);
            case "matches":
                return actual.matches(expected);
            default:
                return false;
        }
    }

    private boolean compareSizes(Integer actual, Integer expected, String operator) {
        if (actual == null || expected == null) {
            return false;
        }
        
        switch (operator) {
            case "greater":
                return actual > expected;
            case "less":
                return actual < expected;
            case "equals":
                return actual.equals(expected);
            default:
                return false;
        }
    }

    private Map<String, Object> buildForwardedEmail(ForwardingConfig config, Map<String, Object> emailData, String targetAddress) {
        Map<String, Object> forwardedEmail = new HashMap<>();
        
        forwardedEmail.put("to", targetAddress);
        forwardedEmail.put("from", emailData.get("from"));
        forwardedEmail.put("subject", generateForwardedSubject(config, (String) emailData.get("subject")));
        forwardedEmail.put("content", generateForwardedContent(config, emailData));
        forwardedEmail.put("contentType", "text/plain");
        
        // 添加转发头部
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("X-Forwarded-For", (String) emailData.get("from"));
        customHeaders.put("X-Forwarded-By", "Email Forwarding Service");
        forwardedEmail.put("headers", customHeaders);
        
        return forwardedEmail;
    }

    private String generateForwardedSubject(ForwardingConfig config, String originalSubject) {
        if (config.getSubjectPrefix() != null && !config.getSubjectPrefix().isEmpty()) {
            return config.getSubjectPrefix() + " " + (originalSubject != null ? originalSubject : "");
        }
        return originalSubject;
    }

    private String generateForwardedContent(ForwardingConfig config, Map<String, Object> emailData) {
        StringBuilder content = new StringBuilder();
        
        // 添加内容前缀
        if (config.getContentPrefix() != null && !config.getContentPrefix().isEmpty()) {
            content.append(config.getContentPrefix()).append("\n\n");
        }
        
        // 添加原始邮件内容
        content.append((String) emailData.get("content"));
        
        // 添加内容后缀
        if (config.getContentSuffix() != null && !config.getContentSuffix().isEmpty()) {
            content.append("\n\n").append(config.getContentSuffix());
        }
        
        return content.toString();
    }

    private void updateForwardingStatistics(ForwardingConfig config, boolean success, String error) {
        try {
            config.setTotalForwarded(config.getTotalForwarded() + 1);
            
            if (success) {
                config.setSuccessfulForwards(config.getSuccessfulForwards() + 1);
                config.setLastForwardedAt(LocalDateTime.now());
            } else {
                config.setFailedForwards(config.getFailedForwards() + 1);
                config.setLastError(error);
                config.setLastErrorAt(LocalDateTime.now());
            }
            
            updateForwardingConfig(config);
        } catch (Exception e) {
            log.error("更新转发统计失败: {}", e.getMessage(), e);
        }
    }

    private void updateSkipStatistics(ForwardingConfig config, String reason) {
        try {
            config.setSkippedCount(config.getSkippedCount() + 1);
            updateForwardingConfig(config);
        } catch (Exception e) {
            log.error("更新跳过统计失败: {}", e.getMessage(), e);
        }
    }

    private void recordForwardingHistory(Long userAliasId, Map<String, Object> forwardInfo) {
        List<Map<String, Object>> history = forwardingHistory.computeIfAbsent(userAliasId, k -> new ArrayList<>());
        history.add(forwardInfo);
        
        // 保持历史记录在合理范围内
        if (history.size() > 1000) {
            history.subList(0, history.size() - 1000).clear();
        }
    }

    private String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(atIndex + 1) : "";
    }

    private String extractHeader(String emailContent, String headerName) {
        String[] lines = emailContent.split("\n");
        String lowerHeaderName = headerName.toLowerCase();
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                break; // 头部结束
            }
            
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String currentHeaderName = line.substring(0, colonIndex).trim().toLowerCase();
                if (currentHeaderName.equals(lowerHeaderName)) {
                    return line.substring(colonIndex + 1).trim();
                }
            }
        }
        
        return null;
    }

    // 模板创建方法的简化实现
    private void createSimpleForwardTemplate(ForwardingConfig config, Map<String, Object> params) {
        config.setName("简单转发");
        config.setForwardingType("SIMPLE");
        config.setTargetAddresses("[\"" + params.get("target_address") + "\"]");
        config.setKeepLocalCopy(Boolean.parseBoolean(params.get("keep_copy").toString()));
    }
    
    private void createConditionalForwardTemplate(ForwardingConfig config, Map<String, Object> params) {
        config.setName("条件转发");
        config.setForwardingType("CONDITIONAL");
        // 简化实现
    }
    
    private void createBackupForwardTemplate(ForwardingConfig config, Map<String, Object> params) {
        config.setName("备份转发");
        config.setForwardingType("COPY");
        // 简化实现
    }
    
    private void createDistributionForwardTemplate(ForwardingConfig config, Map<String, Object> params) {
        config.setName("分发转发");
        config.setForwardingType("REDIRECT");
        // 简化实现
    }
    
    private ForwardingConfig cloneConfig(ForwardingConfig original) {
        ForwardingConfig clone = new ForwardingConfig();
        clone.setName(original.getName());
        clone.setDescription(original.getDescription());
        clone.setForwardingType(original.getForwardingType());
        clone.setTargetAddresses(original.getTargetAddresses());
        clone.setKeepLocalCopy(original.getKeepLocalCopy());
        clone.setConditionType(original.getConditionType());
        clone.setConditions(original.getConditions());
        clone.setForwardAttachments(original.getForwardAttachments());
        clone.setMaxForwardSize(original.getMaxForwardSize());
        clone.setSubjectPrefix(original.getSubjectPrefix());
        clone.setContentPrefix(original.getContentPrefix());
        clone.setContentSuffix(original.getContentSuffix());
        return clone;
    }
    
    private ForwardingConfig convertToConfig(Long userAliasId, Map<String, Object> configMap) {
        ForwardingConfig config = new ForwardingConfig();
        config.setUserAliasId(userAliasId);
        config.setName((String) configMap.get("name"));
        // 简化实现
        return config;
    }
    
    private Map<String, Object> convertToMap(ForwardingConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", config.getName());
        map.put("description", config.getDescription());
        map.put("forwardingType", config.getForwardingType());
        map.put("targetAddresses", config.getTargetAddresses());
        // 简化实现
        return map;
    }
}