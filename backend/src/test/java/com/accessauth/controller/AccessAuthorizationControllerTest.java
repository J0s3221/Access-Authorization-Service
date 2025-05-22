package com.accessauth.controller;

import com.accessauth.domain.Challenge;
import com.accessauth.service.AccessAuthorizationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AccessAuthorizationControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(AccessAuthorizationControllerTest.class);

    private AccessAuthorizationService service;
    private AccessAuthorizationController controller;

    @BeforeEach
    void setUp() {
        logger.info("Setting up mocks and controller...");
        service = mock(AccessAuthorizationService.class);
        controller = new AccessAuthorizationController(service);
    }

    @Test
    void testReturnChallenge_Success() {
        logger.info("Running testReturnChallenge_Success...");
        String userId = "user123";
        Challenge challenge = new Challenge();
        challenge.setChallenge("abc123");

        when(service.generateChallenge(userId)).thenReturn(challenge);

        String result = controller.returnChallenge(userId);

        logger.debug("Expected challenge: abc123, Actual: {}", result);
        assertEquals("abc123", result);

        verify(service).generateChallenge(userId);
        logger.info("testReturnChallenge_Success passed.");
    }

    @Test
    void testReturnChallenge_Failure() {
        logger.info("Running testReturnChallenge_Failure...");
        String userId = "invalid_user";
        when(service.generateChallenge(userId)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                controller.returnChallenge(userId));

        logger.debug("Received exception message: {}", exception.getMessage());
        assertEquals("ID not found or challenge generation failed.", exception.getMessage());
        logger.info("testReturnChallenge_Failure passed.");
    }

    @Test
    void testConfirmSignature_Success() {
        logger.info("Running testConfirmSignature_Success...");
        String signature = "signed123";
        Challenge challenge = new Challenge();
        String expectedResponse = "Access Granted";

        when(service.checkResponse(signature, challenge)).thenReturn(expectedResponse);

        String result = controller.confirmSignature(signature, challenge);

        logger.debug("Expected response: Access Granted, Actual: {}", result);
        assertEquals("Access Granted", result);

        verify(service).checkResponse(signature, challenge);
        logger.info("testConfirmSignature_Success passed.");
    }

    @Test
    void testConfirmSignature_Failure() {
        logger.info("Running testConfirmSignature_Failure...");
        String signature = "invalid";
        Challenge challenge = new Challenge();

        when(service.checkResponse(signature, challenge)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                controller.confirmSignature(signature, challenge));

        logger.debug("Received exception message: {}", exception.getMessage());
        assertEquals("Wrong answer access denied.", exception.getMessage());
        logger.info("testConfirmSignature_Failure passed.");
    }
}
