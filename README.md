# Todo REST API

A RESTful JSON API for managing personal to-do items — create, list (with filtering/sorting), view, update, mark complete/incomplete, and delete. Built with Spring Boot 4.1 / Java 17 as a take-home exercise. The full behavioral spec (Given/When/Then acceptance criteria) lives in [`FEATURE.md`](FEATURE.md).

## Prerequisites

- Java 17 (JDK, not just a JRE)
- No local Maven install needed — this repo includes the Maven Wrapper (`./mvnw`), which downloads the correct Maven version automatically on first run.

## Build & Run

### Option A — command line (no IDE)

```bash
./mvnw clean package        # always build clean — see note below
java -jar target/takehomeexercise-0.0.1-SNAPSHOT.jar
```

Or skip the jar and run directly:

```bash
./mvnw clean spring-boot:run
```

The app starts on http://localhost:8080.

> Always build with `clean` (`./mvnw clean ...`, not a bare `./mvnw ...`). This project uses Lombok and MapStruct together, and a non-clean incremental build can leave their annotation processors with a stale, inconsistent view of the source, silently producing wrong generated code. `clean` forces a full recompile and avoids that class of bug.

### Option B — from an IDE (IntelliJ, Eclipse, VS Code)

1. Open the project as a Maven project (import `pom.xml`).
2. Install the Lombok plugin for your IDE, and enable annotation processing (IntelliJ: *Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing*).
3. If available, prefer "Delegate IDE build/run actions to Maven" (IntelliJ: *Settings → Build Tools → Maven → Runner*) — this routes builds through the same Maven pipeline as the command line, avoiding IDE-specific incremental-compiler quirks with Lombok/MapStruct.
4. Run/Debug the `main` method in `TakeHomeExerciseApplication`.

### Option C — Docker

```bash
docker build -t takehomeexercise .
docker run -p 8080:8080 -v $(pwd)/data:/app/data takehomeexercise
```

The `-v $(pwd)/data:/app/data` volume mount persists the H2 database file across container restarts.

### Credentials — read this before testing the API

Every `/api/v1/todos/**` endpoint requires HTTP Basic authentication. Default credentials, baked in for this demo:

```
username: admin
password: changeme
```

Override them via environment variables if needed: `APP_SECURITY_USERNAME`, `APP_SECURITY_PASSWORD`.

> This is a deliberate demo-only shortcut, not production practice. A real production service should never ship a default password in a committed config file — credentials belong in a secrets manager / vault / CI-injected environment variable with no fallback default. It's called out again in [Trade-offs](#trade-offs) below.

### Testing the API — use Swagger UI

Swagger UI is the recommended way to explore and exercise the API, and needs no setup beyond the login step below:

1. Open http://localhost:8080/swagger-ui/index.html (this page itself needs no login).
2. Click Authorize (padlock icon, top right).
3. Enter `admin` / `changeme`, click Authorize, then close the dialog.
4. Every endpoint's "Try it out" now works seamlessly for the rest of the session — no need to re-enter credentials per request.

The raw OpenAPI spec is also available at http://localhost:8080/v3/api-docs (also public, no login needed).

If you'd rather use Postman or curl: set Basic Auth with the credentials above (e.g. `curl -u admin:changeme http://localhost:8080/api/v1/todos`).

## Running Tests

```bash
./mvnw clean test                                    # run all tests
./mvnw test -Dtest=TodoControllerTest                # run one test class
./mvnw test -Dtest=TodoControllerTest#methodName      # run one test method
./mvnw clean verify                                  # full build + tests + coverage gate
```

`./mvnw clean verify` additionally enforces a 90% line-coverage minimum via JaCoCo (current measured coverage: ~92%) — the build fails if coverage regresses below that. The HTML report is written to `target/site/jacoco/index.html` after either `test` or `verify`.

From an IDE: right-click a test class or method and Run/Debug as usual (again, prefer "delegate to Maven" if available, for the same annotation-processing reason as above).

## Design Choices

### Backend architecture

