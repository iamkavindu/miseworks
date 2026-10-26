package com.miseworks.staffbff.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientRepository authorizedClients,
            @Value("${miseworks.security.absolute-session-timeout}") Duration absoluteTimeout,
            @Value("${miseworks.security.provider-logout-uri}") String logoutUri,
            @Value("${miseworks.security.logout-redirect-uri}") String logoutRedirect)
            throws Exception {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        var providerLogout = UriComponentsBuilder.fromUriString(logoutUri)
                .queryParam(
                        "client_id",
                        registrations.findByRegistrationId("cognito").getClientId())
                .queryParam("logout_uri", logoutRedirect)
                .build()
                .encode()
                .toUriString();
        http.authorizeHttpRequests(requests -> requests.requestMatchers("/logged-out")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2Login(login -> login.authorizationEndpoint(
                                endpoint -> endpoint.authorizationRequestResolver(resolver))
                        .authorizedClientRepository(authorizedClients)
                        .defaultSuccessUrl("/api/session", true))
                .oauth2Client(client -> client.authorizedClientRepository(authorizedClients))
                .logout(logout -> logout.logoutSuccessHandler(
                        (request, response, authentication) -> response.sendRedirect(providerLogout)))
                .addFilterAfter(new AbsoluteSessionTimeoutFilter(absoluteTimeout), SecurityContextHolderFilter.class);
        return http.build();
    }
}
