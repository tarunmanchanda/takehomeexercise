---
name: testing-conventions
description: Enforce this project's BDD-style test naming, structure, mocking, and assertion conventions. Use whenever creating or modifying any test class (entity/repository, service, controller, or integration tests) in this repo.
---

Apply every rule below to any test file touched in this repo. Do not ask which to apply — apply all of them.

## Naming

1. Test method names follow `given<Precondition>_when<Action>_then<ExpectedOutcome>Test()` — e.g. `givenId_whenDeleteEndpointIsCalled_thenDeleteTheTodoAttachedToThatIdTest()`.
2. Every test method name must map to a specific acceptance-criteria bullet in `FEATURE.md` — if you can't point to the Given/When/Then it came from, the spec is missing that case (fix the spec, don't invent untraceable tests).

## Structure

3. Every test body has explicit `// given`, `// when`, `// then` comments delimiting the three sections, matching the method name's clauses exactly.

## Mocking

4. Use **BDDMockito** for all mocking (`given(...).willReturn(...)`), not Mockito's `when(...).thenReturn(...)` style.
5. Every mock interaction that matters to the test must be explicitly verified with `then(mock).should()...` — never rely on implicit stubbing alone as the only check.

## Assertions

6. Use **AssertJ** (`assertThat(...)`) for all assertions. Never use JUnit's built-in `assertEquals`/`assertTrue`/etc.

## Layer-specific guidance

7. Repository tests: `@DataJpaTest`, against the real (embedded test) database — verify persistence behavior (id generation, defaults, timestamps), not business logic.
8. Service tests: plain unit tests, `@ExtendWith(MockitoExtension.class)`, dependencies (repositories, mappers) mocked via BDDMockito.
9. Controller tests: `@WebMvcTest(<Controller>.class)` with the service layer mocked — verify HTTP status, response body shape, and request validation, not business logic.
10. Exactly one full lifecycle test per major feature via `@SpringBootTest` with the real database — proves the layers integrate correctly end-to-end; don't duplicate every case at this level, that's what the lower-level tests are for.

## Worked example

```java
@Test
void givenId_whenDeleteEndpointIsCalled_thenDeleteTheTodoAttachedToThatIdTest() {
    // given
    final Long id = 1L;
    given(todoRepository.existsById(id)).willReturn(true);

    // when
    todoService.deleteTodo(id);

    // then
    then(todoRepository).should().deleteById(id);
}
```
