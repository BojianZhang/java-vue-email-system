package com.enterprise.email.controller;

import com.enterprise.email.dto.AuthResponse;
import com.enterprise.email.dto.LoginRequest;
import com.enterprise.email.dto.RegisterRequest;
import com.enterprise.email.service.AuthService;
import com.enterprise.email.utils.ResponseResult;
import com.enterprise.email.utils.WebUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseResult<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest request) {
        String ipAddress = WebUtils.getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        
        AuthResponse response = authService.login(loginRequest, ipAddress, userAgent);
        
        log.info("用户登录成功: {}, IP: {}", loginRequest.getEmail(), ipAddress);
        
        return ResponseResult.success(response, "登录成功");
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseResult<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);
        
        log.info("用户注册成功: {}", registerRequest.getEmail());
        
        return ResponseResult.success(response, "注册成功");
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh-token")
    public ResponseResult<AuthResponse> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);
        
        return ResponseResult.success(response, "令牌刷新成功");
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseResult<Void> logout(HttpServletRequest request) {
        String token = WebUtils.getJwtFromRequest(request);
        if (token != null) {
            authService.logout(token);
            log.info("用户登出成功");
        }
        
        return ResponseResult.success(null, "登出成功");
    }

    /**
     * 验证邮箱
     */
    @PostMapping("/verify-email")
    public ResponseResult<Void> verifyEmail(@RequestParam("token") String token) {
        boolean success = authService.verifyEmail(token);
        
        if (success) {
            return ResponseResult.success(null, "邮箱验证成功");
        } else {
            return ResponseResult.error("验证失败，令牌无效或已过期");
        }
    }

    /**
     * 发送重置密码邮件
     */
    @PostMapping("/forgot-password")
    public ResponseResult<Void> forgotPassword(@RequestParam("email") String email) {
        authService.sendPasswordResetEmail(email);
        
        return ResponseResult.success(null, "重置密码邮件已发送");
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public ResponseResult<Void> resetPassword(@RequestParam("token") String token,
                                              @RequestParam("password") String password) {
        boolean success = authService.resetPassword(token, password);
        
        if (success) {
            return ResponseResult.success(null, "密码重置成功");
        } else {
            return ResponseResult.error("重置失败，令牌无效或已过期");
        }
    }
}