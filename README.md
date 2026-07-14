# Reservator

Reservator is a Spring Boot modular monolith for organization-scoped sports
reservations. The target modules are `identity`, `catalog`, and `reservation`.
Only slices marked implemented in
[the MVP roadmap](docs/roadmap/mvp-vertical-slices.md) are available at runtime.

The implemented runtime currently covers internal organization/admin provisioning,
venue creation/read, and the complete admin setup path from an inactive bookable
resource through policy, recurring schedule, global-Sport configuration, and
explicit activation. Weekly customer availability is the next planned slice.

## Requirements

- JDK 26
- Docker, when running PostgreSQL locally or executing PostgreSQL integration
  tests

The Gradle wrapper is included; a separate Gradle installation is not required.

## Tests

```powershell
.\gradlew.bat test
```

```bash
./gradlew test
```

Fast behavior tests use H2. PostgreSQL migration tests use Testcontainers and
are skipped when Docker is unavailable.

## Local PostgreSQL

Copy `.env.example` to `.env`, set the database credentials, then start
PostgreSQL:

```bash
docker compose up -d
```

Configure the Spring datasource and the OIDC issuer through environment
variables before running the application:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydatabase
SPRING_DATASOURCE_USERNAME=myuser
SPRING_DATASOURCE_PASSWORD=changeme
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://issuer.example.com
```

Run with `.\gradlew.bat bootRun` on Windows or `./gradlew bootRun` on
macOS/Linux.

## API documentation

With the application running:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Start with [docs/index.md](docs/index.md) for the domain and API documentation.
