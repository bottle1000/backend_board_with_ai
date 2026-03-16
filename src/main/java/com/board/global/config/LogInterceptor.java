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

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String START_TIME_ATTR = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // [Fix] UUID 8자 → 16자 (충돌 확률: 1/16^8 → 1/16^16 으로 대폭 감소)
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        MDC.put("traceId", traceId);
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        // [Add] 클라이언트가 응답 헤더에서 traceId 확인 가능 → 이슈 추적 용이
        response.setHeader(TRACE_ID_HEADER, traceId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // [Add] 요청 처리 시간 측정 → Kibana에서 슬로우 API 감지 가능
        long duration = System.currentTimeMillis() - (long) request.getAttribute(START_TIME_ATTR);

        log.info("[{}] {} {} → {} ({}ms)",
                MDC.get("traceId"),
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);

        MDC.clear();
    }
}