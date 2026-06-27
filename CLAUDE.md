# EventHub — Developer Guide

## Project Context

**EventHub** is a portfolio project designed to demonstrate mid-level (Pleno) Java API development skills to technical interviewers. It's a polished, intentional codebase — not a production system shipped for speed.

**Purpose:** Land a *Desenvolvedor Java API Pleno* role.  
**Audience:** Technical interviewers evaluating API design, architecture maturity, security implementation, and test coverage.

---

## Technology Stack

- **Language:** Java 21
- **Framework:** Spring Boot 4.1.0
- **Build:** Gradle 9.x
- **ORM:** Spring Data JPA with Hibernate
- **Authentication:** Spring Security (JWT in progress)
- **Database:** PostgreSQL 15
- **Migrations:** Flyway
- **Mapping:** MapStruct
- **Database Container:** Docker Compose

---

## Domain Model

**Users** (via `User` entity, `UserRole` enum):
- Two roles: `ORGANIZER` (creates events) and `ATTENDEE` (registers for events)
- Endpoints: `POST /api/users` (create), `GET /api/users/{id}`, `GET /api/users`

**Events** (via `Event` entity):
- Created by organizers
- Endpoints: `POST /api/events` (create), `GET /api/events/{id}`, `GET /api/events`

**Sessions & Registrations:**
- DB schema exists (Flyway V2, V3)
- JPA entities and endpoints not yet implemented
- Planned for post-JWT phase

---

## Development Environment

### Java & Gradle

**System JDK:** Only Java 8 is installed system-wide. The backend requires **Java 21**.

**Portable JDK in use:** `/home/pedro/.jdks/jdk-21.0.11+10`  
Run Gradle with explicit `JAVA_HOME`:

```bash
cd backend
JAVA_HOME=/home/pedro/.jdks/jdk-21.0.11+10 ./gradlew build
```

(Check if system Java 21 has since been installed; if so, you may not need this workaround.)

### Docker & Postgres

**WSL2 Docker integration:** Manually enabled in Docker Desktop settings (Resources > WSL Integration).

**Port mapping:** EventHub's Postgres uses **port 5433** (not 5432) because another local project (`fintech_postgres`) occupies 5432. This is configured in both `docker-compose.yml` and `application.yml` — it's intentional, not a bug.

**Persistent data directory:** Database files are stored in a bind mount at `./postgres_data/` (at the project root). This persists data across container restarts and allows easy inspection of database files on the local filesystem.

```bash
# Start the Postgres container only (for local gradlew bootRun)
docker compose up -d db

# The application connects to: jdbc:postgresql://localhost:5433/eventhub_db
# Credentials: admin / admin123
# Data persists in: ./postgres_data/

# View database files
ls -la postgres_data/
```

**Containerized backend:** `backend/Dockerfile` builds the app into a standalone JRE image. `docker compose up -d --build` runs the app and Postgres together — the app connects to Postgres over the Docker network at `db:5432` (the host-side `5433` mapping is irrelevant inside the network). The app reads `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` / `SERVER_PORT` from the environment (with the same local defaults as before), so the same image deploys to any cloud container platform by pointing those variables at a managed Postgres instance.

---

## Architecture

### Layered Design (Clean Code)

```
controller/        →  EventController, UserController
  ↓
service/          →  EventService, UserService (business logic)
  ↓
repository/       →  EventRepository, UserRepository (Spring Data JPA)
  ↓
domain/           →  Event, User (JPA entities)
```

### Data Flow

**Request → DTO → MapStruct Mapper → Entity → Repository → Response DTO**

- **DTOs:** `EventRequestDTO`, `EventResponseDTO`, `UserRequestDTO`, `UserResponseDTO` (immutable records)
- **Mappers:** `EventMapper`, `UserMapper` (via MapStruct annotation processing)
- **Exceptions:** Global handler (`GlobalExceptionHandler`) catches `ResourceNotFoundException`, `EmailAlreadyExistsException`, validation errors (400), and unknown errors (500)

### Security (Current State)

**Temporary permissive config:** `SecurityConfig` permits all endpoints; BCryptPasswordEncoder is wired but not used in auth yet.

**Next phase:** Replace with Spring Security + JWT:
- Login endpoint: `POST /api/auth/login`
- JWT filter for bearer token validation
- Role-based endpoint authorization (`@PreAuthorize`)
- Logout/token refresh (if scope permits)

---

## Key Guidance

### Code Style

