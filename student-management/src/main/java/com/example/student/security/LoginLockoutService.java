package com.example.student.security;

import com.example.student.common.DistributedCacheService;
import org.springframework.stereotype.Component;

/**
 * 登录失败锁定（防撞库）：
 * - 同一用户名连续失败 >= maxAttempts 次后锁定 lockMinutes 分钟
 * - 计数走 DistributedCacheService：Redis 可用时多实例一致，故障时降级本进程
 * - 锁定期间直接拒绝登录尝试（不校验密码），到期自动解锁
 */
@Component
public class LoginLockoutService {

    private static final String KEY_PREFIX = "login:fail:";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_SECONDS = 15 * 60L;
    /** 失败计数窗口：窗口内未再失败则计数自然过期清零 */
    private static final long COUNTER_SECONDS = 15 * 60L;

    private final DistributedCacheService cache;

    public LoginLockoutService(DistributedCacheService cache) {
        this.cache = cache;
    }

    public boolean isLocked(String username) {
        // 注意：Redis 降级路径下 deserialize 会把 "1" 这类纯数字串还原成 Long，
        // 若此处声明为 String 会在读穿赋值处抛 ClassCastException。
        // 直接用 Object 接收、按“是否存在”判定锁定状态即可。
        Object state = cache.getOrLoad(KEY_PREFIX + "locked:" + username, LOCK_SECONDS, k -> null);
        return state != null;
    }

    public long remainingLockSeconds(String username) {
        // 粗粒度提示：缓存 TTL 即剩余锁定期，读穿刷新不影响原键
        cache.getOrLoad(KEY_PREFIX + "locked:" + username, LOCK_SECONDS, k -> null);
        return LOCK_SECONDS;
    }

    public void recordFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        Long count = cache.incrementCounter(KEY_PREFIX + username, COUNTER_SECONDS);
        if (count != null && count >= MAX_ATTEMPTS) {
            cache.putIfAbsent(KEY_PREFIX + "locked:" + username, "1", LOCK_SECONDS);
            cache.evictByPrefix(KEY_PREFIX + username);
        }
    }

    /** 登录成功后清除失败计数与任何残留锁定标记 */
    public void resetFailures(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        // 计数键为 login:fail:<user>，锁定键为 login:fail:locked:<user>
        // 二者前缀不同（locked: 夹在中间），需分别清除，否则锁定标记会残留。
        cache.evictByPrefix(KEY_PREFIX + username);
        cache.evictByPrefix(KEY_PREFIX + "locked:" + username);
    }

    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }
}
