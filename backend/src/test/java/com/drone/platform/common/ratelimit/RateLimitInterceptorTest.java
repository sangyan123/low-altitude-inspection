package com.drone.platform.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * RateLimitInterceptor 单元测试：按路径分档限流与 429 响应。
 */
@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RateLimitInterceptor interceptor;
    private StringWriter body;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(rateLimitService, new ObjectMapper());
        body = new StringWriter();
    }

    private void stubWriter() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(body, true));
    }

    @Test
    @DisplayName("GET 请求不限流、不计数")
    void get_requests_bypass_rate_limit() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        verifyNoInteractions(rateLimitService);
    }

    @Test
    @DisplayName("登录接口按 5次/分钟 限流")
    void login_uses_strict_limit() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getServletPath()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(rateLimitService.tryAcquire("login:10.0.0.1", 5, 60)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    @DisplayName("AI 接口按 20次/分钟 限流")
    void ai_uses_ai_limit() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getServletPath()).thenReturn("/ai/recognition");
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        when(rateLimitService.tryAcquire("ai:10.0.0.2", 20, 60)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    @DisplayName("其它写接口按 120次/分钟 兜底限流")
    void other_writes_use_default_limit() throws Exception {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getServletPath()).thenReturn("/task/patrol/1");
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");
        when(rateLimitService.tryAcquire("write:10.0.0.3", 120, 60)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    @DisplayName("X-Forwarded-For 优先于 remoteAddr，取第一个IP")
    void x_forwarded_for_preferred_over_remote_addr() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getServletPath()).thenReturn("/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("8.8.8.8, 10.0.0.9");
        when(rateLimitService.tryAcquire("login:8.8.8.8", 5, 60)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        verify(request, never()).getRemoteAddr();
    }

    @Test
    @DisplayName("超限返回 429 与 JSON 错误体")
    void over_limit_returns_429_json() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getServletPath()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.4");
        stubWriter();
        when(rateLimitService.tryAcquire("login:10.0.0.4", 5, 60)).thenReturn(false);

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        verify(response).setStatus(429);
        assertThat(body.toString()).contains("请求过于频繁");
    }
}
