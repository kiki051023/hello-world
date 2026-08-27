package com.example.student.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录图形验证码（P1）：
 * - 4 位数字验证码，Redis 存储 captcha:&lt;key&gt; → code，TTL 5 分钟
 * - 一次性：校验成功即删除（无论对错，防重放）
 * - 图形：暗色透明底 + 金色数字 + 干扰线/噪点，贴合古典暗色主题，不引第三方依赖
 */
@Component
public class CaptchaService {

    private static final String KEY_PREFIX = "captcha:";
    private static final long TTL_SECONDS = 5 * 60L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public CaptchaService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 生成验证码：返回 {captchaKey, captchaImage(data:image/png;base64,...)} */
    public Map<String, String> generate() {
        String code = String.format("%04d", RANDOM.nextInt(10000));
        String key = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + key, code, TTL_SECONDS, TimeUnit.SECONDS);

        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", key);
        result.put("captchaImage", "data:image/png;base64," + renderPng(code));
        return result;
    }

    /**
     * 校验验证码（一次性：无论对错都删除，防重放攻击）。
     * Redis 不可用时返回 true 放行（本系统实际强依赖 Redis，降级仅为避免单点故障时登录全断）。
     */
    public boolean validate(String key, String code) {
        if (key == null || code == null || key.isBlank() || code.isBlank()) {
            return false;
        }
        String storedKey = KEY_PREFIX + key;
        String stored;
        try {
            stored = redisTemplate.opsForValue().get(storedKey);
        } catch (Exception e) {
            return true; // Redis 故障降级放行
        }
        if (stored == null) {
            return false;
        }
        redisTemplate.delete(storedKey);
        return stored.equalsIgnoreCase(code.trim());
    }

    private String renderPng(String code) {
        int w = 120, h = 40;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // 深墨半透明底（贴合登录卡）
        g.setColor(new Color(14, 17, 22, 235));
        g.fillRect(0, 0, w, h);
        // 金色数字
        g.setColor(new Color(201, 162, 39));
        g.setFont(new Font(Font.SERIF, Font.BOLD, 26));
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(code);
        int x = (w - textW) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(code, x, y);
        // 干扰线（淡金）
        g.setColor(new Color(201, 162, 39, 100));
        for (int i = 0; i < 4; i++) {
            g.drawLine(RANDOM.nextInt(w), RANDOM.nextInt(h), RANDOM.nextInt(w), RANDOM.nextInt(h));
        }
        // 噪点
        for (int i = 0; i < 60; i++) {
            g.setColor(new Color(201, 162, 39, 60 + RANDOM.nextInt(100)));
            g.fillRect(RANDOM.nextInt(w), RANDOM.nextInt(h), 1, 1);
        }
        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
    }
}