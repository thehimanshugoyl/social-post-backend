package com.himanshu.social_post_backend;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.himanshu.social_post_backend.dto.request.LoginRequest;
import com.himanshu.social_post_backend.dto.request.RegisterRequest;
import com.himanshu.social_post_backend.model.UserRole;
import com.himanshu.social_post_backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SecurityAndJwtIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("EXP 2.3.1: POST /api/v1/auth/register - Register new user and receive valid JWT")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "developer_alice",
                "alice@socialpost.cu.in",
                "SecureAlice123!",
                UserRole.ROLE_USER
        );

        String responseStr = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("developer_alice")))
                .andExpect(jsonPath("$.data.role", is("ROLE_USER")))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(responseStr);
        String token = jsonNode.get("data").get("accessToken").asText();

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("developer_alice", jwtService.extractUsername(token));
        assertEquals("ROLE_USER", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("EXP 2.3.1: POST /api/v1/auth/login - Successfully authenticate default user and admin")
    void shouldLoginUsersSuccessfully() throws Exception {
        // 1. Authenticate standard user
        LoginRequest userLogin = new LoginRequest("user", "User@123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("user")))
                .andExpect(jsonPath("$.data.role", is("ROLE_USER")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));

        // 2. Authenticate admin user
        LoginRequest adminLogin = new LoginRequest("admin", "Admin@123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("admin")))
                .andExpect(jsonPath("$.data.role", is("ROLE_ADMIN")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));
    }

    @Test
    @DisplayName("EXP 2.3.1: POST /api/v1/auth/login - Fail with 400 Bad Request on invalid credentials")
    void shouldRejectInvalidCredentials() throws Exception {
        LoginRequest invalidLogin = new LoginRequest("user", "WrongPassword999!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid credentials")));
    }

    @Test
    @DisplayName("EXP 2.3.1: RBAC 403 Forbidden - Standard USER denied access to ADMIN dashboard")
    void shouldDenyUserAccessToAdminEndpoints() throws Exception {
        // Login as standard user to obtain JWT
        LoginRequest userLogin = new LoginRequest("user", "User@123");
        String loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andReturn().getResponse().getContentAsString();

        String userJwt = objectMapper.readTree(loginRes).get("data").get("accessToken").asText();

        // Standard user can access their own profile (200 OK)
        mockMvc.perform(get("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("user")));

        // Attempting to access ADMIN dashboard must be rejected with 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + userJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access is denied")));
    }

    @Test
    @DisplayName("EXP 2.3.1: RBAC 200 OK - ADMIN granted access to dashboard and user registry")
    void shouldAllowAdminAccessToAdminEndpoints() throws Exception {
        // Login as admin
        LoginRequest adminLogin = new LoginRequest("admin", "Admin@123");
        String loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andReturn().getResponse().getContentAsString();

        String adminJwt = objectMapper.readTree(loginRes).get("data").get("accessToken").asText();

        // Admin accesses dashboard successfully
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.securityAudit", is("RBAC_ACTIVE")))
                .andExpect(jsonPath("$.data.systemStatus", is("OPERATIONAL")));

        // Admin accesses user audit registry
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("EXP 2.3.1: Authentication 401 Unauthorized - Unauthenticated request to protected endpoint")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized: Authentication token is missing or invalid")));
    }
}
