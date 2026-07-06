# Todo REST API — Feature Specification

## Overview

A RESTful JSON API for managing personal to-do items: create, list, view, update, mark complete/incomplete, and delete. Built as a take-home exercise; this document is the spec implementation and tests are built against — every acceptance criterion below maps 1:1 to a test method.

## Domain Model

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | `Long` | generated | assigned by the server on creation, immutable |
| `title` | `String` | yes | non-blank |
| `description` | `String` | no | free text |
| `dueDate` | `LocalDate` | no | ISO-8601 (`YYYY-MM-DD`) |
| `completed` | `boolean` | — | defaults `false`; server-controlled, not settable via create/update. Corresponds to the assignment's `isCompleted` flag — named `completed` in the Java model/JSON body (idiomatic Java field naming; a field literally named `isCompleted` produces confusing `isIsCompleted`-style accessors and Jackson serialization quirks) |
| `createdAt` | `Instant` | — | set once at creation, immutable thereafter |
| `updatedAt` | `Instant` | — | equals `createdAt` at creation; refreshed to the current time whenever the record changes (update, complete, or incomplete) |
| `version` | `Long` | — | starts at `0`, incremented by JPA on every update; used for optimistic locking, not settable by clients |

## Non-functional requirements

- **RMM Level 2**: correct resource-oriented URIs, correct HTTP verbs, correct HTTP status codes for every outcome. (Level 3 hypermedia/HATEOAS is documented as a deliberate extension point, not implemented — see `.claude/skills/rest-api-standards/SKILL.md`.)
- **Persistence**: file-based H2 (`jdbc:h2:file:...`) — data survives application restarts, satisfying the requirement literally. Repository/service layering (Spring Data JPA) means swapping to a production database later is a datasource/dependency config change only, with zero changes to service or controller code.
- **Validation**: enforced via Bean Validation (`@Valid`/`@NotBlank`) on request DTOs at the controller boundary, plus `@Size` limits on free-text fields (`title` max 200 chars, `description` max 2000 chars), `@Positive` on numeric path identifiers (`id`), and friendly `400` responses (not a generic error page) for malformed query parameters such as an unrecognized filter/sort value.
- **Error handling**: centralized via `@ControllerAdvice`/`ResponseEntityExceptionHandler` — controllers never contain try/catch for business errors. Every error response is a standard Spring `ProblemDetail` (RFC 9457 "Problem Details for HTTP APIs"): `type`, `title`, `status`, `detail`, `instance` (the request path), plus an `errors` extension array of field/violation-level messages when applicable — not a hand-rolled error DTO.
- **Deployment**: the application is packaged as a Docker image via a multi-stage `Dockerfile` (`eclipse-temurin:17-jdk` build stage, `eclipse-temurin:17-jre` runtime stage, non-root user). The `data/` directory (holding the H2 database file) should be mounted as a volume — e.g. `docker run -p 8080:8080 -v $(pwd)/data:/app/data <image>` — so persistence survives container restarts, consistent with the Persistence requirement above.
- **API documentation**: every endpoint is documented with OpenAPI annotations (`@Operation`, `@ApiResponse`, `@Parameter`) and every request/response DTO with `@Schema` (descriptions and examples). The raw spec is served at `/v3/api-docs` and an interactive Swagger UI at `/swagger-ui/index.html`, so the API is self-documenting and testable without external tooling.
- **Authentication**: all `/api/v1/todos/**` endpoints require HTTP Basic authentication (`spring-boot-starter-security`, a single configured user — credentials externalized via `APP_SECURITY_USERNAME`/`APP_SECURITY_PASSWORD` environment variables, defaulting to `admin`/`changeme` for local development). `/v3/api-docs/**`, `/swagger-ui/**`, and `/actuator/health` remain public so the API is still self-documenting and health-checkable without credentials. CSRF is disabled — this is a stateless REST API authenticated per-request via Basic Auth, never via a session/cookie, so there's no CSRF attack surface to protect.
- **Test coverage**: a local JaCoCo coverage gate (`jacoco-maven-plugin`, bound to `./mvnw verify`) enforces a 90% line-coverage minimum, set just below the measured real baseline (92%) rather than an arbitrary target.
- **API versioning**: the base path is `/api/v1/todos`, not a bare `/todos` — a URI path-prefix version, chosen over Spring Framework 7's newer native versioning support (header/query/media-type resolvers) for zero added risk and universal client familiarity. A future breaking change ships as `/api/v2/todos` alongside the existing `v1` route rather than breaking existing clients in place.
- **Optimistic locking**: `Todo` carries a JPA `@Version` column, incremented on every update. If two clients load the same Todo and both attempt to save changes, the second save fails with `409 Conflict` (`ObjectOptimisticLockingFailureException` mapped via `GlobalExceptionHandler`) instead of silently overwriting the first client's change. `version` is exposed in `TodoResponse` for transparency. Verified with a dedicated repository test that forces two independent, non-cached loads of the same row via `TestEntityManager` (not just asserted from the annotation being present).

