# Staff BFF

Browser authentication and shared server-side sessions for the MiseWorks staff web application. Uses Java 25, Spring Boot 4.1.2, Maven 3.10.0 / Wrapper 3.3.4, MVC, validation, Actuator, springdoc 3.1.0, and the Reservations baseline's formatting, coverage and build metadata conventions. It has no domain database, JPA, Flyway, or PostgreSQL dependency.

With Java 25 and Docker available, run from this directory:

```sh
./mvnw clean verify
```

Tests start isolated Floci and Valkey containers. They exercise hosted sign-in, real code/token exchange and ID token validation, PKCE rejection, state/password rejection, session rotation and storage, CSRF enforcement, idle expiry, and logout. Absolute session expiry has clock-controlled boundary tests. Formatting, packaging, and the unchanged 40% line coverage gate run during verification. Use `./mvnw spotless:apply` when formatting changes.

For local startup, first follow [the local environment instructions](../../infrastructure/local/README.md), then:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Default port is 8081 (`SERVER_PORT` overrides it). The explicit local profile imports the generated `../../infrastructure/local/.env`, binds to loopback, and permits HTTP session cookies. The default configuration requires external Cognito/Valkey settings and uses Secure cookies.

| Endpoint | Behavior |
|---|---|
| `/oauth2/authorization/cognito` | Start Cognito Authorization Code login with S256 PKCE |
| `/login/oauth2/code/cognito` | Spring Security OIDC callback |
| `/api/session` | Authenticated subject identifier; login success destination |
| `/api/csrf` | Authenticated CSRF token and header name for mutations |
| `POST /logout` | Requires CSRF; deletes shared BFF session and redirects through provider logout |
| `/logged-out` | Public sign-out confirmation |
| `/actuator/health`, `/v3/api-docs` | Require authentication; anonymous requests return 401 |

Security context, pending authorization requests, authorized clients and their access/refresh tokens remain server-side in Valkey, under `miseworks:staff-bff:sessions`. The HttpOnly, SameSite=Lax session cookie is named `MISEWORKS_SESSION`, uses path `/` and no Domain attribute, and contains only the session identifier. Spring Security rotates the session ID at login. Idle timeout is 30 minutes; absolute lifetime is 8 hours from session creation, enforced on subsequent requests. These are initial configurable defaults, not settled product policy. Local Cognito client token defaults are 15-minute access/ID tokens and a one-day refresh token.

Outside the local profile supply `COGNITO_ISSUER_URI`, `COGNITO_CLIENT_ID`, `COGNITO_CLIENT_SECRET`, `COGNITO_LOGOUT_URI`, `BFF_LOGOUT_REDIRECT_URI`, and `VALKEY_HOST`. `VALKEY_PORT` defaults to 6379. Configure Redis TLS/authentication and deployed ingress/cookie settings for the deployment environment. The local setup is HTTP-only emulation and is not a production deployment configuration.

Automatic token refresh and coordination across replicas, role/permission mapping, all-session invalidation on staff deactivation, revocation enforcement, and BFF-to-Reservations token forwarding remain for subsequent work. Logout removes the BFF session and signs out the provider browser session; it does not promise revocation of all issued tokens or other sessions. This phase verifies OIDC login and shared session behavior only.
