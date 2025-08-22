package com.enterprise.email.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SSL/TLS 安全配置
 * 配置HTTPS重定向和安全头
 */
@Configuration
public class SslSecurityConfig {

    /**
     * HTTPS 重定向过滤器
     */
    @Bean
    public FilterRegistrationBean<HttpsRedirectFilter> httpsRedirectFilter() {
        FilterRegistrationBean<HttpsRedirectFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new HttpsRedirectFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registrationBean.setName("httpsRedirectFilter");
        return registrationBean;
    }

    /**
     * 安全头过滤器
     */
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SecurityHeadersFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registrationBean.setName("securityHeadersFilter");
        return registrationBean;
    }

    /**
     * HTTPS重定向过滤器实现
     */
    public static class HttpsRedirectFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // 获取请求的协议和端口
            String scheme = httpRequest.getScheme();
            int serverPort = httpRequest.getServerPort();
            String serverName = httpRequest.getServerName();
            String requestURI = httpRequest.getRequestURI();
            String queryString = httpRequest.getQueryString();

            // 检查是否需要HTTPS重定向
            boolean needsRedirect = false;
            
            // 如果是HTTP请求且不是本地开发环境
            if ("http".equals(scheme) && !isLocalDevelopment(serverName, serverPort)) {
                needsRedirect = true;
            }

            // 检查X-Forwarded-Proto头（用于负载均衡器后面的情况）
            String xForwardedProto = httpRequest.getHeader("X-Forwarded-Proto");
            if ("http".equals(xForwardedProto) && !isLocalDevelopment(serverName, serverPort)) {
                needsRedirect = true;
            }

            if (needsRedirect) {
                // 构建HTTPS重定向URL
                StringBuilder httpsUrl = new StringBuilder();
                httpsUrl.append("https://").append(serverName);
                
                // 如果不是标准HTTPS端口，添加端口号
                if (serverPort != 80 && serverPort != 443) {
                    httpsUrl.append(":").append(serverPort);
                }
                
                httpsUrl.append(requestURI);
                if (queryString != null && !queryString.isEmpty()) {
                    httpsUrl.append("?").append(queryString);
                }

                // 301永久重定向到HTTPS
                httpResponse.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                httpResponse.setHeader("Location", httpsUrl.toString());
                return;
            }

            chain.doFilter(request, response);
        }

        /**
         * 检查是否为本地开发环境
         */
        private boolean isLocalDevelopment(String serverName, int serverPort) {
            return "localhost".equals(serverName) || 
                   "127.0.0.1".equals(serverName) ||
                   serverPort == 8080 || // 开发端口
                   serverPort == 3000;   // 前端开发端口
        }
    }

    /**
     * 安全头过滤器实现
     */
    public static class SecurityHeadersFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // 只为HTTPS请求添加安全头
            if (isSecureRequest(httpRequest)) {
                addSecurityHeaders(httpResponse);
            }

            chain.doFilter(request, response);
        }

        /**
         * 检查是否为安全请求
         */
        private boolean isSecureRequest(HttpServletRequest request) {
            return "https".equals(request.getScheme()) ||
                   "https".equals(request.getHeader("X-Forwarded-Proto"));
        }

        /**
         * 添加安全头
         */
        private void addSecurityHeaders(HttpServletResponse response) {
            // HSTS (HTTP Strict Transport Security)
            // 强制浏览器使用HTTPS连接，有效期1年，包含子域名，允许预加载
            response.setHeader("Strict-Transport-Security", 
                "max-age=31536000; includeSubDomains; preload");

            // X-Frame-Options
            // 防止点击劫持攻击，禁止页面被嵌入到iframe中
            response.setHeader("X-Frame-Options", "DENY");

            // X-Content-Type-Options
            // 防止浏览器进行MIME类型嗅探
            response.setHeader("X-Content-Type-Options", "nosniff");

            // X-XSS-Protection
            // 启用浏览器的XSS保护机制
            response.setHeader("X-XSS-Protection", "1; mode=block");

            // Referrer-Policy
            // 控制Referer头的发送策略
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

            // Content-Security-Policy (CSP)
            // 内容安全策略，防止XSS攻击
            StringBuilder csp = new StringBuilder();
            csp.append("default-src 'self'; ");
            csp.append("script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://unpkg.com; ");
            csp.append("style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net; ");
            csp.append("img-src 'self' data: https: blob:; ");
            csp.append("font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; ");
            csp.append("connect-src 'self' wss: ws:; ");
            csp.append("media-src 'self'; ");
            csp.append("object-src 'none'; ");
            csp.append("base-uri 'self'; ");
            csp.append("form-action 'self'; ");
            csp.append("frame-ancestors 'none';");
            
            response.setHeader("Content-Security-Policy", csp.toString());

            // Permissions-Policy (原Feature-Policy)
            // 控制浏览器功能的使用权限
            StringBuilder permissionsPolicy = new StringBuilder();
            permissionsPolicy.append("camera=(), ");
            permissionsPolicy.append("microphone=(), ");
            permissionsPolicy.append("geolocation=(), ");
            permissionsPolicy.append("payment=(), ");
            permissionsPolicy.append("usb=(), ");
            permissionsPolicy.append("magnetometer=(), ");
            permissionsPolicy.append("gyroscope=(), ");
            permissionsPolicy.append("accelerometer=()");
            
            response.setHeader("Permissions-Policy", permissionsPolicy.toString());

            // Cross-Origin-Embedder-Policy
            // 防止跨域资源嵌入
            response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");

            // Cross-Origin-Opener-Policy
            // 防止跨域窗口访问
            response.setHeader("Cross-Origin-Opener-Policy", "same-origin");

            // Cross-Origin-Resource-Policy
            // 控制跨域资源访问
            response.setHeader("Cross-Origin-Resource-Policy", "same-origin");

            // Cache-Control for sensitive pages
            // 对于API和敏感页面，禁用缓存
            String requestURI = ((HttpServletRequest) response).getRequestURI();
            if (requestURI != null && (requestURI.startsWith("/api/") || 
                                     requestURI.contains("/admin/") ||
                                     requestURI.contains("/login"))) {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
        }
    }
}