## Capabilities

### 1. Create a Todo

`POST /api/v1/todos`

**Given** a valid request body with at least a non-blank `title`
**When** a client POSTs to `/api/v1/todos`
**Then** the API returns `201 Created`, a `Location` header pointing at the new resource (`/api/v1/todos/{id}`), and a body containing the created Todo with `completed=false`, a server-generated `id`, `createdAt` and `updatedAt` (equal to each other), and the submitted `title`/`description`/`dueDate`.

**Given** a request body with a blank or missing `title`
**When** a client POSTs to `/api/v1/todos`
**Then** the API returns `400 Bad Request` with a validation error describing the offending field, and no Todo is persisted.

**Given** a request body with a `title` longer than 200 characters or a `description` longer than 2000 characters
**When** a client POSTs to `/api/v1/todos`
**Then** the API returns `400 Bad Request` describing the offending field, and no Todo is persisted.

### 2. List Todos (with filtering and sorting)

`GET /api/v1/todos?status={ALL|COMPLETED|INCOMPLETE|OVERDUE}&sortBy={TITLE|DUE_DATE|CREATED_AT}&direction={ASC|DESC}`

All three query params are optional (defaults: `status=ALL`, `sortBy=CREATED_AT`, `direction=ASC`) and accepted case-insensitively (e.g. `status=completed` works the same as `status=COMPLETED`). "Overdue" means `completed=false AND dueDate` is set and before today — a completed item past its due date is not overdue.

**Given** any number of existing Todos (including zero)
**When** a client sends `GET /api/v1/todos` with no query params
**Then** the API returns `200 OK` with a JSON array of all Todos (an empty array if none exist), sorted by `createdAt` ascending, each with essential details (`id`, `title`, `dueDate`, `completed`).

**Given** existing Todos with a mix of completed and incomplete items
**When** a client sends `GET /api/v1/todos?status=COMPLETED`
**Then** the API returns `200 OK` with only the completed Todos.

**Given** existing Todos with a mix of completed and incomplete items
**When** a client sends `GET /api/v1/todos?status=INCOMPLETE`
**Then** the API returns `200 OK` with only the incomplete Todos.

**Given** existing Todos where one incomplete Todo has a `dueDate` before today, one completed Todo has a `dueDate` before today, and one incomplete Todo has no `dueDate`
**When** a client sends `GET /api/v1/todos?status=OVERDUE`
**Then** the API returns `200 OK` with only the incomplete Todo whose `dueDate` is before today — the completed one and the one with no `dueDate` are excluded.

**Given** any number of existing Todos
**When** a client sends `GET /api/v1/todos?status=bogus`
**Then** the API returns `400 Bad Request` describing the invalid `status` value, and no Todos are returned.

**Given** existing Todos with different `title`/`dueDate`/`createdAt` values
**When** a client sends `GET /api/v1/todos?sortBy=TITLE&direction=DESC` (or `DUE_DATE`/`CREATED_AT`, `ASC`/`DESC`)
**Then** the API returns `200 OK` with the Todos ordered by the requested field and direction.

**Given** any number of existing Todos
**When** a client sends `GET /api/v1/todos?sortBy=bogus` or `GET /api/v1/todos?direction=bogus`
**Then** the API returns `400 Bad Request` describing the invalid value, and no Todos are returned.

### 3. View a Todo by id

`GET /api/v1/todos/{id}`

**Given** an existing Todo with a known `id`
**When** a client sends `GET /api/v1/todos/{id}`
**Then** the API returns `200 OK` with the full Todo representation.

