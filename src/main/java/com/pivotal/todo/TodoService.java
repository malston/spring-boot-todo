package com.pivotal.todo;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TodoService {
    private static int todoCount = 3;

    private static List<Todo> todos = new ArrayList<Todo>();

    static {
        todos.add(new Todo(1, "Jack", "Learn Spring Boot", new Date(), false));
        todos.add(new Todo(2, "Jack", "Learn Reactor", new Date(), false));
        todos.add(new Todo(3, "Jill", "Learn Spring WebFlux", new Date(), false));
    }

    public List<Todo> retrieveTodos(String user) {
        return todos.stream()
                .filter(todo -> todo.getUser().equals(user))
                .collect(Collectors.toList());
    }

    public Todo addTodo(String name, String desc, Date targetDate, boolean isDone) {
        Todo todo = new Todo(++todoCount, name, desc, targetDate, isDone);
        todos.add(todo);
        return todo;
    }

    public Todo retrieveTodo(int id) {
        return todos.stream()
                .filter(t -> t.getId() == id)
                .findAny()
                .orElseThrow(() -> new TodoNotFoundException("Todo Not Found"));
    }
}