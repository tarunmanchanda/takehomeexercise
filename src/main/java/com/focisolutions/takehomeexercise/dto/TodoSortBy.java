package com.focisolutions.takehomeexercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Field to sort the Todo list by")
public enum TodoSortBy {
    TITLE("title"),
    DUE_DATE("dueDate"),
    CREATED_AT("createdAt");

    private final String fieldName;

    TodoSortBy(final String fieldName) {
        this.fieldName = fieldName;
    }

}
