package com.himanshu.social_post_backend.dto.request;

import com.himanshu.social_post_backend.model.PostStatus;
import jakarta.validation.constraints.NotNull;

public record PostStatusUpdateRequest(
        @NotNull(message = "Status is required and must be one of: DRAFT, PUBLISHED, ARCHIVED")
        PostStatus status
) {}
