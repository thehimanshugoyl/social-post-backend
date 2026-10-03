package com.himanshu.social_post_backend.controller;

import com.himanshu.social_post_backend.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> profile = Map.of(
                "username", auth.getName(),
                "authorities", auth.getAuthorities(),
                "status", "ACTIVE",
                "message", "Authenticated user profile accessed securely via JWT"
        );
        return ResponseEntity.ok(ApiResponse.ok(profile, "Profile retrieved successfully"));
    }
}
