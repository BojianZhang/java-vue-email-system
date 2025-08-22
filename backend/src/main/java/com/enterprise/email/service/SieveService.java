package com.enterprise.email.service;

import com.enterprise.email.entity.SieveRule;

import java.util.List;
import java.util.Map;

/**
 * Sieve邮件过滤规则服务
 */
public interface SieveService {

    /**
     * 创建Sieve规则
     */
    boolean createSieveRule(SieveRule rule);

    /**
     * 更新Sieve规则
     */
    boolean updateSieveRule(SieveRule rule);

    /**
     * 删除Sieve规则
     */
    boolean deleteSieveRule(Long ruleId);

    /**
     * 获取Sieve规则
     */
    SieveRule getSieveRule(Long ruleId);

    /**
     * 获取用户的所有Sieve规则
     */
    List<SieveRule> getUserSieveRules(Long userAliasId);

    /**
     * 获取用户启用的Sieve规则（按优先级排序）
     */
    List<SieveRule> getUserActiveSieveRules(Long userAliasId);

    /**
     * 应用Sieve规则到邮件
     */
    Map<String, Object> applySieveRules(String emailContent, Long userAliasId);

    /**
     * 测试Sieve规则
     */
    Map<String, Object> testSieveRule(SieveRule rule, String emailContent);

    /**
     * 验证Sieve脚本语法
     */
    Map<String, Object> validateSieveScript(String sieveScript);

    /**
     * 生成Sieve脚本
     */
    String generateSieveScript(SieveRule rule);

    /**
     * 解析Sieve脚本
     */
    Map<String, Object> parseSieveScript(String sieveScript);

    /**
     * 批量应用规则
     */
    Map<String, Object> batchApplyRules(List<String> emails, Long userAliasId);

    /**
     * 获取规则模板
     */
    List<Map<String, Object>> getRuleTemplates();

    /**
     * 根据模板创建规则
     */
    boolean createRuleFromTemplate(Long userAliasId, String templateName, Map<String, Object> parameters);

    /**
     * 导入Sieve规则
     */
    boolean importSieveRules(Long userAliasId, String sieveScript);

    /**
     * 导出Sieve规则
     */
    String exportSieveRules(Long userAliasId);

    /**
     * 复制规则到其他用户
     */
    boolean copyRulesToUser(Long fromUserAliasId, Long toUserAliasId, List<Long> ruleIds);

    /**
     * 获取规则执行统计
     */
    Map<String, Object> getRuleStatistics(Long userAliasId);

    /**
     * 获取规则执行历史
     */
    List<Map<String, Object>> getRuleExecutionHistory(Long userAliasId, int limit);

    /**
     * 清理规则执行历史
     */
    boolean cleanupRuleHistory(Long userAliasId, int days);

    /**
     * 优化规则顺序
     */
    boolean optimizeRuleOrder(Long userAliasId);

    /**
     * 检测规则冲突
     */
    List<Map<String, Object>> detectRuleConflicts(Long userAliasId);

    /**
     * 修复规则冲突
     */
    boolean resolveRuleConflicts(Long userAliasId, List<Map<String, Object>> resolutions);

    /**
     * 启用/禁用规则
     */
    boolean toggleRule(Long ruleId, boolean enabled);

    /**
     * 批量启用/禁用规则
     */
    boolean batchToggleRules(List<Long> ruleIds, boolean enabled);

    /**
     * 调整规则优先级
     */
    boolean adjustRulePriority(Long ruleId, int newPriority);

    /**
     * 批量调整规则优先级
     */
    boolean batchAdjustPriority(List<Map<String, Object>> priorityMappings);

    /**
     * 获取规则建议
     */
    List<Map<String, Object>> getRuleSuggestions(Long userAliasId, List<String> sampleEmails);

    /**
     * 自动创建规则
     */
    boolean autoCreateRules(Long userAliasId, Map<String, Object> preferences);

    /**
     * 学习用户行为并建议规则
     */
    List<Map<String, Object>> learnAndSuggestRules(Long userAliasId);

    /**
     * 获取过滤器性能指标
     */
    Map<String, Object> getFilterPerformanceMetrics(Long userAliasId);

    /**
     * 优化过滤器性能
     */
    boolean optimizeFilterPerformance(Long userAliasId);

    /**
     * 备份规则配置
     */
    String backupRuleConfiguration(Long userAliasId);

    /**
     * 恢复规则配置
     */
    boolean restoreRuleConfiguration(Long userAliasId, String backupData);

    /**
     * 获取规则编辑器配置
     */
    Map<String, Object> getRuleEditorConfig();

    /**
     * 验证规则权限
     */
    boolean validateRulePermissions(Long userAliasId, SieveRule rule);

    /**
     * 获取规则调试信息
     */
    Map<String, Object> getRuleDebugInfo(Long ruleId, String emailContent);

    /**
     * 启用/禁用规则调试模式
     */
    boolean toggleRuleDebugMode(Long ruleId, boolean debugMode);
}