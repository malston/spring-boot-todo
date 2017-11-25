package com.pivotal.todo;

//@ResponseStatus(HttpStatus.NOT_FOUND)
public class TodoNotFoundException extends RuntimeException {

    public TodoNotFoundException(String msg) {
        super(msg);
    }

}