package com.miseworks.staffbff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.floci.testcontainers.FlociContainer;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Import(LocalIdentity.class)
@AutoConfigureMockMvc
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
        properties = {
            "COGNITO_CLIENT_ID=integration-bff",
            "COGNITO_CLIENT_SECRET=integration-only-client-secret",
            "COGNITO_ISSUER_URI=http://identity.invalid/us-east-1_integration",
            "COGNITO_LOGOUT_URI=http://identity.invalid/cognito-idp/logout",
            "BFF_LOGOUT_REDIRECT_URI=http://localhost/logged-out",
            "VALKEY_HOST=unused.invalid",
            "server.servlet.session.cookie.secure=false"
        })
class StaffBffApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext context;

    @Test
    @WithMockUser
    void healthReportsUpWithValkey() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser
    void openApiDocumentSerializes() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
    }

    @Test
    void operationalEndpointsRequireAuthentication() throws Exception {
        try (var client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            for (var path : new String[] {"/actuator/health", "/v3/api-docs"}) {
                var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", "application/json")
                        .GET()
                        .build();
                assertEquals(
                        401,
                        client.send(request, HttpResponse.BodyHandlers.ofString())
                                .statusCode());
            }
        }
    }

    @Test
    @WithMockUser
    void mutationsRequireCsrfBeforeRouting() throws Exception {
        mvc.perform(post("/scaffold-csrf-check")).andExpect(status().isForbidden());
        mvc.perform(post("/scaffold-csrf-check").with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void noLocalPasswordUserIsProvisioned() {
        assertTrue(context.getBeansOfType(UserDetailsService.class).isEmpty());
    }

    @Autowired
    private FlociContainer floci;

    @Autowired
    private SessionRepository<?> sessions;

    @Test
    void loginPersistsTokensAndAuthenticationInValkeyAndLogoutInvalidatesSession() throws Exception {
        try (var browser = browser()) {
            var callback = authorize(browser, LocalIdentity.PASSWORD);
            var beforeLogin = storedSession(browser).getId();
            var response = send(browser, "GET", callback, null);
            assertEquals(302, response.statusCode());
            assertTrue(response.headers().allValues("Set-Cookie").stream()
                    .anyMatch(cookie -> cookie.contains("MISEWORKS_SESSION=")
                            && cookie.contains("HttpOnly")
                            && cookie.contains("SameSite=Lax")));
            var sessionResponse = send(browser, "GET", base() + "/api/session", null);
            assertEquals(200, sessionResponse.statusCode());
            assertTrue(LocalIdentity.JSON.readTree(sessionResponse.body()).has("subject"));
            assertFalse(sessionResponse.body().contains("token"));
            var stored = storedSession(browser);
            assertNotNull(stored);
            assertNotEquals(beforeLogin, stored.getId());
            assertNull(sessions.findById(beforeLogin));
            assertEquals(Duration.ofMinutes(30), stored.getMaxInactiveInterval());
            var clientsAttribute = stored.getAttributeNames().stream()
                    .filter(name -> name.contains("AUTHORIZED_CLIENTS"))
                    .findFirst()
                    .orElseThrow();
            Map<String, OAuth2AuthorizedClient> clients = stored.getAttribute(clientsAttribute);
            assertNotNull(clients.get("cognito").getAccessToken());
            assertNotNull(clients.get("cognito").getRefreshToken());
            // Re-reading /api/session traverses Redis deserialization on every HTTP request.
            assertEquals(
                    200, send(browser, "GET", base() + "/api/session", null).statusCode());
            assertEquals(403, send(browser, "POST", base() + "/logout", "").statusCode());
            var csrfResponse = send(browser, "GET", base() + "/api/csrf", null);
            var csrf = LocalIdentity.JSON.readTree(csrfResponse.body());
            var logout = HttpRequest.newBuilder(URI.create(base() + "/logout"))
                    .timeout(Duration.ofSeconds(10))
                    .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            var loggedOut = browser.send(logout, HttpResponse.BodyHandlers.ofString());
            assertEquals(302, loggedOut.statusCode());
            assertTrue(loggedOut.headers().firstValue("Location").orElseThrow().contains("/cognito-idp/logout"));
            assertNull(sessions.findById(stored.getId()));
            assertEquals(
                    401, send(browser, "GET", base() + "/api/session", null).statusCode());
        }
    }

    @Test
    void wrongPasswordAndWrongStateCannotEstablishSession() throws Exception {
        try (var browser = browser()) {
            var response = submitCredentials(browser, "Wrong-password-Aa1!");
            assertEquals(400, response.statusCode());
            assertTrue(response.body().contains("Incorrect username or password"));
            assertEquals(
                    401, send(browser, "GET", base() + "/api/session", null).statusCode());
        }
        try (var browser = browser()) {
            var callback = authorize(browser, LocalIdentity.PASSWORD);
            callback = callback.replaceAll("([?&])state=[^&]+", "$1state=incorrect-state");
            assertEquals(302, send(browser, "GET", callback, null).statusCode());
            assertEquals(
                    401, send(browser, "GET", base() + "/api/session", null).statusCode());
        }
    }

    @Test
    void authorizationCodeRejectsWrongPkceVerifier() throws Exception {
        try (var browser = browser()) {
            var callback = authorize(browser, LocalIdentity.PASSWORD);
            var code = UriComponentsBuilder.fromUriString(callback)
                    .build()
                    .getQueryParams()
                    .getFirst("code");
            var basic = Base64.getEncoder()
                    .encodeToString(
                            (LocalIdentity.CLIENT + ":" + LocalIdentity.SECRET).getBytes(StandardCharsets.UTF_8));
            var request = HttpRequest.newBuilder(URI.create(floci.getEndpoint() + "/cognito-idp/oauth2/token"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form(Map.of(
                            "grant_type",
                            "authorization_code",
                            "code",
                            code,
                            "redirect_uri",
                            base() + "/login/oauth2/code/cognito",
                            "code_verifier",
                            "incorrect-verifier"))))
                    .build();
            assertEquals(
                    400,
                    browser.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }

    @Test
    void idleExpiryRejectsExistingSessionCookie() throws Exception {
        try (var browser = browser()) {
            send(browser, "GET", authorize(browser, LocalIdentity.PASSWORD), null);
            var stored = storedSession(browser);
            expireSession(sessions, stored.getId());
            assertEquals(
                    401, send(browser, "GET", base() + "/api/session", null).statusCode());
        }
    }

    private static <S extends Session> void expireSession(SessionRepository<S> repository, String id) {
        var stored = repository.findById(id);
        stored.setMaxInactiveInterval(Duration.ofSeconds(1));
        stored.setLastAccessedTime(Instant.now().minusSeconds(10));
        repository.save(stored);
    }

    private HttpClient browser() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
    }

    private String base() {
        return "http://localhost:" + port;
    }

    private Session storedSession(HttpClient browser) {
        var jar = (CookieManager) browser.cookieHandler().orElseThrow();
        var cookie = jar.getCookieStore().getCookies().stream()
                .filter(value -> value.getName().equals("MISEWORKS_SESSION"))
                .findFirst()
                .orElseThrow();
        var id = new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
        return sessions.findById(id);
    }

    private String authorize(HttpClient browser, String password) throws Exception {
        var response = submitCredentials(browser, password);
        assertEquals(302, response.statusCode());
        return response.headers().firstValue("Location").orElseThrow();
    }

    private HttpResponse<String> submitCredentials(HttpClient browser, String password) throws Exception {
        LocalIdentity.api(
                floci,
                "UpdateUserPoolClient",
                Map.of(
                        "UserPoolId",
                        LocalIdentity.POOL,
                        "ClientId",
                        LocalIdentity.CLIENT,
                        "AllowedOAuthFlowsUserPoolClient",
                        true,
                        "AllowedOAuthFlows",
                        new String[] {"code"},
                        "AllowedOAuthScopes",
                        new String[] {"openid", "profile", "email"},
                        "SupportedIdentityProviders",
                        new String[] {"COGNITO"},
                        "CallbackURLs",
                        new String[] {base() + "/login/oauth2/code/cognito"}));
        var start = send(browser, "GET", base() + "/oauth2/authorization/cognito", null);
        assertEquals(302, start.statusCode());
        var authorization = start.headers().firstValue("Location").orElseThrow();
        assertTrue(authorization.contains("code_challenge_method=S256"));
        var login = send(browser, "GET", authorization, null);
        assertEquals(302, login.statusCode());
        var loginUrl = URI.create(authorization)
                .resolve(login.headers().firstValue("Location").orElseThrow())
                .toString();
        var page = send(browser, "GET", loginUrl, null);
        assertEquals(200, page.statusCode());
        var fields = new HashMap<String, String>();
        var matcher =
                Pattern.compile("name=\"([^\"]+)\"[^>]*value=\"([^\"]*)\"").matcher(page.body());
        while (matcher.find()) {
            fields.put(matcher.group(1), HtmlUtils.htmlUnescape(matcher.group(2)));
        }
        fields.put("username", "integration-user");
        fields.put("password", password);
        return send(browser, "POST", loginUrl, form(fields));
    }

    private static String form(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static HttpResponse<String> send(HttpClient client, String method, String url, String body)
            throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10));
        if (method.equals("POST")) {
            request.header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            request.GET();
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
