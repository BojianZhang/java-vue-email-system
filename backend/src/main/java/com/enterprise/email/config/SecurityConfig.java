package com.enterprise.email.config;

import com.enterprise.email.security.JwtAuthenticationFilter;
import com.enterprise.email.security.JwtTokenProvider;
import com.enterprise.email.security.UserDetailsServiceImpl;
import com.enterprise.email.service.LoginAnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtTokenProvider tokenProvider;
    private final LoginAnomalyDetectionService loginAnomalyDetectionService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider, userDetailsService, loginAnomalyDetectionService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // 公开访问的端点
                .requestMatchers("/auth/login", "/auth/register", "/auth/refresh-token").permitAll()
                .requestMatchers("/health", "/info").permitAll()
                .requestMatchers("/druid/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // 管理员端点
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/security/**").hasRole("ADMIN")
                
                // 用户端点
                .requestMatchers(HttpMethod.GET, "/users/profile").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/users/profile").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/users/security/**").hasAnyRole("USER", "ADMIN")
                
                // 邮件相关端点
                .requestMatchers("/emails/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/aliases/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/domains/list").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/domains/**").hasRole("ADMIN")
                
                // 其他需要认证
                .anyRequest().authenticated()
            );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的域名
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));
        
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 允许携带认证信息
        configuration.setAllowCredentials(true);
        
        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        // 预检请求的缓存时间
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}