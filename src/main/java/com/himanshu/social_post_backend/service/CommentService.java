package com.himanshu.social_post_backend.service;

import com.himanshu.social_post_backend.dto.request.CreateCommentRequest;
import com.himanshu.social_post_backend.dto.response.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    CommentResponse addComment(Long postId, CreateCommentRequest request);

    Page<CommentResponse> getCommentsByPostId(Long postId, Pageable pageable);

    void deleteComment(Long postId, Long commentId);
}
