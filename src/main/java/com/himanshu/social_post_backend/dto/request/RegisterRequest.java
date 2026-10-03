package com.himanshu.social_post_backend.dto.request;

import com.himanshu.social_post_backend.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username must not be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid email format")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        String password,

        UserRole role
) {
}
