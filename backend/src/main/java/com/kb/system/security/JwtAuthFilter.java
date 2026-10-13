package com.kb.system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器：
 * 1. 解析 Authorization 头中的 Bearer Token；
 * 2. 校验 Token 签名与有效期；
 * 3. 与 Redis 中的 jti 比对（登录状态真实保存在 Redis，退出即失效）；
 * 4. 通过后加载/缓存用户信息并写入安全上下文。
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String TOKEN_KEY_PREFIX = "auth:token:";
    private static final String USER_KEY_PREFIX = "auth:user:";

    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.jwt.header}")
    private String tokenHeader;

    @Value("${app.jwt.prefix}")
    private String tokenPrefix;

    public JwtAuthFilter(JwtUtils jwtUtils,
                         RedisTemplate<String, Object> redisTemplate,
                         UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(tokenHeader);
        if (StringUtils.hasText(header) && header.startsWith(tokenPrefix)) {
            String token = header.substring(tokenPrefix.length()).trim();
            try {
                if (jwtUtils.validate(token)) {
                    Long userId = jwtUtils.getUserId(token);
                    String jti = jwtUtils.getJti(token);
                    Object cachedJti = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + userId);
                    // Redis 中不存在或 jti 不匹配 => 未登录 / 已退出 / 被踢下线
                    if (cachedJti != null && jti.equals(cachedJti.toString())) {
                        LoginUser loginUser = loadLoginUser(userId, jwtUtils.getUsername(token), token);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(loginUser, null,
                                        loginUser.getAuthorities());
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        log.debug("Token 在 Redis 中已失效: userId={}", userId);
                    }
                }
            } catch (Exception ex) {
                // Token 解析失败时静默放行，由后续安全链路返回 401
                log.debug("Token 解析失败", ex);
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    /** 优先读取 Redis 缓存的登录用户，未命中则回源数据库并回填缓存 */
    private LoginUser loadLoginUser(Long userId, String username, String token) {
        try {
            Object cached = redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
            if (cached instanceof LoginUser loginUser) {
                return loginUser;
            }
        } catch (Exception ex) {
            // 缓存反序列化失败（如旧格式条目）时回源数据库，并覆盖坏缓存
            log.warn("Redis 登录用户缓存读取失败，回源数据库: userId={}", userId, ex);
        }
        LoginUser loginUser = (LoginUser) userDetailsService.loadUserByUsername(username);
        long remaining = jwtUtils.getRemainingSeconds(token);
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + userId, loginUser,
                java.time.Duration.ofSeconds(Math.max(60L, remaining)));
        return loginUser;
    }
}
