package com.focisolutions.takehomeexercise.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.focisolutions.takehomeexercise.entity.Todo;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class TodoRepositoryTest {

    @Autowired
    private TodoRepository todoRepository;

    @Test
    void givenNewTodo_whenSaved_thenIdIsGeneratedAndCreatedAtIsPopulatedTest() {
        // given
        final Todo todo = Todo.builder().title("Buy milk").description("2 litres").dueDate(LocalDate.of(2026, 7, 10)).build();

        // when
        final Todo saved = todoRepository.saveAndFlush(todo);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void givenNewTodo_whenSaved_thenUpdatedAtEqualsCreatedAtTest() {
        // given
        final Todo todo = Todo.builder().title("Buy milk").build();

        // when
        final Todo saved = todoRepository.saveAndFlush(todo);

        // then
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void givenSavedTodo_whenFieldsAreUpdatedAndFlushed_thenUpdatedAtChangesButCreatedAtStaysTheSameTest() {
        // given
        final Todo saved = todoRepository.saveAndFlush(Todo.builder().title("Buy milk").build());
        final Instant originalCreatedAt = saved.getCreatedAt();
        final Instant originalUpdatedAt = saved.getUpdatedAt();

        // when
        saved.updateDetails("Buy oat milk", null, null);
        final Todo updated = todoRepository.saveAndFlush(saved);

        // then
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void givenNewTodo_whenSaved_thenIsCompletedDefaultsToFalseTest() {
        // given
        final Todo todo = Todo.builder().title("Buy milk").build();

        // when
        final Todo saved = todoRepository.saveAndFlush(todo);

        // then
        assertThat(saved.isCompleted()).isFalse();
    }

    @Test
    void givenSavedTodo_whenFindById_thenReturnsMatchingTodoTest() {
        // given
        final Todo saved = todoRepository.saveAndFlush(Todo.builder().title("Buy milk").build());

        // when
        final Optional<Todo> found = todoRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Buy milk");
    }

    @Test
    void givenNoTodosExist_whenFindAll_thenReturnsEmptyListTest() {
        // given
        // (no todos persisted)

        // when
        final List<Todo> todos = todoRepository.findAll();

        // then
        assertThat(todos).isEmpty();
    }

    @Test
    void givenTodosWithMixedCompletionStatus_whenFindByCompletedTrue_thenReturnsOnlyCompletedTodosTest() {
        // given
        final Todo completed = Todo.builder().title("Completed").build();
        completed.markCompleted();
        todoRepository.saveAndFlush(completed);
        todoRepository.saveAndFlush(Todo.builder().title("Incomplete").build());

        // when
        final List<Todo> result = todoRepository.findByCompleted(true, Sort.unsorted());

        // then
        assertThat(result).extracting(Todo::getTitle).containsExactly("Completed");
    }

    @Test
    void givenTodosWithMixedCompletionStatus_whenFindByCompletedFalse_thenReturnsOnlyIncompleteTodosTest() {
        // given
        final Todo completed = Todo.builder().title("Completed").build();
        completed.markCompleted();
        todoRepository.saveAndFlush(completed);
        todoRepository.saveAndFlush(Todo.builder().title("Incomplete").build());

        // when
        final List<Todo> result = todoRepository.findByCompleted(false, Sort.unsorted());

        // then
        assertThat(result).extracting(Todo::getTitle).containsExactly("Incomplete");
    }

    @Test
    void givenIncompleteTodoWithPastDueDate_whenFindByCompletedFalseAndDueDateBefore_thenReturnsOnlyThatTodoTest() {
        // given
        final LocalDate today = LocalDate.now();
        todoRepository.saveAndFlush(Todo.builder().title("Overdue").dueDate(today.minusDays(1)).build());
        todoRepository.saveAndFlush(Todo.builder().title("Future").dueDate(today.plusDays(1)).build());

        // when
        final List<Todo> result = todoRepository.findByCompletedFalseAndDueDateBefore(today, Sort.unsorted());

        // then
        assertThat(result).extracting(Todo::getTitle).containsExactly("Overdue");
    }

    @Test
    void givenCompletedTodoWithPastDueDate_whenFindByCompletedFalseAndDueDateBefore_thenExcludesItTest() {
        // given
        final LocalDate today = LocalDate.now();
        final Todo completedOverdue = Todo.builder().title("Completed overdue").dueDate(today.minusDays(1)).build();
        completedOverdue.markCompleted();
        todoRepository.saveAndFlush(completedOverdue);

        // when
        final List<Todo> result = todoRepository.findByCompletedFalseAndDueDateBefore(today, Sort.unsorted());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void givenTodoWithNullDueDate_whenFindByCompletedFalseAndDueDateBefore_thenExcludesItTest() {
        // given
        todoRepository.saveAndFlush(Todo.builder().title("No due date").build());

        // when
        final List<Todo> result = todoRepository.findByCompletedFalseAndDueDateBefore(LocalDate.now(), Sort.unsorted());

        // then
        assertThat(result).isEmpty();
    }
}
