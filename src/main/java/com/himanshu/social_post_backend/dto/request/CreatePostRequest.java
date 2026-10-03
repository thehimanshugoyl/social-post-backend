package com.himanshu.social_post_backend.dto.request;

import com.himanshu.social_post_backend.model.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreatePostRequest(
        @NotBlank(message = "Title is required and cannot be blank")
        @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
        String title,

        @Pattern(
                regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must contain only lowercase alphanumeric characters and hyphens (e.g., 'spring-boot-architecture')"
        )
        String slug,

        @NotBlank(message = "Content cannot be blank")
        @Size(min = 10, max = 10000, message = "Content must be between 10 and 10,000 characters")
        String content,

        @NotBlank(message = "Author is required")
        @Size(min = 2, max = 100, message = "Author name must be between 2 and 100 characters")
        String author,

        PostStatus status,

        Set<@NotBlank @Size(max = 50) String> tags
) {}
