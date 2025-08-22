package com.enterprise.email.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.email.entity.ExternalAliasSync;

import java.util.List;
import java.util.Map;

/**
 * 外部别名同步服务接口
 */
public interface ExternalAliasSyncService {

    /**
     * 创建外部别名同步配置
     */
    boolean createSyncConfig(ExternalAliasSync syncConfig);

    /**
     * 更新同步配置
     */
    boolean updateSyncConfig(ExternalAliasSync syncConfig);

    /**
     * 删除同步配置
     */
    boolean deleteSyncConfig(Long syncId);

    /**
     * 根据ID获取同步配置
     */
    ExternalAliasSync getSyncConfigById(Long syncId);

    /**
     * 根据别名ID获取同步配置
     */
    ExternalAliasSync getSyncConfigByAliasId(Long aliasId);

    /**
     * 根据用户ID获取所有同步配置
     */
    List<ExternalAliasSync> getSyncConfigsByUserId(Long userId);

    /**
     * 分页查询同步配置
     */
    IPage<ExternalAliasSync> getSyncConfigsPage(Page<ExternalAliasSync> page,
                                               Long userId,
                                               String platformType,
                                               String syncStatus);

    /**
     * 立即同步指定别名名称
     */
    boolean syncAliasName(Long aliasId);

    /**
     * 批量同步用户的所有别名
     */
    Map<String, Object> batchSyncUserAliases(Long userId);

    /**
     * 自动同步任务（定时任务调用）
     */
    void autoSyncTask();

    /**
     * 重试失败的同步任务
     */
    void retryFailedSyncs();

    /**
     * 测试外部平台连接
     */
    boolean testPlatformConnection(ExternalAliasSync syncConfig);

    /**
     * 从外部平台获取别名信息
     */
    ExternalAliasInfo fetchExternalAliasInfo(ExternalAliasSync syncConfig);

    /**
     * 启用/禁用自动同步
     */
    boolean toggleAutoSync(Long syncId, Boolean enabled);

    /**
     * 获取支持的平台类型列表
     */
    List<PlatformType> getSupportedPlatforms();

    /**
     * 验证平台配置
     */
    boolean validatePlatformConfig(ExternalAliasSync syncConfig);

    /**
     * 外部别名信息类
     */
    class ExternalAliasInfo {
        private String aliasId;
        private String aliasAddress;
        private String aliasName;
        private String description;
        private Boolean isActive;
        private String lastModified;

        // Getters and setters
        public String getAliasId() { return aliasId; }
        public void setAliasId(String aliasId) { this.aliasId = aliasId; }
        public String getAliasAddress() { return aliasAddress; }
        public void setAliasAddress(String aliasAddress) { this.aliasAddress = aliasAddress; }
        public String getAliasName() { return aliasName; }
        public void setAliasName(String aliasName) { this.aliasName = aliasName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public String getLastModified() { return lastModified; }
        public void setLastModified(String lastModified) { this.lastModified = lastModified; }
    }

    /**
     * 平台类型类
     */
    class PlatformType {
        private String code;
        private String name;
        private String description;
        private List<String> requiredFields;
        private String apiDocUrl;

        public PlatformType(String code, String name, String description, List<String> requiredFields, String apiDocUrl) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.requiredFields = requiredFields;
            this.apiDocUrl = apiDocUrl;
        }

        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getRequiredFields() { return requiredFields; }
        public void setRequiredFields(List<String> requiredFields) { this.requiredFields = requiredFields; }
        public String getApiDocUrl() { return apiDocUrl; }
        public void setApiDocUrl(String apiDocUrl) { this.apiDocUrl = apiDocUrl; }
    }
}