- Layered architecture (Controller → Service → Repository), DTOs at the boundary. Controllers accept/return DTOs only; JPA entities never leave the service layer (mapped via MapStruct). This decouples the wire format from the persistence model — either can change independently, and each layer is testable in isolation.
- Spring Data JPA repository abstraction over H2. Because persistence is coded against `JpaRepository`, not raw JDBC/SQL, swapping the underlying database later (H2 → Postgres/MySQL) is a dependency + datasource config change, not a code change to the service or controller layers.
- Intention-revealing entity mutators, not blind setters. `Todo` exposes `updateDetails(...)`, `markCompleted()`, `markIncomplete()` instead of generic setters — the entity's public API reads as documentation of the only state transitions that are actually valid, and `createdAt`/`updatedAt` are maintained automatically via JPA lifecycle callbacks (`@PrePersist`/`@PreUpdate`) rather than trusted to the caller.
- RMM Level 2, not Level 3/HATEOAS — a deliberate scope decision, not an oversight. Resource-oriented URIs, correct HTTP verbs, and correct status codes are all in place. Level 3 hypermedia (`EntityModel`, link assemblers on every response) adds real value when clients need to discover valid next actions dynamically; for a scoped CRUD API it's meaningfully more implementation and test surface for a benefit this exercise doesn't need. Documented here as a known, intentional stopping point.
- `ProblemDetail` (RFC 9457) for all error responses, not a hand-rolled error DTO. It's a Spring/industry standard shape (`type`/`title`/`status`/`detail`/`instance`), needs no bespoke documentation for API consumers who already know the RFC, and integrates natively with `ResponseEntityExceptionHandler` for framework-thrown exceptions — extended with an `errors` property for field-level validation detail without breaking the standard shape.
- Enum-based, whitelisted filtering/sorting (`status`, `sortBy`, `direction` query params) rather than exposing Spring's raw `Sort` binding. Whitelisting means a bad value gets a clean `400` (not a leaked field name or a `500`), and it self-documents as a dropdown in Swagger instead of a free-text string.
- OpenAPI/Swagger on every endpoint and DTO. Makes the API self-testable without external tooling (Postman collections, hand-written docs) — see [Testing the API](#testing-the-api--use-swagger-ui) above.
- HTTP Basic authentication, applied uniformly to all `/api/v1/todos/**` endpoints. Simplest correct mechanism for a single-user personal to-do list — no session/cookie state, so CSRF protection is correctly disabled (there's no CSRF attack surface without a session to forge). Swagger, the raw OpenAPI spec, and `/actuator/health` stay public deliberately, so the API remains self-documenting and health-checkable without credentials.
- A local JaCoCo coverage gate, bound to `./mvnw verify` (not `test`, so day-to-day `test` runs stay fast), enforcing a minimum set just below the measured real baseline rather than an arbitrary target — it protects against *regression*, not an aspirational number picked out of thin air.
- Feature-spec-first development. [`FEATURE.md`](FEATURE.md) was written before implementation, with Given/When/Then acceptance criteria; every test method traces back to a specific criterion. This keeps the spec and the test suite from drifting apart and gives durable documentation of intended behavior independent of the code.
- API versioning via a URI path prefix (`/api/v1/todos`), not a bare `/todos`. Spring Framework 7 has newer native versioning support (header/query/media-type resolvers via a `version` attribute on `@RequestMapping`), but it's recently introduced and adds moving parts; a path prefix needs no extra dependency, is trivial to test, and is the convention most API consumers already expect. A future breaking change ships as `/api/v2/todos` alongside the existing route rather than breaking current clients.
- Optimistic locking via a JPA `@Version` column on `Todo`. Because each request loads its own fresh copy of the entity and saves within its own transaction, two concurrent edits to the same Todo are only safe if the second save is rejected rather than silently overwriting the first. The version check happens automatically at the database level (`UPDATE ... WHERE id=? AND version=?`); a conflict throws `ObjectOptimisticLockingFailureException`, mapped to `409 Conflict`. `version` is exposed in `TodoResponse` for transparency, though clients don't need to send it back for the protection to work.
- Docker multi-stage build (JDK for building, JRE-only for running), non-root container user, and a volume-mounted data directory — smaller final image, standard container security practice, and persistence survives container restarts.

### Testing strategy

- A real test pyramid, not just one layer tested exhaustively.
  - *Unit* (`TodoServiceImplTest`): service logic in isolation, repository/mapper mocked via BDDMockito — fast, no Spring context.
  - *Slice* (`TodoControllerTest` via `@WebMvcTest`, `TodoRepositoryTest` via `@DataJpaTest`): the web layer and persistence layer each tested against a narrow, fast-starting Spring context — HTTP/validation/status-code behavior in one, real JPA/SQL behavior in the other, without loading the whole app either way.
  - *Integration* (`TodoLifecycleIntegrationTest`, `TodoFilterAndSortIntegrationTest`, `SecurityIntegrationTest`, `OpenApiSmokeTest`, all `@SpringBootTest`): a small number of full-stack tests proving the layers actually wire together correctly, including real end-to-end security enforcement.

  This gives fast feedback from the many cheap unit/slice tests, reserving the expensive full-context tests for what only they can prove: that everything integrates.
- BDD naming and structure: every test method is named `given<Precondition>_when<Action>_then<Outcome>Test()`, with explicit `// given` / `// when` / `// then` comments in the body. Intent is readable from the method name alone, and each name traces back to a specific acceptance criterion in `FEATURE.md`.
- BDDMockito + AssertJ over vanilla Mockito/JUnit assertions — BDDMockito's `given()`/`then().should()` vocabulary matches the given/when/then test structure, and AssertJ's fluent assertions (`.extracting()`, `.containsExactly()`, etc.) give clearer failure output.
- Edge cases and error paths are tested explicitly, not just happy paths — idempotent complete/incomplete transitions, non-positive `id` validation on every endpoint that takes one, the overdue-excludes-completed-and-null-dueDate business rule, and (for the newly added security layer) both the unauthenticated-401 and authenticated-200 cases plus proof that Swagger/health remain public.
- Concurrency behavior is tested, not just assumed from the `@Version` annotation being present. A repository-level test uses `TestEntityManager` to force two genuinely independent, non-cached loads of the same row (mirroring two concurrent requests), then asserts the second save actually throws `ObjectOptimisticLockingFailureException` rather than silently overwriting the first.

### Potential future enhancements

Not implemented here, but worth naming explicitly to show where this would go next with more time:

- Pagination on `GET /api/v1/todos` — returning the full unpaginated list doesn't scale past a small number of todos.
- CI pipeline (e.g. GitHub Actions running `./mvnw clean verify` on every push/PR) — right now all verification is manual/local.
- Schema migrations (Flyway/Liquibase) instead of `ddl-auto=update` — see [Trade-offs](#trade-offs).
- Real secrets management for the Basic Auth credentials instead of a config-file default — see [Trade-offs](#trade-offs).
- Testcontainers-based integration tests against a real target database (e.g. Postgres) — H2 doesn't perfectly emulate every production database's SQL dialect, so this would catch drift before it reaches production.
- Rate limiting on the API (e.g. Bucket4j, or Resilience4j's rate limiter) — protects the service itself from abuse/overload, independent of any downstream dependency's fragility.
- Further out: structured logging with correlation IDs, richer observability (metrics, tracing), and multi-user support with per-user resource ownership (today's single shared Basic Auth user assumes one user, consistent with a personal to-do list).

## Assumptions

- `completed` is the field/JSON name for the assignment's "is completed" flag (not `isCompleted`) — idiomatic Java naming; a field literally named `isCompleted` produces confusing `isIsCompleted`-style accessors.
- An `updatedAt` timestamp was added beyond what the assignment explicitly asked for, alongside `createdAt` — a small, deliberate addition to show a complete audit trail on each record.
- Base path is `/api/v1/todos` — the `/api/v1` prefix is for versioning (see Design Choices above); `/todos` itself matches the assignment's own endpoint examples literally. Unrelated to the pre-existing `/api/test/ping` liveness endpoint, which predates this feature and isn't part of the versioned Todo API.
- `PUT /api/v1/todos/{id}` is a full replacement of `title`/`description`/`dueDate` only — `completed`, `createdAt`, `updatedAt`, and `id` are server-controlled and not settable through this endpoint (completion has its own dedicated endpoints).
- "Overdue" is defined as `completed=false AND dueDate is set AND dueDate < today` — a completed item past its due date is *not* considered overdue. This specific definition isn't given in the assignment and was an assumption made and documented in `FEATURE.md`.
- `PATCH .../complete` and `.../incomplete` are idempotent — calling either on a todo already in that state returns `200`, not an error.
- `DELETE` returns `204 No Content` (not `200`), per RFC 7231 — there's no representation to return after deletion.
- Optimistic locking conflicts are detected purely server-side (each request's own fresh load-then-save within one transaction) — clients are not required to submit a `version` back on `PUT`/`PATCH` for the protection to work, even though it's exposed in the response for transparency.
- File-based (not in-memory) H2 was chosen specifically to satisfy "data persists across restarts" literally, since there was time to implement the persistent version.
- No multi-user support — a single shared Basic Auth user is assumed sufficient, consistent with the assignment describing a single-user personal to-do list with no mention of multi-user/authorization requirements.
- `title`/`description` length limits (200/2000 characters) are reasonable, arbitrary bounds — not specified by the assignment.
- Default sort with no query params is `createdAt` ascending, approximating natural insertion order for an unsorted request.
- RMM Level 2 (not Level 3/HATEOAS) was assumed sufficient, per the assignment's framing that interface sophistication isn't the primary evaluation focus.

## Trade-offs

- File-based H2 instead of a production RDBMS (Postgres/MySQL). Fast to set up and satisfies the literal "must persist across restarts" requirement, but lacks real production characteristics — realistic concurrent-connection handling, connection pool tuning at scale, and so on. Swapping it out later is a config change, not a rewrite, thanks to the Spring Data JPA abstraction (see Design Choices above).
- `ddl-auto: update` instead of a migration tool (Flyway/Liquibase). Fast for a take-home; in a real production codebase, schema changes should go through auditable, reversible migration scripts instead of Hibernate inferring and silently applying DDL.
- Default Basic Auth credentials committed in `application.yaml`. Done deliberately for this demo so the interviewer can run the app with zero setup (see [Credentials](#credentials--read-this-before-testing-the-api) above) — but this is explicitly *not* how a production service should handle secrets. A real deployment should pull credentials from a secrets manager/vault or CI-injected environment variables with no committed fallback.

## Development Process

Built with Claude Code as an engineering aid, not an autopilot — every decision below was mine to make and verify.

- Spec-first: [`FEATURE.md`](FEATURE.md)'s Given/When/Then acceptance criteria were written before implementation; every test traces back to one.
- Standards-first: coding and REST API conventions were codified upfront in `CLAUDE.md` and two reusable Claude Code skills (`rest-api-standards`, `testing-conventions`), so every change met the same bar instead of varying ad hoc.
- AI accelerated the mechanics — boilerplate, research into Spring Boot 4.1's breaking changes, test scaffolding — while the architecture, trade-offs, and verification (including every claim in this README) were directed and checked by me throughout.
