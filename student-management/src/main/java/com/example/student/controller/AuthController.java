package com.example.student.controller;

import com.example.student.audit.RateLimit;
import com.example.student.common.Result;
import com.example.student.dto.ChangePasswordDTO;
import com.example.student.dto.ForceChangePasswordDTO;
import com.example.student.dto.LoginDTO;
import com.example.student.dto.RefreshDTO;
import com.example.student.exception.BusinessException;
import com.example.student.security.AuthorizationService;
import com.example.student.security.CaptchaService;
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

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private CaptchaService captchaService;

    @Operation(summary = "获取登录验证码")
    @GetMapping("/captcha")
    @RateLimit(bucket = "auth:captcha", limit = 10, message = "验证码获取过于频繁，请稍后再试")
    public Result<Map<String, String>> captcha() {
        return Result.success(captchaService.generate());
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    @RateLimit(bucket = "auth:login:ip", limit = 10, message = "登录过于频繁，请稍后再试")
    @RateLimit(bucket = "auth:login:user", key = "#loginDTO.username", limit = 5,
            message = "该账号登录尝试过多，请稍后再试")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        // 验证码校验（一次性）：失败直接拒绝，不消耗登录失败计数
        if (!captchaService.validate(loginDTO.getCaptchaKey(), loginDTO.getCaptchaCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
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
                // M1-3 移除在线会话
                redisTokenService.removeOnline(userId, jwtUtils.getJtiFromToken(token));
            }
        }
        return Result.success();
    }

    @Operation(summary = "修改当前用户密码（成功后换发新令牌）")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public Result<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = authorizationService.currentUser().getId();
        Map<String, Object> result = userService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success(result);
    }

    @Operation(summary = "强制改密（首次登录/管理员重置密码后，须 mustChangePassword=1；成功后换发新令牌）")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/force-change-password")
    public Result<Map<String, Object>> forceChangePassword(@Valid @RequestBody ForceChangePasswordDTO dto) {
        Long userId = authorizationService.currentUser().getId();
        Map<String, Object> result = userService.forceChangePassword(userId, dto.getNewPassword());
        return Result.success(result);
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
        Integer permissionVersion = jwtUtils.getPermissionVersionFromToken(refreshToken);
        String newAccessToken = jwtUtils.generateAccessToken(userId, username, role, permissionVersion);

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
