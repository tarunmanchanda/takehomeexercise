package com.focisolutions.takehomeexercise.repository;

import com.focisolutions.takehomeexercise.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {
}
