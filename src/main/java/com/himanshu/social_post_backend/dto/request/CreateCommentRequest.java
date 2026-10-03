package com.himanshu.social_post_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Author name is required")
        @Size(min = 2, max = 100, message = "Author name must be between 2 and 100 characters")
        String authorName,

        @NotBlank(message = "Author email is required")
        @Email(message = "Author email must be a valid email format")
        String authorEmail,

        @NotBlank(message = "Comment content cannot be blank")
        @Size(min = 3, max = 1000, message = "Comment must be between 3 and 1,000 characters")
        String content
) {}
