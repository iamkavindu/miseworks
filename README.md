# MiseWorks — Restaurant Operations Platform

Staff-facing operations for one restaurant at one location. Product scope is defined in [the PRD](docs/PRD.md); implementation boundaries, selected versions and verification limits are recorded in [the architecture](docs/architecture.md).

The Reservations baseline and Staff BFF with local Cognito login and Valkey sessions exist today. Other applications and infrastructure will be added incrementally.

With Java 25 and Docker available:

```sh
cd services/reservations-service
./mvnw --version
./mvnw clean verify
```

Verification starts disposable PostgreSQL through Testcontainers. Normal application startup requires `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `COGNITO_ISSUER_URI` supplied externally. Cognito-specific authorization remains incomplete; see the architecture before running beyond baseline checks.

The Staff BFF has an independent build using the same Java 25 baseline. Set up [local Floci and Valkey](infrastructure/local/README.md) before local startup:

```sh
cd apps/staff-bff
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

It listens on port 8081 by default (`SERVER_PORT` overrides it). Open [staff login](http://localhost:8081/oauth2/authorization/cognito) after startup; local bootstrap writes development credentials to the ignored `infrastructure/local/.env`. Maven tests use isolated Docker containers. See [the BFF notes](apps/staff-bff/README.md) for security behavior and remaining integration work.
