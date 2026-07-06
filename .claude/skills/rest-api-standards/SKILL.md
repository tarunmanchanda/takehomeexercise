---
name: rest-api-standards
description: Enforce this project's Java/Spring Boot REST API coding standards — naming, DI, immutability, layering, DTOs, validation, exceptions, logging, config. Use whenever creating or modifying a controller, service, repository, DTO, entity, exception, or config class in this repo.
---

Apply every rule below to any Java code touched in this repo. Do not ask which to apply — apply all of them.

## Naming

1. Package naming: all lowercase, reverse domain (`com.company.orderservice.controller`), no underscores or camelCase.
2. Class naming: PascalCase, nouns (`OrderService`, not `ProcessOrder`). Suffix by role: `*Controller`, `*Service`, `*ServiceImpl`, `*Repository`, `*Dto`, `*Entity`, `*Exception`, `*Config`.
3. Method naming: camelCase, verbs (`findOrderById`, `calculateTotal`). Boolean methods prefixed `is`/`has`/`can` (`isValid`, `hasPermission`).
4. Constants: static final fields in `UPPER_SNAKE_CASE`. No magic numbers or strings — extract to named constants.

## Immutability & encapsulation

5. `final` everywhere applicable: method parameters, local variables that don't reassign, and classes/fields not meant for extension or mutation. Fields injected via constructor are always `private final`.
7. DTOs and value objects are Java `record` types (Java 17+). Entities mutable only where JPA requires it.
8. No public non-final fields ever. Encapsulate with `private` + accessors only when actually needed — no blind getter/setter generation for every field.
9. `Optional` for return types, never for fields or parameters. Never call `.get()` without an `.isPresent()`/`.orElseThrow()` check.
19. Package-private over public as the default visibility — only widen to public when a class/method is genuinely used outside its package.

## Dependency injection

6. Constructor injection only — never field injection with `@Autowired` on fields. All dependencies `private final`, injected via constructor (Lombok `@RequiredArgsConstructor` allowed).

## Layering

12. Controller → Service → Repository, strictly. Controllers never contain business logic. Services never depend on `HttpServletRequest`/`ResponseEntity`.
13. DTOs at the boundary, entities never leave the service layer — controllers accept/return DTOs only, mapping via MapStruct or explicit mapper methods.

## Richardson Maturity Model (RMM) compliance

- **Level 0** — a single URI, single HTTP verb (usually POST), the "swamp of POX." Not acceptable here.
- **Level 1** — multiple resource-based URIs, but still one verb per action, ignoring HTTP semantics. Not acceptable here.
- **Level 2** — distinct resource URIs, correct HTTP verbs (`GET`/`POST`/`PUT`/`PATCH`/`DELETE`) used per their semantics, correct HTTP status codes per outcome (`200`/`201`/`204`/`400`/`404`, etc.).
- **Level 3** — everything in Level 2, plus hypermedia controls (HATEOAS): responses embed links describing valid next actions/transitions from the current resource state.

21. **This API targets Level 2.** Every endpoint must use a resource-oriented noun URI (`/todos`, `/todos/{id}`), the HTTP verb matching its semantics, and the correct status code for every outcome (see the endpoint table in `FEATURE.md`). Level 3/HATEOAS is a deliberate, documented extension point — not implemented, given the scope of this exercise — not an oversight. If extended later, use Spring HATEOAS (`spring-boot-starter-hateoas`, confirmed compatible with this project's Spring Boot version) via `EntityModel`/`CollectionModel` + `RepresentationModelAssembler`, and attach only links for currently-valid transitions (e.g. a completed resource should expose an "incomplete" link, not "complete").

## Validation & errors

14. Bean validation (`@NotNull`, `@Size`, `@Valid`) on all incoming DTOs at the controller boundary — never manual null-checks for input validation.
10. Custom exceptions extend `RuntimeException`, named `*Exception`, carry meaningful context (not just a string message — include the failing ID/entity). Never catch generic `Exception` or swallow exceptions silently.
11. Global exception handling via `@ControllerAdvice` + `@ExceptionHandler` — controllers never contain try/catch for business errors. Error responses are Spring's built-in `ProblemDetail` (RFC 9457), never a hand-rolled error DTO. `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` and overrides its protected `handleXxx` methods for framework-native exceptions (`MethodArgumentNotValidException`, `HandlerMethodValidationException`, `handleTypeMismatch` for `TypeMismatchException`/`MethodArgumentTypeMismatchException`), enriching the response with an `errors` extension property (`problemDetail.setProperty("errors", List<String>)`) for field/violation-level messages and `instance` (the request path). Exceptions not covered by the base class (custom exceptions, `ConstraintViolationException`) get their own explicit `@ExceptionHandler` methods building `ProblemDetail` directly via `ProblemDetail.forStatusAndDetail(status, detail)`. Before assuming a base-class `handleXxx` method's default response body is a populated `ProblemDetail`, check whether that exception type implements `org.springframework.web.ErrorResponse` (directly or transitively, e.g. via `ResponseStatusException`/`ErrorResponseException`) — if it doesn't (like `TypeMismatchException`), the default body is `null` and calling/enriching `super.handleXxx(...)` throws a `NullPointerException`; build the `ProblemDetail` yourself instead and return via `handleExceptionInternal(ex, problemDetail, headers, status, request)`.

## Logging & code hygiene

15. No `System.out.println` — use SLF4J (`private static final Logger log = LoggerFactory.getLogger(ClassName.class)`), parameterized logging (`log.info("Order {} created", id)`), never string concatenation.
16. Streams over loops where it improves readability, but no deeply nested/chained streams (max ~3 operations) — extract to named methods if it gets complex.
17. No wildcard imports (`import java.util.*` banned). No unused imports.
18. Javadoc required on all public classes and public methods in service/API-facing layers — one-line summary minimum, `@param`/`@return`/`@throws` when non-obvious.

## API documentation (OpenAPI/Swagger)

22. Every controller endpoint must be documented with OpenAPI annotations: `@Tag` at the class level, `@Operation(summary = ...)` per method, and `@ApiResponse` for every status code the endpoint can actually return (mirror the outcomes already documented in `FEATURE.md`) — error responses reference `@Schema(implementation = ProblemDetail.class)` (see rule 11), never a custom error DTO. Path variables and query params get `@Parameter(description = ...)`. Every request/response DTO (and enum used as a query param) gets `@Schema` with a `description`, plus `example` values on record components where a realistic example adds clarity. Swagger UI (`/swagger-ui/index.html`) and the raw OpenAPI spec (`/v3/api-docs`) must stay reachable so the API is self-documenting and testable without external tooling. These annotations are additive documentation on top of Bean Validation (`@NotBlank`, `@Size`, `@Positive`, etc.) — they don't replace it; springdoc automatically reflects Bean Validation constraints (required, `maxLength`, etc.) into the generated schema with no extra annotation needed for that part. Note: `ProblemDetail`'s extension-property map reflects as a generic `properties: object` in the generated schema — springdoc can't know about the specific `errors` key by name from `ProblemDetail.class` alone; this is an accepted tradeoff of RFC 9457's extensible model, not a bug to fix. A class that's package-private by default (rule 19) may be widened to `public` specifically to be referenced from `@Schema(implementation = ...)` across packages — that's a genuine cross-package use, not an exception to the rule.

## Configuration

20. Configuration via `@ConfigurationProperties` with typed, immutable config classes (records) — never inject raw values with scattered `@Value("${...}")` across the codebase.