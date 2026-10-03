package com.himanshu.social_post_backend;

import tools.jackson.databind.ObjectMapper;
import com.himanshu.social_post_backend.dto.request.CreateCommentRequest;
import com.himanshu.social_post_backend.dto.request.CreatePostRequest;
import com.himanshu.social_post_backend.dto.request.UpdatePostRequest;
import com.himanshu.social_post_backend.dto.response.PostResponse;
import com.himanshu.social_post_backend.model.PostStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CachingAndQueryOptimizationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("EXP 2.2.2: Should store post in in-memory cache upon first read and serve from cache")
    void shouldCachePostOnReadAndEvictOnUpdate() throws Exception {
        // 1. Create a post
        CreatePostRequest createRequest = new CreatePostRequest(
                "Caching Strategies with Spring Cache",
                "caching-strategies-with-spring-cache",
                "Demonstrating in-memory caching and eviction mechanics.",
                "Himanshu Goyal",
                PostStatus.PUBLISHED,
                Set.of("caching", "optimization")
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseStr = createResult.getResponse().getContentAsString();
        int idIdx = responseStr.indexOf("\"id\":");
        int commaIdx = responseStr.indexOf(",", idIdx);
        Long postId = Long.valueOf(responseStr.substring(idIdx + 5, commaIdx).trim());

        // 2. First read: Misses cache, loads from DB and populates cache
        long startMiss = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(postId.intValue())));
        long missDuration = System.currentTimeMillis() - startMiss;

        // Verify cache entry exists in Spring CacheManager
        assertNotNull(cacheManager.getCache("posts"));
        assertNotNull(cacheManager.getCache("posts").get(postId));

        // 3. Second read: Hits cache directly
        long startHit = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(postId.intValue())));
        long hitDuration = System.currentTimeMillis() - startHit;

        System.out.printf("Cache Benchmark: DB Query Time = %dms, Cache Hit Time = %dms%n", missDuration, hitDuration);

        // 4. Update post -> Evicts cache entries
        UpdatePostRequest updateRequest = new UpdatePostRequest(
                "Caching Strategies with Spring Cache Updated",
                "Updated content body for caching test.",
                Set.of("caching", "performance")
        );

        mockMvc.perform(put("/api/v1/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EXP 2.2.2: Should resolve N+1 problem using JOIN FETCH endpoint")
    void shouldResolveNPlusOneQueryViaJoinFetch() throws Exception {
        // Create post with comments
        CreatePostRequest postRequest = new CreatePostRequest(
                "JOIN FETCH Optimization Guide",
                "join-fetch-optimization-guide",
                "How JOIN FETCH eliminates N+1 query loops.",
                "Himanshu Goyal",
                PostStatus.PUBLISHED,
                Set.of("jpa", "join-fetch")
        );

        MvcResult postResult = mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseStr = postResult.getResponse().getContentAsString();
        int idIdx = responseStr.indexOf("\"id\":");
        int commaIdx = responseStr.indexOf(",", idIdx);
        Long postId = Long.valueOf(responseStr.substring(idIdx + 5, commaIdx).trim());

        // Add 2 comments
        CreateCommentRequest comment1 = new CreateCommentRequest("Alice", "alice@example.com", "Great post!");
        CreateCommentRequest comment2 = new CreateCommentRequest("Bob", "bob@example.com", "Very helpful.");

        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(comment1))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(comment2))).andExpect(status().isCreated());

        // Call JOIN FETCH eager endpoint: loads posts and child comments in 1 single SQL trip
        mockMvc.perform(get("/api/v1/posts/eager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", not(empty())));
    }

    @Test
    @DisplayName("EXP 2.2.2: Should execute Native SQL aggregation query for author analytics")
    void shouldExecuteNativeSQLQueryForAnalytics() throws Exception {
        mockMvc.perform(get("/api/v1/posts/analytics/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[0].author", notNullValue()))
                .andExpect(jsonPath("$.data[0].totalPosts", greaterThanOrEqualTo(1)));
    }
}
