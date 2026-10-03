package com.himanshu.social_post_backend.controller;

import com.himanshu.social_post_backend.common.api.ApiResponse;
import com.himanshu.social_post_backend.common.api.PagedResponse;
import com.himanshu.social_post_backend.dto.request.CreateCommentRequest;
import com.himanshu.social_post_backend.dto.response.CommentResponse;
import com.himanshu.social_post_backend.service.CommentService;
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
@RequestMapping("/api/v1/posts/{postId}/comments")
@Validated
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Add a comment to a specific post.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable @Positive(message = "Post ID must be a positive number") Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse comment = commentService.addComment(postId, request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{commentId}")
                .buildAndExpand(comment.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.created(comment, "Comment added successfully"));
    }

    /**
     * Get paginated comments for a specific post.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CommentResponse>>> getComments(
            @PathVariable @Positive(message = "Post ID must be a positive number") Long postId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be >= 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be >= 1")
            @Max(value = 100, message = "Page size must not exceed 100") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommentResponse> commentPage = commentService.getCommentsByPostId(postId, pageable);
        PagedResponse<CommentResponse> response = PagedResponse.of(commentPage);

        return ResponseEntity.ok(ApiResponse.ok(response, "Comments retrieved successfully"));
    }

    /**
     * Delete a specific comment from a post.
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable @Positive(message = "Post ID must be a positive number") Long postId,
            @PathVariable @Positive(message = "Comment ID must be a positive number") Long commentId) {
        commentService.deleteComment(postId, commentId);
        return ResponseEntity.noContent().build();
    }
}
