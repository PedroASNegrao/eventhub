# EventHub – Event Management API

A production-grade Spring Boot REST API for managing events and user registrations, designed to demonstrate mid-level Java API development maturity.

## Overview

EventHub is an event management platform where **organizers** create events and sessions, and **attendees** register to attend. The API provides full CRUD operations for users, events, and (later) sessions and registrations.

**Status:** Core API complete (User & Event endpoints); JWT authentication in progress.

---

## Quick Start

### Prerequisites

- **Java 21** (portable version at `/home/pedro/.jdks/jdk-21.0.11+10` or system-installed)
- **Docker & Docker Compose** (for Postgres)
- **Gradle 9.x** (bundled in `backend/gradlew`)

### Setup & Run

1. **Start the database:**
   ```bash
   docker compose up -d
   ```
   Postgres will run on `localhost:5433` (not 5432 — port conflict with another project).
   
   Database files are persisted in `postgres_data/` at the project root.

2. **Build the backend:**
   ```bash
   cd backend
   JAVA_HOME=/home/pedro/.jdks/jdk-21.0.11+10 ./gradlew build
   ```
   (Omit `JAVA_HOME=...` if Java 21 is already on your `$PATH`.)

3. **Run the application:**
   ```bash
   JAVA_HOME=/home/pedro/.jdks/jdk-21.0.11+10 ./gradlew bootRun
   ```

4. **The API is now available at:**
   ```
   http://localhost:8080
   ```

### Run everything with Docker (app + Postgres)

No local JDK/Gradle setup needed — this builds the API image and starts it alongside Postgres:

```bash
docker compose up -d --build
```

The API is available at `http://localhost:8080`; Postgres is still exposed on `localhost:5433` for inspection. To stop:

```bash
docker compose down
```

### Deploying to a cloud environment

The backend image (`backend/Dockerfile`) is self-contained and reads its database connection from environment variables, so it runs the same way locally or in the cloud:

| Variable | Purpose |
|----------|---------|
| `SPRING_DATASOURCE_URL` | JDBC URL of the Postgres instance (e.g. a managed cloud database) |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `SERVER_PORT` | Port the app listens on (defaults to `8080`) |

Build and push the image, then run it on any container platform (Render, Railway, Fly.io, ECS, etc.) with those variables pointed at your cloud Postgres instance:

```bash
docker build -t eventhub-app backend/
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db> \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  eventhub-app
```

Flyway migrations run automatically on startup against whatever database is configured.

---

## API Endpoints

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create a new user (organizer or attendee) |
| GET | `/api/users/{id}` | Retrieve a user by ID |
| GET | `/api/users` | List all users |

**Create User (POST /api/users)**

```json
{
  "name": "John Organizer",
  "email": "john@example.com",
  "password": "securePassword123",
  "role": "ORGANIZER"
}
```

Response: `201 Created`
```json
{
  "id": 1,
  "name": "John Organizer",
  "email": "john@example.com",
  "role": "ORGANIZER"
}
```

Errors:
- `409 Conflict` — Email already registered (`EmailAlreadyExistsException`)
- `400 Bad Request` — Validation failed (missing/invalid fields)

---

### Events

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/events` | Create a new event (organizer only) |
| GET | `/api/events/{id}` | Retrieve an event by ID |
| GET | `/api/events` | List all events |

**Create Event (POST /api/events)**

```json
{
  "title": "Spring Boot Workshop",
  "description": "Learn Spring Boot best practices",
  "organizerId": 1,
  "date": "2026-07-15T10:00:00",
  "location": "São Paulo, SP"
}
```

Response: `201 Created`
```json
{
  "id": 1,
  "title": "Spring Boot Workshop",
  "description": "Learn Spring Boot best practices",
  "organizerId": 1,
  "date": "2026-07-15T10:00:00",
  "location": "São Paulo, SP",
  "createdAt": "2026-06-27T12:30:00"
}
```

Errors:
- `404 Not Found` — Organizer (organizerId) does not exist
- `400 Bad Request` — Validation failed
- `500 Internal Server Error` — Unexpected error

---

### Sessions & Registrations

**Not yet implemented.** Database schema exists (Flyway V2, V3); endpoints are planned for post-JWT phase.

---

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

---

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

---

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

---

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

---

## Project Structure

```
eventhub/
├── backend/
│   ├── build.gradle                    # Gradle build config
│   ├── src/main/
│   │   ├── java/com/eventhub/
│   │   │   ├── EventHubApplication.java   # Main entry point
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java    # Spring Security (temp: permissive)
│   │   │   ├── controller/
│   │   │   │   ├── EventController.java
│   │   │   │   └── UserController.java
│   │   │   ├── service/
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
│   │   │   │   └── UserResponseDTO.java
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── ResourceNotFoundException.java
│   │   │       ├── EmailAlreadyExistsException.java
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

---

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

---

## Next Steps

### Phase: Spring Security + JWT

- Login endpoint: `POST /api/auth/login` (email/password → JWT)
- JWT validation filter: Extract & verify bearer token from request
- Role-based access control: `@PreAuthorize("hasRole(...)")` on endpoints
- Token refresh (optional): `POST /api/auth/refresh`

### Phase: Sessions & Registrations

- Session CRUD endpoints
- Registration endpoints (attendee joins event)
- Query endpoints (events by organizer, registrations by user)

### Phase: Testing

- Unit tests for services (mock repositories)
- Integration tests for controllers (MockMvc + test database)
- Testcontainers for Postgres in CI/CD

---

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

---

## Contributing

This is a portfolio project. Code style follows clean architecture principles:
- Lean layers (no unnecessary facades/factories)
- Immutable DTOs (Java records)
- MapStruct for mapping (no manual converters)
- Global exception handling (consistent error responses)

For detailed development guidelines, see [CLAUDE.md](CLAUDE.md).

---

## License

Portfolio project. Not licensed for redistribution.

---

## Author

Built as a demonstration for mid-level (Pleno) Java API developer interviews.  
**Contact:** convidaprototipo@gmail.com
