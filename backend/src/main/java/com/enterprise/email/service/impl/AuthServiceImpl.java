package com.enterprise.email.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.email.dto.AuthResponse;
import com.enterprise.email.dto.LoginRequest;
import com.enterprise.email.dto.RegisterRequest;
import com.enterprise.email.entity.User;
import com.enterprise.email.mapper.UserMapper;
import com.enterprise.email.security.JwtTokenProvider;
import com.enterprise.email.security.UserPrincipal;
import com.enterprise.email.service.AuthService;
import com.enterprise.email.service.LoginAnomalyDetectionService;
import com.enterprise.email.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginAnomalyDetectionService loginAnomalyDetectionService;
    private final RedisUtil redisUtil;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest, String ipAddress, String userAgent) {
        // 验证用户凭证
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                loginRequest.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();

        // 记录登录日志并进行异常检测
        loginAnomalyDetectionService.recordLogin(user.getId(), ipAddress, userAgent);

        // 更新最后登录信息
        baseMapper.updateLastLogin(user.getId(), ipAddress);

        // 生成JWT令牌
        String accessToken = tokenProvider.generateToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        // 缓存刷新令牌
        redisUtil.set("refresh_token:" + user.getId(), refreshToken, 7 * 24 * 60 * 60); // 7天

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(86400L) // 24小时
            .userInfo(buildUserInfo(user))
            .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        User existingUser = baseMapper.findByUsername(registerRequest.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        existingUser = baseMapper.findByEmail(registerRequest.getEmail());
        if (existingUser != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 检查密码确认
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 创建新用户
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setDisplayName(registerRequest.getDisplayName());
        newUser.setRole(User.ROLE_USER);
        newUser.setIsActive(true);
        newUser.setEmailVerified(false);
        newUser.setLoginCount(0);
        newUser.setStorageQuota(1024L); // 默认1GB
        newUser.setStorageUsed(0L);
        newUser.setTimezone("Asia/Shanghai");
        newUser.setLanguage("zh-CN");

        save(newUser);

        // 创建UserPrincipal并生成令牌
        UserPrincipal userPrincipal = new UserPrincipal(newUser);
        String accessToken = tokenProvider.generateToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        // 缓存刷新令牌
        redisUtil.set("refresh_token:" + newUser.getId(), refreshToken, 7 * 24 * 60 * 60);

        log.info("新用户注册成功: {}", newUser.getEmail());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(86400L)
            .userInfo(buildUserInfo(newUser))
            .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("刷新令牌无效或已过期");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);

        // 检查Redis中的刷新令牌
        String cachedToken = (String) redisUtil.get("refresh_token:" + userId);
        if (!refreshToken.equals(cachedToken)) {
            throw new RuntimeException("刷新令牌无效");
        }

        User user = getById(userId);
        if (user == null || !user.getIsActive()) {
            throw new RuntimeException("用户不存在或已被禁用");
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String newAccessToken = tokenProvider.generateToken(userPrincipal);
        String newRefreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        // 更新缓存的刷新令牌
        redisUtil.set("refresh_token:" + userId, newRefreshToken, 7 * 24 * 60 * 60);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .expiresIn(86400L)
            .userInfo(buildUserInfo(user))
            .build();
    }

    @Override
    public void logout(String token) {
        if (tokenProvider.validateToken(token)) {
            Long userId = tokenProvider.getUserIdFromToken(token);
            // 删除刷新令牌
            redisUtil.delete("refresh_token:" + userId);
            
            // 将访问令牌加入黑名单
            redisUtil.set("blacklist_token:" + token, "true", 86400); // 24小时
            
            // 更新登录日志中的登出时间
            loginAnomalyDetectionService.recordLogout(userId);
        }
    }

    @Override
    public boolean verifyEmail(String token) {
        // TODO: 实现邮箱验证逻辑
        return false;
    }

    @Override
    public void sendPasswordResetEmail(String email) {
        // TODO: 实现发送重置密码邮件逻辑
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        // TODO: 实现重置密码逻辑
        return false;
    }

    /**
     * 构建用户信息
     */
    private AuthResponse.UserInfo buildUserInfo(User user) {
        return AuthResponse.UserInfo.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .role(user.getRole())
            .isActive(user.getIsActive())
            .avatarUrl(user.getAvatarUrl())
            .build();
    }
}