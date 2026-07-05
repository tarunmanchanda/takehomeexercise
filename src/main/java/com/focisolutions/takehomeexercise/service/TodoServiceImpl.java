package com.focisolutions.takehomeexercise.service;

import com.focisolutions.takehomeexercise.dto.TodoCreateRequest;
import com.focisolutions.takehomeexercise.dto.TodoResponse;
import com.focisolutions.takehomeexercise.dto.TodoUpdateRequest;
import com.focisolutions.takehomeexercise.entity.Todo;
import com.focisolutions.takehomeexercise.exception.TodoNotFoundException;
import com.focisolutions.takehomeexercise.mapper.TodoMapper;
import com.focisolutions.takehomeexercise.repository.TodoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class TodoServiceImpl implements TodoService {

    private static final Logger log = LoggerFactory.getLogger(TodoServiceImpl.class);

    private final TodoRepository todoRepository;
    private final TodoMapper todoMapper;

    @Override
    public TodoResponse createTodo(final TodoCreateRequest request) {
        final Todo todo = new Todo(request.title(), request.description(), request.dueDate());
        final Todo saved = todoRepository.save(todo);
        log.info("Created todo {}", saved.getId());
        return todoMapper.toResponse(saved);
    }

    @Override
    public TodoResponse findTodoById(final Long id) {
        return todoMapper.toResponse(getTodoOrThrow(id));
    }

    @Override
    public List<TodoResponse> findAllTodos() {
        return todoRepository.findAll().stream().map(todoMapper::toResponse).toList();
    }

    @Override
    public TodoResponse updateTodo(final Long id, final TodoUpdateRequest request) {
        final Todo todo = getTodoOrThrow(id);
        todo.updateDetails(request.title(), request.description(), request.dueDate());
        final Todo saved = todoRepository.save(todo);
        log.info("Updated todo {}", id);
        return todoMapper.toResponse(saved);
    }

    @Override
    public TodoResponse markCompleted(final Long id) {
        final Todo todo = getTodoOrThrow(id);
        todo.markCompleted();
        final Todo saved = todoRepository.save(todo);
        log.info("Marked todo {} as completed", id);
        return todoMapper.toResponse(saved);
    }

    @Override
    public TodoResponse markIncomplete(final Long id) {
        final Todo todo = getTodoOrThrow(id);
        todo.markIncomplete();
        final Todo saved = todoRepository.save(todo);
        log.info("Marked todo {} as incomplete", id);
        return todoMapper.toResponse(saved);
    }

    @Override
    public void deleteTodo(final Long id) {
        if (!todoRepository.existsById(id)) {
            throw new TodoNotFoundException(id);
        }
        todoRepository.deleteById(id);
        log.info("Deleted todo {}", id);
    }

    private Todo getTodoOrThrow(final Long id) {
        return todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
    }
}
