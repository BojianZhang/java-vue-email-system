package com.enterprise.email.service.impl;

import com.enterprise.email.entity.SieveRule;
import com.enterprise.email.mapper.SieveRuleMapper;
import com.enterprise.email.service.SieveService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sieve邮件过滤规则服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SieveServiceImpl implements SieveService {

    private final SieveRuleMapper sieveRuleMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 规则执行历史缓存
    private final Map<Long, List<Map<String, Object>>> executionHistory = new HashMap<>();

    @Override
    public boolean createSieveRule(SieveRule rule) {
        try {
            rule.setCreatedAt(LocalDateTime.now());
            rule.setUpdatedAt(LocalDateTime.now());
            rule.setEnabled(true);
            rule.setStatus("ACTIVE");
            
            // 设置默认值
            setDefaultValues(rule);
            
            // 生成Sieve脚本
            if (rule.getSieveScript() == null || rule.getSieveScript().isEmpty()) {
                rule.setSieveScript(generateSieveScript(rule));
            }
            
            // 验证脚本语法
            Map<String, Object> validation = validateSieveScript(rule.getSieveScript());
            rule.setSyntaxValid(Boolean.TRUE.equals(validation.get("valid")));
            if (!rule.getSyntaxValid()) {
                rule.setSyntaxError((String) validation.get("error"));
            }
            rule.setLastValidatedAt(LocalDateTime.now());
            
            int result = sieveRuleMapper.insert(rule);
            if (result > 0) {
                log.info("Sieve规则创建成功: {}", rule.getRuleName());
                return true;
            }
        } catch (Exception e) {
            log.error("创建Sieve规则失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateSieveRule(SieveRule rule) {
        try {
            rule.setUpdatedAt(LocalDateTime.now());
            
            // 重新生成Sieve脚本
            if (rule.getConditions() != null || rule.getActions() != null) {
                rule.setSieveScript(generateSieveScript(rule));
            }
            
            // 重新验证脚本语法
            if (rule.getSieveScript() != null) {
                Map<String, Object> validation = validateSieveScript(rule.getSieveScript());
                rule.setSyntaxValid(Boolean.TRUE.equals(validation.get("valid")));
                if (!rule.getSyntaxValid()) {
                    rule.setSyntaxError((String) validation.get("error"));
                }
                rule.setLastValidatedAt(LocalDateTime.now());
            }
            
            int result = sieveRuleMapper.updateById(rule);
            if (result > 0) {
                log.info("Sieve规则更新成功: {}", rule.getRuleName());
                return true;
            }
        } catch (Exception e) {
            log.error("更新Sieve规则失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteSieveRule(Long ruleId) {
        try {
            SieveRule rule = sieveRuleMapper.selectById(ruleId);
            if (rule != null) {
                rule.setDeleted(true);
                rule.setUpdatedAt(LocalDateTime.now());
                int result = sieveRuleMapper.updateById(rule);
                if (result > 0) {
                    log.info("Sieve规则删除成功: {}", rule.getRuleName());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("删除Sieve规则失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public SieveRule getSieveRule(Long ruleId) {
        return sieveRuleMapper.selectById(ruleId);
    }

    @Override
    public List<SieveRule> getUserSieveRules(Long userAliasId) {
        return sieveRuleMapper.selectByUserAliasId(userAliasId);
    }

    @Override
    public List<SieveRule> getUserActiveSieveRules(Long userAliasId) {
        return sieveRuleMapper.selectActiveByUserAliasId(userAliasId);
    }

    @Override
    public Map<String, Object> applySieveRules(String emailContent, Long userAliasId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> appliedRules = new ArrayList<>();
        
        try {
            List<SieveRule> rules = getUserActiveSieveRules(userAliasId);
            
            // 解析邮件头部和正文
            Map<String, Object> emailData = parseEmailContent(emailContent);
            
            boolean processed = false;
            String finalAction = "keep";
            Map<String, Object> actionData = new HashMap<>();
            
            for (SieveRule rule : rules) {
                try {
                    // 检查规则是否在有效期内
                    if (!isRuleEffective(rule)) {
                        continue;
                    }
                    
                    // 评估规则条件
                    boolean conditionMet = evaluateRuleConditions(rule, emailData);
                    
                    if (conditionMet) {
                        // 执行规则动作
                        Map<String, Object> actionResult = executeRuleActions(rule, emailData);
                        
                        Map<String, Object> ruleExecution = new HashMap<>();
                        ruleExecution.put("ruleId", rule.getId());
                        ruleExecution.put("ruleName", rule.getRuleName());
                        ruleExecution.put("action", actionResult.get("action"));
                        ruleExecution.put("actionData", actionResult.get("actionData"));
                        ruleExecution.put("appliedAt", LocalDateTime.now());
                        appliedRules.add(ruleExecution);
                        
                        // 更新规则应用统计
                        updateRuleStatistics(rule, true, null);
                        
                        // 记录执行历史
                        recordRuleExecution(userAliasId, ruleExecution);
                        
                        // 检查是否停止处理
                        if ("stop".equals(actionResult.get("action")) || !rule.getContinueProcessing()) {
                            finalAction = (String) actionResult.get("action");
                            actionData = (Map<String, Object>) actionResult.get("actionData");
                            processed = true;
                            break;
                        }
                        
                        // 更新最终动作
                        if (!"keep".equals(actionResult.get("action"))) {
                            finalAction = (String) actionResult.get("action");
                            actionData = (Map<String, Object>) actionResult.get("actionData");
                            processed = true;
                        }
                    }
                } catch (Exception e) {
                    log.error("应用Sieve规则失败: ruleId={}, error={}", rule.getId(), e.getMessage(), e);
                    updateRuleStatistics(rule, false, e.getMessage());
                }
            }
            
            result.put("processed", processed);
            result.put("finalAction", finalAction);
            result.put("actionData", actionData);
            result.put("appliedRules", appliedRules);
            result.put("totalRules", rules.size());
            result.put("processedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("应用Sieve规则失败: userAliasId={}, error={}", userAliasId, e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> testSieveRule(SieveRule rule, String emailContent) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证脚本语法
            Map<String, Object> validation = validateSieveScript(rule.getSieveScript());
            if (!Boolean.TRUE.equals(validation.get("valid"))) {
                result.put("syntaxValid", false);
                result.put("syntaxError", validation.get("error"));
                return result;
            }
            
            // 解析邮件内容
            Map<String, Object> emailData = parseEmailContent(emailContent);
            
            // 评估条件
            boolean conditionMet = evaluateRuleConditions(rule, emailData);
            result.put("conditionMet", conditionMet);
            
            if (conditionMet) {
                // 模拟执行动作
                Map<String, Object> actionResult = executeRuleActions(rule, emailData);
                result.put("action", actionResult.get("action"));
                result.put("actionData", actionResult.get("actionData"));
            } else {
                result.put("action", "keep");
                result.put("actionData", new HashMap<>());
            }
            
            result.put("emailData", emailData);
            result.put("ruleConditions", parseConditions(rule.getConditions()));
            result.put("ruleActions", parseActions(rule.getActions()));
            result.put("testedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("测试Sieve规则失败: {}", e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> validateSieveScript(String sieveScript) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (sieveScript == null || sieveScript.trim().isEmpty()) {
                result.put("valid", false);
                result.put("error", "Sieve脚本不能为空");
                return result;
            }
            
            // 基本语法检查
            List<String> errors = new ArrayList<>();
            
            // 检查require语句
            if (!sieveScript.contains("require")) {
                errors.add("缺少require语句");
            }
            
            // 检查括号匹配
            if (!checkBracketMatching(sieveScript)) {
                errors.add("括号不匹配");
            }
            
            // 检查基本语法结构
            if (!checkBasicSyntax(sieveScript)) {
                errors.add("基本语法错误");
            }
            
            // 检查支持的命令
            List<String> unsupportedCommands = checkUnsupportedCommands(sieveScript);
            if (!unsupportedCommands.isEmpty()) {
                errors.add("不支持的命令: " + String.join(", ", unsupportedCommands));
            }
            
            if (errors.isEmpty()) {
                result.put("valid", true);
                result.put("message", "Sieve脚本语法正确");
            } else {
                result.put("valid", false);
                result.put("error", String.join("; ", errors));
            }
            
            result.put("validatedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("验证Sieve脚本失败: {}", e.getMessage(), e);
            result.put("valid", false);
            result.put("error", "验证过程中发生错误: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public String generateSieveScript(SieveRule rule) {
        try {
            StringBuilder script = new StringBuilder();
            
            // 添加require语句
            script.append("require [\"fileinto\", \"reject\", \"envelope\", \"body\", \"variables\"];\n\n");
            
            // 添加注释
            script.append("# ").append(rule.getRuleName()).append("\n");
            if (rule.getDescription() != null && !rule.getDescription().isEmpty()) {
                script.append("# ").append(rule.getDescription()).append("\n");
            }
            script.append("\n");
            
            // 解析条件
            List<Map<String, Object>> conditions = parseConditions(rule.getConditions());
            List<Map<String, Object>> actions = parseActions(rule.getActions());
            
            // 生成if语句
            if (!conditions.isEmpty()) {
                script.append("if ");
                
                if (conditions.size() == 1) {
                    script.append(generateConditionScript(conditions.get(0)));
                } else {
                    String operator = "allof"; // 默认AND
                    if ("ANY".equals(rule.getConditionType())) {
                        operator = "anyof";
                    }
                    
                    script.append(operator).append(" (");
                    for (int i = 0; i < conditions.size(); i++) {
                        if (i > 0) script.append(",\n    ");
                        script.append(generateConditionScript(conditions.get(i)));
                    }
                    script.append(")");
                }
                
                script.append(" {\n");
                
                // 生成动作
                for (Map<String, Object> action : actions) {
                    script.append("    ").append(generateActionScript(action)).append("\n");
                }
                
                // 添加stop语句（如果不继续处理）
                if (!rule.getContinueProcessing()) {
                    script.append("    stop;\n");
                }
                
                script.append("}\n");
            }
            
            return script.toString();
            
        } catch (Exception e) {
            log.error("生成Sieve脚本失败: {}", e.getMessage(), e);
            return "";
        }
    }

    @Override
    public Map<String, Object> parseSieveScript(String sieveScript) {
        Map<String, Object> parsed = new HashMap<>();
        
        try {
            // 解析require语句
            List<String> requirements = parseRequirements(sieveScript);
            parsed.put("requirements", requirements);
            
            // 解析规则
            List<Map<String, Object>> rules = parseRules(sieveScript);
            parsed.put("rules", rules);
            
            // 解析注释
            List<String> comments = parseComments(sieveScript);
            parsed.put("comments", comments);
            
            parsed.put("parsedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("解析Sieve脚本失败: {}", e.getMessage(), e);
            parsed.put("error", e.getMessage());
        }
        
        return parsed;
    }

    @Override
    public Map<String, Object> batchApplyRules(List<String> emails, Long userAliasId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            for (String email : emails) {
                Map<String, Object> emailResult = applySieveRules(email, userAliasId);
                emailResult.put("emailHash", email.hashCode());
                results.add(emailResult);
            }
            
            // 统计结果
            long processedCount = results.stream()
                .mapToLong(r -> Boolean.TRUE.equals(r.get("processed")) ? 1 : 0)
                .sum();
            
            result.put("totalEmails", emails.size());
            result.put("processedEmails", processedCount);
            result.put("unprocessedEmails", emails.size() - processedCount);
            result.put("results", results);
            result.put("processedAt", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("批量应用规则失败: {}", e.getMessage(), e);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getRuleTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();
        
        // 垃圾邮件过滤模板
        Map<String, Object> spamFilter = new HashMap<>();
        spamFilter.put("name", "spam_filter");
        spamFilter.put("displayName", "垃圾邮件过滤");
        spamFilter.put("description", "将包含垃圾邮件关键词的邮件移动到垃圾邮件文件夹");
        spamFilter.put("category", "filter");
        spamFilter.put("parameters", Arrays.asList("keywords", "folder"));
        templates.add(spamFilter);
        
        // 发件人过滤模板
        Map<String, Object> senderFilter = new HashMap<>();
        senderFilter.put("name", "sender_filter");
        senderFilter.put("displayName", "发件人过滤");
        senderFilter.put("description", "根据发件人地址过滤邮件");
        senderFilter.put("category", "filter");
        senderFilter.put("parameters", Arrays.asList("sender", "action", "folder"));
        templates.add(senderFilter);
        
        // 主题过滤模板
        Map<String, Object> subjectFilter = new HashMap<>();
        subjectFilter.put("name", "subject_filter");
        subjectFilter.put("displayName", "主题过滤");
        subjectFilter.put("description", "根据邮件主题过滤邮件");
        subjectFilter.put("category", "filter");
        subjectFilter.put("parameters", Arrays.asList("subject", "action", "folder"));
        templates.add(subjectFilter);
        
        // 邮件转发模板
        Map<String, Object> forwardTemplate = new HashMap<>();
        forwardTemplate.put("name", "forward_rule");
        forwardTemplate.put("displayName", "邮件转发");
        forwardTemplate.put("description", "将符合条件的邮件转发到指定地址");
        forwardTemplate.put("category", "forward");
        forwardTemplate.put("parameters", Arrays.asList("condition", "forward_address", "keep_copy"));
        templates.add(forwardTemplate);
        
        // 自动回复模板
        Map<String, Object> autoReply = new HashMap<>();
        autoReply.put("name", "auto_reply");
        autoReply.put("displayName", "自动回复");
        autoReply.put("description", "对符合条件的邮件发送自动回复");
        autoReply.put("category", "reply");
        autoReply.put("parameters", Arrays.asList("condition", "reply_message", "frequency"));
        templates.add(autoReply);
        
        return templates;
    }

    @Override
    public boolean createRuleFromTemplate(Long userAliasId, String templateName, Map<String, Object> parameters) {
        try {
            SieveRule rule = new SieveRule();
            rule.setUserAliasId(userAliasId);
            
            switch (templateName) {
                case "spam_filter":
                    createSpamFilterRule(rule, parameters);
                    break;
                case "sender_filter":
                    createSenderFilterRule(rule, parameters);
                    break;
                case "subject_filter":
                    createSubjectFilterRule(rule, parameters);
                    break;
                case "forward_rule":
                    createForwardRule(rule, parameters);
                    break;
                case "auto_reply":
                    createAutoReplyRule(rule, parameters);
                    break;
                default:
                    log.error("未知的规则模板: {}", templateName);
                    return false;
            }
            
            return createSieveRule(rule);
            
        } catch (Exception e) {
            log.error("从模板创建规则失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean importSieveRules(Long userAliasId, String sieveScript) {
        try {
            // 解析Sieve脚本
            Map<String, Object> parsed = parseSieveScript(sieveScript);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rules = (List<Map<String, Object>>) parsed.get("rules");
            
            if (rules == null || rules.isEmpty()) {
                return false;
            }
            
            // 创建规则
            for (Map<String, Object> ruleData : rules) {
                SieveRule rule = convertToSieveRule(userAliasId, ruleData);
                createSieveRule(rule);
            }
            
            log.info("导入Sieve规则成功: userAliasId={}, count={}", userAliasId, rules.size());
            return true;
            
        } catch (Exception e) {
            log.error("导入Sieve规则失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String exportSieveRules(Long userAliasId) {
        try {
            List<SieveRule> rules = getUserSieveRules(userAliasId);
            
            StringBuilder script = new StringBuilder();
            script.append("# Sieve rules export for user ").append(userAliasId).append("\n");
            script.append("# Generated at ").append(LocalDateTime.now()).append("\n\n");
            
            Set<String> allRequirements = new HashSet<>();
            allRequirements.add("fileinto");
            allRequirements.add("reject");
            allRequirements.add("envelope");
            allRequirements.add("body");
            allRequirements.add("variables");
            
            script.append("require [");
            script.append(String.join("\", \"", allRequirements));
            script.append("\"];\n\n");
            
            for (SieveRule rule : rules) {
                if (rule.getEnabled() && rule.getSieveScript() != null) {
                    script.append("# Rule: ").append(rule.getRuleName()).append("\n");
                    script.append("# Priority: ").append(rule.getPriority()).append("\n");
                    script.append(rule.getSieveScript()).append("\n\n");
                }
            }
            
            return script.toString();
            
        } catch (Exception e) {
            log.error("导出Sieve规则失败: {}", e.getMessage(), e);
            return "";
        }
    }

    // 其他方法的简化实现
    @Override
    public boolean copyRulesToUser(Long fromUserAliasId, Long toUserAliasId, List<Long> ruleIds) {
        try {
            for (Long ruleId : ruleIds) {
                SieveRule originalRule = getSieveRule(ruleId);
                if (originalRule != null && originalRule.getUserAliasId().equals(fromUserAliasId)) {
                    SieveRule newRule = cloneRule(originalRule);
                    newRule.setUserAliasId(toUserAliasId);
                    newRule.setRuleName(newRule.getRuleName() + " (复制)");
                    createSieveRule(newRule);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("复制规则失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getRuleStatistics(Long userAliasId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<SieveRule> rules = getUserSieveRules(userAliasId);
            
            long totalRules = rules.size();
            long activeRules = rules.stream().mapToLong(r -> r.getEnabled() ? 1 : 0).sum();
            long totalApplications = rules.stream().mapToLong(r -> r.getAppliedCount() != null ? r.getAppliedCount() : 0).sum();
            long totalErrors = rules.stream().mapToLong(r -> r.getErrorCount() != null ? r.getErrorCount() : 0).sum();
            
            stats.put("totalRules", totalRules);
            stats.put("activeRules", activeRules);
            stats.put("inactiveRules", totalRules - activeRules);
            stats.put("totalApplications", totalApplications);
            stats.put("totalErrors", totalErrors);
            
            if (totalApplications > 0) {
                double successRate = (double) (totalApplications - totalErrors) / totalApplications * 100;
                stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
            } else {
                stats.put("successRate", 0.0);
            }
            
        } catch (Exception e) {
            log.error("获取规则统计失败: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    @Override
    public List<Map<String, Object>> getRuleExecutionHistory(Long userAliasId, int limit) {
        List<Map<String, Object>> history = executionHistory.get(userAliasId);
        if (history == null) {
            return new ArrayList<>();
        }
        
        int start = Math.max(0, history.size() - limit);
        return new ArrayList<>(history.subList(start, history.size()));
    }

    @Override
    public boolean cleanupRuleHistory(Long userAliasId, int days) {
        try {
            List<Map<String, Object>> history = executionHistory.get(userAliasId);
            if (history != null) {
                LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
                history.removeIf(record -> {
                    LocalDateTime recordTime = (LocalDateTime) record.get("appliedAt");
                    return recordTime != null && recordTime.isBefore(cutoff);
                });
            }
            return true;
        } catch (Exception e) {
            log.error("清理规则历史失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // 简化实现的其他方法
    @Override
    public boolean optimizeRuleOrder(Long userAliasId) { return true; }

    @Override
    public List<Map<String, Object>> detectRuleConflicts(Long userAliasId) { return new ArrayList<>(); }

    @Override
    public boolean resolveRuleConflicts(Long userAliasId, List<Map<String, Object>> resolutions) { return true; }

    @Override
    public boolean toggleRule(Long ruleId, boolean enabled) {
        try {
            SieveRule rule = getSieveRule(ruleId);
            if (rule != null) {
                rule.setEnabled(enabled);
                return updateSieveRule(rule);
            }
        } catch (Exception e) {
            log.error("切换规则状态失败: {}", e.getMessage(), e);
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
            log.error("批量切换规则状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean adjustRulePriority(Long ruleId, int newPriority) {
        try {
            SieveRule rule = getSieveRule(ruleId);
            if (rule != null) {
                rule.setPriority(newPriority);
                return updateSieveRule(rule);
            }
        } catch (Exception e) {
            log.error("调整规则优先级失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean batchAdjustPriority(List<Map<String, Object>> priorityMappings) { return true; }

    @Override
    public List<Map<String, Object>> getRuleSuggestions(Long userAliasId, List<String> sampleEmails) { return new ArrayList<>(); }

    @Override
    public boolean autoCreateRules(Long userAliasId, Map<String, Object> preferences) { return true; }

    @Override
    public List<Map<String, Object>> learnAndSuggestRules(Long userAliasId) { return new ArrayList<>(); }

    @Override
    public Map<String, Object> getFilterPerformanceMetrics(Long userAliasId) { return new HashMap<>(); }

    @Override
    public boolean optimizeFilterPerformance(Long userAliasId) { return true; }

    @Override
    public String backupRuleConfiguration(Long userAliasId) { return exportSieveRules(userAliasId); }

    @Override
    public boolean restoreRuleConfiguration(Long userAliasId, String backupData) { return importSieveRules(userAliasId, backupData); }

    @Override
    public Map<String, Object> getRuleEditorConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("supportedConditions", Arrays.asList("header", "body", "size", "envelope"));
        config.put("supportedActions", Arrays.asList("keep", "discard", "redirect", "fileinto", "reject"));
        config.put("supportedComparators", Arrays.asList("contains", "is", "matches", "regex"));
        return config;
    }

    @Override
    public boolean validateRulePermissions(Long userAliasId, SieveRule rule) {
        return rule.getUserAliasId().equals(userAliasId);
    }

    @Override
    public Map<String, Object> getRuleDebugInfo(Long ruleId, String emailContent) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            SieveRule rule = getSieveRule(ruleId);
            if (rule != null) {
                debugInfo = testSieveRule(rule, emailContent);
                debugInfo.put("debugMode", true);
                debugInfo.put("ruleScript", rule.getSieveScript());
            }
        } catch (Exception e) {
            log.error("获取规则调试信息失败: {}", e.getMessage(), e);
            debugInfo.put("error", e.getMessage());
        }
        
        return debugInfo;
    }

    @Override
    public boolean toggleRuleDebugMode(Long ruleId, boolean debugMode) {
        try {
            SieveRule rule = getSieveRule(ruleId);
            if (rule != null) {
                rule.setTestMode(debugMode);
                return updateSieveRule(rule);
            }
        } catch (Exception e) {
            log.error("切换规则调试模式失败: {}", e.getMessage(), e);
        }
        return false;
    }

    // ========== 私有辅助方法 ==========

    private void setDefaultValues(SieveRule rule) {
        if (rule.getPriority() == null) rule.setPriority(100);
        if (rule.getRuleType() == null) rule.setRuleType("FILTER");
        if (rule.getConditionType() == null) rule.setConditionType("ALL");
        if (rule.getActionType() == null) rule.setActionType("KEEP");
        if (rule.getContinueProcessing() == null) rule.setContinueProcessing(true);
        if (rule.getEnableLogging() == null) rule.setEnableLogging(true);
        if (rule.getTestMode() == null) rule.setTestMode(false);
        if (rule.getAppliedCount() == null) rule.setAppliedCount(0L);
        if (rule.getErrorCount() == null) rule.setErrorCount(0L);
        if (rule.getSyntaxValid() == null) rule.setSyntaxValid(false);
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
            emailData.put("body", body.toString());
            emailData.put("size", emailContent.length());
            
            // 提取常用字段
            emailData.put("from", headers.get("from"));
            emailData.put("to", headers.get("to"));
            emailData.put("subject", headers.get("subject"));
            emailData.put("date", headers.get("date"));
            
        } catch (Exception e) {
            log.error("解析邮件内容失败: {}", e.getMessage(), e);
        }
        
        return emailData;
    }

    private boolean isRuleEffective(SieveRule rule) {
        LocalDateTime now = LocalDateTime.now();
        
        if (rule.getEffectiveFrom() != null && now.isBefore(rule.getEffectiveFrom())) {
            return false;
        }
        
        if (rule.getEffectiveUntil() != null && now.isAfter(rule.getEffectiveUntil())) {
            return false;
        }
        
        return true;
    }

    private boolean evaluateRuleConditions(SieveRule rule, Map<String, Object> emailData) {
        try {
            List<Map<String, Object>> conditions = parseConditions(rule.getConditions());
            if (conditions.isEmpty()) {
                return true; // 没有条件则总是匹配
            }
            
            boolean allMatch = true;
            boolean anyMatch = false;
            
            for (Map<String, Object> condition : conditions) {
                boolean match = evaluateSingleCondition(condition, emailData);
                allMatch = allMatch && match;
                anyMatch = anyMatch || match;
            }
            
            return "ANY".equals(rule.getConditionType()) ? anyMatch : allMatch;
            
        } catch (Exception e) {
            log.error("评估规则条件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean evaluateSingleCondition(Map<String, Object> condition, Map<String, Object> emailData) {
        String type = (String) condition.get("type");
        String comparator = (String) condition.get("comparator");
        String value = (String) condition.get("value");
        
        switch (type) {
            case "header":
                String headerName = (String) condition.get("header");
                @SuppressWarnings("unchecked")
                Map<String, String> headers = (Map<String, String>) emailData.get("headers");
                String headerValue = headers.get(headerName.toLowerCase());
                return compareValues(headerValue, value, comparator);
                
            case "body":
                String body = (String) emailData.get("body");
                return compareValues(body, value, comparator);
                
            case "size":
                Integer size = (Integer) emailData.get("size");
                Integer targetSize = Integer.valueOf(value);
                return compareSizes(size, targetSize, comparator);
                
            case "envelope":
                String envelopeField = (String) condition.get("envelope");
                String envelopeValue = (String) emailData.get(envelopeField);
                return compareValues(envelopeValue, value, comparator);
                
            default:
                return false;
        }
    }

    private boolean compareValues(String actual, String expected, String comparator) {
        if (actual == null) {
            return false;
        }
        
        switch (comparator) {
            case "is":
                return actual.equals(expected);
            case "contains":
                return actual.contains(expected);
            case "matches":
                return actual.matches(expected);
            case "regex":
                Pattern pattern = Pattern.compile(expected);
                return pattern.matcher(actual).find();
            default:
                return false;
        }
    }

    private boolean compareSizes(Integer actual, Integer expected, String comparator) {
        if (actual == null || expected == null) {
            return false;
        }
        
        switch (comparator) {
            case "over":
                return actual > expected;
            case "under":
                return actual < expected;
            case "equals":
                return actual.equals(expected);
            default:
                return false;
        }
    }

    private Map<String, Object> executeRuleActions(SieveRule rule, Map<String, Object> emailData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Map<String, Object>> actions = parseActions(rule.getActions());
            
            for (Map<String, Object> action : actions) {
                String actionType = (String) action.get("type");
                Map<String, Object> actionData = new HashMap<>();
                
                switch (actionType) {
                    case "keep":
                        result.put("action", "keep");
                        break;
                        
                    case "discard":
                        result.put("action", "discard");
                        break;
                        
                    case "fileinto":
                        String folder = (String) action.get("folder");
                        actionData.put("folder", folder);
                        result.put("action", "fileinto");
                        result.put("actionData", actionData);
                        break;
                        
                    case "redirect":
                        String address = (String) action.get("address");
                        actionData.put("address", address);
                        result.put("action", "redirect");
                        result.put("actionData", actionData);
                        break;
                        
                    case "reject":
                        String message = (String) action.get("message");
                        actionData.put("message", message);
                        result.put("action", "reject");
                        result.put("actionData", actionData);
                        break;
                        
                    case "stop":
                        result.put("action", "stop");
                        break;
                        
                    default:
                        result.put("action", "keep");
                }
                
                // 只执行第一个动作
                break;
            }
            
        } catch (Exception e) {
            log.error("执行规则动作失败: {}", e.getMessage(), e);
            result.put("action", "keep");
        }
        
        return result;
    }

    private List<Map<String, Object>> parseConditions(String conditionsJson) {
        try {
            if (conditionsJson == null || conditionsJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(conditionsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析条件失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> parseActions(String actionsJson) {
        try {
            if (actionsJson == null || actionsJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(actionsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析动作失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void updateRuleStatistics(SieveRule rule, boolean success, String error) {
        try {
            if (success) {
                rule.setAppliedCount(rule.getAppliedCount() + 1);
                rule.setLastAppliedAt(LocalDateTime.now());
            } else {
                rule.setErrorCount(rule.getErrorCount() + 1);
                rule.setLastError(error);
                rule.setLastErrorAt(LocalDateTime.now());
            }
            updateSieveRule(rule);
        } catch (Exception e) {
            log.error("更新规则统计失败: {}", e.getMessage(), e);
        }
    }

    private void recordRuleExecution(Long userAliasId, Map<String, Object> execution) {
        List<Map<String, Object>> history = executionHistory.computeIfAbsent(userAliasId, k -> new ArrayList<>());
        history.add(execution);
        
        // 保持历史记录在合理范围内
        if (history.size() > 1000) {
            history.subList(0, history.size() - 1000).clear();
        }
    }

    private boolean checkBracketMatching(String script) {
        int braceCount = 0;
        int parenCount = 0;
        int bracketCount = 0;
        
        for (char c : script.toCharArray()) {
            switch (c) {
                case '{': braceCount++; break;
                case '}': braceCount--; break;
                case '(': parenCount++; break;
                case ')': parenCount--; break;
                case '[': bracketCount++; break;
                case ']': bracketCount--; break;
            }
        }
        
        return braceCount == 0 && parenCount == 0 && bracketCount == 0;
    }

    private boolean checkBasicSyntax(String script) {
        // 简化的语法检查
        return script.contains("if") || script.contains("require") || script.contains("fileinto") || script.contains("redirect");
    }

    private List<String> checkUnsupportedCommands(String script) {
        List<String> unsupported = new ArrayList<>();
        String[] unsupportedCommands = {"vacation", "notify", "include"};
        
        for (String command : unsupportedCommands) {
            if (script.contains(command)) {
                unsupported.add(command);
            }
        }
        
        return unsupported;
    }

    private String generateConditionScript(Map<String, Object> condition) {
        String type = (String) condition.get("type");
        String comparator = (String) condition.get("comparator");
        String value = (String) condition.get("value");
        
        switch (type) {
            case "header":
                String header = (String) condition.get("header");
                return String.format("header :%s \"%s\" \"%s\"", comparator, header, value);
            case "body":
                return String.format("body :%s \"%s\"", comparator, value);
            case "size":
                return String.format("size :%s %s", comparator, value);
            case "envelope":
                String envelope = (String) condition.get("envelope");
                return String.format("envelope :%s \"%s\" \"%s\"", comparator, envelope, value);
            default:
                return "true";
        }
    }

    private String generateActionScript(Map<String, Object> action) {
        String type = (String) action.get("type");
        
        switch (type) {
            case "fileinto":
                return String.format("fileinto \"%s\";", action.get("folder"));
            case "redirect":
                return String.format("redirect \"%s\";", action.get("address"));
            case "reject":
                return String.format("reject \"%s\";", action.get("message"));
            case "discard":
                return "discard;";
            case "stop":
                return "stop;";
            default:
                return "keep;";
        }
    }

    // 其他解析和转换方法的简化实现
    private List<String> parseRequirements(String script) { return Arrays.asList("fileinto", "reject"); }
    private List<Map<String, Object>> parseRules(String script) { return new ArrayList<>(); }
    private List<String> parseComments(String script) { return new ArrayList<>(); }
    
    private void createSpamFilterRule(SieveRule rule, Map<String, Object> params) {
        rule.setRuleName("垃圾邮件过滤");
        rule.setRuleType("FILTER");
        // 简化实现
    }
    
    private void createSenderFilterRule(SieveRule rule, Map<String, Object> params) {
        rule.setRuleName("发件人过滤");
        rule.setRuleType("FILTER");
        // 简化实现
    }
    
    private void createSubjectFilterRule(SieveRule rule, Map<String, Object> params) {
        rule.setRuleName("主题过滤");
        rule.setRuleType("FILTER");
        // 简化实现
    }
    
    private void createForwardRule(SieveRule rule, Map<String, Object> params) {
        rule.setRuleName("邮件转发");
        rule.setRuleType("FORWARD");
        // 简化实现
    }
    
    private void createAutoReplyRule(SieveRule rule, Map<String, Object> params) {
        rule.setRuleName("自动回复");
        rule.setRuleType("VACATION");
        // 简化实现
    }
    
    private SieveRule convertToSieveRule(Long userAliasId, Map<String, Object> ruleData) {
        SieveRule rule = new SieveRule();
        rule.setUserAliasId(userAliasId);
        rule.setRuleName((String) ruleData.get("name"));
        // 简化实现
        return rule;
    }
    
    private SieveRule cloneRule(SieveRule original) {
        SieveRule clone = new SieveRule();
        clone.setRuleName(original.getRuleName());
        clone.setDescription(original.getDescription());
        clone.setSieveScript(original.getSieveScript());
        clone.setPriority(original.getPriority());
        clone.setRuleType(original.getRuleType());
        clone.setConditionType(original.getConditionType());
        clone.setConditions(original.getConditions());
        clone.setActionType(original.getActionType());
        clone.setActions(original.getActions());
        clone.setContinueProcessing(original.getContinueProcessing());
        return clone;
    }
}