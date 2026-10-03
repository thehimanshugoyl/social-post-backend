package com.himanshu.social_post_backend.service;

import com.himanshu.social_post_backend.common.exception.BadRequestException;
import com.himanshu.social_post_backend.common.exception.DuplicateResourceException;
import com.himanshu.social_post_backend.dto.request.LoginRequest;
import com.himanshu.social_post_backend.dto.request.RegisterRequest;
import com.himanshu.social_post_backend.dto.response.AuthResponse;
import com.himanshu.social_post_backend.model.RefreshToken;
import com.himanshu.social_post_backend.model.User;
import com.himanshu.social_post_backend.model.UserRole;
import com.himanshu.social_post_backend.repository.UserRepository;
import com.himanshu.social_post_backend.security.JwtService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenLifecycleService tokenLifecycleService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenLifecycleService tokenLifecycleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenLifecycleService = tokenLifecycleService;
    }

    @PostConstruct
    public void seedInitialUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", "admin@socialpost.cu.in", passwordEncoder.encode("Admin@123"), UserRole.ROLE_ADMIN);
            userRepository.save(admin);
            log.info("Initialized default ADMIN user: 'admin'");
        }

        if (!userRepository.existsByUsername("user")) {
            User standardUser = new User("user", "user@socialpost.cu.in", passwordEncoder.encode("User@123"), UserRole.ROLE_USER);
            userRepository.save(standardUser);
            log.info("Initialized default USER: 'user'");
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already registered");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already registered");
        }

        UserRole role = request.role() != null ? request.role() : UserRole.ROLE_USER;
        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                role
        );
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {} and role: {}", savedUser.getId(), savedUser.getRole());

        String accessToken = jwtService.generateAccessToken(savedUser.getUsername(), savedUser.getRole().name(), savedUser.getId());
        RefreshToken refreshToken = tokenLifecycleService.createRefreshToken(savedUser.getUsername());

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationSeconds(),
                savedUser.getUsername(),
                savedUser.getRole().name(),
                savedUser.getId()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadRequestException("Invalid credentials. Username or password incorrect."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequestException("Invalid credentials. Username or password incorrect.");
        }

        if (!user.isEnabled()) {
            throw new BadRequestException("Account is disabled. Contact system administrator.");
        }

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name(), user.getId());
        RefreshToken refreshToken = tokenLifecycleService.createRefreshToken(user.getUsername());

        log.info("User '{}' authenticated successfully. JWT and Refresh Token issued.", user.getUsername());

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationSeconds(),
                user.getUsername(),
                user.getRole().name(),
                user.getId()
        );
    }
}
