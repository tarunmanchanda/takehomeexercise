package com.focisolutions.takehomeexercise.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.focisolutions.takehomeexercise.dto.TodoCreateRequest;
import com.focisolutions.takehomeexercise.dto.TodoResponse;
import com.focisolutions.takehomeexercise.dto.TodoUpdateRequest;
import com.focisolutions.takehomeexercise.exception.TodoNotFoundException;
import com.focisolutions.takehomeexercise.service.TodoService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TodoService todoService;

    @Test
    void givenValidCreateRequestBody_whenPostToTodos_thenReturns201WithLocationHeaderTest() throws Exception {
        // given
        final TodoCreateRequest request = new TodoCreateRequest("Buy milk", "2 litres", LocalDate.of(2026, 7, 10));
        final TodoResponse response = new TodoResponse(1L, "Buy milk", "2 litres", LocalDate.of(2026, 7, 10), false, Instant.now());
        given(todoService.createTodo(request)).willReturn(response);

        // when
        // then
        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/todos/1"))
                .andExpect(jsonPath("$.title").value("Buy milk"))
                .andExpect(jsonPath("$.completed").value(false));
        then(todoService).should().createTodo(request);
    }

    @Test
    void givenBlankTitleInCreateRequestBody_whenPostToTodos_thenReturns400Test() throws Exception {
        // given
        final TodoCreateRequest request = new TodoCreateRequest(" ", null, null);

        // when
        // then
        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        then(todoService).should(never()).createTodo(any());
    }

    @Test
    void givenExistingId_whenGetTodoById_thenReturns200Test() throws Exception {
        // given
        final TodoResponse response = new TodoResponse(1L, "Buy milk", null, null, false, Instant.now());
        given(todoService.findTodoById(1L)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Buy milk"));
        then(todoService).should().findTodoById(1L);
    }

    @Test
    void givenNonExistingId_whenGetTodoById_thenReturns404Test() throws Exception {
        // given
        given(todoService.findTodoById(eq(404L))).willThrow(new TodoNotFoundException(404L));

        // when
        // then
        mockMvc.perform(get("/todos/404")).andExpect(status().isNotFound());
        then(todoService).should().findTodoById(404L);
    }

    @Test
    void givenTodosExist_whenGetAllTodos_thenReturns200WithListTest() throws Exception {
        // given
        final TodoResponse response = new TodoResponse(1L, "Buy milk", null, null, false, Instant.now());
        given(todoService.findAllTodos()).willReturn(List.of(response));

        // when
        // then
        mockMvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        then(todoService).should().findAllTodos();
    }

    @Test
    void givenExistingIdAndValidBody_whenPutTodo_thenReturns200WithUpdatedFieldsTest() throws Exception {
        // given
        final TodoUpdateRequest request = new TodoUpdateRequest("Buy oat milk", "2 litres", LocalDate.of(2026, 7, 10));
        final TodoResponse response = new TodoResponse(1L, "Buy oat milk", "2 litres", LocalDate.of(2026, 7, 10), false, Instant.now());
        given(todoService.updateTodo(eq(1L), any(TodoUpdateRequest.class))).willReturn(response);

        // when
        // then
        mockMvc.perform(put("/todos/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy oat milk"));
        then(todoService).should().updateTodo(eq(1L), any(TodoUpdateRequest.class));
    }

    @Test
    void givenNonExistingId_whenPutTodo_thenReturns404Test() throws Exception {
        // given
        final TodoUpdateRequest request = new TodoUpdateRequest("Buy oat milk", null, null);
        given(todoService.updateTodo(eq(404L), any(TodoUpdateRequest.class))).willThrow(new TodoNotFoundException(404L));

        // when
        // then
        mockMvc.perform(put("/todos/404")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenBlankTitleInUpdateRequestBody_whenPutTodo_thenReturns400Test() throws Exception {
        // given
        final TodoUpdateRequest request = new TodoUpdateRequest(" ", null, null);

        // when
        // then
        mockMvc.perform(put("/todos/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        then(todoService).should(never()).updateTodo(any(), any());
    }

    @Test
    void givenIncompleteTodoId_whenPatchComplete_thenReturns200WithCompletedTrueTest() throws Exception {
        // given
        final TodoResponse response = new TodoResponse(1L, "Buy milk", null, null, true, Instant.now());
        given(todoService.markCompleted(1L)).willReturn(response);

        // when
        // then
        mockMvc.perform(patch("/todos/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
        then(todoService).should().markCompleted(1L);
    }

    @Test
    void givenNonExistingId_whenPatchComplete_thenReturns404Test() throws Exception {
        // given
        given(todoService.markCompleted(404L)).willThrow(new TodoNotFoundException(404L));

        // when
        // then
        mockMvc.perform(patch("/todos/404/complete")).andExpect(status().isNotFound());
    }

    @Test
    void givenCompletedTodoId_whenPatchIncomplete_thenReturns200WithCompletedFalseTest() throws Exception {
        // given
        final TodoResponse response = new TodoResponse(1L, "Buy milk", null, null, false, Instant.now());
        given(todoService.markIncomplete(1L)).willReturn(response);

        // when
        // then
        mockMvc.perform(patch("/todos/1/incomplete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));
        then(todoService).should().markIncomplete(1L);
    }

    @Test
    void givenNonExistingId_whenPatchIncomplete_thenReturns404Test() throws Exception {
        // given
        given(todoService.markIncomplete(404L)).willThrow(new TodoNotFoundException(404L));

        // when
        // then
        mockMvc.perform(patch("/todos/404/incomplete")).andExpect(status().isNotFound());
    }

    @Test
    void givenId_whenDeleteEndpointIsCalled_thenDeleteTheTodoAttachedToThatIdTest() throws Exception {
        // given
        // (todoService.deleteTodo is void; no exception stubbed means it succeeds)

        // when
        // then
        mockMvc.perform(delete("/todos/1")).andExpect(status().isNoContent());
        then(todoService).should().deleteTodo(1L);
    }

    @Test
    void givenNonExistingId_whenDeleteEndpointIsCalled_thenReturns404Test() throws Exception {
        // given
        willThrow(new TodoNotFoundException(404L)).given(todoService).deleteTodo(404L);

        // when
        // then
        mockMvc.perform(delete("/todos/404")).andExpect(status().isNotFound());
    }
}
