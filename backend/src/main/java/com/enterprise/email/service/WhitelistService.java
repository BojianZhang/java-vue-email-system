package com.enterprise.email.service;

import com.enterprise.email.entity.Whitelist;

import java.util.List;
import java.util.Map;

/**
 * 白名单服务
 */
public interface WhitelistService {

    /**
     * 创建白名单
     */
    boolean createWhitelist(Whitelist whitelist);

    /**
     * 更新白名单
     */
    boolean updateWhitelist(Whitelist whitelist);

    /**
     * 删除白名单
     */
    boolean deleteWhitelist(Long whitelistId);

    /**
     * 获取白名单
     */
    Whitelist getWhitelist(Long whitelistId);

    /**
     * 根据类型获取白名单
     */
    List<Whitelist> getWhitelistsByType(String type);

    /**
     * 根据域名获取白名单
     */
    List<Whitelist> getWhitelistsByDomain(String domain);

    /**
     * 检查是否在白名单中
     */
    boolean isInWhitelist(String value, String type, String domain);

    /**
     * 检查邮件地址是否在白名单中
     */
    boolean isEmailInWhitelist(String email, String domain);

    /**
     * 检查IP地址是否在白名单中
     */
    boolean isIpInWhitelist(String ipAddress, String domain);

    /**
     * 检查域名是否在白名单中
     */
    boolean isDomainInWhitelist(String domain);

    /**
     * 批量添加白名单
     */
    Map<String, Object> batchAddWhitelists(List<Whitelist> whitelists);

    /**
     * 批量删除白名单
     */
    boolean batchDeleteWhitelists(List<Long> whitelistIds);

    /**
     * 导入白名单
     */
    boolean importWhitelists(String data, String format);

    /**
     * 导出白名单
     */
    String exportWhitelists(String type, String format);

    /**
     * 获取白名单统计信息
     */
    Map<String, Object> getWhitelistStatistics();

    /**
     * 启用/禁用白名单
     */
    boolean toggleWhitelist(Long whitelistId, boolean enabled);

    /**
     * 批量启用/禁用白名单
     */
    boolean batchToggleWhitelists(List<Long> whitelistIds, boolean enabled);

    /**
     * 验证白名单有效性
     */
    Map<String, Object> validateWhitelist(Whitelist whitelist);

    /**
     * 搜索白名单
     */
    List<Whitelist> searchWhitelists(Map<String, Object> criteria);

    /**
     * 获取即将过期的白名单
     */
    List<Whitelist> getExpiringWhitelists(int days);

    /**
     * 清理过期白名单
     */
    boolean cleanupExpiredWhitelists();

    /**
     * 获取白名单模板
     */
    List<Map<String, Object>> getWhitelistTemplates();

    /**
     * 从模板创建白名单
     */
    boolean createFromTemplate(String templateName, Map<String, Object> parameters);

    /**
     * 复制白名单
     */
    boolean copyWhitelist(Long whitelistId, String newName);

    /**
     * 合并白名单
     */
    boolean mergeWhitelists(List<Long> whitelistIds, String newName);

    /**
     * 获取白名单使用情况
     */
    Map<String, Object> getWhitelistUsage(Long whitelistId);

    /**
     * 记录白名单匹配
     */
    void recordWhitelistMatch(Long whitelistId, String matchedValue, String sourceIp);

    /**
     * 获取白名单匹配历史
     */
    List<Map<String, Object>> getWhitelistMatchHistory(Long whitelistId, int limit);

    /**
     * 分析白名单效果
     */
    Map<String, Object> analyzeWhitelistEffectiveness();

    /**
     * 优化白名单配置
     */
    List<Map<String, Object>> optimizeWhitelistConfiguration();

    /**
     * 检测白名单冲突
     */
    List<Map<String, Object>> detectWhitelistConflicts();

    /**
     * 修复白名单冲突
     */
    boolean resolveWhitelistConflicts(List<Map<String, Object>> resolutions);

    /**
     * 备份白名单配置
     */
    String backupWhitelistConfiguration();

    /**
     * 恢复白名单配置
     */
    boolean restoreWhitelistConfiguration(String backupData);

    /**
     * 同步白名单到外部系统
     */
    boolean syncWhitelistToExternal(String system, Map<String, Object> config);

    /**
     * 从外部系统同步白名单
     */
    boolean syncWhitelistFromExternal(String system, Map<String, Object> config);

    /**
     * 获取白名单建议
     */
    List<Map<String, Object>> getWhitelistSuggestions(String domain);

    /**
     * 自动学习白名单
     */
    boolean autoLearnWhitelist(String domain, Map<String, Object> config);
}