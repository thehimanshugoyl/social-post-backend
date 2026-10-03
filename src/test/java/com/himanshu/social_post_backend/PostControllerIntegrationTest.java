package com.himanshu.social_post_backend;

import tools.jackson.databind.ObjectMapper;
import com.himanshu.social_post_backend.dto.request.CreateCommentRequest;
import com.himanshu.social_post_backend.dto.request.CreatePostRequest;
import com.himanshu.social_post_backend.dto.request.PostStatusUpdateRequest;
import com.himanshu.social_post_backend.dto.request.UpdatePostRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PostControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("POST /api/v1/posts - Should create post and return 201 Created with standardized envelope")
    void shouldCreatePostSuccessfully() throws Exception {
        CreatePostRequest request = new CreatePostRequest(
                "Designing Scalable REST APIs in Spring Boot",
                "designing-scalable-rest-apis-in-spring-boot",
                "A comprehensive guide covering layered architecture, DTOs, and global exception handling.",
                "Himanshu Goyal",
                PostStatus.PUBLISHED,
                Set.of("spring-boot", "architecture", "rest-api")
        );

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status", is(201)))
                .andExpect(jsonPath("$.message", containsString("Post created successfully")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.slug", is("designing-scalable-rest-apis-in-spring-boot")))
                .andExpect(jsonPath("$.data.author", is("Himanshu Goyal")))
                .andExpect(jsonPath("$.data.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/posts - Should return 400 Bad Request when validation fails")
    void shouldReturnBadRequestWhenInputIsInvalid() throws Exception {
        // Missing title, content too short, empty author
        CreatePostRequest invalidRequest = new CreatePostRequest(
                "",
                "invalid slug with spaces",
                "short",
                "",
                null,
                Set.of()
        );

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", not(empty())))
                .andExpect(jsonPath("$.validationErrors[*].field", hasItems("title", "content", "author")));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{id} - Should return 404 Not Found when post does not exist")
    void shouldReturnNotFoundForNonExistentPost() throws Exception {
        mockMvc.perform(get("/api/v1/posts/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Post not found with id: '99999'")));
    }

    @Test
    @DisplayName("GET /api/v1/posts - Should return paginated response")
    void shouldReturnPaginatedPosts() throws Exception {
        CreatePostRequest request = new CreatePostRequest(
                "Pagination and Sorting in Spring Boot",
                "pagination-and-sorting-in-spring-boot",
                "Demonstrating Spring Data Pageable and Page responses.",
                "Himanshu Goyal",
                PostStatus.PUBLISHED,
                Set.of("pagination", "spring")
        );

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", isA(Iterable.class)))
                .andExpect(jsonPath("$.data.pageNumber", is(0)))
                .andExpect(jsonPath("$.data.pageSize", is(5)))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("POST /api/v1/posts/{postId}/comments - Should add comment and validate email")
    void shouldAddCommentAndRejectInvalidEmail() throws Exception {
        // First create a post
        CreatePostRequest postRequest = new CreatePostRequest(
                "Commentable Post Title Example",
                "commentable-post-title-example",
                "Content body for post testing comments functionality.",
                "Author",
                PostStatus.PUBLISHED,
                Set.of()
        );

        String postResponseStr = mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extract ID directly without heavy JSON parser
        int idIdx = postResponseStr.indexOf("\"id\":");
        int commaIdx = postResponseStr.indexOf(",", idIdx);
        String idStr = postResponseStr.substring(idIdx + 5, commaIdx).trim();
        Long postId = Long.valueOf(idStr);

        // 1. Invalid email comment should fail with 400
        CreateCommentRequest invalidComment = new CreateCommentRequest(
                "Reviewer",
                "invalid-email-address",
                "Great article!"
        );

        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidComment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("authorEmail")));

        // 2. Valid comment should succeed with 201
        CreateCommentRequest validComment = new CreateCommentRequest(
                "Reviewer",
                "reviewer@example.com",
                "Great article on scalable architecture!"
        );

        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validComment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.authorEmail", is("reviewer@example.com")));
    }
}
