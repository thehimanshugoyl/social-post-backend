package com.himanshu.social_post_backend.dto.response;

import com.himanshu.social_post_backend.model.PostStatus;

import java.time.Instant;
import java.util.Set;

public record PostResponse(
        Long id,
        String title,
        String slug,
        String content,
        String author,
        PostStatus status,
        Set<String> tags,
        int commentCount,
        Instant createdAt,
        Instant updatedAt
) {}
