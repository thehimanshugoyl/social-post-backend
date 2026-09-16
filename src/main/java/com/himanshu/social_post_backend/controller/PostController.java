package com.himanshu.social_post_backend.controller;

import com.himanshu.social_post_backend.dto.ApiResponse;
import com.himanshu.social_post_backend.dto.PostRequest;
import com.himanshu.social_post_backend.dto.PostResponse;
import com.himanshu.social_post_backend.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@Valid @RequestBody PostRequest request) {
        PostResponse response = postService.createPost(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponse>>> getAllPosts() {
        List<PostResponse> posts = postService.getAllPosts();
        return ResponseEntity.ok(ApiResponse.success("Posts retrieved successfully", posts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable Long id) {
        PostResponse post = postService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success("Post retrieved successfully", post));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest request) {
        PostResponse response = postService.updatePost(id, request);
        return ResponseEntity.ok(ApiResponse.success("Post updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
    }
}