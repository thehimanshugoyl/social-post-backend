package com.himanshu.social_post_backend;

import tools.jackson.databind.ObjectMapper;
import com.himanshu.social_post_backend.common.filter.CorrelationIdFilter;
import com.himanshu.social_post_backend.dto.request.CreatePostRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class StructuredLoggingAndExceptionHandlingTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CorrelationIdFilter correlationIdFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(correlationIdFilter)
                .build();
    }

    @Test
    @DisplayName("Should generate X-Correlation-Id header when not provided by client")
    void shouldGenerateCorrelationIdWhenNotProvided() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/posts/88888"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.correlationId", notNullValue()))
                .andReturn();

        String headerCorrelationId = result.getResponse().getHeader("X-Correlation-Id");
        assertNotNull(headerCorrelationId);
    }

    @Test
    @DisplayName("Should propagate custom X-Correlation-Id provided in request header")
    void shouldPropagateProvidedCorrelationId() throws Exception {
        String customId = "trace-himanshu-2026-xyz";

        mockMvc.perform(get("/api/v1/posts/77777")
                        .header("X-Correlation-Id", customId))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", customId))
                .andExpect(jsonPath("$.correlationId", is(customId)))
                .andExpect(jsonPath("$.message", containsString("Post not found with id: '77777'")));
    }

    @Test
    @DisplayName("Should handle validation errors centrally and include correlation ID without leaking stack trace")
    void shouldHandleValidationErrorsWithCorrelationId() throws Exception {
        CreatePostRequest invalidRequest = new CreatePostRequest(
                "",
                "invalid slug",
                "too short",
                "",
                null,
                Set.of()
        );

        String customTrace = "trace-val-400-test";

        mockMvc.perform(post("/api/v1/posts")
                        .header("X-Correlation-Id", customTrace)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Correlation-Id", customTrace))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.correlationId", is(customTrace)))
                .andExpect(jsonPath("$.validationErrors", not(empty())))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
