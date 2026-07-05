package com.focisolutions.takehomeexercise.service;

import com.focisolutions.takehomeexercise.dto.TodoCreateRequest;
import com.focisolutions.takehomeexercise.dto.TodoResponse;
import com.focisolutions.takehomeexercise.dto.TodoUpdateRequest;
import java.util.List;

/**
 * Application service for managing Todo items.
 */
public interface TodoService {

    /**
     * Creates and persists a new Todo.
     *
     * @param request the validated creation request
     * @return the created Todo
     */
    TodoResponse createTodo(TodoCreateRequest request);

    /**
     * Finds a Todo by its id.
     *
     * @param id the Todo id
     * @return the matching Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    TodoResponse findTodoById(Long id);

    /**
     * Lists all Todos.
     *
     * @return all existing Todos, empty if none exist
     */
    List<TodoResponse> findAllTodos();

    /**
     * Updates the title, description, and due date of an existing Todo.
     *
     * @param id the Todo id
     * @param request the validated update request
     * @return the updated Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    TodoResponse updateTodo(Long id, TodoUpdateRequest request);

    /**
     * Marks a Todo as completed. Idempotent — completing an already-complete Todo is not an error.
     *
     * @param id the Todo id
     * @return the updated Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    TodoResponse markCompleted(Long id);

    /**
     * Marks a Todo as not completed. Idempotent — marking an already-incomplete Todo is not an error.
     *
     * @param id the Todo id
     * @return the updated Todo
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    TodoResponse markIncomplete(Long id);

    /**
     * Deletes a Todo.
     *
     * @param id the Todo id
     * @throws com.focisolutions.takehomeexercise.exception.TodoNotFoundException if no Todo exists with the given id
     */
    void deleteTodo(Long id);
}
