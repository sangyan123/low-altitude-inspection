package com.drone.platform.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 接口限流拦截器。
 * 规则：
 *  - 登录接口：同 IP 5 次/分钟（防暴力破解）
 *  - AI 识别接口：同 IP 20 次/分钟（防刷计费接口）
 *  - 其它写接口（POST/PUT/DELETE）：同 IP 120 次/分钟（防刷兜底）
 *  - GET 查询不限流（由认证体系保护）
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int LOGIN_LIMIT = 5;
    private static final int AI_LIMIT = 20;
    private static final int WRITE_LIMIT = 120;
    private static final int WINDOW_SECONDS = 60;

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String ip = clientIp(request);
        String path = request.getServletPath();
        String key;
        int limit;

        if (path.startsWith("/auth/login")) {
            key = "login:" + ip;
            limit = LOGIN_LIMIT;
        } else if (path.startsWith("/ai/")) {
            key = "ai:" + ip;
            limit = AI_LIMIT;
        } else {
            key = "write:" + ip;
            limit = WRITE_LIMIT;
        }

        if (!rateLimitService.tryAcquire(key, limit, WINDOW_SECONDS)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("utf-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Map.of("code", 429, "message", "请求过于频繁，请稍后再试", "data", "")));
            return false;
        }
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
