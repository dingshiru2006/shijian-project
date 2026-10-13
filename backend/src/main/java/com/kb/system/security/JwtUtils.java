package com.kb.system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类：生成 / 解析 / 校验
 */
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String secret;

    /** 过期时间（秒） */
    @Value("${app.jwt.expiration}")
    private long expiration;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token。jti 为本次登录唯一标识，需存入 Redis 以支持"退出后立即失效"。
     */
    public GeneratedToken generate(Long userId, String username) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration * 1000L);
        String token = Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("jti", jti)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
        return new GeneratedToken(token, jti, expiration);
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    public Long getUserId(String token) {
        return parse(token).get("uid", Long.class);
    }

    public String getJti(String token) {
        return parse(token).get("jti", String.class);
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    /** Token 剩余有效期（秒，至少 1） */
    public long getRemainingSeconds(String token) {
        Date expiry = parse(token).getExpiration();
        long remaining = (expiry.getTime() - System.currentTimeMillis()) / 1000L;
        return Math.max(1L, remaining);
    }

    /**
     * 生成结果：token 字符串、jti、有效期（秒）
     */
    public record GeneratedToken(String token, String jti, long expiresIn) {
    }
}
