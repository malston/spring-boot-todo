package com.pivotal.todo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
public class TodoController {
	@Autowired
	private TodoService todoService;

	@GetMapping("/users/{name}/todos")
	public Resources<Resource<Todo>> retrieveTodos(@PathVariable String name) {
		List<Todo> todos = todoService.retrieveTodos(name);
		List<Resource<Todo>> todoResources = todos.stream()
				.map(todo -> {
                    Resource<Todo> todoResource = new Resource<Todo>(todo);
                    ControllerLinkBuilder linkTo = linkTo(methodOn(this.getClass()).retrieveTodos(name));
                    todoResource.add(linkTo.withRel("parent"));
                    todoResource.add(linkTo(methodOn(this.getClass()).retrieveTodo(name, todo.getId())).withSelfRel());
                    return todoResource;
				}).collect(Collectors.toList());
		Resources<Resource<Todo>> resource = new Resources<>(todoResources);
		resource.add(linkTo(methodOn(this.getClass()).retrieveTodos(name)).withSelfRel());
		return resource;
	}

	@GetMapping(path = "/users/{name}/todos/{id}")
	public Resource<Todo> retrieveTodo(@PathVariable String name, @PathVariable int id) {
		Todo todo = todoService.retrieveTodo(id);
		Resource<Todo> todoResource = new Resource<Todo>(todo);
		ControllerLinkBuilder linkTo = linkTo(methodOn(this.getClass()).retrieveTodos(name));
		todoResource.add(linkTo.withRel("parent"));
		return todoResource;
	}

	@PostMapping("/users/{name}/todos")
	ResponseEntity<?> add(@PathVariable String name, @Valid @RequestBody Todo todo) {
		Todo createdTodo = todoService.addTodo(name, todo.getDesc(), todo.getTargetDate(), todo.isDone());
		if (createdTodo == null) {
			return ResponseEntity.noContent().build();
		}
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(createdTodo.getId()).toUri();
		return ResponseEntity.created(location).build();
	}
}