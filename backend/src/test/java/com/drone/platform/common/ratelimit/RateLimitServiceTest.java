package com.drone.platform.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RateLimitService 单元测试：Redis 固定窗口计数与故障降级。
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new RateLimitService(redisTemplate);
    }

    @Test
    @DisplayName("窗口首次访问：允许并设置过期时间")
    void first_hit_sets_expiry_and_allows() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean allowed = service.tryAcquire("login:1.2.3.4", 5, 60);

        assertThat(allowed).isTrue();
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("计数未超上限：放行")
    void within_limit_allows() {
        when(valueOperations.increment(anyString())).thenReturn(5L);

        assertThat(service.tryAcquire("login:1.2.3.4", 5, 60)).isTrue();
    }

    @Test
    @DisplayName("计数超过上限：拒绝")
    void exceeding_limit_rejects() {
        when(valueOperations.increment(anyString())).thenReturn(6L);

        assertThat(service.tryAcquire("login:1.2.3.4", 5, 60)).isFalse();
    }

    @Test
    @DisplayName("超限拒绝时不再重复设置过期时间")
    void reject_does_not_touch_expiry_again() {
        when(valueOperations.increment(anyString())).thenReturn(9L);

        service.tryAcquire("login:1.2.3.4", 5, 60);

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Redis 故障时降级放行")
    void redis_failure_degrades_to_allow() {
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("redis down"));

        assertThat(service.tryAcquire("login:1.2.3.4", 5, 60)).isTrue();
    }
}
