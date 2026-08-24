package com.example.student.controller;

import com.example.student.common.Result;
import com.example.student.dto.LoginDTO;
import com.example.student.dto.RefreshDTO;
import com.example.student.exception.BusinessException;
import com.example.student.security.RedisTokenService;
import com.example.student.service.UserService;
import com.example.student.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisTokenService redisTokenService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO.getUsername(), loginDTO.getPassword()));
    }

    @Operation(summary = "登出")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = parseBearerToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            // 访问令牌加入黑名单，TTL 与剩余有效期一致
            redisTokenService.blacklistAccessToken(jwtUtils.getJtiFromToken(token),
                    jwtUtils.getRemainingSeconds(token));
            // 吊销该用户的刷新令牌，使其立即失效
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId != null) {
                redisTokenService.removeRefreshToken(userId);
            }
        }
        return Result.success();
    }

    @Operation(summary = "刷新令牌（用 refreshToken 换取新的 accessToken）")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@Valid @RequestBody RefreshDTO refreshDTO) {
        String refreshToken = refreshDTO.getRefreshToken();
        if (!jwtUtils.validateToken(refreshToken)
                || !JwtUtils.TOKEN_TYPE_REFRESH.equals(jwtUtils.getTypeFromToken(refreshToken))) {
            throw new BusinessException("刷新令牌无效");
        }
        // 已吊销的刷新令牌拒绝
        String jti = jwtUtils.getJtiFromToken(refreshToken);
        if (jti != null && redisTokenService.isBlacklisted(jti)) {
            throw new BusinessException("刷新令牌已失效，请重新登录");
        }
        // 校验刷新令牌是否与服务端记录一致（登出后记录已删除）
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        String storedJti = redisTokenService.getRefreshTokenJti(userId);
        if (storedJti == null || !storedJti.equals(jti)) {
            throw new BusinessException("刷新令牌已失效，请重新登录");
        }
        String username = jwtUtils.getUsernameFromToken(refreshToken);
        String role = jwtUtils.getRoleFromToken(refreshToken);
        String newAccessToken = jwtUtils.generateAccessToken(userId, username, role);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        return Result.success(result);
    }

    private String parseBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
