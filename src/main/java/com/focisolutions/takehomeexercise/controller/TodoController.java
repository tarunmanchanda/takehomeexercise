package com.focisolutions.takehomeexercise.controller;

import com.focisolutions.takehomeexercise.dto.TodoCreateRequest;
import com.focisolutions.takehomeexercise.dto.TodoFilter;
import com.focisolutions.takehomeexercise.dto.TodoResponse;
import com.focisolutions.takehomeexercise.dto.TodoSortBy;
import com.focisolutions.takehomeexercise.dto.TodoUpdateRequest;
import com.focisolutions.takehomeexercise.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ProblemDetail;
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
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Todos", description = "Manage personal to-do items")
public class TodoController {

    private final TodoService todoService;

    /**
     * Creates a new Todo.
     *
     * @param request the validated creation request
     * @return {@code 201 Created} with a {@code Location} header and the created Todo
     */
    @Operation(summary = "Create a new Todo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Todo created",
                    content = @Content(schema = @Schema(implementation = TodoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<TodoResponse> create(@Valid @RequestBody final TodoCreateRequest request) {
        final TodoResponse created = todoService.createTodo(request);
        return ResponseEntity.created(URI.create("/api/v1/todos/" + created.id())).body(created);
    }

    /**
     * Lists Todos, optionally filtered by status and sorted by a given field/direction.
     *
     * @param status the status filter to apply
     * @param sortBy the field to sort by
     * @param direction the sort direction
     * @return matching Todos, empty if none match
     */
    @Operation(summary = "List Todos, with optional filtering and sorting")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching Todos returned"),
            @ApiResponse(responseCode = "400", description = "Invalid status, sortBy, or direction value",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public List<TodoResponse> getAll(
            @Parameter(description = "Status filter") @RequestParam(defaultValue = "ALL") final TodoFilter status,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = "CREATED_AT") final TodoSortBy sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") final Sort.Direction direction) {
        return todoService.findAllTodos(status, sortBy, direction);
    }

    /**
     * Finds a Todo by its id.
     *
     * @param id the Todo id, must be positive
     * @return the matching Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    @Operation(summary = "Get a Todo by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Todo found"),
            @ApiResponse(responseCode = "404", description = "No Todo with the given id",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "id is not positive",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public TodoResponse getById(@Parameter(description = "Todo id") @PathVariable @Positive final Long id) {
        return todoService.findTodoById(id);
    }

    /**
     * Updates the title, description, and due date of an existing Todo.
     *
     * @param id the Todo id, must be positive
     * @param request the validated update request
     * @return the updated Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if the Todo was concurrently modified
     */
    @Operation(summary = "Update a Todo's title, description, and due date")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Todo updated"),
            @ApiResponse(responseCode = "404", description = "No Todo with the given id",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or id is not positive",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The Todo was modified by another request in the meantime (optimistic locking conflict)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public TodoResponse update(@Parameter(description = "Todo id") @PathVariable @Positive final Long id,
            @Valid @RequestBody final TodoUpdateRequest request) {
        return todoService.updateTodo(id, request);
    }

    /**
     * Marks a Todo as completed. Idempotent — completing an already-complete Todo is not an error.
     *
     * @param id the Todo id, must be positive
     * @return the updated Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if the Todo was concurrently modified
     */
    @Operation(summary = "Mark a Todo as completed", description = "Idempotent -- completing an already-complete Todo is not an error.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Todo marked completed"),
            @ApiResponse(responseCode = "404", description = "No Todo with the given id",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "id is not positive",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The Todo was modified by another request in the meantime (optimistic locking conflict)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/complete")
    public TodoResponse markComplete(@Parameter(description = "Todo id") @PathVariable @Positive final Long id) {
        return todoService.markCompleted(id);
    }

    /**
     * Marks a Todo as not completed. Idempotent — marking an already-incomplete Todo is not an error.
     *
     * @param id the Todo id, must be positive
     * @return the updated Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if the Todo was concurrently modified
     */
    @Operation(summary = "Mark a Todo as not completed", description = "Idempotent -- marking an already-incomplete Todo is not an error.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Todo marked incomplete"),
            @ApiResponse(responseCode = "404", description = "No Todo with the given id",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "id is not positive",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The Todo was modified by another request in the meantime (optimistic locking conflict)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/incomplete")
    public TodoResponse markIncomplete(@Parameter(description = "Todo id") @PathVariable @Positive final Long id) {
        return todoService.markIncomplete(id);
    }

    /**
     * Deletes a Todo.
     *
     * @param id the Todo id, must be positive
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    @Operation(summary = "Delete a Todo")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Todo deleted"),
            @ApiResponse(responseCode = "404", description = "No Todo with the given id",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "id is not positive",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Todo id") @PathVariable @Positive final Long id) {
        todoService.deleteTodo(id);
        return ResponseEntity.noContent().build();
    }
}
