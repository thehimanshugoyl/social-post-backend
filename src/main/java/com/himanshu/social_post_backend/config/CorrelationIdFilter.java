package com.himanshu.social_post_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Reuse an incoming correlation ID if the client sent one (useful when
        // chaining calls between services), otherwise generate a new one.
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        long startTime = System.currentTimeMillis();

        try {
            logger.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Completed request: {} {} -> status={} ({} ms)",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);

            // Always clear MDC after the request finishes, to avoid leaking
            // this correlation ID into a future request handled by the same thread.
            MDC.remove(MDC_KEY);
        }
    }
}