- **No unnecessary comments.** Name functions/variables clearly; explain only non-obvious constraints (e.g., why a specific database constraint exists).
- **Lean abstractions.** The current layered structure (controller → service → repository) is sufficient; don't add facades, builders, or event publishers until the need is clear.
- **Immutable DTOs.** Use Java records (JDK 21) for all request/response DTOs; `@Value` from Lombok is not used here.

### Mapping & DTOs

- Use **MapStruct** annotations (`@Mapper`, `@Mapping`) for boilerplate-free entity ↔ DTO conversion.
- Keep mappers in `mapper/` package; ensure they have a single responsibility (one mapper per entity pair is typical).
- DTOs are the API contract — never expose raw JPA entities in endpoints.

### Database Changes

- **Use Flyway migrations** for all schema changes. Versioned migrations (V1, V2, V3, ...) are tracked in `src/main/resources/db/migration/`.
- Never manually alter the database; Flyway enforces a single source of truth.
- In `application.yml`, `ddl-auto: validate` ensures Hibernate only checks that code and schema match — it doesn't auto-generate or drop tables.

### Testing

- **Not yet implemented.** The test skeleton exists (`EventHubApplicationTests`) but is empty (`contextLoads()`).
- When tests are added, use `@SpringBootTest` for integration tests (with a test database container or in-memory H2).
- Unit tests for services should mock repositories; integration tests should use real Postgres (via Docker Testcontainers or a dev database).

### Package Structure

All code lives under `com.eventhub.*`:
- `com.eventhub` — `EventHubApplication`, configuration
- `com.eventhub.domain` — JPA entities (`User`, `Event`)
- `com.eventhub.dto` — Request/response DTOs
- `com.eventhub.mapper` — MapStruct mappers
- `com.eventhub.controller` — HTTP endpoints
- `com.eventhub.service` — Business logic
- `com.eventhub.repository` — Spring Data JPA interfaces
- `com.eventhub.exception` — Exception classes and global handler
- `com.eventhub.config` — Spring configuration beans

---

## Roadmap

### Completed

- Flyway migrations (V1–V3): schema for users, events, sessions, registrations
- Domain entities: `User`, `Event`, `UserRole`
- DTOs (records) and MapStruct mappers for User and Event
- Service layer: `UserService`, `EventService`
- Controllers: basic CRUD endpoints for users and events
- Global exception handler (404, 409, 400, 500)
- Temporary permissive `SecurityConfig` (no-op auth for API testability)
- Docker Compose for Postgres
- Backend containerization: `Dockerfile` for the app, wired into `docker-compose.yml`, env-var-driven datasource config for cloud deployment

### Next (High Priority)

**Spring Security + JWT Authentication**

- Login endpoint (`POST /api/auth/login`) → returns JWT
- JWT filter: extract bearer token from request headers, validate signature, set `Authentication` in context
- Protect endpoints with `@PreAuthorize("hasRole('ORGANIZER')")` etc.
- This phase is the primary interview-impressing item; implement with security best practices in mind.

### Later

- **Session & Registration entities:** Build `Session`, `Registration` JPA entities and DTOs; implement `SessionService`, `RegistrationService`
- **Session & Registration endpoints:** CRUD endpoints for sessions and event registrations
- **Unit & integration tests:** @SpringBootTest, MockMvc, testcontainers for Postgres

---

## Important Caveats

### Instructions vs. Reality

The original instructions (pasted from external source) assume **Maven** and **Spring Boot 3**. The actual project uses **Gradle** and **Spring Boot 4.1.0**. If instructions are pasted again, adapt artifact names and syntax — don't copy literally.

Base package is `com.eventhub` (not a placeholder like `com.yourname.eventhub`).

### When to Re-Read This File

- Before picking up a new task or phase
- When debugging environment issues (JDK, Docker, port conflicts)
- When deciding on architecture for a new feature (tests, entities, endpoints)

---

## Quick Commands

```bash
# Backend setup
cd backend
JAVA_HOME=/home/pedro/.jdks/jdk-21.0.11+10 ./gradlew clean build

# Start Postgres only (for local gradlew bootRun)
docker compose up -d db

# Run the app locally
JAVA_HOME=/home/pedro/.jdks/jdk-21.0.11+10 ./gradlew bootRun

# Or run the full stack (app + Postgres) containerized, no JDK needed
docker compose up -d --build

# App runs on http://localhost:8080
# Postgres: localhost:5433 (credentials: admin/admin123)
# Data persists in: ./postgres_data/

# View Flyway migrations
ls -la backend/src/main/resources/db/migration/

# View database files
ls -la postgres_data/
```

---

## Contact

Portfolio inquiries: convidaprototipo@gmail.com
