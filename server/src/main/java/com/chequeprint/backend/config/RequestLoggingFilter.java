package com.chequeprint.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final long SLOW_REQUEST_MS = 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            String message = "{} {} -> {} ({} ms)";
            if (elapsed >= SLOW_REQUEST_MS || response.getStatus() >= 500) {
                log.warn(message, request.getMethod(), request.getRequestURI(), response.getStatus(), elapsed);
            } else {
                log.info(message, request.getMethod(), request.getRequestURI(), response.getStatus(), elapsed);
            }
        }
    }
}
