package com.himanshu.social_post_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StoreOAuthCredentialRequest(
        @NotBlank(message = "Service name must not be blank")
        String serviceName,

        @NotBlank(message = "Client ID must not be blank")
        String clientId,

        @NotBlank(message = "Client Secret must not be blank")
        String clientSecret,

        @NotBlank(message = "Access Token must not be blank")
        String accessToken,

        String refreshToken,

        String scope,

        Long expiresInSeconds
) {
}
