package com.miseworks.staffbff;

import io.floci.testcontainers.FlociContainer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@TestConfiguration(proxyBeanMethods = false)
class LocalIdentity {

    static final String POOL = "us-east-1_integration";
    static final String CLIENT = "integration-bff";
    static final String SECRET = "integration-only-client-secret";
    static final String PASSWORD = "Integration-only-Aa1!";
    static final String ISSUER = "http://identity.invalid/" + POOL;
    static final JsonMapper JSON = JsonMapper.builder().build();

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ServiceConnection(name = "redis")
    GenericContainer<?> valkey() {
        return new GenericContainer<>(DockerImageName.parse("valkey/valkey:8.1.3-alpine"))
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort());
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    FlociContainer floci() {
        return new FlociContainer("floci/floci:2.3.0")
                .withDockerSocket(false)
                .withEnv("FLOCI_BASE_URL", "http://identity.invalid");
    }

    @Bean
    ClientRegistrationRepository registrations(FlociContainer floci) throws Exception {
        api(
                floci,
                "CreateUserPool",
                Map.of(
                        "PoolName",
                        "integration",
                        "UserPoolTags",
                        Map.of(
                                "floci:override-id", POOL,
                                "floci:override-cognito-client-id", "use-name",
                                "floci:override-cognito-client-secret", SECRET)));
        // Wildcard-free callback configured by the test once the embedded server port is known.
        api(
                floci,
                "CreateUserPoolClient",
                Map.of(
                        "UserPoolId",
                        POOL,
                        "ClientName",
                        CLIENT,
                        "GenerateSecret",
                        true,
                        "AllowedOAuthFlowsUserPoolClient",
                        true,
                        "AllowedOAuthFlows",
                        new String[] {"code"},
                        "AllowedOAuthScopes",
                        new String[] {"openid", "profile", "email"},
                        "SupportedIdentityProviders",
                        new String[] {"COGNITO"},
                        "CallbackURLs",
                        new String[] {"http://localhost/unused"}));
        api(
                floci,
                "AdminCreateUser",
                Map.of("UserPoolId", POOL, "Username", "integration-user", "MessageAction", "SUPPRESS"));
        api(
                floci,
                "AdminSetUserPassword",
                Map.of("UserPoolId", POOL, "Username", "integration-user", "Password", PASSWORD, "Permanent", true));
        var endpoint = floci.getEndpoint();
        // Discovery URLs are fixed by Floci's base URL, while Testcontainers maps random ports.
        // Retain the issued token's exact issuer and point network requests at the isolated container.
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("cognito")
                .clientId(CLIENT)
                .clientSecret(SECRET)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .issuerUri(ISSUER)
                .authorizationUri(endpoint + "/cognito-idp/oauth2/authorize")
                .tokenUri(endpoint + "/cognito-idp/oauth2/token")
                .jwkSetUri(endpoint + "/" + POOL + "/.well-known/jwks.json")
                .userInfoUri(endpoint + "/cognito-idp/oauth2/userInfo")
                .userNameAttributeName("sub")
                .build());
    }

    static JsonNode api(FlociContainer floci, String action, Map<String, ?> payload) throws Exception {
        try (var client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var request = HttpRequest.newBuilder(URI.create(floci.getEndpoint()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-amz-json-1.1")
                    .header("X-Amz-Target", "AWSCognitoIdentityProviderService." + action)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(payload)))
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(action + " failed: HTTP " + response.statusCode());
            }
            return JSON.readTree(response.body());
        }
    }
}
