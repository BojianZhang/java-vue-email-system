package com.enterprise.email.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.email.entity.User;
import com.enterprise.email.dto.LoginRequest;
import com.enterprise.email.dto.RegisterRequest;
import com.enterprise.email.dto.AuthResponse;

/**
 * 认证服务接口
 */
public interface AuthService extends IService<User> {

    /**
     * 用户登录
     */
    AuthResponse login(LoginRequest loginRequest, String ipAddress, String userAgent);

    /**
     * 用户注册
     */
    AuthResponse register(RegisterRequest registerRequest);

    /**
     * 刷新令牌
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * 用户登出
     */
    void logout(String token);

    /**
     * 验证邮箱
     */
    boolean verifyEmail(String token);

    /**
     * 发送重置密码邮件
     */
    void sendPasswordResetEmail(String email);

    /**
     * 重置密码
     */
    boolean resetPassword(String token, String newPassword);
}