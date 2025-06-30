package com.accessauth.service;

import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;

@Service
public class CryptoService {
   
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/NoPadding";
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
     * Encrypt challenge using symmetric key with IV
     * @param challenge the challenge to encrypt
     * @param symmetricKey the symmetric key (Base64 encoded)
     * @return encrypted challenge as Base64 string (IV + encrypted data)
     * @throws Exception if encryption fails
     */
    public String encryptChallenge(String challenge, String symmetricKey) throws Exception {
        try {
            // Decode the symmetric key from Base64
            byte[] keyBytes = Base64.getDecoder().decode(symmetricKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            
            // Generate random IV (16 bytes for AES)
            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION); // Should be "AES/CBC/NoPadding"
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            
            // Convert challenge to bytes and ensure proper padding for NoPadding mode
            byte[] challengeBytes = challenge.getBytes("UTF-8");
            byte[] paddedInput = padToBlockSize(challengeBytes);
            
            // Encrypt the challenge
            byte[] encryptedBytes = cipher.doFinal(paddedInput);
            
            // Combine IV + encrypted data
            byte[] result = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, result, iv.length, encryptedBytes.length);
            
            // Return as Base64 encoded string
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new Exception("Failed to encrypt challenge: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt challenge using symmetric key (for verification purposes)
     * @param encryptedChallenge the encrypted challenge (Base64 encoded, IV + encrypted data)
     * @param symmetricKey the symmetric key (Base64 encoded)
     * @return decrypted challenge string
     * @throws Exception if decryption fails
     */
    public String decryptChallenge(String encryptedChallenge, String symmetricKey) throws Exception {
        try {
            // Decode the symmetric key from Base64
            byte[] keyBytes = Base64.getDecoder().decode(symmetricKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            
            // Decode the encrypted data from Base64
            byte[] encryptedData = Base64.getDecoder().decode(encryptedChallenge);
            
            // Extract IV (first 16 bytes) and ciphertext (remaining bytes)
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, 16);
            byte[] ciphertext = Arrays.copyOfRange(encryptedData, 16, encryptedData.length);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION); // Should be "AES/CBC/NoPadding"
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            
            // Decrypt the challenge
            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            
            // Remove padding and return as string
            byte[] unpaddedBytes = removePadding(decryptedBytes);
            return new String(unpaddedBytes, "UTF-8");
        } catch (Exception e) {
            throw new Exception("Failed to decrypt challenge: " + e.getMessage(), e);
        }
    }

    /**
     * Pad data to AES block size (16 bytes) for NoPadding mode
     * Uses PKCS7 padding
     */
    private byte[] padToBlockSize(byte[] data) {
        int blockSize = 16; // AES block size
        int paddingLength = blockSize - (data.length % blockSize);
        
        byte[] paddedData = new byte[data.length + paddingLength];
        System.arraycopy(data, 0, paddedData, 0, data.length);
        
        // PKCS7 padding: fill with the padding length value
        for (int i = data.length; i < paddedData.length; i++) {
            paddedData[i] = (byte) paddingLength;
        }
        
        return paddedData;
    }

    /**
     * Remove PKCS7 padding from decrypted data
     */
    private byte[] removePadding(byte[] paddedData) {
        if (paddedData.length == 0) {
            return paddedData;
        }
        
        int paddingLength = paddedData[paddedData.length - 1] & 0xFF;
        
        // Validate padding
        if (paddingLength > 16 || paddingLength > paddedData.length) {
            return paddedData; // Invalid padding, return as is
        }
        
        // Check if all padding bytes are correct
        for (int i = paddedData.length - paddingLength; i < paddedData.length; i++) {
            if ((paddedData[i] & 0xFF) != paddingLength) {
                return paddedData; // Invalid padding, return as is
            }
        }
        
        // Remove padding
        byte[] unpaddedData = new byte[paddedData.length - paddingLength];
        System.arraycopy(paddedData, 0, unpaddedData, 0, unpaddedData.length);
        
        return unpaddedData;
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