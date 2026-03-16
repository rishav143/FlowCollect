package com.flowcollect.application.organization;

import com.flowcollect.exception.http.InternalException;
import com.flowcollect.infrastructure.config.GatewayEncryptionProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for Razorpay API credentials stored at rest.
 *
 * Format of the stored ciphertext (Base64 encoded):
 *   [ 12-byte IV ][ encrypted bytes ][ 16-byte GCM auth tag ]
 *
 * The same platform key encrypts every organization's credentials.
 * Each encrypt call generates a fresh random IV so two identical plaintexts
 * produce different ciphertexts.
 */
@Service
public class GatewayCredentialEncryption {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;

    public GatewayCredentialEncryption(GatewayEncryptionProperties props) {
        String rawKey = props.getKey();
        if (rawKey == null || rawKey.length() < 32) {
            throw new IllegalStateException(
                    "gateway.encryption.key must be at least 32 characters. " +
                    "Set the GATEWAY_ENCRYPTION_KEY environment variable.");
        }
        byte[] keyBytes = rawKey.substring(0, 32).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts a plaintext credential value.
     *
     * @return Base64-encoded ciphertext containing IV + encrypted data.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] combined = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new InternalException("Failed to encrypt gateway credential: " + e.getMessage());
        }
    }

    /**
     * Decrypts a Base64-encoded ciphertext back to the original plaintext.
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            ByteBuffer buffer = ByteBuffer.wrap(combined);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new InternalException("Failed to decrypt gateway credential: " + e.getMessage());
        }
    }
}
