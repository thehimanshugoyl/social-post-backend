package com.himanshu.social_post_backend.dto.response;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long postId,
        String authorName,
        String authorEmail,
        String content,
        Instant createdAt
) {}
