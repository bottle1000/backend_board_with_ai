package com.board.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        MDC.put("traceId", traceId);
        request.setAttribute("startTime", System.currentTimeMillis());

        log.info("[{}] --> {} {}", traceId, request.getMethod(), request.getRequestURI());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long duration = System.currentTimeMillis() - (long) request.getAttribute("startTime");

        log.info("[{}] <-- {} {} {} ({}ms)",
                MDC.get("traceId"),
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);

        MDC.clear();
    }
}
