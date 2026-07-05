package com.focisolutions.takehomeexercise.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.focisolutions.takehomeexercise.entity.Todo;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

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
}
