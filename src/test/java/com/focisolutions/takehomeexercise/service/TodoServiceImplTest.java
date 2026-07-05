package com.focisolutions.takehomeexercise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.focisolutions.takehomeexercise.dto.TodoCreateRequest;
import com.focisolutions.takehomeexercise.dto.TodoResponse;
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
        final TodoCreateRequest request = new TodoCreateRequest("Buy milk", "2 litres", LocalDate.of(2026, 7, 10));
        final Todo saved = new Todo("Buy milk", "2 litres", LocalDate.of(2026, 7, 10));
        final TodoResponse response = new TodoResponse(1L, "Buy milk", "2 litres", LocalDate.of(2026, 7, 10), false, Instant.now());
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
        final Todo todo = new Todo("Buy milk", null, null);
        final TodoResponse response = new TodoResponse(id, "Buy milk", null, null, false, Instant.now());
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
    void givenTodosExist_whenFindAllTodos_thenReturnsMappedListTest() {
        // given
        final Todo todo = new Todo("Buy milk", null, null);
        final TodoResponse response = new TodoResponse(1L, "Buy milk", null, null, false, Instant.now());
        given(todoRepository.findAll()).willReturn(List.of(todo));
        given(todoMapper.toResponse(todo)).willReturn(response);

        // when
        final List<TodoResponse> result = todoServiceImpl.findAllTodos();

        // then
        assertThat(result).containsExactly(response);
        then(todoRepository).should().findAll();
    }
}
