package com.himanshu.social_post_backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Enterprise AES-256-GCM authenticated symmetric encryption service.
 * Employs Galois/Counter Mode (GCM) with 128-bit authentication tag and random 96-bit IV
 * for cryptographic confidentiality and tampering detection at rest.
 */
@Service
public class AesEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptionService.class);
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // 128 bit authentication tag

    private final byte[] keyBytes;
    private final SecureRandom secureRandom;

    public AesEncryptionService(
            @Value("${security.aes.secret-key:cu-himanshu-aes256-key-32bytes!}") String secretKey) {
        try {
            // Derive a deterministic 256-bit (32 byte) key via SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            this.keyBytes = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            this.secureRandom = new SecureRandom();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize AES key derivation", e);
        }
    }

    /**
     * Encrypts plaintext into a Base64-encoded payload containing IV + ciphertext + authentication tag.
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherBytes);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("AES encryption failed: {}", e.getMessage());
            throw new IllegalStateException("Failed to encrypt sensitive data", e);
        }
    }

    /**
     * Decrypts Base64-encoded IV + ciphertext payload back to UTF-8 plaintext.
     */
    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null) {
            return null;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(cipherTextBase64);

            if (decoded.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Ciphertext payload too short for GCM parameters");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES decryption failed: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to decrypt ciphertext or authentication tag mismatch", e);
        }
    }
}
