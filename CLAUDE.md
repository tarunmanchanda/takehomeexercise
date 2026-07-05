# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build/run with the Maven wrapper (no local Maven install required):

```bash
./mvnw test                          # run all tests
./mvnw test -Dtest=TestControllerTest              # run a single test class
./mvnw test -Dtest=TestControllerTest#ping_returnsOkWithUpMessage  # run a single test method
./mvnw -DskipTests package           # build the jar without running tests
./mvnw spring-boot:run               # run the app locally (port 8080)
```

## Architecture

Minimal Spring Boot 4.1 / Java 17 REST service, group `com.focisolutions`, base package `com.focisolutions.takehomeexercise`.

- `TakeHomeExerciseApplication` — `@SpringBootApplication` entry point.
- `controller/` — `@RestController` classes. `TestController` exposes `GET /api/test/ping` as a lightweight liveness check returning a plain string.
- `application.yaml` — Actuator is enabled with only the `health` endpoint exposed at `/actuator/health` (`management.endpoints.web.exposure.include: health`), with `show-details: always`. Don't broaden `exposure.include` (e.g. to `*`) without confirming — it would expose internal actuator endpoints (env, beans, etc.) publicly since no security dependency is configured.

### Spring Boot 4.1 module split (important, non-obvious)

This project pins `spring-boot-starter-parent` to `4.1.0`, which restructured starters and moved test-annotation packages compared to Spring Boot 3.x / older training data. When adding dependencies or imports, verify against `./mvnw dependency:tree` rather than assuming Boot 3-era names:

- Web starter is `spring-boot-starter-webmvc`, not `spring-boot-starter-web`.
- Test starter is `spring-boot-starter-webmvc-test`, not `spring-boot-starter-test`.
- `@WebMvcTest` lives in `org.springframework.boot.webmvc.test.autoconfigure`, not `org.springframework.boot.test.autoconfigure.web.servlet`.
- `@DataJpaTest` requires its own `spring-boot-starter-data-jpa-test` dependency (test scope) and lives in `org.springframework.boot.data.jpa.test.autoconfigure`, not bundled with `spring-boot-starter-webmvc-test` and not at the Boot 3-era `org.springframework.boot.test.autoconfigure.orm.jpa` path.
- **Jackson is Jackson 3, not 2** (`spring-boot-starter-jackson` pulls `tools.jackson.core:jackson-databind:3.x`, not `com.fasterxml.jackson.core:jackson-databind:2.x`). Core classes (`ObjectMapper`, `JacksonException`, etc.) moved package from `com.fasterxml.jackson.*` to `tools.jackson.*` — only `jackson-annotations` (`@JsonProperty` etc.) stayed at the old `com.fasterxml.jackson.annotation` package for compatibility. Also, Jackson 3's `JacksonException` is unchecked (extends `RuntimeException`), so `objectMapper.writeValueAsString(...)` no longer requires a `throws` declaration. This is the single most surprising Boot 4.1 change found so far — double-check any Jackson import.
- IDE dependency-resolution warnings for `4.1.0` artifacts can be stale/wrong — confirm with `./mvnw dependency:tree` before trusting them.

## Testing

Test naming/structure (`given_when_then` method names + comments), BDDMockito, and AssertJ conventions are enforced by `.claude/skills/testing-conventions/SKILL.md` — read that when writing or reviewing any test in this repo. REST-specific standards, including the Richardson Maturity Model target, live in `.claude/skills/rest-api-standards/SKILL.md`.

## Java coding standards

Mirrored from `~/.claude/CLAUDE.md` (global) — update both if these change.

1. Package naming: all lowercase, reverse domain (`com.company.orderservice.controller`), no underscores or camelCase.
2. Class naming: PascalCase, nouns (`OrderService`, not `ProcessOrder`). Suffix by role: `*Controller`, `*Service`, `*ServiceImpl`, `*Repository`, `*Dto`, `*Entity`, `*Exception`, `*Config`.
3. Method naming: camelCase, verbs (`findOrderById`, `calculateTotal`). Boolean methods prefixed `is`/`has`/`can` (`isValid`, `hasPermission`).
4. Constants: static final fields in `UPPER_SNAKE_CASE`. No magic numbers or strings — extract to named constants.
5. `final` everywhere applicable: method parameters, local variables that don't reassign, and classes/fields not meant for extension or mutation. Fields injected via constructor are always `private final`.
6. Constructor injection only — never field injection with `@Autowired` on fields. All dependencies `private final`, injected via constructor (Lombok `@RequiredArgsConstructor` allowed).
7. Immutability by default: DTOs and value objects as Java `record` types (Java 17+). Entities mutable only where JPA requires it.
8. No public non-final fields ever. Encapsulate with `private` + accessors only when actually needed — no blind getter/setter generation for every field.
9. `Optional` for return types, never for fields or parameters. Never call `.get()` without an `.isPresent()`/`.orElseThrow()` check.
10. Custom exceptions extend `RuntimeException`, named `*Exception`, carry meaningful context (not just a string message — include the failing ID/entity). Never catch generic `Exception` or swallow exceptions silently.
11. Global exception handling via `@ControllerAdvice` + `@ExceptionHandler` — controllers never contain try/catch for business errors.
12. Layered architecture strictly enforced: Controller → Service → Repository. Controllers never contain business logic. Services never depend on `HttpServletRequest`/`ResponseEntity`.
13. DTOs at the boundary, entities never leave the service layer — controllers accept/return DTOs only, mapping done via MapStruct or explicit mapper methods.
14. Bean validation (`@NotNull`, `@Size`, `@Valid`) on all incoming DTOs at the controller boundary — never manual null-checks for input validation.
15. No `System.out.println` — use SLF4J (`private static final Logger log = LoggerFactory.getLogger(ClassName.class)`), parameterized logging (`log.info("Order {} created", id)`), never string concatenation.
16. Streams over loops where it improves readability, but no deeply nested/chained streams (max ~3 operations) — extract to named methods if it gets complex.
17. No wildcard imports (`import java.util.*` banned). No unused imports.
18. Javadoc required on all public classes and public methods in service/API-facing layers — one-line summary minimum, `@param`/`@return`/`@throws` when non-obvious.
19. Package-private over public as the default visibility — only widen to public when a class/method is genuinely used outside its package.
20. Configuration via `@ConfigurationProperties` with typed, immutable config classes (records) — never inject raw values with scattered `@Value("${...}")` across the codebase.

### Project-specific addition (not mirrored to global)

21. Use Lombok's `@Builder` uniformly for **every** constructible class and record in this codebase — entities and DTOs/records alike — instead of `new X(...)`, for one consistent construction style across the whole project. On records, `@Builder` sits directly on the record declaration and uses the canonical constructor; no extra annotations needed. On the `Todo` JPA entity, pair it with `@NoArgsConstructor(access = AccessLevel.PROTECTED)` (JPA requires a no-arg constructor) and `@AllArgsConstructor(access = AccessLevel.PRIVATE)` to back the builder. Exception classes (e.g. `TodoNotFoundException`) are exempt — a builder adds no value for a single-purpose exception payload.