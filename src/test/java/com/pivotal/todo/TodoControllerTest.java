package com.pivotal.todo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(TodoController.class)
public class TodoControllerTest {

    private static final int CREATED_TODO_ID = 4;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TodoService service;

    @Test
    public void retrieveTodos() throws Exception {
        List<Todo> mockList = Arrays.asList(new Todo(1, "Jack", "Learn Spring Boot", new Date(), false),
                new Todo(2, "Jack", "Learn Reactor", new Date(), false));
        when(service.retrieveTodos(anyString())).thenReturn(mockList);
        MvcResult result =
                mvc.perform(get("/users/Jack/todos").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();
        String expected = "[" + "{id:1,user:Jack,desc:\"Learn Spring Boot\",done:false}" + ","
                + "{id:2,user:Jack,desc:\"Learn Reactor\",done:false}" + "]";
        assertEquals(expected, result.getResponse().getContentAsString(), false);
    }

    @Test
    public void retrieveTodo() throws Exception {
        Todo mockTodo = new Todo(1, "Jack", "Learn Spring Boot", new Date(), false);
        when(service.retrieveTodo(anyInt())).thenReturn(mockTodo);
        MvcResult result =
                mvc.perform(get("/users/Jack/todos/1").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();
        String expected = "{id:1,user:Jack,desc:\"Learn Spring Boot\",done:false}";
        assertEquals(expected, result.getResponse().getContentAsString(), false);
    }

    @Test
    public void shouldReturnErrorMessage_whenTodoNotFoundExceptionIsThrown() throws Exception {
        String expected = "{message:\"Todo Not Found\",details:\"Any details you would want to add\"}";
        when(this.service.retrieveTodo(5)).thenThrow(new TodoNotFoundException("Todo Not Found"));
        MvcResult result =
                mvc.perform(get("/users/Jack/todos/5").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError())
                    .andReturn();
        assertEquals(expected, result.getResponse().getContentAsString(), false);
    }

    @Test
    public void createTodo() throws Exception {
        Todo mockTodo = new Todo(CREATED_TODO_ID, "Jack", "Learn Spring Boot", new Date(), false);
        String todo = "{\"user\":\"Jack\",\"desc\":\"Learn Spring Boot\",\"done\":\"false\"}";

        when(service.addTodo(anyString(), anyString(), isNull(), anyBoolean())).thenReturn(mockTodo);
        mvc.perform(post("/users/Jack/todos").content(todo).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(header().string("location", containsString("/users/Jack/todos/" + CREATED_TODO_ID)));
    }
}