package com.chequeprint.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory rate limiting filter and request source logger for Spring Boot backend.
 * Protects endpoints against flood loops by limiting clients to 60 requests/minute.
 */
@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long ONE_MINUTE_MS = 60_000L;

    private final Map<String, ClientRateLimit> rateLimitMap = new ConcurrentHashMap<>();

    private static class ClientRateLimit {
        final long windowStart;
        final AtomicInteger requestCount;

        ClientRateLimit(long windowStart) {
            this.windowStart = windowStart;
            this.requestCount = new AtomicInteger(1);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. Log request source & URI
        log.info("📥 [Backend HTTP Request] IP: {} | {} {} | SessionId: {}", 
                clientIp, method, uri, request.getRequestedSessionId() != null ? request.getRequestedSessionId() : "N/A");

        // 2. Evaluate Rate Limiting
        long now = System.currentTimeMillis();
        ClientRateLimit rateLimit = rateLimitMap.compute(clientIp, (ip, current) -> {
            if (current == null || (now - current.windowStart) > ONE_MINUTE_MS) {
                return new ClientRateLimit(now);
            }
            current.requestCount.incrementAndGet();
            return current;
        });

        if (rateLimit.requestCount.get() > MAX_REQUESTS_PER_MINUTE) {
            log.warn("⚠️ [Rate Limit Exceeded] IP {} exceeded threshold ({} reqs/min) on {}", 
                    clientIp, rateLimit.requestCount.get(), uri);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // HTTP 429
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded (max 60 req/min). Please try again shortly.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
