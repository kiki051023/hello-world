package com.example.student.security;

import com.example.student.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisTokenService redisTokenService;

    private final PermissionVersionPolicy permissionVersionPolicy = new PermissionVersionPolicy();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = parseBearerToken(request);
        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            // 刷新令牌不可作为访问凭据
            if (!JwtUtils.TOKEN_TYPE_ACCESS.equals(jwtUtils.getTypeFromToken(token))) {
                filterChain.doFilter(request, response);
                return;
            }
            // 已登出/吊销的访问令牌视为无效
            String jti = jwtUtils.getJtiFromToken(token);
            if (jti != null && redisTokenService.isBlacklisted(jti)) {
                filterChain.doFilter(request, response);
                return;
            }
            String username = jwtUtils.getUsernameFromToken(token);
            String role = jwtUtils.getRoleFromToken(token);
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (!permissionVersionPolicy.isCurrent(jwtUtils.getPermissionVersionFromToken(token),
                    redisTokenService.getPermissionVersion(userId))) {
                filterChain.doFilter(request, response);
                return;
            }
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null,
                            Collections.singletonList(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // M1-3 在线会话心跳：每次鉴权请求刷新 TTL（活跃窗口 30 分钟）
            redisTokenService.touchOnline(userId, jti, username, null);
        }
        filterChain.doFilter(request, response);
    }

    private String parseBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
