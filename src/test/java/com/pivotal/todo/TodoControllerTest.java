package com.pivotal.todo;

import com.google.gson.JsonObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
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

    @Autowired
    LinkDiscoverers links;

    @Test
    public void retrieveTodos() throws Exception {
        List<Todo> mockList = Arrays.asList(new Todo(1, "Jack", "Learn Spring Boot", new Date(), false),
                new Todo(2, "Jack", "Learn Reactor", new Date(), false));
        when(service.retrieveTodos(anyString())).thenReturn(mockList);
        String expected = "{\"_embedded\":{\"todoList\":[{\"id\":1,\"user\":\"Jack\",\"desc\":\"Learn Spring Boot\",\"done\":false," +
                "\"_links\":{\"parent\":{\"href\":\"http://localhost/users/Jack/todos\"}," +
                "\"self\":{\"href\":\"http://localhost/users/Jack/todos/1\"}}}," +
                "{\"id\":2,\"user\":\"Jack\",\"desc\":\"Learn Reactor\",\"done\":false," +
                "\"_links\":{\"parent\":{\"href\":\"http://localhost/users/Jack/todos\"}," +
                "\"self\":{\"href\":\"http://localhost/users/Jack/todos/2\"}}}]}," +
                "\"_links\":{\"self\":{\"href\":\"http://localhost/users/Jack/todos\"}}}";
        MvcResult result =
                mvc.perform(get("/users/Jack/todos").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(contentMatches(expected))
                    .andExpect(linkWithRelIsPresent("$._embedded.todoList[0]", "parent"))
                    .andExpect(linkWithRelIsPresent("$._embedded.todoList[0]", "self"))
                    .andExpect(linkWithHrefIsEqualTo("$._embedded.todoList[0]", "parent", "http://localhost/users/Jack/todos"))
                    .andExpect(linkWithHrefIsEqualTo("$._embedded.todoList[0]", "self", "http://localhost/users/Jack/todos/1"))
                    .andReturn();
    }

    @Test
    public void retrieveTodo() throws Exception {
        Todo mockTodo = new Todo(1, "Jack", "Learn Spring Boot", new Date(), false);
        when(service.retrieveTodo(anyInt())).thenReturn(mockTodo);
        String expected = "{id:1,user:Jack,desc:\"Learn Spring Boot\",done:false}";
        MvcResult result =
                mvc.perform(get("/users/Jack/todos/1").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(contentMatches(expected))
                    .andReturn();
    }

    @Test
    public void shouldReturnErrorMessage_whenTodoNotFoundExceptionIsThrown() throws Exception {
        String expected = "{message:\"Todo Not Found\",details:\"Any details you would want to add\"}";
        when(this.service.retrieveTodo(5)).thenThrow(new TodoNotFoundException("Todo Not Found"));
        MvcResult result =
                mvc.perform(get("/users/Jack/todos/5").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError())
                    .andExpect(contentMatches(expected))
                    .andReturn();
    }

    @Test
    public void createTodo() throws Exception {
        Todo mockTodo = new Todo(CREATED_TODO_ID, "Jack", "Learn Spring Boot", new Date(), false);
        String todo = "{\"user\":\"Jack\",\"desc\":\"Learn Spring Boot\",\"done\":\"false\"}";

        when(service.addTodo(anyString(), anyString(), isNull(), anyBoolean())).thenReturn(mockTodo);

        mvc.perform(post("/users/{name}/todos", "Jack").
                contentType(MediaType.APPLICATION_JSON).
                content(todo)).
                andExpect(status().isCreated()).
                andExpect(header().string("location", containsString("/users/Jack/todos/" + CREATED_TODO_ID)));
    }

    protected ResultMatcher contentMatches(final String json) {
        return new JSONAssertMatcher(json);
    }

    protected ResultMatcher linkWithRelIsPresent(final String path, final String rel) {
        return new LinkWithRelMatcher(path, rel);
    }

    protected ResultMatcher linkWithHrefIsEqualTo(final String path, final String rel, final String href) {
        return new LinkWithHrefMatcher(path, rel, href);
    }

    private class LinkWithHrefMatcher implements ResultMatcher {

        private final String path;
        private final String rel;
        private String href;

        public LinkWithHrefMatcher(String path, String rel, String href) {
            this.path = path;
            this.rel = rel;
            this.href = href;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.test.web.servlet.ResultMatcher#match(org.springframework.test.web.servlet.MvcResult)
         */
        @Override
        public void match(MvcResult result) throws Exception {

            MockHttpServletResponse response = result.getResponse();
            String content = response.getContentAsString();
            LinkDiscoverer discoverer = links.getLinkDiscovererFor(response.getContentType());

            Configuration configuration = Configuration.builder().jsonProvider(new GsonJsonProvider()).build();
            JsonObject jsonObject = JsonPath.parse(content, configuration).read(path);

            assertThat(discoverer.findLinkWithRel(rel, jsonObject.toString()).getHref(), is(href));
        }
    }

    private class LinkWithRelMatcher implements ResultMatcher {

        private final String path;
        private final String rel;

        public LinkWithRelMatcher(String path, String rel) {
            this.path = path;
            this.rel = rel;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.test.web.servlet.ResultMatcher#match(org.springframework.test.web.servlet.MvcResult)
         */
        @Override
        public void match(MvcResult result) throws Exception {

            MockHttpServletResponse response = result.getResponse();
            String content = response.getContentAsString();
            LinkDiscoverer discoverer = links.getLinkDiscovererFor(response.getContentType());

            Configuration configuration = Configuration.builder().jsonProvider(new GsonJsonProvider()).build();
            JsonObject jsonObject = JsonPath.parse(content, configuration).read(path);

            assertThat(discoverer.findLinkWithRel(rel, jsonObject.toString()), is(notNullValue()));
        }
    }

    private class JSONAssertMatcher implements ResultMatcher {

        private final String json;

        public JSONAssertMatcher(String json) {
            this.json = json;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.test.web.servlet.ResultMatcher#match(org.springframework.test.web.servlet.MvcResult)
         */
        @Override
        public void match(MvcResult result) throws Exception {
            assertEquals(json, result.getResponse().getContentAsString(), false);
        }
    }
}