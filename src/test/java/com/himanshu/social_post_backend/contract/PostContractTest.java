package com.himanshu.social_post_backend.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exp 3.2.1: Consumer-Driven Contract Testing with Pact (V4 specification).
 * Formalizes and verifies the API contract between the PostComposer frontend consumer
 * and the SocialPostBackend provider, preventing breaking changes in distributed CI/CD.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "SocialPostBackend")
public class PostContractTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Pact(consumer = "PostComposerFrontend", provider = "SocialPostBackend")
    public V4Pact createPostPact(PactBuilder builder) {
        builder
                .given("Server is ready to accept new posts")
                .expectsToReceiveHttpInteraction("A POST request to create a social post", http -> http
                        .withRequest(request -> request
                                .method("POST")
                                .path("/api/v1/posts")
                                .headers(Map.of("Content-Type", "application/json"))
                                .body(new PactDslJsonBody()
                                        .stringValue("title", "Contract Testing in Spring Boot")
                                        .stringValue("slug", "contract-testing-spring-boot")
                                        .stringValue("content", "Verifying consumer-driven contracts using Pact.")
                                        .stringValue("author", "himanshu")
                                        .stringValue("status", "PUBLISHED")
                                )
                        )
                        .willRespondWith(response -> response
                                .status(201)
                                .headers(Map.of("Content-Type", "application/json"))
                                .body(new PactDslJsonBody()
                                        .booleanValue("success", true)
                                        .integerType("status", 201)
                                        .stringValue("message", "Post created successfully")
                                        .object("data")
                                        .numberType("id", 101)
                                        .stringValue("title", "Contract Testing in Spring Boot")
                                        .stringValue("slug", "contract-testing-spring-boot")
                                        .stringValue("author", "himanshu")
                                        .stringValue("status", "PUBLISHED")
                                        .closeObject()
                                )
                        )
                );
        return builder.toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPostPact")
    @DisplayName("Contract Test: PostComposer creates post according to Pact agreement")
    void testCreatePostContract(MockServer mockServer) {
        String url = mockServer.getUrl() + "/api/v1/posts";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestJson = """
                {
                    "title": "Contract Testing in Spring Boot",
                    "slug": "contract-testing-spring-boot",
                    "content": "Verifying consumer-driven contracts using Pact.",
                    "author": "himanshu",
                    "status": "PUBLISHED"
                }
                """;

        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"success\":true");
        assertThat(response.getBody()).contains("\"status\":201");
        assertThat(response.getBody()).contains("\"slug\":\"contract-testing-spring-boot\"");
    }
}
