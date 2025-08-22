package com.enterprise.email.dto;

import lombok.Data;
import lombok.Builder;

/**
 * 认证响应DTO
 */
@Data
@Builder
public class AuthResponse {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String displayName;
        private String role;
        private Boolean isActive;
        private String avatarUrl;
    }
}