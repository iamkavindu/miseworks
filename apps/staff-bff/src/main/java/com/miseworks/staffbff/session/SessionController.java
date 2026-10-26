package com.miseworks.staffbff.session;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SessionController {

    @GetMapping("/api/session")
    Map<String, String> session(Authentication authentication) {
        return Map.of("subject", authentication.getName());
    }

    @GetMapping("/api/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/logged-out")
    String loggedOut() {
        return "Signed out.";
    }
}
