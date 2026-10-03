package com.himanshu.social_post_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdatePostRequest(
        @NotBlank(message = "Title is required and cannot be blank")
        @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
        String title,

        @NotBlank(message = "Content cannot be blank")
        @Size(min = 10, max = 10000, message = "Content must be between 10 and 10,000 characters")
        String content,

        Set<@NotBlank @Size(max = 50) String> tags
) {}
