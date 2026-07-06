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

/**
 * REST API for managing Todo items: create, list, view, update, mark complete/incomplete, and delete.
 */
@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
@Validated
public class TodoController {

    private final TodoService todoService;

    /**
     * Creates a new Todo.
     *
     * @param request the validated creation request
     * @return {@code 201 Created} with a {@code Location} header and the created Todo
     */
    @PostMapping
    public ResponseEntity<TodoResponse> create(@Valid @RequestBody final TodoCreateRequest request) {
        final TodoResponse created = todoService.createTodo(request);
        return ResponseEntity.created(URI.create("/todos/" + created.id())).body(created);
    }

    /**
     * Lists Todos, optionally filtered by status and sorted by a given field/direction.
     *
     * @param status the status filter to apply
     * @param sortBy the field to sort by
     * @param direction the sort direction
     * @return matching Todos, empty if none match
     */
    @GetMapping
    public List<TodoResponse> getAll(
            @RequestParam(defaultValue = "ALL") final TodoFilter status,
            @RequestParam(defaultValue = "CREATED_AT") final TodoSortBy sortBy,
            @RequestParam(defaultValue = "ASC") final Sort.Direction direction) {
        return todoService.findAllTodos(status, sortBy, direction);
    }

    /**
     * Finds a Todo by its id.
     *
     * @param id the Todo id, must be positive
     * @return the matching Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    @GetMapping("/{id}")
    public TodoResponse getById(@PathVariable @Positive final Long id) {
        return todoService.findTodoById(id);
    }

    /**
     * Updates the title, description, and due date of an existing Todo.
     *
     * @param id the Todo id, must be positive
     * @param request the validated update request
     * @return the updated Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    @PutMapping("/{id}")
    public TodoResponse update(@PathVariable @Positive final Long id, @Valid @RequestBody final TodoUpdateRequest request) {
        return todoService.updateTodo(id, request);
    }

    /**
     * Marks a Todo as completed. Idempotent — completing an already-complete Todo is not an error.
     *
     * @param id the Todo id, must be positive
     * @return the updated Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    @PatchMapping("/{id}/complete")
    public TodoResponse markComplete(@PathVariable @Positive final Long id) {
        return todoService.markCompleted(id);
    }

    /**
     * Marks a Todo as not completed. Idempotent — marking an already-incomplete Todo is not an error.
     *
     * @param id the Todo id, must be positive
     * @return the updated Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    @PatchMapping("/{id}/incomplete")
    public TodoResponse markIncomplete(@PathVariable @Positive final Long id) {
        return todoService.markIncomplete(id);
    }

    /**
     * Deletes a Todo.
     *
     * @param id the Todo id, must be positive
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive final Long id) {
        todoService.deleteTodo(id);
        return ResponseEntity.noContent().build();
    }
}
