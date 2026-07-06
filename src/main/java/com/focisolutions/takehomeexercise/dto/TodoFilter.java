package com.focisolutions.takehomeexercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status filter for listing Todos. OVERDUE means incomplete with a dueDate before today.")
public enum TodoFilter {
    ALL,
    COMPLETED,
    INCOMPLETE,
    OVERDUE
}
