package com.himanshu.social_post_backend.controller;

import com.himanshu.social_post_backend.common.api.ApiResponse;
import com.himanshu.social_post_backend.dto.request.LoginRequest;
import com.himanshu.social_post_backend.dto.request.RefreshTokenRequest;
import com.himanshu.social_post_backend.dto.request.RegisterRequest;
import com.himanshu.social_post_backend.dto.response.AuthResponse;
import com.himanshu.social_post_backend.service.AuthService;
import com.himanshu.social_post_backend.service.TokenLifecycleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenLifecycleService tokenLifecycleService;

    public AuthController(AuthService authService, TokenLifecycleService tokenLifecycleService) {
        this.authService = authService;
        this.tokenLifecycleService = tokenLifecycleService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Authentication successful. JWT access token issued."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = tokenLifecycleService.rotateRefreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response, "Token successfully rotated. New access token and refresh token generated."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        tokenLifecycleService.revokeToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Revoked", "Refresh token successfully revoked. Session terminated."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.ok(null, "Unauthenticated"));
        }
        Map<String, Object> details = Map.of(
                "username", auth.getName(),
                "authorities", auth.getAuthorities(),
                "authenticated", auth.isAuthenticated()
        );
        return ResponseEntity.ok(ApiResponse.ok(details, "Authenticated user profile"));
    }
}
