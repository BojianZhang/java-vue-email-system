package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AD配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ad_configurations")
public class AdConfiguration {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 配置名称
     */
    @TableField("name")
    private String name;

    /**
     * AD域名
     */
    @TableField("domain_name")
    private String domainName;

    /**
     * 域控制器地址
     */
    @TableField("domain_controller")
    private String domainController;

    /**
     * 服务器端口
     */
    @TableField("port")
    private Integer port;

    /**
     * 全局目录端口
     */
    @TableField("global_catalog_port")
    private Integer globalCatalogPort;

    /**
     * 基础DN
     */
    @TableField("base_dn")
    private String baseDn;

    /**
     * 用户容器DN
     */
    @TableField("user_container_dn")
    private String userContainerDn;

    /**
     * 组容器DN
     */
    @TableField("group_container_dn")
    private String groupContainerDn;

    /**
     * 用户搜索过滤器
     */
    @TableField("user_search_filter")
    private String userSearchFilter;

    /**
     * 组搜索过滤器
     */
    @TableField("group_search_filter")
    private String groupSearchFilter;

    /**
     * 用户主体名称属性
     */
    @TableField("user_principal_name_attribute")
    private String userPrincipalNameAttribute;

    /**
     * SAM账户名属性
     */
    @TableField("sam_account_name_attribute")
    private String samAccountNameAttribute;

    /**
     * 显示名属性
     */
    @TableField("display_name_attribute")
    private String displayNameAttribute;

    /**
     * 邮箱属性
     */
    @TableField("mail_attribute")
    private String mailAttribute;

    /**
     * 代理地址属性
     */
    @TableField("proxy_addresses_attribute")
    private String proxyAddressesAttribute;

    /**
     * 部门属性
     */
    @TableField("department_attribute")
    private String departmentAttribute;

    /**
     * 职位属性
     */
    @TableField("title_attribute")
    private String titleAttribute;

    /**
     * 电话属性
     */
    @TableField("phone_attribute")
    private String phoneAttribute;

    /**
     * 组织单位属性
     */
    @TableField("organizational_unit_attribute")
    private String organizationalUnitAttribute;

    /**
     * 服务账户用户名
     */
    @TableField("service_account_username")
    private String serviceAccountUsername;

    /**
     * 服务账户密码
     */
    @TableField("service_account_password")
    private String serviceAccountPassword;

    /**
     * 服务账户域
     */
    @TableField("service_account_domain")
    private String serviceAccountDomain;

    /**
     * 是否启用SSL
     */
    @TableField("enable_ssl")
    private Boolean enableSsl;

    /**
     * 是否启用TLS
     */
    @TableField("enable_tls")
    private Boolean enableTls;

    /**
     * 是否启用全局目录
     */
    @TableField("enable_global_catalog")
    private Boolean enableGlobalCatalog;

    /**
     * 连接超时（秒）
     */
    @TableField("connection_timeout")
    private Integer connectionTimeout;

    /**
     * 读取超时（秒）
     */
    @TableField("read_timeout")
    private Integer readTimeout;

    /**
     * 认证方式
     */
    @TableField("authentication_method")
    private String authenticationMethod; // SIMPLE, KERBEROS, NTLM

    /**
     * Kerberos配置
     */
    @TableField("kerberos_config")
    private String kerberosConfig; // JSON格式

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 是否为默认配置
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 同步策略
     */
    @TableField("sync_strategy")
    private String syncStrategy; // MANUAL, SCHEDULED, REAL_TIME

    /**
     * 同步间隔（分钟）
     */
    @TableField("sync_interval")
    private Integer syncInterval;

    /**
     * 上次同步时间
     */
    @TableField("last_sync_at")
    private LocalDateTime lastSyncAt;

    /**
     * 下次同步时间
     */
    @TableField("next_sync_at")
    private LocalDateTime nextSyncAt;

    /**
     * 同步状态
     */
    @TableField("sync_status")
    private String syncStatus; // IDLE, SYNCING, SUCCESS, FAILED

    /**
     * 同步错误信息
     */
    @TableField("sync_error")
    private String syncError;

    /**
     * 用户同步范围
     */
    @TableField("user_sync_scope")
    private String userSyncScope; // ALL, OU_SPECIFIC, GROUP_SPECIFIC

    /**
     * 组同步范围
     */
    @TableField("group_sync_scope")
    private String groupSyncScope; // ALL, OU_SPECIFIC, SECURITY_GROUPS_ONLY

    /**
     * 包含的组织单位
     */
    @TableField("included_ous")
    private String includedOus; // JSON数组

    /**
     * 排除的组织单位
     */
    @TableField("excluded_ous")
    private String excludedOus; // JSON数组

    /**
     * 包含的组
     */
    @TableField("included_groups")
    private String includedGroups; // JSON数组

    /**
     * 排除的组
     */
    @TableField("excluded_groups")
    private String excludedGroups; // JSON数组

    /**
     * 用户映射配置
     */
    @TableField("user_mapping")
    private String userMapping; // JSON格式

    /**
     * 组映射配置
     */
    @TableField("group_mapping")
    private String groupMapping; // JSON格式

    /**
     * 属性映射配置
     */
    @TableField("attribute_mapping")
    private String attributeMapping; // JSON格式

    /**
     * 密码策略配置
     */
    @TableField("password_policy")
    private String passwordPolicy; // JSON格式

    /**
     * 账户锁定策略
     */
    @TableField("account_lockout_policy")
    private String accountLockoutPolicy; // JSON格式

    /**
     * 高级配置
     */
    @TableField("advanced_config")
    private String advancedConfig; // JSON格式

    /**
     * 测试状态
     */
    @TableField("test_status")
    private String testStatus; // UNTESTED, PASSED, FAILED

    /**
     * 测试时间
     */
    @TableField("test_at")
    private LocalDateTime testAt;

    /**
     * 测试错误信息
     */
    @TableField("test_error")
    private String testError;

    /**
     * 统计信息
     */
    @TableField("stats")
    private String stats; // JSON格式

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建者
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}