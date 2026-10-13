package com.drone.platform.common.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * 基于 Redis 固定窗口的限流计数。
 * Redis 不可用时降级放行，避免存储故障拖垮全部接口。
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试获取一次配额。
     *
     * @param key           限流维度（如 login:1.2.3.4）
     * @param limit         窗口内最大次数
     * @param windowSeconds 窗口长度（秒）
     * @return true=放行 false=超限
     */
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        try {
            long window = Instant.now().getEpochSecond() / windowSeconds;
            String redisKey = "rate:" + key + ":" + window;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds + 1L));
            }
            return count == null || count <= limit;
        } catch (Exception e) {
            return true;
        }
    }
}
