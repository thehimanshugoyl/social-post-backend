package com.himanshu.social_post_backend.service;

import com.himanshu.social_post_backend.common.exception.BadRequestException;
import com.himanshu.social_post_backend.dto.response.AuthResponse;
import com.himanshu.social_post_backend.model.RefreshToken;
import com.himanshu.social_post_backend.model.User;
import com.himanshu.social_post_backend.repository.RefreshTokenRepository;
import com.himanshu.social_post_backend.repository.UserRepository;
import com.himanshu.social_post_backend.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service managing token lifecycle: generation, expiration verification,
 * rotation, and revocation for replay attack mitigation.
 */
@Service
public class TokenLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(TokenLifecycleService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final long refreshTokenExpirationMs;

    public TokenLifecycleService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtService jwtService,
            @Value("${security.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Creates a new refresh token with 7-day validity and stores it in the database.
     */
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        Instant expiryDate = Instant.now().plusMillis(refreshTokenExpirationMs);
        String tokenValue = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken(tokenValue, username, expiryDate);
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Rotates a refresh token: invalidates old token, issues a new refresh token and access token.
     * Prevents token reuse attacks by revoking family tokens if a revoked token is reused.
     */
    @Transactional
    public AuthResponse rotateRefreshToken(String requestRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token. Token does not exist."));

        if (token.isRevoked()) {
            log.warn("SECURITY ALERT: Attempted reuse of revoked refresh token by user '{}'. Revoking all active tokens!", token.getUsername());
            refreshTokenRepository.revokeAllByUsername(token.getUsername());
            throw new BadRequestException("Security Alert: Replay attack detected. Token was previously revoked. All sessions invalidated.");
        }

        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new BadRequestException("Refresh token has expired. Please authenticate again.");
        }

        User user = userRepository.findByUsername(token.getUsername())
                .orElseThrow(() -> new BadRequestException("User associated with refresh token no longer exists."));

        // Invalidate old token (Token Rotation)
        token.setRevoked(true);

        // Issue new refresh token
        RefreshToken newRefreshToken = createRefreshToken(user.getUsername());
        token.setReplacedByToken(newRefreshToken.getToken());
        refreshTokenRepository.save(token);

        // Issue new access token
        String newAccessToken = jwtService.generateAccessToken(
                user.getUsername(),
                user.getRole().name(),
                user.getId()
        );

        log.info("Successfully rotated refresh token for user: '{}'", user.getUsername());

        return AuthResponse.of(
                newAccessToken,
                newRefreshToken.getToken(),
                jwtService.getAccessTokenExpirationSeconds(),
                user.getUsername(),
                user.getRole().name(),
                user.getId()
        );
    }

    /**
     * Revokes a specific refresh token (e.g. on user logout).
     */
    @Transactional
    public void revokeToken(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Refresh token revoked for user: '{}'", token.getUsername());
        });
    }

    /**
     * Revokes all active refresh tokens for a user (e.g. on password reset or security lockout).
     */
    @Transactional
    public void revokeAllUserTokens(String username) {
        refreshTokenRepository.revokeAllByUsername(username);
        log.info("All active refresh tokens revoked for user: '{}'", username);
    }
}
