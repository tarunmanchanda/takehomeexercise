package com.focisolutions.takehomeexercise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.focisolutions.takehomeexercise.dto.TodoCreateRequest;
import com.focisolutions.takehomeexercise.dto.TodoFilter;
import com.focisolutions.takehomeexercise.dto.TodoResponse;
import com.focisolutions.takehomeexercise.dto.TodoSortBy;
import com.focisolutions.takehomeexercise.dto.TodoUpdateRequest;
import com.focisolutions.takehomeexercise.entity.Todo;
import com.focisolutions.takehomeexercise.exception.TodoNotFoundException;
import com.focisolutions.takehomeexercise.mapper.TodoMapper;
import com.focisolutions.takehomeexercise.repository.TodoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TodoServiceImplTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private TodoMapper todoMapper;

    @InjectMocks
    private TodoServiceImpl todoServiceImpl;

    @Test
    void givenValidCreateRequest_whenCreateTodo_thenSavesAndReturnsMappedResponseTest() {
        // given
        final TodoCreateRequest request = TodoCreateRequest.builder()
                .title("Buy milk").description("2 litres").dueDate(LocalDate.of(2026, 7, 10)).build();
        final Todo saved = Todo.builder().title("Buy milk").description("2 litres").dueDate(LocalDate.of(2026, 7, 10)).build();
        final TodoResponse response = TodoResponse.builder()
                .id(1L).title("Buy milk").description("2 litres").dueDate(LocalDate.of(2026, 7, 10))
                .completed(false).createdAt(Instant.now()).build();
        given(todoRepository.save(any(Todo.class))).willReturn(saved);
        given(todoMapper.toResponse(saved)).willReturn(response);

        // when
        final TodoResponse result = todoServiceImpl.createTodo(request);

        // then
        assertThat(result).isEqualTo(response);
        then(todoRepository).should().save(any(Todo.class));
    }

    @Test
    void givenExistingId_whenFindTodoById_thenReturnsMappedResponseTest() {
        // given
        final Long id = 1L;
        final Todo todo = Todo.builder().title("Buy milk").build();
        final TodoResponse response = TodoResponse.builder().id(id).title("Buy milk").completed(false).createdAt(Instant.now()).build();
        given(todoRepository.findById(id)).willReturn(Optional.of(todo));
        given(todoMapper.toResponse(todo)).willReturn(response);

        // when
        final TodoResponse result = todoServiceImpl.findTodoById(id);

        // then
        assertThat(result).isEqualTo(response);
        then(todoRepository).should().findById(id);
    }

    @Test
    void givenNonExistingId_whenFindTodoById_thenThrowsTodoNotFoundExceptionTest() {
        // given
        final Long id = 404L;
        given(todoRepository.findById(id)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> todoServiceImpl.findTodoById(id)).isInstanceOf(TodoNotFoundException.class);
        then(todoRepository).should().findById(id);
    }

    @Test
    void givenFilterAll_whenFindAllTodos_thenDelegatesToRepositoryFindAllWithSortTest() {
        // given
        final Todo todo = Todo.builder().title("Buy milk").build();
        final TodoResponse response = TodoResponse.builder().id(1L).title("Buy milk").completed(false).createdAt(Instant.now()).build();
        final Sort expectedSort = Sort.by(Sort.Direction.ASC, "createdAt");
        given(todoRepository.findAll(expectedSort)).willReturn(List.of(todo));
        given(todoMapper.toResponse(todo)).willReturn(response);

        // when
        final List<TodoResponse> result = todoServiceImpl.findAllTodos(TodoFilter.ALL, TodoSortBy.CREATED_AT, Sort.Direction.ASC);

        // then
        assertThat(result).containsExactly(response);
        then(todoRepository).should().findAll(expectedSort);
    }

    @Test
    void givenFilterCompleted_whenFindAllTodos_thenDelegatesToRepositoryFindByCompletedTrueTest() {
        // given
        final Todo todo = Todo.builder().title("Buy milk").build();
        final TodoResponse response = TodoResponse.builder().id(1L).title("Buy milk").completed(true).createdAt(Instant.now()).build();
        final Sort expectedSort = Sort.by(Sort.Direction.DESC, "title");
        given(todoRepository.findByCompleted(true, expectedSort)).willReturn(List.of(todo));
        given(todoMapper.toResponse(todo)).willReturn(response);

        // when
        final List<TodoResponse> result = todoServiceImpl.findAllTodos(TodoFilter.COMPLETED, TodoSortBy.TITLE, Sort.Direction.DESC);

        // then
        assertThat(result).containsExactly(response);
        then(todoRepository).should().findByCompleted(true, expectedSort);
    }

    @Test
    void givenFilterIncomplete_whenFindAllTodos_thenDelegatesToRepositoryFindByCompletedFalseTest() {
        // given
        final Todo todo = Todo.builder().title("Buy milk").build();
        final TodoResponse response = TodoResponse.builder().id(1L).title("Buy milk").completed(false).createdAt(Instant.now()).build();
        final Sort expectedSort = Sort.by(Sort.Direction.ASC, "dueDate");
        given(todoRepository.findByCompleted(false, expectedSort)).willReturn(List.of(todo));
        given(todoMapper.toResponse(todo)).willReturn(response);

        // when
        final List<TodoResponse> result = todoServiceImpl.findAllTodos(TodoFilter.INCOMPLETE, TodoSortBy.DUE_DATE, Sort.Direction.ASC);

        // then
        assertThat(result).containsExactly(response);
        then(todoRepository).should().findByCompleted(false, expectedSort);
    }

    @Test
    void givenFilterOverdue_whenFindAllTodos_thenDelegatesToRepositoryFindByCompletedFalseAndDueDateBeforeTodayTest() {
        // given
        final Todo todo = Todo.builder().title("Buy milk").build();
        final TodoResponse response = TodoResponse.builder().id(1L).title("Buy milk").completed(false).createdAt(Instant.now()).build();
        final Sort expectedSort = Sort.by(Sort.Direction.ASC, "createdAt");
        given(todoRepository.findByCompletedFalseAndDueDateBefore(any(LocalDate.class), eq(expectedSort)))
                .willReturn(List.of(todo));
        given(todoMapper.toResponse(todo)).willReturn(response);

        // when
        final List<TodoResponse> result = todoServiceImpl.findAllTodos(TodoFilter.OVERDUE, TodoSortBy.CREATED_AT, Sort.Direction.ASC);

        // then
        assertThat(result).containsExactly(response);
        then(todoRepository).should().findByCompletedFalseAndDueDateBefore(any(LocalDate.class), eq(expectedSort));
    }

    @Test
    void givenExistingIdAndValidUpdateRequest_whenUpdateTodo_thenUpdatesFieldsAndReturnsMappedResponseTest() {
        // given
        final Long id = 1L;
        final Todo todo = Todo.builder().title("Buy milk").build();
        final TodoUpdateRequest request = TodoUpdateRequest.builder()
                .title("Buy oat milk").description("2 litres").dueDate(LocalDate.of(2026, 7, 10)).build();
        final TodoResponse response = TodoResponse.builder()
                .id(id).title("Buy oat milk").description("2 litres").dueDate(LocalDate.of(2026, 7, 10))
                .completed(false).createdAt(Instant.now()).build();
        given(todoRepository.findById(id)).willReturn(Optional.of(todo));
        given(todoRepository.save(todo)).willReturn(todo);
        given(todoMapper.toResponse(todo)).willReturn(response);

        // when
        final TodoResponse result = todoServiceImpl.updateTodo(id, request);

        // then
        assertThat(result).isEqualTo(response);
        assertThat(todo.getTitle()).isEqualTo("Buy oat milk");
        then(todoRepository).should().save(todo);
    }

    @Test
    void givenNonExistingId_whenUpdateTodo_thenThrowsTodoNotFoundExceptionTest() {
        // given
        final Long id = 404L;
        final TodoUpdateRequest request = TodoUpdateRequest.builder().title("Buy oat milk").build();
        given(todoRepository.findById(id)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> todoServiceImpl.updateTodo(id, request)).isInstanceOf(TodoNotFoundException.class);
        then(todoRepository).should(never()).save(any(Todo.class));
    }

    @Test
    void givenIncompleteTodo_whenMarkTodoCompleted_thenSetsCompletedTrueTest() {
        // given
        final Long id = 1L;
        final Todo todo = Todo.builder().title("Buy milk").build();
        final TodoResponse response = TodoResponse.builder().id(id).title("Buy milk").completed(true).createdAt(Instant.now()).build();
        given(todoRepository.findById(id)).willReturn(Optional.of(todo));
        given(todoRepository.save(todo)).willReturn(todo);
        given(todoMapper.toResponse(todo)).willReturn(response);

        // when
        final TodoResponse result = todoServiceImpl.markCompleted(id);

        // then
        assertThat(result.completed()).isTrue();
        assertThat(todo.isCompleted()).isTrue();
        then(todoRepository).should().save(todo);
    }

    @Test
    void givenAlreadyCompletedTodo_whenMarkTodoCompleted_thenRemainsCompletedTest() {
        // given
        final Long id = 1L;
        final Todo todo = Todo.builder().title("Buy milk").build();
        todo.markCompleted();
        final TodoResponse response = TodoResponse.builder().id(id).title("Buy milk").completed(true).createdAt(Instant.now()).build();
        given(todoRepository.findById(id)).willReturn(Optional.of(todo));
        given(todoRepository.save(todo)).willReturn(todo);
        given(todoMapper.toResponse(todo)).willReturn(response);

        // when
        final TodoResponse result = todoServiceImpl.markCompleted(id);

        // then
        assertThat(result.completed()).isTrue();
        then(todoRepository).should().save(todo);
    }

    @Test
    void givenNonExistingId_whenMarkTodoCompleted_thenThrowsTodoNotFoundExceptionTest() {
        // given
        final Long id = 404L;
        given(todoRepository.findById(id)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> todoServiceImpl.markCompleted(id)).isInstanceOf(TodoNotFoundException.class);
        then(todoRepository).should(never()).save(any(Todo.class));
    }

    @Test
    void givenCompletedTodo_whenMarkTodoIncomplete_thenSetsCompletedFalseTest() {
        // given
        final Long id = 1L;
        final Todo todo = Todo.builder().title("Buy milk").build();
        todo.markCompleted();
        final TodoResponse response = TodoResponse.builder().id(id).title("Buy milk").completed(false).createdAt(Instant.now()).build();
        given(todoRepository.findById(id)).willReturn(Optional.of(todo));
        given(todoRepository.save(todo)).willReturn(todo);
        given(todoMapper.toResponse(todo)).willReturn(response);

        // when
        final TodoResponse result = todoServiceImpl.markIncomplete(id);

        // then
        assertThat(result.completed()).isFalse();
        assertThat(todo.isCompleted()).isFalse();
        then(todoRepository).should().save(todo);
    }

    @Test
    void givenAlreadyIncompleteTodo_whenMarkTodoIncomplete_thenRemainsIncompleteTest() {
        // given
        final Long id = 1L;
        final Todo todo = Todo.builder().title("Buy milk").build();
        final TodoResponse response = TodoResponse.builder().id(id).title("Buy milk").completed(false).createdAt(Instant.now()).build();
        given(todoRepository.findById(id)).willReturn(Optional.of(todo));
        given(todoRepository.save(todo)).willReturn(todo);
        given(todoMapper.toResponse(todo)).willReturn(response);

        // when
        final TodoResponse result = todoServiceImpl.markIncomplete(id);

        // then
        assertThat(result.completed()).isFalse();
        then(todoRepository).should().save(todo);
    }

    @Test
    void givenNonExistingId_whenMarkTodoIncomplete_thenThrowsTodoNotFoundExceptionTest() {
        // given
        final Long id = 404L;
        given(todoRepository.findById(id)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> todoServiceImpl.markIncomplete(id)).isInstanceOf(TodoNotFoundException.class);
        then(todoRepository).should(never()).save(any(Todo.class));
    }

    @Test
    void givenId_whenDeleteEndpointIsCalled_thenDeleteTheTodoAttachedToThatIdTest() {
        // given
        final Long id = 1L;
        given(todoRepository.existsById(id)).willReturn(true);

        // when
        todoServiceImpl.deleteTodo(id);

        // then
        then(todoRepository).should().deleteById(id);
    }

    @Test
    void givenNonExistingId_whenDeleteTodo_thenThrowsTodoNotFoundExceptionTest() {
        // given
        final Long id = 404L;
        given(todoRepository.existsById(id)).willReturn(false);

        // when
        // then
        assertThatThrownBy(() -> todoServiceImpl.deleteTodo(id)).isInstanceOf(TodoNotFoundException.class);
        then(todoRepository).should(never()).deleteById(any());
    }
}
