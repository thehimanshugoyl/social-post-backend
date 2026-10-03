package com.himanshu.social_post_backend.dto.response;

import java.time.Instant;

public record OAuthCredentialResponse(
        String serviceName,
        String clientId,
        String maskedClientSecret,
        String maskedAccessToken,
        String scope,
        Instant expiresAt,
        Instant updatedAt
) {
    public static OAuthCredentialResponse of(String serviceName, String clientId, String rawSecret, String rawToken, String scope, Instant expiresAt, Instant updatedAt) {
        String maskedSecret = mask(rawSecret);
        String maskedTok = mask(rawToken);
        return new OAuthCredentialResponse(serviceName, clientId, maskedSecret, maskedTok, scope, expiresAt, updatedAt);
    }

    private static String mask(String str) {
        if (str == null || str.length() <= 8) {
            return "********";
        }
        return str.substring(0, 4) + "••••••••" + str.substring(str.length() - 4);
    }
}
