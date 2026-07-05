package com.focisolutions.takehomeexercise.service;

import com.focisolutions.takehomeexercise.dto.TodoCreateRequest;
import com.focisolutions.takehomeexercise.dto.TodoResponse;
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
}
