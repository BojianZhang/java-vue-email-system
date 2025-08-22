package com.enterprise.email.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * LDAP配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ldap_configurations")
public class LdapConfiguration {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 配置名称
     */
    @TableField("name")
    private String name;

    /**
     * LDAP服务器地址
     */
    @TableField("server_url")
    private String serverUrl;

    /**
     * 服务器端口
     */
    @TableField("port")
    private Integer port;

    /**
     * 协议类型
     */
    @TableField("protocol")
    private String protocol; // LDAP, LDAPS

    /**
     * 基础DN
     */
    @TableField("base_dn")
    private String baseDn;

    /**
     * 用户搜索基础DN
     */
    @TableField("user_base_dn")
    private String userBaseDn;

    /**
     * 用户搜索过滤器
     */
    @TableField("user_search_filter")
    private String userSearchFilter;

    /**
     * 用户名属性
     */
    @TableField("username_attribute")
    private String usernameAttribute;

    /**
     * 邮箱属性
     */
    @TableField("email_attribute")
    private String emailAttribute;

    /**
     * 显示名属性
     */
    @TableField("display_name_attribute")
    private String displayNameAttribute;

    /**
     * 组搜索基础DN
     */
    @TableField("group_base_dn")
    private String groupBaseDn;

    /**
     * 组搜索过滤器
     */
    @TableField("group_search_filter")
    private String groupSearchFilter;

    /**
     * 组成员属性
     */
    @TableField("group_member_attribute")
    private String groupMemberAttribute;

    /**
     * 绑定DN
     */
    @TableField("bind_dn")
    private String bindDn;

    /**
     * 绑定密码
     */
    @TableField("bind_password")
    private String bindPassword;

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
     * 连接池大小
     */
    @TableField("pool_size")
    private Integer poolSize;

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