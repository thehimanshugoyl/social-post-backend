package com.himanshu.social_post_backend.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity storing sensitive third-party OAuth credentials.
 * Client secrets and tokens are encrypted at rest using AES-256-GCM.
 */
@Entity
@Table(name = "oauth_credentials", indexes = {
        @Index(name = "idx_oauth_service_name", columnList = "serviceName", unique = true)
})
public class OAuthCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String serviceName;

    @Column(nullable = false, length = 120)
    private String clientId;

    @Column(nullable = false, length = 512)
    private String encryptedClientSecret;

    @Column(nullable = false, length = 512)
    private String encryptedAccessToken;

    @Column(length = 512)
    private String encryptedRefreshToken;

    @Column(length = 120)
    private String scope;

    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public OAuthCredential() {
    }

    public OAuthCredential(String serviceName, String clientId, String encryptedClientSecret,
                           String encryptedAccessToken, String encryptedRefreshToken, String scope, Instant expiresAt) {
        this.serviceName = serviceName;
        this.clientId = clientId;
        this.encryptedClientSecret = encryptedClientSecret;
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.scope = scope;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getEncryptedClientSecret() {
        return encryptedClientSecret;
    }

    public void setEncryptedClientSecret(String encryptedClientSecret) {
        this.encryptedClientSecret = encryptedClientSecret;
    }

    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }

    public void setEncryptedAccessToken(String encryptedAccessToken) {
        this.encryptedAccessToken = encryptedAccessToken;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public void setEncryptedRefreshToken(String encryptedRefreshToken) {
        this.encryptedRefreshToken = encryptedRefreshToken;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
