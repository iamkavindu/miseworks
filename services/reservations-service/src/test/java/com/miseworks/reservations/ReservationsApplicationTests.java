package com.miseworks.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
        properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://identity.invalid/test-pool")
class ReservationsApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser
    void healthReportsUpWithPostgres() throws Exception {
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
        var response = httpGet("/v3/api-docs");
        assertEquals(401, httpGet("/actuator/health").statusCode());
        assertEquals(401, response.statusCode());
        assertTrue(
                response.headers().firstValue("WWW-Authenticate").orElseThrow().startsWith("Bearer"));
    }

    @Test
    void flywayInitializesHistoryOnPostgres18WithoutBusinessMigrations() {
        assertTrue(jdbc.queryForObject("show server_version", String.class).startsWith("18."));
        assertEquals(0, jdbc.queryForObject("select count(*) from flyway_schema_history", Integer.class));
    }

    private HttpResponse<String> httpGet(String path) throws Exception {
        try (var client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
