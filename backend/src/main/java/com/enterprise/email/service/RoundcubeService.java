package com.enterprise.email.service;

import com.enterprise.email.entity.UserAlias;

import java.util.Map;

/**
 * Roundcube Webmail集成服务
 */
public interface RoundcubeService {

    /**
     * 创建Roundcube用户会话
     */
    String createUserSession(UserAlias alias, String password);

    /**
     * 验证Roundcube会话
     */
    boolean validateSession(String sessionId);

    /**
     * 销毁Roundcube会话
     */
    void destroySession(String sessionId);

    /**
     * 获取Roundcube配置
     */
    Map<String, Object> getRoundcubeConfig(String domain);

    /**
     * 更新Roundcube配置
     */
    boolean updateRoundcubeConfig(String domain, Map<String, Object> config);

    /**
     * 生成Roundcube登录令牌
     */
    String generateLoginToken(UserAlias alias);

    /**
     * 验证登录令牌
     */
    boolean validateLoginToken(String token, String email);

    /**
     * 获取Roundcube访问URL
     */
    String getRoundcubeUrl(UserAlias alias);

    /**
     * 同步用户设置到Roundcube
     */
    boolean syncUserSettings(UserAlias alias, Map<String, Object> settings);

    /**
     * 从Roundcube获取用户设置
     */
    Map<String, Object> getUserSettings(UserAlias alias);

    /**
     * 安装/更新Roundcube插件
     */
    boolean installPlugin(String pluginName, String version);

    /**
     * 启用/禁用Roundcube插件
     */
    boolean togglePlugin(String pluginName, boolean enabled);

    /**
     * 获取Roundcube状态
     */
    Map<String, Object> getRoundcubeStatus();

    /**
     * 重启Roundcube服务
     */
    boolean restartRoundcube();
}