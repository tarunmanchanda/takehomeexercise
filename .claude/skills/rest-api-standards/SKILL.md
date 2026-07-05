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
11. Global exception handling via `@ControllerAdvice` + `@ExceptionHandler` — controllers never contain try/catch for business errors.

## Logging & code hygiene

15. No `System.out.println` — use SLF4J (`private static final Logger log = LoggerFactory.getLogger(ClassName.class)`), parameterized logging (`log.info("Order {} created", id)`), never string concatenation.
16. Streams over loops where it improves readability, but no deeply nested/chained streams (max ~3 operations) — extract to named methods if it gets complex.
17. No wildcard imports (`import java.util.*` banned). No unused imports.
18. Javadoc required on all public classes and public methods in service/API-facing layers — one-line summary minimum, `@param`/`@return`/`@throws` when non-obvious.

## Configuration

20. Configuration via `@ConfigurationProperties` with typed, immutable config classes (records) — never inject raw values with scattered `@Value("${...}")` across the codebase.