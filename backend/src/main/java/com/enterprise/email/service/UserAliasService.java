package com.enterprise.email.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.email.entity.UserAlias;

import java.util.List;

/**
 * 用户别名服务接口
 */
public interface UserAliasService extends IService<UserAlias> {

    /**
     * 根据用户ID获取别名列表
     */
    List<UserAlias> getAliasesByUserId(Long userId);

    /**
     * 根据别名地址获取别名
     */
    UserAlias getAliasByAddress(String aliasAddress);

    /**
     * 检查别名是否属于用户
     */
    boolean isAliasOwnedByUser(Long aliasId, Long userId);

    /**
     * 创建用户别名
     */
    UserAlias createAlias(Long userId, Long domainId, String aliasAddress, String aliasName);

    /**
     * 更新别名信息
     */
    void updateAlias(Long aliasId, Long userId, String aliasName, Boolean isDefault);

    /**
     * 删除用户别名
     */
    void deleteAlias(Long aliasId, Long userId);

    /**
     * 设置默认别名
     */
    void setDefaultAlias(Long aliasId, Long userId);

    /**
     * 获取用户的默认别名
     */
    UserAlias getDefaultAlias(Long userId);

    /**
     * 获取所有活跃的别名
     */
    List<UserAlias> getAllActiveAliases();

    /**
     * 验证别名地址是否可用
     */
    boolean isAliasAddressAvailable(String aliasAddress, Long domainId);

    /**
     * 切换到指定别名
     */
    UserAlias switchToAlias(Long aliasId, Long userId);

    /**
     * 获取别名的邮件统计信息
     */
    List<AliasStats> getAliasStatsForUser(Long userId);

    /**
     * 别名统计信息类
     */
    class AliasStats {
        private Long aliasId;
        private String aliasAddress;
        private String aliasName;
        private Long totalEmails;
        private Long unreadEmails;
        private Boolean isDefault;

        // 构造函数
        public AliasStats(Long aliasId, String aliasAddress, String aliasName, 
                         Long totalEmails, Long unreadEmails, Boolean isDefault) {
            this.aliasId = aliasId;
            this.aliasAddress = aliasAddress;
            this.aliasName = aliasName;
            this.totalEmails = totalEmails;
            this.unreadEmails = unreadEmails;
            this.isDefault = isDefault;
        }

        // Getters
        public Long getAliasId() { return aliasId; }
        public String getAliasAddress() { return aliasAddress; }
        public String getAliasName() { return aliasName; }
        public Long getTotalEmails() { return totalEmails; }
        public Long getUnreadEmails() { return unreadEmails; }
        public Boolean getIsDefault() { return isDefault; }

        // Setters
        public void setAliasId(Long aliasId) { this.aliasId = aliasId; }
        public void setAliasAddress(String aliasAddress) { this.aliasAddress = aliasAddress; }
        public void setAliasName(String aliasName) { this.aliasName = aliasName; }
        public void setTotalEmails(Long totalEmails) { this.totalEmails = totalEmails; }
        public void setUnreadEmails(Long unreadEmails) { this.unreadEmails = unreadEmails; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    }
}