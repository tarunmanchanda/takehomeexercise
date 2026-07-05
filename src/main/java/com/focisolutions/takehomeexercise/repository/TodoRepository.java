package com.focisolutions.takehomeexercise.repository;

import com.focisolutions.takehomeexercise.entity.Todo;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByCompleted(boolean completed, Sort sort);

    List<Todo> findByCompletedFalseAndDueDateBefore(LocalDate date, Sort sort);
}
