package com.enterprise.email.service.impl;

import com.enterprise.email.entity.AutoresponderConfig;
import com.enterprise.email.mapper.AutoresponderConfigMapper;
import com.enterprise.email.service.AutoresponderService;
import com.enterprise.email.service.SmtpService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动回复服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoresponderServiceImpl implements AutoresponderService {

    private final AutoresponderConfigMapper autoresponderConfigMapper;
    private final SmtpService smtpService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 回复历史缓存
    private final Map<Long, List<Map<String, Object>>> replyHistory = new HashMap<>();
    // 发送记录缓存
    private final Map<String, Map<String, LocalDateTime>> sendRecords = new HashMap<>();

    @Override
    public boolean createAutoresponderConfig(AutoresponderConfig config) {
        try {
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setEnabled(true);
            config.setStatus("ACTIVE");
            
            // 设置默认值
            setDefaultValues(config);
            
            int result = autoresponderConfigMapper.insert(config);
            if (result > 0) {
                log.info("自动回复配置创建成功: {}", config.getName());
                return true;
            }
        } catch (Exception e) {
            log.error("创建自动回复配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateAutoresponderConfig(AutoresponderConfig config) {
        try {
            config.setUpdatedAt(LocalDateTime.now());
            int result = autoresponderConfigMapper.updateById(config);
            if (result > 0) {
                log.info("自动回复配置更新成功: {}", config.getName());
                return true;
            }
        } catch (Exception e) {
            log.error("更新自动回复配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteAutoresponderConfig(Long configId) {
        try {
            AutoresponderConfig config = autoresponderConfigMapper.selectById(configId);
            if (config != null) {
                config.setDeleted(true);
                config.setUpdatedAt(LocalDateTime.now());
                int result = autoresponderConfigMapper.updateById(config);
                if (result > 0) {
                    log.info("自动回复配置删除成功: {}", config.getName());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除自动回复配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public AutoresponderConfig getAutoresponderConfig(Long configId) {
        return autoresponderConfigMapper.selectById(configId);
    }

    @Override
    public List<AutoresponderConfig> getUserAutoresponderConfigs(Long userAliasId) {
        return autoresponderConfigMapper.selectByUserAliasId(userAliasId);
    }

    @Override
    public List<AutoresponderConfig> getUserActiveConfigs(Long userAliasId) {
        return autoresponderConfigMapper.selectActiveByUserAliasId(userAliasId);
    }

    @Override
    public Map<String, Object> processAutoReply(String emailContent, Long userAliasId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> sentReplies = new ArrayList<>();
        
        try {
            List<AutoresponderConfig> configs = getUserActiveConfigs(userAliasId);
            
            // 解析邮件内容
            Map<String, Object> emailData = parseEmailContent(emailContent);
            
            boolean replySent = false;
            
            for (AutoresponderConfig config : configs) {
                try {
                    // 检查是否在有效期内
                    if (!isConfigEffective(config)) {
                        continue;
                    }
                    
                    // 检查是否应该发送自动回复
                    Map<String, Object> shouldReply = shouldSendAutoReply(config, emailData);
                    
                    if (Boolean.TRUE.equals(shouldReply.get("shouldSend"))) {
                        // 发送自动回复
                        boolean sent = sendAutoReply(config, emailData);
                        
                        if (sent) {
                            Map<String, Object> replyInfo = new HashMap<>();
                            replyInfo.put("configId", config.getId());
                            replyInfo.put("configName", config.getName());
                            replyInfo.put("replySubject", config.getSubject());
                            replyInfo.put("sentAt", LocalDateTime.now());
                            sentReplies.add(replyInfo);
                            
                            replySent = true;
                            
                            // 更新统计信息
                            updateReplyStatistics(config, true, null);
                            
                            // 记录回复历史
                            recordAutoReplyHistory(userAliasId, replyInfo);
                        } else {
                            updateReplyStatistics(config, false, "发送失败");
                        }
                    } else {
                        // 记录跳过原因
                        updateSkipStatistics(config, (String) shouldReply.get("reason"));
                    }
                } catch (Exception e) {
                    log.error("处理自动回复失败: configId={}, error={}", config.getId(), e.getMessage(), e);
                    updateReplyStatistics(config, false, e.getMessage());
                }
            }
            
            result.put("replySent", replySent);
            result.put("sentReplies", sentReplies);
            result.put("totalConfigs", configs.size());
            result.put("processedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("处理自动回复失败: userAliasId={}, error={}", userAliasId, e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> testAutoReplyConfig(AutoresponderConfig config, String emailContent) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证配置
            Map<String, Object> validation = validateAutoresponderConfig(config);
            if (!Boolean.TRUE.equals(validation.get("valid"))) {
                result.put("valid", false);
                result.put("errors", validation.get("errors"));
                return result;
            }
            
            // 解析邮件内容
            Map<String, Object> emailData = parseEmailContent(emailContent);
            
            // 检查是否应该发送回复
            Map<String, Object> shouldReply = shouldSendAutoReply(config, emailData);
            result.put("shouldSend", shouldReply.get("shouldSend"));
            result.put("reason", shouldReply.get("reason"));
            
            if (Boolean.TRUE.equals(shouldReply.get("shouldSend"))) {
                // 生成预览内容
                Map<String, Object> preview = previewAutoReply(config, emailData);
                result.put("preview", preview);
            }
            
            result.put("emailData", emailData);
            result.put("configEffective", isConfigEffective(config));
            result.put("testedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("测试自动回复配置失败: {}", e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public boolean sendAutoReply(AutoresponderConfig config, Map<String, Object> originalEmail) {
        try {
            String fromEmail = (String) originalEmail.get("from");
            String originalSubject = (String) originalEmail.get("subject");
            
            // 生成回复内容
            String replyContent = personalizeAutoReplyContent(
                config.getMessage(), 
                originalEmail, 
                getUserData(config.getUserAliasId())
            );
            
            // 构建回复邮件
            Map<String, Object> replyEmail = new HashMap<>();
            replyEmail.put("to", fromEmail);
            replyEmail.put("from", config.getFromAddress());
            replyEmail.put("fromName", config.getFromName());
            replyEmail.put("subject", generateReplySubject(config.getSubject(), originalSubject));
            replyEmail.put("content", replyContent);
            replyEmail.put("contentType", config.getContentType());
            
            // 是否包含原邮件
            if (config.getIncludeOriginal()) {
                String originalContent = (String) originalEmail.get("content");
                replyContent += "\n\n--- Original Message ---\n" + originalContent;
                replyEmail.put("content", replyContent);
            }
            
            // 发送邮件
            boolean sent = smtpService.sendEmail(replyEmail);
            
            if (sent) {
                // 记录发送记录
                recordSendRecord(config.getId(), fromEmail);
                
                // 更新配置统计
                config.setCurrentReplies(config.getCurrentReplies() + 1);
                config.setTotalReplies(config.getTotalReplies() + 1);
                config.setLastReplyAt(LocalDateTime.now());
                updateAutoresponderConfig(config);
                
                log.info("自动回复发送成功: configId={}, to={}", config.getId(), fromEmail);
                return true;
            }
            
        } catch (Exception e) {
            log.error("发送自动回复失败: {}", e.getMessage(), e);
        }
        
        return false;
    }

    @Override
    public Map<String, Object> shouldSendAutoReply(AutoresponderConfig config, Map<String, Object> emailData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String fromEmail = (String) emailData.get("from");
            
            // 检查黑名单
            if (isInBlacklist(fromEmail, config)) {
                result.put("shouldSend", false);
                result.put("reason", "发件人在黑名单中");
                return result;
            }
            
            // 检查白名单（如果设置了白名单，只回复白名单中的地址）
            if (hasWhitelist(config) && !isInWhitelist(fromEmail, config)) {
                result.put("shouldSend", false);
                result.put("reason", "发件人不在白名单中");
                return result;
            }
            
            // 检查频率限制
            if (!checkFrequencyLimit(config, fromEmail)) {
                result.put("shouldSend", false);
                result.put("reason", "超出频率限制");
                return result;
            }
            
            // 检查最大回复次数
            if (config.getMaxReplies() != null && config.getCurrentReplies() >= config.getMaxReplies()) {
                result.put("shouldSend", false);
                result.put("reason", "已达到最大回复次数");
                return result;
            }
            
            // 检查时间段限制
            if (!isInActiveTimeRange(config)) {
                result.put("shouldSend", false);
                result.put("reason", "不在活跃时间段内");
                return result;
            }
            
            // 检查星期限制
            if (!isInActiveDays(config)) {
                result.put("shouldSend", false);
                result.put("reason", "不在活跃星期内");
                return result;
            }
            
            // 检查内部邮件限制
            if (config.getInternalOnly() && !isInternalEmail(fromEmail)) {
                result.put("shouldSend", false);
                result.put("reason", "仅回复内部邮件");
                return result;
            }
            
            // 检查触发条件
            if (!checkTriggerConditions(config, emailData)) {
                result.put("shouldSend", false);
                result.put("reason", "不满足触发条件");
                return result;
            }
            
            result.put("shouldSend", true);
            result.put("reason", "满足所有条件");
            
        } catch (Exception e) {
            log.error("检查自动回复条件失败: {}", e.getMessage(), e);
            result.put("shouldSend", false);
            result.put("reason", "检查条件时发生错误: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> validateAutoresponderConfig(AutoresponderConfig config) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        try {
            // 检查必需字段
            if (config.getName() == null || config.getName().trim().isEmpty()) {
                errors.add("名称不能为空");
            }
            
            if (config.getSubject() == null || config.getSubject().trim().isEmpty()) {
                errors.add("回复主题不能为空");
            }
            
            if (config.getMessage() == null || config.getMessage().trim().isEmpty()) {
                errors.add("回复内容不能为空");
            }
            
            if (config.getFromAddress() == null || config.getFromAddress().trim().isEmpty()) {
                errors.add("发件人地址不能为空");
            } else if (!isValidEmail(config.getFromAddress())) {
                errors.add("发件人地址格式无效");
            }
            
            // 检查时间配置
            if (config.getStartDate() != null && config.getEndDate() != null) {
                if (config.getStartDate().isAfter(config.getEndDate())) {
                    errors.add("开始时间不能晚于结束时间");
                }
            }
            
            // 检查回复间隔
            if (config.getReplyInterval() != null && config.getReplyInterval() <= 0) {
                errors.add("回复间隔必须大于0");
            }
            
            // 检查最大回复次数
            if (config.getMaxReplies() != null && config.getMaxReplies() <= 0) {
                errors.add("最大回复次数必须大于0");
            }
            
            if (errors.isEmpty()) {
                result.put("valid", true);
                result.put("message", "配置验证通过");
            } else {
                result.put("valid", false);
                result.put("errors", errors);
            }
            
        } catch (Exception e) {
            log.error("验证自动回复配置失败: {}", e.getMessage(), e);
            result.put("valid", false);
            result.put("errors", Arrays.asList("验证过程中发生错误: " + e.getMessage()));
        }
        
        return result;
    }

    @Override
    public boolean toggleAutoresponder(Long configId, boolean enabled) {
        try {
            AutoresponderConfig config = getAutoresponderConfig(configId);
            if (config != null) {
                config.setEnabled(enabled);
                config.setStatus(enabled ? "ACTIVE" : "INACTIVE");
                return updateAutoresponderConfig(config);
            }
        } catch (Exception e) {
            log.error("切换自动回复状态失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean batchToggleAutoresponders(List<Long> configIds, boolean enabled) {
        try {
            for (Long configId : configIds) {
                toggleAutoresponder(configId, enabled);
            }
            return true;
        } catch (Exception e) {
            log.error("批量切换自动回复状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getAutoReplyTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();
        
        // 外出自动回复模板
        Map<String, Object> outOfOffice = new HashMap<>();
        outOfOffice.put("name", "out_of_office");
        outOfOffice.put("displayName", "外出自动回复");
        outOfOffice.put("category", "vacation");
        outOfOffice.put("subject", "Re: {original_subject} - 外出自动回复");
        outOfOffice.put("message", "您好，\n\n我目前外出办公，无法及时回复您的邮件。如有紧急事务，请联系 {emergency_contact}。\n\n我将在 {return_date} 回到办公室后尽快回复您。\n\n谢谢！\n{sender_name}");
        outOfOffice.put("parameters", Arrays.asList("emergency_contact", "return_date", "sender_name"));
        templates.add(outOfOffice);
        
        // 假期自动回复模板
        Map<String, Object> vacation = new HashMap<>();
        vacation.put("name", "vacation");
        vacation.put("displayName", "假期自动回复");
        vacation.put("category", "vacation");
        vacation.put("subject", "Re: {original_subject} - 假期自动回复");
        vacation.put("message", "您好，\n\n我目前正在休假，将于 {return_date} 返回工作。在此期间，我将无法及时查看和回复邮件。\n\n如有紧急事务，请联系我的同事 {backup_contact}。\n\n感谢您的理解！\n{sender_name}");
        vacation.put("parameters", Arrays.asList("return_date", "backup_contact", "sender_name"));
        templates.add(vacation);
        
        // 会议中自动回复模板
        Map<String, Object> inMeeting = new HashMap<>();
        inMeeting.put("name", "in_meeting");
        inMeeting.put("displayName", "会议中自动回复");
        inMeeting.put("category", "busy");
        inMeeting.put("subject", "Re: {original_subject} - 会议中自动回复");
        inMeeting.put("message", "您好，\n\n我目前正在参加会议，将在会议结束后尽快回复您的邮件。\n\n如有紧急事务，请直接致电 {phone_number}。\n\n谢谢！\n{sender_name}");
        inMeeting.put("parameters", Arrays.asList("phone_number", "sender_name"));
        templates.add(inMeeting);
        
        // 收到确认自动回复模板
        Map<String, Object> received = new HashMap<>();
        received.put("name", "received_confirmation");
        received.put("displayName", "收到确认自动回复");
        received.put("category", "confirmation");
        received.put("subject", "Re: {original_subject} - 邮件已收到");
        received.put("message", "您好，\n\n您的邮件已经收到，我会在 {response_time} 内回复您。\n\n如有紧急事务，请直接联系我。\n\n谢谢！\n{sender_name}");
        received.put("parameters", Arrays.asList("response_time", "sender_name"));
        templates.add(received);
        
        return templates;
    }

    @Override
    public boolean createAutoReplyFromTemplate(Long userAliasId, String templateName, Map<String, Object> parameters) {
        try {
            AutoresponderConfig config = new AutoresponderConfig();
            config.setUserAliasId(userAliasId);
            
            switch (templateName) {
                case "out_of_office":
                    createOutOfOfficeTemplate(config, parameters);
                    break;
                case "vacation":
                    createVacationTemplate(config, parameters);
                    break;
                case "in_meeting":
                    createInMeetingTemplate(config, parameters);
                    break;
                case "received_confirmation":
                    createReceivedConfirmationTemplate(config, parameters);
                    break;
                default:
                    log.error("未知的模板: {}", templateName);
                    return false;
            }
            
            return createAutoresponderConfig(config);
            
        } catch (Exception e) {
            log.error("从模板创建自动回复失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean copyAutoresponderConfig(Long configId, Long targetUserAliasId) {
        try {
            AutoresponderConfig originalConfig = getAutoresponderConfig(configId);
            if (originalConfig != null) {
                AutoresponderConfig newConfig = cloneConfig(originalConfig);
                newConfig.setUserAliasId(targetUserAliasId);
                newConfig.setName(newConfig.getName() + " (复制)");
                return createAutoresponderConfig(newConfig);
            }
        } catch (Exception e) {
            log.error("复制自动回复配置失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean importAutoresponderConfigs(Long userAliasId, String configData) {
        try {
            List<Map<String, Object>> configs = objectMapper.readValue(configData, new TypeReference<List<Map<String, Object>>>() {});
            
            for (Map<String, Object> configMap : configs) {
                AutoresponderConfig config = convertToConfig(userAliasId, configMap);
                createAutoresponderConfig(config);
            }
            
            log.info("导入自动回复配置成功: userAliasId={}, count={}", userAliasId, configs.size());
            return true;
            
        } catch (Exception e) {
            log.error("导入自动回复配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String exportAutoresponderConfigs(Long userAliasId) {
        try {
            List<AutoresponderConfig> configs = getUserAutoresponderConfigs(userAliasId);
            List<Map<String, Object>> exportData = new ArrayList<>();
            
            for (AutoresponderConfig config : configs) {
                Map<String, Object> configMap = convertToMap(config);
                exportData.add(configMap);
            }
            
            return objectMapper.writeValueAsString(exportData);
            
        } catch (Exception e) {
            log.error("导出自动回复配置失败: {}", e.getMessage(), e);
            return "";
        }
    }

    @Override
    public Map<String, Object> getAutoReplyStatistics(Long userAliasId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<AutoresponderConfig> configs = getUserAutoresponderConfigs(userAliasId);
            
            long totalConfigs = configs.size();
            long activeConfigs = configs.stream().mapToLong(c -> c.getEnabled() ? 1 : 0).sum();
            long totalReplies = configs.stream().mapToLong(c -> c.getTotalReplies() != null ? c.getTotalReplies() : 0).sum();
            long totalTriggers = configs.stream().mapToLong(c -> c.getTotalTriggers() != null ? c.getTotalTriggers() : 0).sum();
            long totalSkipped = configs.stream().mapToLong(c -> c.getSkippedCount() != null ? c.getSkippedCount() : 0).sum();
            long totalErrors = configs.stream().mapToLong(c -> c.getErrorCount() != null ? c.getErrorCount() : 0).sum();
            
            stats.put("totalConfigs", totalConfigs);
            stats.put("activeConfigs", activeConfigs);
            stats.put("inactiveConfigs", totalConfigs - activeConfigs);
            stats.put("totalReplies", totalReplies);
            stats.put("totalTriggers", totalTriggers);
            stats.put("totalSkipped", totalSkipped);
            stats.put("totalErrors", totalErrors);
            
            if (totalTriggers > 0) {
                double replyRate = (double) totalReplies / totalTriggers * 100;
                stats.put("replyRate", Math.round(replyRate * 100.0) / 100.0);
            } else {
                stats.put("replyRate", 0.0);
            }
            
        } catch (Exception e) {
            log.error("获取自动回复统计失败: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    @Override
    public List<Map<String, Object>> getAutoReplyHistory(Long userAliasId, int limit) {
        List<Map<String, Object>> history = replyHistory.get(userAliasId);
        if (history == null) {
            return new ArrayList<>();
        }
        
        int start = Math.max(0, history.size() - limit);
        return new ArrayList<>(history.subList(start, history.size()));
    }

    @Override
    public boolean cleanupAutoReplyHistory(Long userAliasId, int days) {
        try {
            List<Map<String, Object>> history = replyHistory.get(userAliasId);
            if (history != null) {
                LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
                history.removeIf(record -> {
                    LocalDateTime recordTime = (LocalDateTime) record.get("sentAt");
                    return recordTime != null && recordTime.isBefore(cutoff);
                });
            }
            return true;
        } catch (Exception e) {
            log.error("清理自动回复历史失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean resetAutoReplyCounters(Long configId) {
        try {
            AutoresponderConfig config = getAutoresponderConfig(configId);
            if (config != null) {
                config.setCurrentReplies(0);
                config.setTotalReplies(0L);
                config.setTotalTriggers(0L);
                config.setSkippedCount(0L);
                config.setErrorCount(0L);
                return updateAutoresponderConfig(config);
            }
        } catch (Exception e) {
            log.error("重置自动回复计数器失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean batchResetCounters(List<Long> configIds) {
        try {
            for (Long configId : configIds) {
                resetAutoReplyCounters(configId);
            }
            return true;
        } catch (Exception e) {
            log.error("批量重置计数器失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // 简化实现的其他方法
    @Override
    public Map<String, Object> getPerformanceMetrics(Long userAliasId) { return new HashMap<>(); }

    @Override
    public boolean optimizeAutoReplyPerformance(Long userAliasId) { return true; }

    @Override
    public List<Map<String, Object>> detectAutoReplyConflicts(Long userAliasId) { return new ArrayList<>(); }

    @Override
    public boolean resolveAutoReplyConflicts(Long userAliasId, List<Map<String, Object>> resolutions) { return true; }

    @Override
    public Map<String, Object> previewAutoReply(AutoresponderConfig config, Map<String, Object> sampleEmail) {
        Map<String, Object> preview = new HashMap<>();
        
        try {
            String personalizedContent = personalizeAutoReplyContent(
                config.getMessage(), 
                sampleEmail, 
                getUserData(config.getUserAliasId())
            );
            
            preview.put("subject", generateReplySubject(config.getSubject(), (String) sampleEmail.get("subject")));
            preview.put("content", personalizedContent);
            preview.put("fromAddress", config.getFromAddress());
            preview.put("fromName", config.getFromName());
            preview.put("contentType", config.getContentType());
            
        } catch (Exception e) {
            log.error("预览自动回复失败: {}", e.getMessage(), e);
            preview.put("error", e.getMessage());
        }
        
        return preview;
    }

    @Override
    public String personalizeAutoReplyContent(String template, Map<String, Object> emailData, Map<String, Object> userData) {
        try {
            String content = template;
            
            // 替换邮件数据变量
            content = content.replace("{original_subject}", (String) emailData.getOrDefault("subject", ""));
            content = content.replace("{sender_email}", (String) emailData.getOrDefault("from", ""));
            content = content.replace("{current_date}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            content = content.replace("{current_time}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            
            // 替换用户数据变量
            content = content.replace("{sender_name}", (String) userData.getOrDefault("name", ""));
            content = content.replace("{sender_title}", (String) userData.getOrDefault("title", ""));
            content = content.replace("{department}", (String) userData.getOrDefault("department", ""));
            content = content.replace("{phone_number}", (String) userData.getOrDefault("phone", ""));
            
            return content;
        } catch (Exception e) {
            log.error("个性化自动回复内容失败: {}", e.getMessage(), e);
            return template;
        }
    }

    // 其他简化实现的方法
    @Override
    public List<Map<String, Object>> getAutoReplySuggestions(Long userAliasId, List<String> sampleEmails) { return new ArrayList<>(); }

    @Override
    public List<Map<String, Object>> learnAndSuggestAutoReplies(Long userAliasId) { return new ArrayList<>(); }

    @Override
    public boolean setVacationMode(Long userAliasId, Map<String, Object> vacationConfig) { return true; }

    @Override
    public Map<String, Object> getVacationModeConfig(Long userAliasId) { return new HashMap<>(); }

    @Override
    public boolean stopVacationMode(Long userAliasId) { return true; }

    @Override
    public boolean setOutOfOfficeReply(Long userAliasId, Map<String, Object> oooConfig) { return true; }

    @Override
    public Map<String, Object> getOutOfOfficeConfig(Long userAliasId) { return new HashMap<>(); }

    @Override
    public boolean stopOutOfOfficeReply(Long userAliasId) { return true; }

    @Override
    public Map<String, Object> batchProcessAutoReplies(List<String> emails, Long userAliasId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (String email : emails) {
            Map<String, Object> emailResult = processAutoReply(email, userAliasId);
            results.add(emailResult);
        }
        
        result.put("totalEmails", emails.size());
        result.put("results", results);
        return result;
    }

    @Override
    public Map<String, Object> getAutoReplyDebugInfo(Long configId, String emailContent) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            AutoresponderConfig config = getAutoresponderConfig(configId);
            if (config != null) {
                debugInfo = testAutoReplyConfig(config, emailContent);
                debugInfo.put("debugMode", true);
            }
        } catch (Exception e) {
            log.error("获取自动回复调试信息失败: {}", e.getMessage(), e);
            debugInfo.put("error", e.getMessage());
        }
        
        return debugInfo;
    }

    @Override
    public boolean toggleDebugMode(Long configId, boolean debugMode) { return true; }

    @Override
    public String generateAutoReplyReport(Long userAliasId, String startDate, String endDate) { return ""; }

    @Override
    public Map<String, Object> getRealTimeAutoReplyStatus(Long userAliasId) { return new HashMap<>(); }

    @Override
    public String backupAutoresponderConfigs(Long userAliasId) { return exportAutoresponderConfigs(userAliasId); }

    @Override
    public boolean restoreAutoresponderConfigs(Long userAliasId, String backupData) { return importAutoresponderConfigs(userAliasId, backupData); }

    // ========== 私有辅助方法 ==========

    private void setDefaultValues(AutoresponderConfig config) {
        if (config.getContentType() == null) config.setContentType("TEXT");
        if (config.getTriggerType() == null) config.setTriggerType("ALL");
        if (config.getFrequencyLimit() == null) config.setFrequencyLimit("DAILY");
        if (config.getReplyInterval() == null) config.setReplyInterval(24);
        if (config.getIncludeOriginal() == null) config.setIncludeOriginal(false);
        if (config.getInternalOnly() == null) config.setInternalOnly(false);
        if (config.getCurrentReplies() == null) config.setCurrentReplies(0);
        if (config.getTotalReplies() == null) config.setTotalReplies(0L);
        if (config.getTotalTriggers() == null) config.setTotalTriggers(0L);
        if (config.getSkippedCount() == null) config.setSkippedCount(0L);
        if (config.getErrorCount() == null) config.setErrorCount(0L);
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
            emailData.put("content", body.toString());
            emailData.put("from", headers.get("from"));
            emailData.put("to", headers.get("to"));
            emailData.put("subject", headers.get("subject"));
            emailData.put("date", headers.get("date"));
            
        } catch (Exception e) {
            log.error("解析邮件内容失败: {}", e.getMessage(), e);
        }
        
        return emailData;
    }

    private boolean isConfigEffective(AutoresponderConfig config) {
        LocalDateTime now = LocalDateTime.now();
        
        if (config.getStartDate() != null && now.isBefore(config.getStartDate())) {
            return false;
        }
        
        if (config.getEndDate() != null && now.isAfter(config.getEndDate())) {
            return false;
        }
        
        return true;
    }

    private boolean isInBlacklist(String email, AutoresponderConfig config) {
        if (config.getBlacklistEmails() != null) {
            List<String> blacklistEmails = Arrays.asList(config.getBlacklistEmails().split(","));
            if (blacklistEmails.contains(email.trim())) {
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

    private boolean hasWhitelist(AutoresponderConfig config) {
        return (config.getWhitelistEmails() != null && !config.getWhitelistEmails().trim().isEmpty()) ||
               (config.getWhitelistDomains() != null && !config.getWhitelistDomains().trim().isEmpty());
    }

    private boolean isInWhitelist(String email, AutoresponderConfig config) {
        if (config.getWhitelistEmails() != null) {
            List<String> whitelistEmails = Arrays.asList(config.getWhitelistEmails().split(","));
            if (whitelistEmails.contains(email.trim())) {
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

    private boolean checkFrequencyLimit(AutoresponderConfig config, String fromEmail) {
        String key = config.getId() + "_" + fromEmail;
        Map<String, LocalDateTime> records = sendRecords.get(key);
        
        if (records == null) {
            return true;
        }
        
        LocalDateTime lastSent = records.get("lastSent");
        if (lastSent == null) {
            return true;
        }
        
        LocalDateTime now = LocalDateTime.now();
        long hoursSinceLastSent = java.time.Duration.between(lastSent, now).toHours();
        
        return hoursSinceLastSent >= config.getReplyInterval();
    }

    private boolean isInActiveTimeRange(AutoresponderConfig config) {
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

    private boolean isInActiveDays(AutoresponderConfig config) {
        if (config.getActiveDays() == null || config.getActiveDays().trim().isEmpty()) {
            return true;
        }
        
        try {
            List<Integer> activeDays = objectMapper.readValue(config.getActiveDays(), List.class);
            int currentDay = LocalDateTime.now().getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
            return activeDays.contains(currentDay);
        } catch (Exception e) {
            log.error("检查活跃星期失败: {}", e.getMessage(), e);
            return true;
        }
    }

    private boolean isInternalEmail(String email) {
        // 简化实现，实际应该检查是否为内部域名
        return email.contains("@company.com") || email.contains("@internal.com");
    }

    private boolean checkTriggerConditions(AutoresponderConfig config, Map<String, Object> emailData) {
        if ("ALL".equals(config.getTriggerType())) {
            return true;
        }
        
        try {
            List<Map<String, Object>> conditions = objectMapper.readValue(
                config.getTriggerConditions(), new TypeReference<List<Map<String, Object>>>() {}
            );
            
            for (Map<String, Object> condition : conditions) {
                if (!evaluateTriggerCondition(condition, emailData)) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("检查触发条件失败: {}", e.getMessage(), e);
            return true;
        }
    }

    private boolean evaluateTriggerCondition(Map<String, Object> condition, Map<String, Object> emailData) {
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

    private void updateReplyStatistics(AutoresponderConfig config, boolean success, String error) {
        try {
            config.setTotalTriggers(config.getTotalTriggers() + 1);
            
            if (success) {
                config.setTotalReplies(config.getTotalReplies() + 1);
                config.setLastReplyAt(LocalDateTime.now());
            } else {
                config.setErrorCount(config.getErrorCount() + 1);
                config.setLastError(error);
                config.setLastErrorAt(LocalDateTime.now());
            }
            
            updateAutoresponderConfig(config);
        } catch (Exception e) {
            log.error("更新回复统计失败: {}", e.getMessage(), e);
        }
    }

    private void updateSkipStatistics(AutoresponderConfig config, String reason) {
        try {
            config.setTotalTriggers(config.getTotalTriggers() + 1);
            config.setSkippedCount(config.getSkippedCount() + 1);
            updateAutoresponderConfig(config);
        } catch (Exception e) {
            log.error("更新跳过统计失败: {}", e.getMessage(), e);
        }
    }

    private void recordSendRecord(Long configId, String email) {
        String key = configId + "_" + email;
        Map<String, LocalDateTime> record = new HashMap<>();
        record.put("lastSent", LocalDateTime.now());
        sendRecords.put(key, record);
    }

    private void recordAutoReplyHistory(Long userAliasId, Map<String, Object> replyInfo) {
        List<Map<String, Object>> history = replyHistory.computeIfAbsent(userAliasId, k -> new ArrayList<>());
        history.add(replyInfo);
        
        // 保持历史记录在合理范围内
        if (history.size() > 1000) {
            history.subList(0, history.size() - 1000).clear();
        }
    }

    private Map<String, Object> getUserData(Long userAliasId) {
        // 简化实现，实际应该从数据库获取用户信息
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "用户姓名");
        userData.put("title", "职位");
        userData.put("department", "部门");
        userData.put("phone", "联系电话");
        return userData;
    }

    private String generateReplySubject(String template, String originalSubject) {
        if (template.contains("{original_subject}")) {
            return template.replace("{original_subject}", originalSubject != null ? originalSubject : "");
        }
        return template;
    }

    private String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(atIndex + 1) : "";
    }

    private boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        return pattern.matcher(email).matches();
    }

    // 模板创建方法的简化实现
    private void createOutOfOfficeTemplate(AutoresponderConfig config, Map<String, Object> params) {
        config.setName("外出自动回复");
        config.setSubject("Re: {original_subject} - 外出自动回复");
        // 简化实现
    }
    
    private void createVacationTemplate(AutoresponderConfig config, Map<String, Object> params) {
        config.setName("假期自动回复");
        config.setSubject("Re: {original_subject} - 假期自动回复");
        // 简化实现
    }
    
    private void createInMeetingTemplate(AutoresponderConfig config, Map<String, Object> params) {
        config.setName("会议中自动回复");
        config.setSubject("Re: {original_subject} - 会议中自动回复");
        // 简化实现
    }
    
    private void createReceivedConfirmationTemplate(AutoresponderConfig config, Map<String, Object> params) {
        config.setName("收到确认自动回复");
        config.setSubject("Re: {original_subject} - 邮件已收到");
        // 简化实现
    }
    
    private AutoresponderConfig cloneConfig(AutoresponderConfig original) {
        AutoresponderConfig clone = new AutoresponderConfig();
        clone.setName(original.getName());
        clone.setDescription(original.getDescription());
        clone.setSubject(original.getSubject());
        clone.setMessage(original.getMessage());
        clone.setContentType(original.getContentType());
        clone.setFromAddress(original.getFromAddress());
        clone.setFromName(original.getFromName());
        clone.setTriggerType(original.getTriggerType());
        clone.setTriggerConditions(original.getTriggerConditions());
        clone.setFrequencyLimit(original.getFrequencyLimit());
        clone.setReplyInterval(original.getReplyInterval());
        clone.setIncludeOriginal(original.getIncludeOriginal());
        clone.setInternalOnly(original.getInternalOnly());
        return clone;
    }
    
    private AutoresponderConfig convertToConfig(Long userAliasId, Map<String, Object> configMap) {
        AutoresponderConfig config = new AutoresponderConfig();
        config.setUserAliasId(userAliasId);
        config.setName((String) configMap.get("name"));
        // 简化实现
        return config;
    }
    
    private Map<String, Object> convertToMap(AutoresponderConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", config.getName());
        map.put("description", config.getDescription());
        map.put("subject", config.getSubject());
        map.put("message", config.getMessage());
        // 简化实现
        return map;
    }
}