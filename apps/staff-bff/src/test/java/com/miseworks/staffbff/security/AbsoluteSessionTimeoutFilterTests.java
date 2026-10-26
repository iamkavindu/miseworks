package com.miseworks.staffbff.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AbsoluteSessionTimeoutFilterTests {

    @Test
    void absoluteExpiryInvalidatesSessionAndClearsAuthenticationAtTheBoundary() throws Exception {
        var request = new MockHttpServletRequest();
        var session = new MockHttpSession();
        request.setSession(session);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("staff", null));
        var clock =
                Clock.fixed(Instant.ofEpochMilli(session.getCreationTime()).plus(Duration.ofHours(8)), ZoneOffset.UTC);
        var filter = new AbsoluteSessionTimeoutFilter(Duration.ofHours(8), clock);
        try {
            filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
                assertTrue(session.isInvalid());
                assertNull(SecurityContextHolder.getContext().getAuthentication());
            });
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void sessionRemainsValidBeforeAbsoluteDeadline() throws Exception {
        var request = new MockHttpServletRequest();
        var session = new MockHttpSession();
        request.setSession(session);
        var clock = Clock.fixed(
                Instant.ofEpochMilli(session.getCreationTime())
                        .plus(Duration.ofHours(8))
                        .minusMillis(1),
                ZoneOffset.UTC);
        new AbsoluteSessionTimeoutFilter(Duration.ofHours(8), clock)
                .doFilter(request, new MockHttpServletResponse(), (req, res) -> assertFalse(session.isInvalid()));
    }

    @Test
    void nonPositiveTimeoutIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AbsoluteSessionTimeoutFilter(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new AbsoluteSessionTimeoutFilter(Duration.ofSeconds(-1)));
    }
}
