package com.himanshu.social_post_backend.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for issuing, parsing, and cryptographically validating JSON Web Tokens (JWT)
 * utilizing HMAC-SHA256 (HS256) signature verification.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String HMAC_ALGO = "HmacSHA256";

    private final String secretKey;
    private final long accessTokenExpirationMs;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${security.jwt.secret:cu-fullstack2-himanshu-goyal-24bda70369-super-secure-jwt-secret-key-384bits}") String secretKey,
            @Value("${security.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
            ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Generates a signed JWT access token for an authenticated user with embedded role claims.
     */
    public String generateAccessToken(String username, String role, Long userId) {
        long now = Instant.now().getEpochSecond();
        long exp = now + (accessTokenExpirationMs / 1000);

        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", username);
        payload.put("role", role);
        payload.put("userId", userId);
        payload.put("iat", now);
        payload.put("exp", exp);
        payload.put("type", "ACCESS");

        return createSignedToken(header, payload);
    }

    /**
     * Validates signature and expiration of the given JWT token.
     */
    public boolean isTokenValid(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String content = parts[0] + "." + parts[1];
            String expectedSignature = signHmac(content, secretKey);

            if (!MessageDigest.isEqual(
                    parts[2].getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("Invalid JWT signature detected");
                return false;
            }

            if (isTokenExpired(token)) {
                log.warn("Expired JWT token presented");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates token and checks if subject matches the given username.
     */
    public boolean isTokenValid(String token, String username) {
        String tokenUsername = extractUsername(token);
        return tokenUsername != null && tokenUsername.equalsIgnoreCase(username) && isTokenValid(token);
    }

    public String extractUsername(String token) {
        JsonNode claims = parseClaims(token);
        return (claims != null && claims.has("sub")) ? claims.get("sub").asText() : null;
    }

    public String extractRole(String token) {
        JsonNode claims = parseClaims(token);
        return (claims != null && claims.has("role")) ? claims.get("role").asText() : null;
    }

    public Long extractUserId(String token) {
        JsonNode claims = parseClaims(token);
        return (claims != null && claims.has("userId")) ? claims.get("userId").asLong() : null;
    }

    public boolean isTokenExpired(String token) {
        JsonNode claims = parseClaims(token);
        if (claims == null || !claims.has("exp")) {
            return true;
        }
        long exp = claims.get("exp").asLong();
        return Instant.now().getEpochSecond() > exp;
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    private JsonNode parseClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(decoded);
        } catch (Exception e) {
            log.warn("Failed to parse JWT payload: {}", e.getMessage());
            return null;
        }
    }

    private String createSignedToken(Map<String, Object> header, Map<String, Object> payload) {
        try {
            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String content = encodedHeader + "." + encodedPayload;
            String signature = signHmac(content, secretKey);

            return content + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build signed JWT token", e);
        }
    }

    private String signHmac(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC signature", e);
        }
    }
}
