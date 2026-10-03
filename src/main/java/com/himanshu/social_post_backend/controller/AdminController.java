package com.himanshu.social_post_backend.controller;

import com.himanshu.social_post_backend.common.api.ApiResponse;
import com.himanshu.social_post_backend.model.User;
import com.himanshu.social_post_backend.repository.CommentRepository;
import com.himanshu.social_post_backend.repository.PostRepository;
import com.himanshu.social_post_backend.repository.UserRepository;
import com.himanshu.social_post_backend.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostService postService;

    public AdminController(PostRepository postRepository, CommentRepository commentRepository, UserRepository userRepository, PostService postService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.postService = postService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminDashboard() {
        long totalPosts = postRepository.count();
        long totalComments = commentRepository.count();
        long totalUsers = userRepository.count();

        Map<String, Object> metrics = Map.of(
                "totalPosts", totalPosts,
                "totalComments", totalComments,
                "registeredUsers", totalUsers,
                "systemStatus", "OPERATIONAL",
                "securityAudit", "RBAC_ACTIVE"
        );

        return ResponseEntity.ok(ApiResponse.ok(metrics, "Admin dashboard metrics retrieved successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "email", u.getEmail(),
                        "role", u.getRole().name(),
                        "enabled", u.isEnabled()
                )).toList();

        return ResponseEntity.ok(ApiResponse.ok(users, "User registry retrieved for admin audit"));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<String>> administrativeDeletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted", "Post ID " + id + " purged by administrator"));
    }
}
