package com.focisolutions.takehomeexercise.controller;

import com.focisolutions.takehomeexercise.dto.TodoCreateRequest;
import com.focisolutions.takehomeexercise.dto.TodoFilter;
import com.focisolutions.takehomeexercise.dto.TodoResponse;
import com.focisolutions.takehomeexercise.dto.TodoSortBy;
import com.focisolutions.takehomeexercise.dto.TodoUpdateRequest;
import com.focisolutions.takehomeexercise.service.TodoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
@Validated
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    public ResponseEntity<TodoResponse> create(@Valid @RequestBody final TodoCreateRequest request) {
        final TodoResponse created = todoService.createTodo(request);
        return ResponseEntity.created(URI.create("/todos/" + created.id())).body(created);
    }

    @GetMapping
    public List<TodoResponse> getAll(
            @RequestParam(defaultValue = "ALL") final TodoFilter status,
            @RequestParam(defaultValue = "CREATED_AT") final TodoSortBy sortBy,
            @RequestParam(defaultValue = "ASC") final Sort.Direction direction) {
        return todoService.findAllTodos(status, sortBy, direction);
    }

    @GetMapping("/{id}")
    public TodoResponse getById(@PathVariable @Positive final Long id) {
        return todoService.findTodoById(id);
    }

    @PutMapping("/{id}")
    public TodoResponse update(@PathVariable @Positive final Long id, @Valid @RequestBody final TodoUpdateRequest request) {
        return todoService.updateTodo(id, request);
    }

    @PatchMapping("/{id}/complete")
    public TodoResponse markComplete(@PathVariable @Positive final Long id) {
        return todoService.markCompleted(id);
    }

    @PatchMapping("/{id}/incomplete")
    public TodoResponse markIncomplete(@PathVariable @Positive final Long id) {
        return todoService.markIncomplete(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive final Long id) {
        todoService.deleteTodo(id);
        return ResponseEntity.noContent().build();
    }
}
