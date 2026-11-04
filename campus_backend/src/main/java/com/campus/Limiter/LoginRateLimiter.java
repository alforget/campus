package com.campus.Limiter;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class LoginRateLimiter {

    private static final int MAX_FAIL = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final Duration LOCK   = Duration.ofMinutes(10);

    private static final String FAIL_USER = "login:fail:user:";
    private static final String FAIL_IP   = "login:fail:ip:";
    private static final String LOCK_USER = "login:lock:user:";
    private static final String LOCK_IP   = "login:lock:ip:";

    /** INCR 且只在第一次失败时设过期，避免窗口被无限延长 */
    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) " +
                    "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
                    "return c",
            Long.class);

    private final StringRedisTemplate redis;

    public LoginRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 认证前调用：返回剩余锁定秒数，0 表示未锁 */
    public long lockedSeconds(String username, String ip) {
        Long u = ttl(LOCK_USER + username);
        Long i = ttl(LOCK_IP + ip);
        long s = Math.max(u == null ? 0 : u, i == null ? 0 : i);
        return Math.max(s, 0);
    }

    /** 密码校验失败后调用 */
    public void onFailure(String username, String ip) {
        bump(FAIL_USER + username, LOCK_USER + username);
        bump(FAIL_IP + ip,         LOCK_IP + ip);
    }

    /** 登录成功后调用：清空计数，不碰 lock（万一锁还没过期也不应被成功登录清掉，见下文说明） */
    public void onSuccess(String username, String ip) {
        redis.delete(FAIL_USER + username);
        redis.delete(FAIL_IP + ip);
    }

    private void bump(String failKey, String lockKey) {
        Long c = redis.execute(INCR_WITH_TTL, List.of(failKey),
                String.valueOf(WINDOW.getSeconds()));
        if (c != null && c >= MAX_FAIL) {
            redis.opsForValue().set(lockKey, "1", LOCK);
        }
    }

    private Long ttl(String key) {
        return redis.getExpire(key, TimeUnit.SECONDS);
    }
}