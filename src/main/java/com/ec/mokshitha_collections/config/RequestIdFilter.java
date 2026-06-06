package com.ec.mokshitha_collections.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Tags every request with a short correlation id, surfaced both as a response
 * header (X-Request-Id) and as MDC field "rid" so it shows up on every log line.
 * Lets you grep a single user's request flow across the log file.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Request-Id";
    private static final String MDC_KEY = "rid";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Reuse client-supplied id if present (lets a reverse proxy thread one through).
        String existing = request.getHeader(HEADER_NAME);
        String rid = (existing == null || existing.isBlank())
                ? UUID.randomUUID().toString().substring(0, 8)
                : sanitize(existing);

        MDC.put(MDC_KEY, rid);
        response.setHeader(HEADER_NAME, rid);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /** Strip CR/LF/anything weird so a malicious client can't inject log fields. */
    private static String sanitize(String raw) {
        return raw.replaceAll("[^A-Za-z0-9_\\-]", "").substring(0, Math.min(raw.length(), 64));
    }
}
