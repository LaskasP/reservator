# Reservator

Multi-tenant reservation engine for generic resources (courts, rooms, etc.) with configurable time slots, schedule management, and a full reservation lifecycle (hold -> book -> confirm).

Built with Spring Boot 4.0.5, Java 25, PostgreSQL, and Redis.

## Prerequisites

| Tool       | Version | Purpose                          |
|------------|---------|----------------------------------|
| Java (JDK) | 25      | Compile and run the application  |
| Docker     | 24+     | PostgreSQL and Redis containers  |
| Gradle     | -       | Bundled via `gradlew` wrapper    |

## Environment Setup

Copy the example env file and adjust values as needed:

```bash
cp .env.example .env
```

The `.env` file is git-ignored. Default values in `.env.example`:

```
POSTGRES_DB=mydatabase
POSTGRES_USER=myuser
POSTGRES_PASSWORD=changeme
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=changeme
```

## Running the Application

### With Docker (recommended)

Spring Boot Docker Compose integration automatically starts PostgreSQL and Redis from `compose.yaml`:

```bash
# Windows
gradlew.bat bootRun

# macOS / Linux
./gradlew bootRun
```

The app starts on `http://localhost:8080`.

> If you prefer to manage containers manually, start them first and point the app at them:
>
> ```bash
> docker compose up -d
> ```
>
> Then set the datasource properties in `application.properties` or via environment variables and run `bootRun`.

### Without Docker

You need a running PostgreSQL and Redis instance. Configure the connection in environment variables or `application.properties`:

```bash
# Windows
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydatabase
set SPRING_DATASOURCE_USERNAME=myuser
set SPRING_DATASOURCE_PASSWORD=secret
set SPRING_DATA_REDIS_HOST=localhost
set SPRING_DATA_REDIS_PORT=6379
set SPRING_DOCKER_COMPOSE_ENABLED=false
gradlew.bat bootRun

# macOS / Linux
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydatabase
export SPRING_DATASOURCE_USERNAME=myuser
export SPRING_DATASOURCE_PASSWORD=secret
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
export SPRING_DOCKER_COMPOSE_ENABLED=false
./gradlew bootRun
```

## API Documentation (Swagger UI)

Once the application is running, open:

```
http://localhost:8080/swagger-ui/index.html
```

The OpenAPI spec (JSON) is available at:

```
http://localhost:8080/v3/api-docs
```

## Running Tests

Tests use an in-memory H2 database -- no Docker or external services required.

```bash
# Windows
gradlew.bat test

# macOS / Linux
./gradlew test
```

Test reports are generated at `build/reports/tests/test/index.html`.


## Load Testing (Gatling)

Load tests live in the `:load-tests` Gradle submodule using the Gatling Java DSL.

### Available Simulations

| Simulation                    | Description                                                        |
|-------------------------------|--------------------------------------------------------------------|
| `HotSlotContentionSimulation` | 200 concurrent users race to book the exact same slot              |
| `MixedWorkloadSimulation`     | 200 req/s mixed traffic (availability, hold, book, cancel, admin)  |

### Running Against a Local Instance

Start the application first (see above), then run the simulations:

```bash
# Run all simulations
# Windows
gradlew.bat :load-tests:gatlingRun

# macOS / Linux
./gradlew :load-tests:gatlingRun

# Run a specific simulation
# Windows
gradlew.bat :load-tests:gatlingRun-com.skouna.reservator.loadtest.HotSlotContentionSimulation
gradlew.bat :load-tests:gatlingRun-com.skouna.reservator.loadtest.MixedWorkloadSimulation

# macOS / Linux
./gradlew :load-tests:gatlingRun-com.skouna.reservator.loadtest.HotSlotContentionSimulation
./gradlew :load-tests:gatlingRun-com.skouna.reservator.loadtest.MixedWorkloadSimulation
```

Override the target URL with a system property:

```bash
./gradlew :load-tests:gatlingRun -DbaseUrl=http://your-host:8080
```

Gatling reports are generated at `load-tests/build/reports/gatling/`.

### Full Stack with Observability (Docker Compose)

The `docker/docker-compose.loadtest.yml` spins up the entire stack including monitoring:

| Service             | Port  | URL                          |
|---------------------|-------|------------------------------|
| Reservator App      | 8080  | http://localhost:8080        |
| PostgreSQL          | 5432  | -                            |
| Redis               | 6379  | -                            |
| Prometheus          | 9090  | http://localhost:9090        |
| Grafana             | 3000  | http://localhost:3000        |
| Postgres Exporter   | 9187  | -                            |
| Redis Exporter      | 9121  | -                            |

> **Note:** A `Dockerfile` in the project root is required before using this compose file. It is not yet created.

```bash
# Start the full observability stack
docker compose -f docker/docker-compose.loadtest.yml --env-file .env up -d

# Run load tests against it
gradlew.bat :load-tests:gatlingRun -DbaseUrl=http://localhost:8080   # Windows
./gradlew :load-tests:gatlingRun -DbaseUrl=http://localhost:8080     # macOS / Linux

# Tear down
docker compose -f docker/docker-compose.loadtest.yml down
```

Grafana default credentials are set in your `.env` file (`GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD`).

