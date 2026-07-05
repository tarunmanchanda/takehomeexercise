package com.focisolutions.takehomeexercise.exception;

public class TodoNotFoundException extends RuntimeException {

    public TodoNotFoundException(final Long id) {
        super("Todo not found with id " + id);
    }
}
