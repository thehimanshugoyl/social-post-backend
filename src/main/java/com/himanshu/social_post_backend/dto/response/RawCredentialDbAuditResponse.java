package com.himanshu.social_post_backend.dto.response;

import java.time.Instant;

public record RawCredentialDbAuditResponse(
        String serviceName,
        String clientId,
        String rawDbEncryptedClientSecret,
        String rawDbEncryptedAccessToken,
        String rawDbEncryptedRefreshToken,
        String encryptionAlgorithm,
        Instant updatedAt
) {
}
