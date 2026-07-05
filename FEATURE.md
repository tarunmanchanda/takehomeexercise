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

## Non-functional requirements

- **RMM Level 2**: correct resource-oriented URIs, correct HTTP verbs, correct HTTP status codes for every outcome. (Level 3 hypermedia/HATEOAS is documented as a deliberate extension point, not implemented — see `.claude/skills/rest-api-standards/SKILL.md`.)
- **Persistence**: file-based H2 (`jdbc:h2:file:...`) — data survives application restarts, satisfying the requirement literally. Repository/service layering (Spring Data JPA) means swapping to a production database later is a datasource/dependency config change only, with zero changes to service or controller code.
- **Validation**: enforced via Bean Validation (`@Valid`/`@NotBlank`) on request DTOs at the controller boundary.
- **Error handling**: centralized via `@ControllerAdvice` — controllers never contain try/catch for business errors.

## Capabilities

### 1. Create a Todo

`POST /todos`

**Given** a valid request body with at least a non-blank `title`
**When** a client POSTs to `/todos`
**Then** the API returns `201 Created`, a `Location` header pointing at the new resource (`/todos/{id}`), and a body containing the created Todo with `completed=false`, a server-generated `id` and `createdAt`, and the submitted `title`/`description`/`dueDate`.

**Given** a request body with a blank or missing `title`
**When** a client POSTs to `/todos`
**Then** the API returns `400 Bad Request` with a validation error describing the offending field, and no Todo is persisted.

### 2. List all Todos

`GET /todos`

**Given** any number of existing Todos (including zero)
**When** a client sends `GET /todos`
**Then** the API returns `200 OK` with a JSON array of all Todos (an empty array if none exist), each with essential details (`id`, `title`, `dueDate`, `completed`).

### 3. View a Todo by id

`GET /todos/{id}`

**Given** an existing Todo with a known `id`
**When** a client sends `GET /todos/{id}`
**Then** the API returns `200 OK` with the full Todo representation.

**Given** an `id` with no matching Todo
**When** a client sends `GET /todos/{id}`
**Then** the API returns `404 Not Found`.

### 4. Update a Todo

`PUT /todos/{id}`

**Given** an existing Todo and a valid request body (non-blank `title`, optional `description`/`dueDate`)
**When** a client sends `PUT /todos/{id}`
**Then** the API returns `200 OK` with the updated `title`/`description`/`dueDate`, leaving `completed`, `createdAt`, and `id` unchanged.

**Given** an `id` with no matching Todo
**When** a client sends `PUT /todos/{id}`
**Then** the API returns `404 Not Found` and no Todo is modified.

**Given** an existing Todo and a request body with a blank `title`
**When** a client sends `PUT /todos/{id}`
**Then** the API returns `400 Bad Request` and the existing Todo is left unmodified.

### 5. Mark a Todo complete

`PATCH /todos/{id}/complete`

**Given** an existing, incomplete Todo
**When** a client sends `PATCH /todos/{id}/complete`
**Then** the API returns `200 OK` with `completed=true`.

**Given** an existing Todo that is already complete
**When** a client sends `PATCH /todos/{id}/complete`
**Then** the API returns `200 OK` with `completed=true` unchanged (idempotent — not an error).

**Given** an `id` with no matching Todo
**When** a client sends `PATCH /todos/{id}/complete`
**Then** the API returns `404 Not Found`.

### 6. Mark a Todo incomplete

`PATCH /todos/{id}/incomplete`

**Given** an existing, completed Todo
**When** a client sends `PATCH /todos/{id}/incomplete`
**Then** the API returns `200 OK` with `completed=false`.

**Given** an existing Todo that is already incomplete
**When** a client sends `PATCH /todos/{id}/incomplete`
**Then** the API returns `200 OK` with `completed=false` unchanged (idempotent — not an error).

**Given** an `id` with no matching Todo
**When** a client sends `PATCH /todos/{id}/incomplete`
**Then** the API returns `404 Not Found`.

### 7. Delete a Todo

`DELETE /todos/{id}`

**Given** an existing Todo with a known `id`
**When** a client sends `DELETE /todos/{id}`
**Then** the API returns `204 No Content`, the Todo is removed, and a subsequent `GET /todos/{id}` returns `404`.

**Given** an `id` with no matching Todo
**When** a client sends `DELETE /todos/{id}`
**Then** the API returns `404 Not Found` and no state changes.
