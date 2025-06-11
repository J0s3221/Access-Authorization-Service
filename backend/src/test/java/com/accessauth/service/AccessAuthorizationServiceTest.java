package com.accessauth.service;

import com.accessauth.repository.AccessKey;
import com.accessauth.repository.AccessKeyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessAuthorizationServiceTest {

    private AccessKeyRepository repository;
    private  CryptoService cryptoService;
    private static final Logger logger = LoggerFactory.getLogger(AccessAuthorizationServiceTest.class);

    @BeforeEach
    void setUp() {
        repository = mock(AccessKeyRepository.class);
    }

    @Test
    @DisplayName("generateChallenge returns a valid Challenge when ID exists")
    void testGenerateChallenge_Success() {
        String id = "abc123";
        String publicKey = "samplePublicKey";
        AccessKey accessKey = new AccessKey(id, publicKey);

        when(repository.findById(id)).thenReturn(Optional.of(accessKey));

        String challenge = cryptoService.generateChallenge();

        assertNotNull(challenge, "Challenge should not be null");
        assertNotNull(challenge, "Challenge string should not be null");
        assertFalse(challenge.isEmpty(), "Challenge string should not be empty");
        logger.info("Generated challenge: {}", challenge);
    }

    @Test
    @DisplayName("generateChallenge returns null when ID is not found")
    void testGenerateChallenge_IdNotFound() {
        String id = "nonexistent";

        when(repository.findById(id)).thenReturn(Optional.empty());

        String challenge = cryptoService.generateChallenge();

        assertNull(challenge, "Challenge should be null when ID is not found");
    }

    @Nested
    @DisplayName("Edge case tests for generateChallenge")
    class GenerateChallengeEdgeCases {

        @ParameterizedTest(name = "ID: \"{0}\" exists: {1}")
        @CsvSource({
            "null,false",
            "'',false",
            "'   ',false",
            "valid123,true"
        })
        void testEdgeCases(String id, boolean shouldExist) {
            if (id != null && shouldExist) {
                when(repository.findById(id)).thenReturn(Optional.of(new AccessKey(id, "key")));
            } else {
                when(repository.findById(id)).thenReturn(Optional.empty());
            }

            String challenge = cryptoService.generateChallenge();
            assertEquals(shouldExist, challenge != null);
        }
    }
}
