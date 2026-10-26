package com.miseworks.staffbff.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class AbsoluteSessionTimeoutFilter extends OncePerRequestFilter {

    private final Duration timeout;
    private final Clock clock;

    AbsoluteSessionTimeoutFilter(Duration timeout) {
        this(timeout, Clock.systemUTC());
    }

    AbsoluteSessionTimeoutFilter(Duration timeout, Clock clock) {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Absolute session timeout must be positive");
        }
        this.timeout = timeout;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var session = request.getSession(false);
        if (session != null && clock.millis() - session.getCreationTime() >= timeout.toMillis()) {
            session.invalidate();
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }
}
