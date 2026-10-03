package com.himanshu.social_post_backend;

import tools.jackson.databind.ObjectMapper;
import com.himanshu.social_post_backend.dto.request.CreatePostRequest;
import com.himanshu.social_post_backend.model.PostStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PaginationAndSortingTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Seed test data with distinct titles for pagination and sorting verification
        createTestPost("Alpha Post on Spring Boot", "alpha-post-on-spring-boot", PostStatus.PUBLISHED);
        createTestPost("Beta Post on Data Structures", "beta-post-on-data-structures", PostStatus.PUBLISHED);
        createTestPost("Gamma Post on System Design", "gamma-post-on-system-design", PostStatus.DRAFT);
        createTestPost("Delta Post on Machine Learning", "delta-post-on-machine-learning", PostStatus.PUBLISHED);
    }

    private void createTestPost(String title, String slug, PostStatus status) throws Exception {
        CreatePostRequest request = new CreatePostRequest(
                title,
                slug,
                "Content body for pagination test: " + title,
                "Himanshu Goyal",
                status,
                Set.of("tech", "spring")
        );
        mockMvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Test
    @DisplayName("EXP 2.2.1: Should return paginated results with proper metadata (page 0, size 2)")
    void shouldReturnFirstPageWithMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.pageNumber", is(0)))
                .andExpect(jsonPath("$.data.pageSize", is(2)))
                .andExpect(jsonPath("$.data.isFirst", is(true)))
                .andExpect(jsonPath("$.data.hasNext", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    @DisplayName("EXP 2.2.1: Should traverse to page 1 and return distinct slice of data")
    void shouldReturnSecondPageCorrectly() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.pageNumber", is(1)))
                .andExpect(jsonPath("$.data.pageSize", is(2)))
                .andExpect(jsonPath("$.data.hasPrevious", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("EXP 2.2.1: Should order posts by title ascending when sortBy=title and direction=asc")
    void shouldSortPostsByTitleAscending() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .param("sortBy", "title")
                        .param("direction", "asc")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].title", containsString("Alpha")));
    }

    @Test
    @DisplayName("EXP 2.2.1: Should filter by status and paginate concurrently")
    void shouldFilterByStatusAndPaginate() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .param("status", "DRAFT")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[*].status", everyItem(is("DRAFT"))));
    }
}
