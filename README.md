# EventHub

EventHub is a Spring Boot REST API for managing events and registrations, built as a portfolio project Java API development.

**Domain:** organizers create events; attendees register to attend.

**Status:** User & Event CRUD complete. Spring Security + JWT authentication complete. Sessions & Registrations endpoints planned next.

## Run EventHub locally

EventHub is a [Spring Boot](https://spring.io/guides/gs/spring-boot) application built with [Gradle](https://spring.io/guides/gs/gradle/), backed by PostgreSQL. Java 21 is required for the build, and Docker is required to run Postgres.

You first need to clone the project locally:

```bash
git clone git@github.com:PedroASNegrao/eventhub.git
cd eventhub
```

Start Postgres:

```bash
docker compose up -d db
```

Then run the application from the `backend` directory:

```bash
cd backend
./gradlew bootRun
```

(If Java 21 isn't on your `$PATH`, prefix with `JAVA_HOME=/path/to/jdk-21`.)

You can then access the API at <http://localhost:8080>.

Or run the full stack — app and Postgres together, no local JDK/Gradle needed:

```bash
docker compose up -d --build
```

## Database configuration

In its default configuration, EventHub connects to PostgreSQL at `jdbc:postgresql://localhost:5433/eventhub_db` (credentials `admin`/`admin123`), as set in `docker-compose.yml` and `backend/src/main/resources/application.yml`. Port `5433` is used instead of the default `5432` to avoid clashing with another local project's Postgres instance.

Schema is managed entirely by [Flyway](backend/src/main/resources/db/migration) migrations under `backend/src/main/resources/db/migration/` — Hibernate only validates the schema (`ddl-auto: validate`), it never generates or alters tables.

Database files persist in `postgres_data/` at the project root (a Docker bind mount), so they survive container restarts and can be inspected directly on disk.

For cloud deployment, the same image (`backend/Dockerfile`) reads its datasource from environment variables instead of `application.yml` defaults:

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL of the target Postgres instance |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `SERVER_PORT` | Port the app listens on (defaults to `8080`) |

## API

### Auth

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Exchange email/password for an access + refresh token pair |
| `POST` | `/api/auth/refresh` | Exchange a valid refresh token for a new token pair |

Send the access token as `Authorization: Bearer <token>` on subsequent requests. Tokens are signed JWTs (HS256) carrying the user's email, id, and role; access tokens expire after 15 minutes, refresh tokens after 7 days (both configurable via `JWT_ACCESS_EXPIRATION` / `JWT_REFRESH_EXPIRATION`).

### Users

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/users` | Create a user (`ORGANIZER` or `ATTENDEE`) — public, used for signup |
| `GET` | `/api/users/{id}` | Retrieve a user by ID — requires authentication |
| `GET` | `/api/users` | List all users — requires authentication |

### Events

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/events` | Create an event — requires the `ORGANIZER` role |
| `GET` | `/api/events/{id}` | Retrieve an event by ID — requires authentication |
| `GET` | `/api/events` | List all events — requires authentication |

### Sessions & Registrations

Not yet implemented. The Flyway schema exists (`V2`, `V3`); entities and endpoints are planned next.

## Error Handling

The API uses a standardized error response format:

```json
{
  "message": "Resource not found",
  "status": 404,
  "timestamp": "2026-06-27T12:30:00"
}
```

For validation errors (400):
```json
{
  "message": "Validation failed",
  "status": 400,
  "errors": {
    "email": "Email is required",
    "name": "Name must not be blank"
  },
  "timestamp": "2026-06-27T12:30:00"
}
```

Errors follow this shape across all endpoints — `404` for missing resources, `409` for duplicate emails, `400` with field-level detail for validation failures — via `GlobalExceptionHandler`.

## Architecture

### Layered Design

```
┌─────────────────────────┐
│   HTTP Controllers      │  EventController, UserController
├─────────────────────────┤
│   Service Layer         │  EventService, UserService
├─────────────────────────┤
│   Repository Layer      │  Spring Data JPA (EventRepository, UserRepository)
├─────────────────────────┤
│   Domain Entities       │  Event, User (JPA mapped to Postgres)
└─────────────────────────┘
```

### Key Components

- **DTOs:** Immutable records (`EventRequestDTO`, `UserResponseDTO`, etc.) for API contracts
- **Mappers:** MapStruct-based entity ↔ DTO conversion (zero manual boilerplate)
- **Exception Handling:** Centralized `GlobalExceptionHandler` catches all exceptions and returns consistent 4xx/5xx responses
- **Database Migrations:** Flyway versioned SQL migrations in `src/main/resources/db/migration/`
- **Security:** Stateless JWT authentication — `JwtAuthenticationFilter` authenticates each request from the bearer token's claims; `@PreAuthorize` enforces role checks per endpoint

## Database Schema

### Users Table (V1)
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Events Table (V2)
```sql
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    organizer_id INT NOT NULL REFERENCES users(id),
    date TIMESTAMP NOT NULL,
    location VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Sessions Table (V2)
```sql
CREATE TABLE sessions (
    id SERIAL PRIMARY KEY,
    event_id INT NOT NULL REFERENCES events(id),
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Registrations Table (V3)
```sql
CREATE TABLE registrations (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id),
    event_id INT NOT NULL REFERENCES events(id),
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, event_id)
);
```

Flyway automatically applies these migrations on startup (configured in `application.yml`).

## Configuration

### application.yml

```yaml
spring:
  application:
    name: eventhub
  datasource:
    url: jdbc:postgresql://localhost:5433/eventhub_db
    username: admin
    password: admin123
  jpa:
    hibernate:
      ddl-auto: validate  # Only validates; migrations are handled by Flyway
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080

jwt:
  secret: ${JWT_SECRET:...}                          # HS256 key; override in production
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:900000}     # 15 minutes, in ms
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000} # 7 days, in ms
```

### Docker Compose (docker-compose.yml)

```yaml
services:
  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: eventhub_db
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
    ports:
      - "5433:5432"
    volumes:
      - ./postgres_data:/var/lib/postgresql/data
```

**Persistent Storage:** Database files are stored in the `postgres_data/` directory at the project root. This bind mount persists data across container restarts.

## Project Structure

```
eventhub/
├── backend/
│   ├── build.gradle                    # Gradle build config
│   ├── src/main/
│   │   ├── java/com/eventhub/
│   │   │   ├── EventHubApplication.java   # Main entry point
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java    # Stateless JWT security filter chain
│   │   │   ├── security/
│   │   │   │   ├── JwtService.java               # Token issuing & parsing
│   │   │   │   ├── JwtAuthenticationFilter.java  # Authenticates requests from bearer tokens
│   │   │   │   ├── JwtAuthenticationEntryPoint.java  # 401 JSON response
│   │   │   │   ├── JwtAccessDeniedHandler.java       # 403 JSON response
│   │   │   │   └── TokenType.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── EventController.java
│   │   │   │   └── UserController.java
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── EventService.java
│   │   │   │   └── UserService.java
│   │   │   ├── repository/
│   │   │   │   ├── EventRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── mapper/
│   │   │   │   ├── EventMapper.java
│   │   │   │   └── UserMapper.java
│   │   │   ├── domain/
│   │   │   │   ├── User.java
│   │   │   │   ├── Event.java
│   │   │   │   └── UserRole.java
│   │   │   ├── dto/
│   │   │   │   ├── EventRequestDTO.java
│   │   │   │   ├── EventResponseDTO.java
│   │   │   │   ├── UserRequestDTO.java
│   │   │   │   ├── UserResponseDTO.java
│   │   │   │   ├── LoginRequestDTO.java
│   │   │   │   ├── RefreshRequestDTO.java
│   │   │   │   └── TokenResponseDTO.java
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── ResourceNotFoundException.java
│   │   │       ├── EmailAlreadyExistsException.java
│   │   │       ├── InvalidCredentialsException.java
│   │   │       ├── ErrorResponseDTO.java
│   │   │       └── ValidationErrorResponseDTO.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           ├── V1__create_users_table.sql
│   │           ├── V2__create_events_and_sessions_table.sql
│   │           └── V3__create_registrations_table.sql
│   └── src/test/
│       └── java/com/eventhub/
│           └── EventHubApplicationTests.java  # Test skeleton
├── postgres_data/                      # Postgres persistent data (bind mount)
├── docker-compose.yml                  # Docker services (Postgres)
├── CLAUDE.md                           # Developer guide
├── README.md                           # Project documentation
└── .gitignore
```

## Development Notes

### Key Technologies

- **Spring Boot 4.1.0** — Latest LTS with improved observability and performance
- **Spring Data JPA** — Simplifies database access with repository pattern
- **MapStruct 1.6.3** — Compile-time DTO mapping (zero reflection)
- **Flyway** — Schema versioning and migration management
- **Gradle** — Faster, more flexible than Maven; better for monorepos

### Design Patterns

- **Repository Pattern** — Spring Data JPA repositories abstract database access
- **DTO Pattern** — Decouples API contract from internal entity structure
- **Mapper Pattern** — MapStruct handles entity ↔ DTO conversion without boilerplate
- **Service Pattern** — Business logic isolated in service layer
- **Global Exception Handler** — Centralized error responses across all endpoints

## Next Steps

### Phase: Sessions & Registrations

- Session CRUD endpoints
- Registration endpoints (attendee joins event)
- Query endpoints (events by organizer, registrations by user)

### Phase: Testing

- Unit tests for services (mock repositories)
- Integration tests for controllers (MockMvc + test database)
- Testcontainers for Postgres in CI/CD

## Troubleshooting

### Gradle Build Fails – "JAVA_HOME not set"

Run with explicit JDK path:
```bash
JAVA_HOME=/home/pedro/.jdks/jdk-21.0.11+10 ./gradlew build
```

### "Connection refused" to Postgres

Ensure Docker container is running:
```bash
docker compose ps
# If not running:
docker compose up -d
```

And verify port 5433 (not 5432):
```bash
docker compose logs db
```

### Flyway Migration Errors

If a migration fails, check the database state:
```bash
docker exec eventhub_db psql -U admin -d eventhub_db -c "\dt"
```

Never manually drop or alter tables — rollback, fix the migration, and re-run.

### Reset Database Data

To completely reset the database (useful during development), stop the container and clear the `postgres_data/` directory:
```bash
docker compose down
rm -rf postgres_data/*
docker compose up -d
# Flyway will re-apply all migrations
```

## Looking for something in particular?

| Concern | Class or file |
|---|---|
| Application entry point | [EventHubApplication](backend/src/main/java/com/eventhub/EventHubApplication.java) |
| Security configuration | [SecurityConfig](backend/src/main/java/com/eventhub/config/SecurityConfig.java) |
| JWT issuing & parsing | [JwtService](backend/src/main/java/com/eventhub/security/JwtService.java) |
| JWT request authentication | [JwtAuthenticationFilter](backend/src/main/java/com/eventhub/security/JwtAuthenticationFilter.java) |
| Auth endpoints | [AuthController](backend/src/main/java/com/eventhub/controller/AuthController.java) |
| Event endpoints | [EventController](backend/src/main/java/com/eventhub/controller/EventController.java) |
| User endpoints | [UserController](backend/src/main/java/com/eventhub/controller/UserController.java) |
| Event business logic | [EventService](backend/src/main/java/com/eventhub/service/EventService.java) |
| User business logic | [UserService](backend/src/main/java/com/eventhub/service/UserService.java) |
| Entity ↔ DTO mapping | [EventMapper](backend/src/main/java/com/eventhub/mapper/EventMapper.java), [UserMapper](backend/src/main/java/com/eventhub/mapper/UserMapper.java) |
| Centralized error handling | [GlobalExceptionHandler](backend/src/main/java/com/eventhub/exception/GlobalExceptionHandler.java) |
| Schema migrations | [db/migration](backend/src/main/resources/db/migration) |
| Application config | [application.yml](backend/src/main/resources/application.yml) |

