package com.himanshu.social_post_backend.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        String role,
        Long userId
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, String username, String role, Long userId) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, username, role, userId);
    }
}
