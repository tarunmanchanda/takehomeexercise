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
./mvnw clean verify                  # full build + tests + JaCoCo coverage gate (90% line minimum)
```

Coverage report after `verify`: `target/site/jacoco/index.html`. The gate is bound to `verify`, not `test`, so a plain `./mvnw test` stays fast and always runs regardless of coverage.

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
- **`@Validated` on a controller class throws `jakarta.validation.ConstraintViolationException`, not `HandlerMethodValidationException`**, for constraint failures on `@PathVariable`/`@RequestParam` (e.g. `@Positive Long id`). Spring Framework 6.1+ added a newer web-native validation path that throws `HandlerMethodValidationException`, but that path only applies when the controller bean is *not* AOP-proxied — adding class-level `@Validated` triggers Spring's classic `MethodValidationPostProcessor`/`MethodValidationInterceptor` AOP proxy (visible as a CGLIB proxy in stack traces), which takes over and throws the older exception type instead. Confirmed empirically via `@WebMvcTest` (jar inspection alone said `HandlerMethodValidationException` exists in this Spring version — true, but not what actually gets thrown here). `GlobalExceptionHandler` registers handlers for both, since which one fires is proxy-configuration-dependent, not something to assume from the class simply existing. Both are converted to Spring's `ProblemDetail` (RFC 9457) — see `.claude/skills/rest-api-standards/SKILL.md` rule 11 for the full design, including a bytecode-verified gotcha about which exception types' `super.handleXxx(...)` calls actually return a populated `ProblemDetail` body vs. `null`.
- **`@WebMvcTest` does *not* auto-configure Spring Security in Boot 4.1**, even with `spring-boot-starter-security` on the classpath — contrary to Boot 3.x-era expectations and even a literal quote from the `@WebMvcTest` javadoc claiming otherwise (the javadoc appears stale/inherited from an older major version's doc comment; verify actual behavior, not doc comments, when they might predate a module split). Confirmed by decompiling `spring-boot-webmvc-test-4.1.0.jar`'s `META-INF/spring/*.imports` files: no security auto-configuration class is imported for `@WebMvcTest`/`@AutoConfigureMockMvc`. A separate `spring-boot-starter-security-test` artifact exists in the Boot 4.1 BOM (matching this project's established "everything got split into its own module" pattern) and would be needed to get that integration — this project deliberately does **not** add it, since the full-context `@SpringBootTest` integration tests already give real, end-to-end security enforcement coverage (see `SecurityIntegrationTest`), making `TodoControllerTest`/`TestControllerTest` correctly security-agnostic slice tests. Confirmed empirically: `TodoControllerTest`'s 28 tests pass unmodified with the security starter on the classpath.

### Lombok + MapStruct incremental-compilation bug (fixed, but know the symptom)

Maven's default incremental compilation could pass only *changed* files to `javac`, leaving an annotation-processing round without a consistent view of the whole source set. Symptom: `TodoMapperImpl` (generated) silently falls back to declaring all-null locals and building a fully-null `TodoResponse` via the raw constructor, instead of calling `entity.getX()` through the builder — no compile error, no exception, just wrong data at runtime (e.g. `POST /todos` returns `{"id":null,...}` and `Location: /todos/null`). Reproduced reliably via `./mvnw test -Dtest=<SomeTest>` (not `clean`) after touching `Todo.java` or `TodoMapper.java`. Fixed by setting `<useIncrementalCompilation>false</useIncrementalCompilation>` on `maven-compiler-plugin` in `pom.xml` — forces a full recompile of all sources together every time, so Lombok's generated getters are always visible to MapStruct's processor in the same round. If a generated `*Impl.java` mapper ever looks suspiciously empty/null-only again, check this setting is still in place before assuming the mapping annotations are wrong.

## Testing

Test naming/structure (`given_when_then` method names + comments), BDDMockito, and AssertJ conventions are enforced by `.claude/skills/testing-conventions/SKILL.md` — read that when writing or reviewing any test in this repo. REST-specific standards, including the Richardson Maturity Model target and the OpenAPI/Swagger documentation requirement (every endpoint/DTO annotated, Swagger UI kept reachable), live in `.claude/skills/rest-api-standards/SKILL.md`.

### springdoc-openapi + Jackson 3 (fragile, watch this)

`springdoc-openapi-starter-webmvc-ui:3.0.3` (the version needed for Spring Boot 4.1/Framework 7) pulls `swagger-core-jakarta`, which depends on Jackson 2 (`com.fasterxml.jackson.databind`) — while this app runs Jackson 3 (`tools.jackson.*`) via Boot 4.1's own Jackson auto-config. This was verified working at the time it was added (full test suite green, live `/v3/api-docs` and `/swagger-ui/index.html` both correct, existing endpoints unaffected), but there are open, unresolved upstream issues (springdoc-openapi #3200/#3157, swagger-core #4991/#5031/#5225) describing real Jackson-2-vs-3 conflicts on this exact combination with no confirmed fix. If springdoc/Jackson-related errors appear after a dependency bump, this is the first place to look.

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