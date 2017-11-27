package com.pivotal.todo;

import com.google.gson.JsonObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class AbstractMockMvcTest {
    @Autowired
    LinkDiscoverers links;

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
