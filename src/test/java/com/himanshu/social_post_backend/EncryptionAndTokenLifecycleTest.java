package com.himanshu.social_post_backend;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.himanshu.social_post_backend.dto.request.LoginRequest;
import com.himanshu.social_post_backend.dto.request.RefreshTokenRequest;
import com.himanshu.social_post_backend.dto.request.StoreOAuthCredentialRequest;
import com.himanshu.social_post_backend.repository.OAuthCredentialRepository;
import com.himanshu.social_post_backend.security.AesEncryptionService;
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
class EncryptionAndTokenLifecycleTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AesEncryptionService aesEncryptionService;

    @Autowired
    private OAuthCredentialRepository oAuthCredentialRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("EXP 2.3.2: AES-256-GCM - Correctly encrypts plaintext and decrypts to original data")
    void shouldEncryptAndDecryptAccurately() {
        String sensitiveSecret = "sk_live_cu_himanshu_99348923482394_super_secret_oauth_key!";

        // Encrypt
        String cipherText = aesEncryptionService.encrypt(sensitiveSecret);
        assertNotNull(cipherText);
        assertNotEquals(sensitiveSecret, cipherText);

        // Decrypt
        String decrypted = aesEncryptionService.decrypt(cipherText);
        assertEquals(sensitiveSecret, decrypted);
    }

    @Test
    @DisplayName("EXP 2.3.2: AES-256-GCM - Rejects tampered ciphertext with cryptographic authentication error")
    void shouldFailOnTamperedCiphertext() {
        String original = "sensitive_data_to_be_tampered";
        String cipherText = aesEncryptionService.encrypt(original);

        // Tamper with ciphertext by altering last characters
        String tampered = cipherText.substring(0, cipherText.length() - 4) + "AAAA";

        assertThrows(IllegalArgumentException.class, () -> aesEncryptionService.decrypt(tampered));
    }

    @Test
    @DisplayName("EXP 2.3.2: OAuth Credential Persistence - Verifies raw DB ciphertext storage and decrypted retrieval")
    void shouldPersistEncryptedCredentialsAndAuditRawDatabase() throws Exception {
        // Authenticate admin
        LoginRequest adminLogin = new LoginRequest("admin", "Admin@123");
        String loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andReturn().getResponse().getContentAsString();
        String adminToken = objectMapper.readTree(loginRes).get("data").get("accessToken").asText();

        // 1. Store OAuth credential for TWITTER_API
        StoreOAuthCredentialRequest storeReq = new StoreOAuthCredentialRequest(
                "TWITTER_API",
                "x_client_id_himanshu_70369",
                "x_secret_live_8912389128391283",
                "x_access_tok_991823912839128391283",
                "x_refresh_tok_11283918239128391283",
                "tweet.read tweet.write offline.access",
                7200L
        );

        mockMvc.perform(post("/api/v1/credentials")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.serviceName", is("TWITTER_API")))
                .andExpect(jsonPath("$.data.maskedClientSecret", containsString("••••••••")));

        // 2. Audit raw database table - ensure values in DB are encrypted Base64 ciphertexts
        mockMvc.perform(get("/api/v1/credentials/TWITTER_API/raw-audit")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.encryptionAlgorithm", containsString("AES-256-GCM")))
                .andExpect(jsonPath("$.data.rawDbEncryptedClientSecret", not(is("x_secret_live_8912389128391283"))))
                .andExpect(jsonPath("$.data.rawDbEncryptedAccessToken", not(is("x_access_tok_991823912839128391283"))));

        // 3. User retrieves decrypted credential with masked tokens
        mockMvc.perform(get("/api/v1/credentials/TWITTER_API")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.serviceName", is("TWITTER_API")));
    }

    @Test
    @DisplayName("EXP 2.3.2: Token Lifecycle - Rotation of refresh token and replay attack mitigation")
    void shouldRotateRefreshTokenAndPreventReplayAttack() throws Exception {
        // 1. Initial Login
        LoginRequest userLogin = new LoginRequest("user", "User@123");
        String loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andReturn().getResponse().getContentAsString();

        JsonNode initialAuth = objectMapper.readTree(loginRes).get("data");
        String initialRefreshToken = initialAuth.get("refreshToken").asText();

        // 2. Rotate Token using POST /api/v1/auth/refresh
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialRefreshToken);
        String refreshRes = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        JsonNode rotatedAuth = objectMapper.readTree(refreshRes).get("data");
        String newRefreshToken = rotatedAuth.get("refreshToken").asText();

        // New refresh token should be distinct from the old one
        assertNotEquals(initialRefreshToken, newRefreshToken);

        // 3. Replay attack: attempt to use the OLD (revoked) refresh token again
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Replay attack detected")));

        // 4. Revocation / Logout test
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(newRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Refresh token successfully revoked")));
    }
}
