package com.focisolutions.takehomeexercise.dto;

import lombok.Getter;

@Getter
public enum TodoSortBy {
    TITLE("title"),
    DUE_DATE("dueDate"),
    CREATED_AT("createdAt");

    private final String fieldName;

    TodoSortBy(final String fieldName) {
        this.fieldName = fieldName;
    }

}
