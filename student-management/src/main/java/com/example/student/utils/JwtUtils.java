package com.example.student.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtils {

    /** 访问令牌：用于访问业务接口，TTL 较短 */
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    /** 刷新令牌：仅用于换取新的访问令牌，TTL 较长 */
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expire:1800000}")
    private Long accessExpire;

    @Value("${jwt.refresh-expire:604800000}")
    private Long refreshExpire;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, TOKEN_TYPE_ACCESS, accessExpire);
    }

    public String generateRefreshToken(Long userId, String username, String role) {
        // 刷新令牌也携带角色，避免刷新时回查数据库；角色变更需重新登录生效
        return buildToken(userId, username, role, TOKEN_TYPE_REFRESH, refreshExpire);
    }

    private String buildToken(Long userId, String username, String role, String type, Long expire) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        if (role != null) {
            claims.put("role", role);
        }
        claims.put("type", type);
        claims.put("jti", UUID.randomUUID().toString());
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expire))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("username") : null;
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("role") : null;
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? Long.valueOf(claims.get("userId").toString()) : null;
    }

    public String getJtiFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("jti", String.class) : null;
    }

    public String getTypeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("type") : null;
    }

    /** 令牌剩余有效秒数（<=0 表示已过期） */
    public long getRemainingSeconds(String token) {
        Claims claims = parseToken(token);
        if (claims == null || claims.getExpiration() == null) {
            return 0;
        }
        return Math.max(0, (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000);
    }

    public long getRefreshExpireSeconds() {
        return (refreshExpire != null ? refreshExpire : 604800000L) / 1000;
    }
}
