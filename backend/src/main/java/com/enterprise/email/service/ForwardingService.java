package com.enterprise.email.service;

import com.enterprise.email.entity.ForwardingConfig;

import java.util.List;
import java.util.Map;

/**
 * 邮件转发服务
 */
public interface ForwardingService {

    /**
     * 创建转发配置
     */
    boolean createForwardingConfig(ForwardingConfig config);

    /**
     * 更新转发配置
     */
    boolean updateForwardingConfig(ForwardingConfig config);

    /**
     * 删除转发配置
     */
    boolean deleteForwardingConfig(Long configId);

    /**
     * 获取转发配置
     */
    ForwardingConfig getForwardingConfig(Long configId);

    /**
     * 获取用户的所有转发配置
     */
    List<ForwardingConfig> getUserForwardingConfigs(Long userAliasId);

    /**
     * 获取用户启用的转发配置
     */
    List<ForwardingConfig> getUserActiveConfigs(Long userAliasId);

    /**
     * 处理邮件转发
     */
    Map<String, Object> processEmailForwarding(String emailContent, Long userAliasId);

    /**
     * 测试转发配置
     */
    Map<String, Object> testForwardingConfig(ForwardingConfig config, String emailContent);

    /**
     * 转发邮件
     */
    boolean forwardEmail(ForwardingConfig config, Map<String, Object> emailData);

    /**
     * 检查是否应该转发
     */
    Map<String, Object> shouldForwardEmail(ForwardingConfig config, Map<String, Object> emailData);

    /**
     * 验证转发配置
     */
    Map<String, Object> validateForwardingConfig(ForwardingConfig config);

    /**
     * 启用/禁用转发
     */
    boolean toggleForwarding(Long configId, boolean enabled);

    /**
     * 批量启用/禁用转发
     */
    boolean batchToggleForwarding(List<Long> configIds, boolean enabled);

    /**
     * 获取转发模板
     */
    List<Map<String, Object>> getForwardingTemplates();

    /**
     * 根据模板创建转发规则
     */
    boolean createForwardingFromTemplate(Long userAliasId, String templateName, Map<String, Object> parameters);

    /**
     * 复制转发配置
     */
    boolean copyForwardingConfig(Long configId, Long targetUserAliasId);

    /**
     * 导入转发配置
     */
    boolean importForwardingConfigs(Long userAliasId, String configData);

    /**
     * 导出转发配置
     */
    String exportForwardingConfigs(Long userAliasId);

    /**
     * 获取转发统计信息
     */
    Map<String, Object> getForwardingStatistics(Long userAliasId);

    /**
     * 获取转发历史记录
     */
    List<Map<String, Object>> getForwardingHistory(Long userAliasId, int limit);

    /**
     * 清理转发历史
     */
    boolean cleanupForwardingHistory(Long userAliasId, int days);

    /**
     * 重置转发计数器
     */
    boolean resetForwardingCounters(Long configId);

    /**
     * 批量重置计数器
     */
    boolean batchResetCounters(List<Long> configIds);

    /**
     * 检测转发循环
     */
    Map<String, Object> detectForwardingLoop(String emailContent, List<String> targetAddresses);

    /**
     * 验证转发地址
     */
    Map<String, Object> validateForwardingAddresses(List<String> addresses);

    /**
     * 获取转发性能指标
     */
    Map<String, Object> getForwardingPerformanceMetrics(Long userAliasId);

    /**
     * 优化转发性能
     */
    boolean optimizeForwardingPerformance(Long userAliasId);

    /**
     * 检测转发冲突
     */
    List<Map<String, Object>> detectForwardingConflicts(Long userAliasId);

    /**
     * 解决转发冲突
     */
    boolean resolveForwardingConflicts(Long userAliasId, List<Map<String, Object>> resolutions);

    /**
     * 预览转发邮件
     */
    Map<String, Object> previewForwardedEmail(ForwardingConfig config, Map<String, Object> originalEmail);

    /**
     * 批量处理转发
     */
    Map<String, Object> batchProcessForwarding(List<String> emails, Long userAliasId);

    /**
     * 设置全局转发
     */
    boolean setGlobalForwarding(Long userAliasId, Map<String, Object> globalConfig);

    /**
     * 获取全局转发配置
     */
    Map<String, Object> getGlobalForwardingConfig(Long userAliasId);

    /**
     * 停止全局转发
     */
    boolean stopGlobalForwarding(Long userAliasId);

    /**
     * 设置临时转发
     */
    boolean setTemporaryForwarding(Long userAliasId, Map<String, Object> tempConfig);

    /**
     * 获取临时转发配置
     */
    Map<String, Object> getTemporaryForwardingConfig(Long userAliasId);

    /**
     * 停止临时转发
     */
    boolean stopTemporaryForwarding(Long userAliasId);

    /**
     * 获取转发建议
     */
    List<Map<String, Object>> getForwardingSuggestions(Long userAliasId, List<String> sampleEmails);

    /**
     * 自动配置转发
     */
    boolean autoConfigureForwarding(Long userAliasId, Map<String, Object> preferences);

    /**
     * 获取转发调试信息
     */
    Map<String, Object> getForwardingDebugInfo(Long configId, String emailContent);

    /**
     * 启用/禁用调试模式
     */
    boolean toggleForwardingDebugMode(Long configId, boolean debugMode);

    /**
     * 生成转发报告
     */
    String generateForwardingReport(Long userAliasId, String startDate, String endDate);

    /**
     * 获取实时转发状态
     */
    Map<String, Object> getRealTimeForwardingStatus(Long userAliasId);

    /**
     * 测试转发连接
     */
    Map<String, Object> testForwardingConnectivity(List<String> targetAddresses);

    /**
     * 获取转发队列状态
     */
    Map<String, Object> getForwardingQueueStatus();

    /**
     * 清理转发队列
     */
    boolean cleanupForwardingQueue();

    /**
     * 重试失败的转发
     */
    boolean retryFailedForwards(Long userAliasId);

    /**
     * 备份转发配置
     */
    String backupForwardingConfigs(Long userAliasId);

    /**
     * 恢复转发配置
     */
    boolean restoreForwardingConfigs(Long userAliasId, String backupData);
}