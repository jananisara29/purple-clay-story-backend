package com.purpleclay.jewelry.security.filter;

import com.purpleclay.jewelry.util.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {

        long start = System.currentTimeMillis();

        LogContext.setRequestId();
        LogContext.set(LogContext.ENDPOINT, request.getMethod() + " " + request.getRequestURI());

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();

            if (shouldLog(request.getRequestURI())) {
                if (status >= 500) {
                    log.error("REQUEST status={} duration={}ms uri={} requestId={}",
                        status, duration, request.getRequestURI(), LogContext.getRequestId());
                } else if (status >= 400) {
                    log.warn("REQUEST status={} duration={}ms uri={} requestId={}",
                        status, duration, request.getRequestURI(), LogContext.getRequestId());
                } else {
                    log.info("REQUEST status={} duration={}ms uri={} requestId={}",
                        status, duration, request.getRequestURI(), LogContext.getRequestId());
                }
            }

            LogContext.clear();
        }
    }

    private boolean shouldLog(String uri) {
        return !uri.contains("/swagger") && !uri.contains("/api-docs") && !uri.contains("/actuator");
    }
}
