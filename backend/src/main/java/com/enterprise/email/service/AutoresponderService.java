package com.enterprise.email.service;

import com.enterprise.email.entity.AutoresponderConfig;

import java.util.List;
import java.util.Map;

/**
 * 自动回复服务
 */
public interface AutoresponderService {

    /**
     * 创建自动回复配置
     */
    boolean createAutoresponderConfig(AutoresponderConfig config);

    /**
     * 更新自动回复配置
     */
    boolean updateAutoresponderConfig(AutoresponderConfig config);

    /**
     * 删除自动回复配置
     */
    boolean deleteAutoresponderConfig(Long configId);

    /**
     * 获取自动回复配置
     */
    AutoresponderConfig getAutoresponderConfig(Long configId);

    /**
     * 获取用户的所有自动回复配置
     */
    List<AutoresponderConfig> getUserAutoresponderConfigs(Long userAliasId);

    /**
     * 获取用户启用的自动回复配置
     */
    List<AutoresponderConfig> getUserActiveConfigs(Long userAliasId);

    /**
     * 处理邮件自动回复
     */
    Map<String, Object> processAutoReply(String emailContent, Long userAliasId);

    /**
     * 测试自动回复配置
     */
    Map<String, Object> testAutoReplyConfig(AutoresponderConfig config, String emailContent);

    /**
     * 发送自动回复邮件
     */
    boolean sendAutoReply(AutoresponderConfig config, Map<String, Object> originalEmail);

    /**
     * 检查是否应该发送自动回复
     */
    Map<String, Object> shouldSendAutoReply(AutoresponderConfig config, Map<String, Object> emailData);

    /**
     * 验证自动回复配置
     */
    Map<String, Object> validateAutoresponderConfig(AutoresponderConfig config);

    /**
     * 启用/禁用自动回复
     */
    boolean toggleAutoresponder(Long configId, boolean enabled);

    /**
     * 批量启用/禁用自动回复
     */
    boolean batchToggleAutoresponders(List<Long> configIds, boolean enabled);

    /**
     * 获取自动回复模板
     */
    List<Map<String, Object>> getAutoReplyTemplates();

    /**
     * 根据模板创建自动回复
     */
    boolean createAutoReplyFromTemplate(Long userAliasId, String templateName, Map<String, Object> parameters);

    /**
     * 复制自动回复配置
     */
    boolean copyAutoresponderConfig(Long configId, Long targetUserAliasId);

    /**
     * 导入自动回复配置
     */
    boolean importAutoresponderConfigs(Long userAliasId, String configData);

    /**
     * 导出自动回复配置
     */
    String exportAutoresponderConfigs(Long userAliasId);

    /**
     * 获取自动回复统计信息
     */
    Map<String, Object> getAutoReplyStatistics(Long userAliasId);

    /**
     * 获取自动回复历史记录
     */
    List<Map<String, Object>> getAutoReplyHistory(Long userAliasId, int limit);

    /**
     * 清理自动回复历史
     */
    boolean cleanupAutoReplyHistory(Long userAliasId, int days);

    /**
     * 重置自动回复计数器
     */
    boolean resetAutoReplyCounters(Long configId);

    /**
     * 批量重置计数器
     */
    boolean batchResetCounters(List<Long> configIds);

    /**
     * 获取自动回复性能指标
     */
    Map<String, Object> getPerformanceMetrics(Long userAliasId);

    /**
     * 优化自动回复性能
     */
    boolean optimizeAutoReplyPerformance(Long userAliasId);

    /**
     * 检测自动回复冲突
     */
    List<Map<String, Object>> detectAutoReplyConflicts(Long userAliasId);

    /**
     * 解决自动回复冲突
     */
    boolean resolveAutoReplyConflicts(Long userAliasId, List<Map<String, Object>> resolutions);

    /**
     * 预览自动回复内容
     */
    Map<String, Object> previewAutoReply(AutoresponderConfig config, Map<String, Object> sampleEmail);

    /**
     * 个性化自动回复内容
     */
    String personalizeAutoReplyContent(String template, Map<String, Object> emailData, Map<String, Object> userData);

    /**
     * 获取自动回复建议
     */
    List<Map<String, Object>> getAutoReplySuggestions(Long userAliasId, List<String> sampleEmails);

    /**
     * 学习并建议自动回复
     */
    List<Map<String, Object>> learnAndSuggestAutoReplies(Long userAliasId);

    /**
     * 设置自动回复假期模式
     */
    boolean setVacationMode(Long userAliasId, Map<String, Object> vacationConfig);

    /**
     * 获取假期模式配置
     */
    Map<String, Object> getVacationModeConfig(Long userAliasId);

    /**
     * 停止假期模式
     */
    boolean stopVacationMode(Long userAliasId);

    /**
     * 设置外出回复
     */
    boolean setOutOfOfficeReply(Long userAliasId, Map<String, Object> oooConfig);

    /**
     * 获取外出回复配置
     */
    Map<String, Object> getOutOfOfficeConfig(Long userAliasId);

    /**
     * 停止外出回复
     */
    boolean stopOutOfOfficeReply(Long userAliasId);

    /**
     * 批量处理自动回复
     */
    Map<String, Object> batchProcessAutoReplies(List<String> emails, Long userAliasId);

    /**
     * 获取自动回复调试信息
     */
    Map<String, Object> getAutoReplyDebugInfo(Long configId, String emailContent);

    /**
     * 启用/禁用调试模式
     */
    boolean toggleDebugMode(Long configId, boolean debugMode);

    /**
     * 获取自动回复报告
     */
    String generateAutoReplyReport(Long userAliasId, String startDate, String endDate);

    /**
     * 获取实时自动回复状态
     */
    Map<String, Object> getRealTimeAutoReplyStatus(Long userAliasId);

    /**
     * 备份自动回复配置
     */
    String backupAutoresponderConfigs(Long userAliasId);

    /**
     * 恢复自动回复配置
     */
    boolean restoreAutoresponderConfigs(Long userAliasId, String backupData);
}