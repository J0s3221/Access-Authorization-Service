package com.accessauth.service;

import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {
   
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String DIGEST_ALGORITHM = "SHA-256";
   
    /**
     * Generate a random challenge
     * @return Base64 encoded challenge string
     */
    public String generateChallenge() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32]; // 256-bit challenge
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
   
    /**
     * Encrypt challenge using symmetric key
     * @param challenge the challenge to encrypt
     * @param symmetricKey the symmetric key (Base64 encoded)
     * @return encrypted challenge as Base64 string
     * @throws Exception if encryption fails
     */
    public String encryptChallenge(String challenge, String symmetricKey) throws Exception {
        try {
            // Decode the symmetric key from Base64
            byte[] keyBytes = Base64.getDecoder().decode(symmetricKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
           
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
           
            // Encrypt the challenge
            byte[] challengeBytes = challenge.getBytes("UTF-8");
            byte[] encryptedBytes = cipher.doFinal(challengeBytes);
           
            // Return as Base64 encoded string
            return Base64.getEncoder().encodeToString(encryptedBytes);
           
        } catch (Exception e) {
            throw new Exception("Failed to encrypt challenge: " + e.getMessage(), e);
        }
    }
   
    /**
     * Decrypt challenge using symmetric key (for verification purposes)
     * @param encryptedChallenge the encrypted challenge (Base64 encoded)
     * @param symmetricKey the symmetric key (Base64 encoded)
     * @return decrypted challenge string
     * @throws Exception if decryption fails
     */
    public String decryptChallenge(String encryptedChallenge, String symmetricKey) throws Exception {
        try {
            // Decode the symmetric key from Base64
            byte[] keyBytes = Base64.getDecoder().decode(symmetricKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
           
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
           
            // Decrypt the challenge
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedChallenge);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
           
            // Return as string
            return new String(decryptedBytes, "UTF-8");
           
        } catch (Exception e) {
            throw new Exception("Failed to decrypt challenge: " + e.getMessage(), e);
        }
    }
   
    /**
     * Compute digest of a challenge for challenge-response authentication
     * @param challenge the challenge string to compute digest for
     * @return Base64 encoded digest of the challenge
     * @throws Exception if digest computation fails
     */
    public String computeChallengeDigest(String challenge) throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] challengeBytes = challenge.getBytes("UTF-8");
            byte[] digestBytes = digest.digest(challengeBytes);
            return Base64.getEncoder().encodeToString(digestBytes);
        } catch (Exception e) {
            throw new Exception("Failed to compute challenge digest: " + e.getMessage(), e);
        }
    }
   
    /**
     * Generate a new symmetric key
     * @return Base64 encoded symmetric key
     */
    public String generateSymmetricKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256-bit key
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}