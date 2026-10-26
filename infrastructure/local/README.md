# Local authentication environment

Run from the repository root with Docker Compose v2+, Python 3, and Java 25 available:

```sh
docker compose -f infrastructure/local/compose.yaml up -d --wait
python3 infrastructure/local/bootstrap.py
cd apps/staff-bff
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Open [staff login](http://localhost:8081/oauth2/authorization/cognito). Use `LOCAL_MANAGER_USERNAME` and `LOCAL_MANAGER_PASSWORD` from `infrastructure/local/.env`. Bootstrap generates development credentials locally and writes that ignored file with mode 600; it also contains the BFF client configuration. It never reads AWS credentials or sends requests beyond the hardcoded local Floci endpoint. Run it again after recreating the environment; repeated runs reuse resources and the saved password. The development username does not grant application manager permissions: role mapping and staff administration are later work.

Compose runs `floci/floci:2.3.0` on `127.0.0.1:4566` and `valkey/valkey:8.1.3-alpine` on `127.0.0.1:6379`. Named volumes retain emulator and Valkey data. Valkey runs directly as a Compose service; this phase does not emulate the ElastiCache control plane or mount the host Docker socket into Floci. Sessions use the same Redis protocol intended for ElastiCache for Valkey.

The BFF runs on the host, and browser/discovery/JWKS/token URLs consistently use `localhost`. Use `localhost`, rather than `127.0.0.1`, for browser login: Cognito callbacks are registered exactly. If applications move inside Docker, configure a common reachable issuer and registered callbacks deliberately.

After starting the BFF, run from the repository root:

```sh
python3 infrastructure/local/smoke-login.py
```

This follows the hosted login form, OIDC callback, authenticated session, CSRF logout, and provider sign-out without printing credentials or tokens. It uses HTTP requests and a cookie jar, rather than browser automation.

To check shared sessions across two BFF processes, start a temporary second process from `apps/staff-bff/`:

```sh
java -jar target/staff-bff-1.0.0.jar --spring.profiles.active=local --server.port=8082
```

Then run `python3 infrastructure/local/smoke-login.py --replica-url http://localhost:8082` from the repository root. Login goes through port 8081; both processes must recognize the session, and logout must remove access through both. Stop the temporary replica after verification.

Stop the BFF with Ctrl+C. Stop the local infrastructure while retaining its volumes with:

```sh
docker compose -f infrastructure/local/compose.yaml down
```

Automated Maven integration tests start separate disposable Floci and Valkey containers on random ports. They do not need or mutate this Compose environment.

References: [Floci Cognito](https://floci.io/floci/services/cognito/), [Docker installation](https://floci.io/floci/getting-started/installation/), and [Spring Session Redis configuration](https://docs.spring.io/spring-session/reference/configuration/redis.html).
