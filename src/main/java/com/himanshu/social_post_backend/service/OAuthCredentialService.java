package com.himanshu.social_post_backend.service;

import com.himanshu.social_post_backend.common.exception.ResourceNotFoundException;
import com.himanshu.social_post_backend.dto.request.StoreOAuthCredentialRequest;
import com.himanshu.social_post_backend.dto.response.OAuthCredentialResponse;
import com.himanshu.social_post_backend.dto.response.RawCredentialDbAuditResponse;
import com.himanshu.social_post_backend.model.OAuthCredential;
import com.himanshu.social_post_backend.repository.OAuthCredentialRepository;
import com.himanshu.social_post_backend.security.AesEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OAuthCredentialService {

    private static final Logger log = LoggerFactory.getLogger(OAuthCredentialService.class);

    private final OAuthCredentialRepository credentialRepository;
    private final AesEncryptionService aesEncryptionService;

    public OAuthCredentialService(OAuthCredentialRepository credentialRepository, AesEncryptionService aesEncryptionService) {
        this.credentialRepository = credentialRepository;
        this.aesEncryptionService = aesEncryptionService;
    }

    @Transactional
    public OAuthCredentialResponse storeCredential(StoreOAuthCredentialRequest request) {
        log.info("Encrypting and persisting OAuth credential for service: '{}'", request.serviceName());

        String encSecret = aesEncryptionService.encrypt(request.clientSecret());
        String encAccess = aesEncryptionService.encrypt(request.accessToken());
        String encRefresh = request.refreshToken() != null ? aesEncryptionService.encrypt(request.refreshToken()) : null;

        Instant expiresAt = request.expiresInSeconds() != null
                ? Instant.now().plusSeconds(request.expiresInSeconds())
                : Instant.now().plusSeconds(3600);

        OAuthCredential credential = credentialRepository.findByServiceName(request.serviceName())
                .orElseGet(OAuthCredential::new);

        credential.setServiceName(request.serviceName().toUpperCase());
        credential.setClientId(request.clientId());
        credential.setEncryptedClientSecret(encSecret);
        credential.setEncryptedAccessToken(encAccess);
        credential.setEncryptedRefreshToken(encRefresh);
        credential.setScope(request.scope());
        credential.setExpiresAt(expiresAt);

        OAuthCredential saved = credentialRepository.save(credential);
        log.info("Persisted AES-256 encrypted credential for '{}' with ID: {}", saved.getServiceName(), saved.getId());

        return OAuthCredentialResponse.of(
                saved.getServiceName(),
                saved.getClientId(),
                request.clientSecret(),
                request.accessToken(),
                saved.getScope(),
                saved.getExpiresAt(),
                saved.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public OAuthCredentialResponse getCredential(String serviceName) {
        OAuthCredential cred = credentialRepository.findByServiceName(serviceName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("OAuthCredential", "serviceName", serviceName));

        String decSecret = aesEncryptionService.decrypt(cred.getEncryptedClientSecret());
        String decAccess = aesEncryptionService.decrypt(cred.getEncryptedAccessToken());

        return OAuthCredentialResponse.of(
                cred.getServiceName(),
                cred.getClientId(),
                decSecret,
                decAccess,
                cred.getScope(),
                cred.getExpiresAt(),
                cred.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public RawCredentialDbAuditResponse getRawEncryptedAudit(String serviceName) {
        OAuthCredential cred = credentialRepository.findByServiceName(serviceName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("OAuthCredential", "serviceName", serviceName));

        return new RawCredentialDbAuditResponse(
                cred.getServiceName(),
                cred.getClientId(),
                cred.getEncryptedClientSecret(),
                cred.getEncryptedAccessToken(),
                cred.getEncryptedRefreshToken(),
                "AES-256-GCM (96-bit IV, 128-bit Tag)",
                cred.getUpdatedAt()
        );
    }
}