**Given** an `id` with no matching Todo
**When** a client sends `GET /api/v1/todos/{id}`
**Then** the API returns `404 Not Found`.

**Given** a non-positive `id` (e.g. `0` or `-1`)
**When** a client sends `GET /api/v1/todos/{id}`
**Then** the API returns `400 Bad Request` describing the invalid `id`.

### 4. Update a Todo

`PUT /api/v1/todos/{id}`

**Given** an existing Todo and a valid request body (non-blank `title`, optional `description`/`dueDate`)
**When** a client sends `PUT /api/v1/todos/{id}`
**Then** the API returns `200 OK` with the updated `title`/`description`/`dueDate`, a refreshed `updatedAt` (later than the original), and `completed`/`createdAt`/`id` unchanged.

**Given** an `id` with no matching Todo
**When** a client sends `PUT /api/v1/todos/{id}`
**Then** the API returns `404 Not Found` and no Todo is modified.

**Given** an existing Todo and a request body with a blank `title`
**When** a client sends `PUT /api/v1/todos/{id}`
**Then** the API returns `400 Bad Request` and the existing Todo is left unmodified.

**Given** an existing Todo and a request body with a `title` longer than 200 characters or a `description` longer than 2000 characters
**When** a client sends `PUT /api/v1/todos/{id}`
**Then** the API returns `400 Bad Request` and the existing Todo is left unmodified.

**Given** a Todo that was concurrently modified by another request since it was loaded (its `version` has moved on)
**When** a client sends `PUT /api/v1/todos/{id}`
**Then** the API returns `409 Conflict` and the existing (already-changed) Todo is left as the other request left it.

**Given** a non-positive `id`
**When** a client sends `PUT /api/v1/todos/{id}`
**Then** the API returns `400 Bad Request` describing the invalid `id`.

### 5. Mark a Todo complete

`PATCH /api/v1/todos/{id}/complete`

**Given** an existing, incomplete Todo
**When** a client sends `PATCH /api/v1/todos/{id}/complete`
**Then** the API returns `200 OK` with `completed=true` and a refreshed `updatedAt` (later than the original).

**Given** an existing Todo that is already complete
**When** a client sends `PATCH /api/v1/todos/{id}/complete`
**Then** the API returns `200 OK` with `completed=true` unchanged (idempotent — not an error).

**Given** an `id` with no matching Todo
**When** a client sends `PATCH /api/v1/todos/{id}/complete`
**Then** the API returns `404 Not Found`.

**Given** a non-positive `id`
**When** a client sends `PATCH /api/v1/todos/{id}/complete`
**Then** the API returns `400 Bad Request` describing the invalid `id`.

### 6. Mark a Todo incomplete

`PATCH /api/v1/todos/{id}/incomplete`

**Given** an existing, completed Todo
**When** a client sends `PATCH /api/v1/todos/{id}/incomplete`
**Then** the API returns `200 OK` with `completed=false` and a refreshed `updatedAt` (later than the original).

**Given** an existing Todo that is already incomplete
**When** a client sends `PATCH /api/v1/todos/{id}/incomplete`
**Then** the API returns `200 OK` with `completed=false` unchanged (idempotent — not an error).

**Given** an `id` with no matching Todo
**When** a client sends `PATCH /api/v1/todos/{id}/incomplete`
**Then** the API returns `404 Not Found`.

**Given** a non-positive `id`
**When** a client sends `PATCH /api/v1/todos/{id}/incomplete`
**Then** the API returns `400 Bad Request` describing the invalid `id`.

### 7. Delete a Todo

`DELETE /api/v1/todos/{id}`

**Given** an existing Todo with a known `id`
**When** a client sends `DELETE /api/v1/todos/{id}`
**Then** the API returns `204 No Content`, the Todo is removed, and a subsequent `GET /api/v1/todos/{id}` returns `404`.

**Given** an `id` with no matching Todo
**When** a client sends `DELETE /api/v1/todos/{id}`
**Then** the API returns `404 Not Found` and no state changes.

**Given** a non-positive `id`
**When** a client sends `DELETE /api/v1/todos/{id}`
**Then** the API returns `400 Bad Request` describing the invalid `id`.
