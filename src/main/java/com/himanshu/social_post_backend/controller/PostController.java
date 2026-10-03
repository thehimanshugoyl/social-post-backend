package com.himanshu.social_post_backend.controller;

import com.himanshu.social_post_backend.common.api.ApiResponse;
import com.himanshu.social_post_backend.common.api.PagedResponse;
import com.himanshu.social_post_backend.dto.request.CreatePostRequest;
import com.himanshu.social_post_backend.dto.request.PostStatusUpdateRequest;
import com.himanshu.social_post_backend.dto.request.UpdatePostRequest;
import com.himanshu.social_post_backend.dto.response.PostResponse;
import com.himanshu.social_post_backend.model.PostStatus;
import com.himanshu.social_post_backend.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/posts")
@Validated
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Create a new post.
     * Returns 201 Created with a Location header and a standardized response envelope.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody CreatePostRequest request) {
        PostResponse createdPost = postService.createPost(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdPost.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.created(createdPost, "Post created successfully"));
    }

    /**
     * Get a paginated, sorted, and filtered list of posts.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getPosts(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be >= 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be >= 1")
            @Max(value = 100, message = "Page size must not exceed 100") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<PostResponse> postPage = postService.getPosts(status, keyword, pageable);
        PagedResponse<PostResponse> pagedResponse = PagedResponse.of(postPage);

        return ResponseEntity.ok(ApiResponse.ok(pagedResponse, "Posts fetched successfully"));
    }

    /**
     * Optimized read endpoint using JOIN FETCH to eliminate the N+1 query problem.
     */
    @GetMapping("/eager")
    public ResponseEntity<ApiResponse<java.util.List<PostResponse>>> getPostsEager() {
        java.util.List<PostResponse> posts = postService.getAllPostsWithCommentsEager();
        return ResponseEntity.ok(ApiResponse.ok(posts, "Posts with comments fetched eagerly via single query"));
    }

    /**
     * Aggregation analytics endpoint powered by cached Native SQL query.
     */
    @GetMapping("/analytics/authors")
    public ResponseEntity<ApiResponse<java.util.List<com.himanshu.social_post_backend.dto.response.AuthorStatsProjection>>> getAuthorAnalytics() {
        java.util.List<com.himanshu.social_post_backend.dto.response.AuthorStatsProjection> stats = postService.getAuthorAnalytics();
        return ResponseEntity.ok(ApiResponse.ok(stats, "Author analytics retrieved successfully"));
    }

    /**
     * Get a post by its numeric identifier (cached).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(
            @PathVariable @Positive(message = "Post ID must be a positive number") Long id) {
        PostResponse post = postService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.ok(post, "Post retrieved successfully"));
    }

    /**
     * Get a post by its SEO-friendly unique slug.
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostBySlug(@PathVariable String slug) {
        PostResponse post = postService.getPostBySlug(slug);
        return ResponseEntity.ok(ApiResponse.ok(post, "Post retrieved successfully"));
    }

    /**
     * Update an existing post.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable @Positive(message = "Post ID must be a positive number") Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        PostResponse updatedPost = postService.updatePost(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updatedPost, "Post updated successfully"));
    }

    /**
     * Partially update the lifecycle status of a post.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PostResponse>> updatePostStatus(
            @PathVariable @Positive(message = "Post ID must be a positive number") Long id,
            @Valid @RequestBody PostStatusUpdateRequest request) {
        PostResponse updatedPost = postService.updatePostStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.ok(updatedPost, "Post status updated successfully"));
    }

    /**
     * Delete a post by ID. Returns HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable @Positive(message = "Post ID must be a positive number") Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
