package com.himanshu.social_post_backend.controller;

import com.himanshu.social_post_backend.common.api.ApiResponse;
import com.himanshu.social_post_backend.dto.request.StoreOAuthCredentialRequest;
import com.himanshu.social_post_backend.dto.response.OAuthCredentialResponse;
import com.himanshu.social_post_backend.dto.response.RawCredentialDbAuditResponse;
import com.himanshu.social_post_backend.service.OAuthCredentialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/credentials")
public class OAuthCredentialController {

    private final OAuthCredentialService credentialService;

    public OAuthCredentialController(OAuthCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OAuthCredentialResponse>> storeCredential(
            @Valid @RequestBody StoreOAuthCredentialRequest request) {
        OAuthCredentialResponse response = credentialService.storeCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "OAuth credentials securely encrypted and persisted using AES-256-GCM"));
    }

    @GetMapping("/{serviceName}")
    public ResponseEntity<ApiResponse<OAuthCredentialResponse>> getCredential(@PathVariable String serviceName) {
        OAuthCredentialResponse response = credentialService.getCredential(serviceName);
        return ResponseEntity.ok(ApiResponse.ok(response, "Decrypted OAuth credentials retrieved successfully"));
    }

    @GetMapping("/{serviceName}/raw-audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RawCredentialDbAuditResponse>> getRawEncryptedAudit(@PathVariable String serviceName) {
        RawCredentialDbAuditResponse audit = credentialService.getRawEncryptedAudit(serviceName);
        return ResponseEntity.ok(ApiResponse.ok(audit, "Database ciphertext audit retrieved. Demonstrates zero plaintext exposure at rest."));
    }
}
