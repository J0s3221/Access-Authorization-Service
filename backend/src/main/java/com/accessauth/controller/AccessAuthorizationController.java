package com.accessauth.controller;

import com.google.gson.*;
import com.accessauth.service.UserService;
import com.accessauth.service.CryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.concurrent.*;
import jakarta.annotation.PreDestroy;
import java.util.Optional;
import java.nio.charset.StandardCharsets;

import com.accessauth.socket.*;

@Component
public class AccessAuthorizationController {
    private SecureSocketServer server;
    private SecureSocket currentClient;
    
    // Simple single-client state
    private String currentChallenge;
    private String currentSymmetricKey;
    private String currentUserId;
    private String serverState;
    private ScheduledFuture<?> timeoutTask;
    
    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(1);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CryptoService cryptoService;

    @Value("${protocol.server.port:12345}")
    private int serverPort;

    private static final long PROTOCOL_TIMEOUT = 8000; // 8 seconds
    
    // Protocol step constants
    private static final String AUTH_REQUEST = "auth_request";
    private static final String STEP_CHALLENGE = "challenge";
    private static final String CHALLENGE_RESPONSE = "challenge_response";
    private static final String STEP_AUTH_SUCCESS = "auth_success";
    private static final String STEP_AUTH_ERROR = "auth_error";
    private static final String STEP_TIMEOUT = "timeout";
    
    /**
     * Convert string to hexadecimal representation
     * @param input the input string
     * @return hexadecimal string
     */
    private String stringToHex(String input) {
        if (input == null) return null;
        
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Convert hexadecimal string back to regular string
     * @param hexInput the hexadecimal input string
     * @return decoded string
     */
    private String hexToString(String hexInput) {
        if (hexInput == null || hexInput.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hexadecimal string");
        }
        
        byte[] bytes = new byte[hexInput.length() / 2];
        for (int i = 0; i < hexInput.length(); i += 2) {
            String hex = hexInput.substring(i, i + 2);
            bytes[i / 2] = (byte) Integer.parseInt(hex, 16);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @EventListener(ApplicationReadyEvent.class)  
    public void startServer() {
        new Thread(() -> {
            try {
                this.server = new SecureSocketServer(serverPort, "src/main/resources/keystore.jks", "password");
                server.start();
                System.out.println("[Server] Protocol server started on port " + serverPort);

                while (server.isRunning()) {
                    SecureSocket clientSocket = server.acceptConnection();
                    System.out.println("[Server] New client connected");
                    handleClient(clientSocket);
                }
            } catch (Exception e) {
                System.err.println("[Server] Error: " + e.getMessage());
            }
        }).start();
    }

    private void handleClient(SecureSocket clientSocket) {
        // Set current client and reset state
        this.currentClient = clientSocket;
        resetState();
        
        System.out.println("[Server] Client connected, state reset");
        
        // Start listening for incoming messages
        clientSocket.startJsonListening(message -> {
            try {
                handleIncomingMessage(message);
            } catch (Exception e) {
                System.err.println("[Server] Error handling message: " + e.getMessage());
                sendProtocolMessage(STEP_AUTH_ERROR, "Message processing failed", null);
                resetState();
            }
        });
    }
    
    /**
     * Reset all state variables
     */
    private void resetState() {
        currentChallenge = null;
        currentSymmetricKey = null;
        currentUserId = null;
        serverState = null;
        
        // Cancel any existing timeout
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(true);
        }
        
        System.out.println("[Server] State reset");
    }

    /**
     * Handle incoming JSON messages from client based on protocol step type
     * @param message the received JSON message
     */
    private void handleIncomingMessage(JsonObject message) {
        // Cancel any existing timeout task
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(true);
        }
        
        System.out.println("[Server] Received message: " + message.toString());
        
        // Check if message contains "type" field for protocol step
        if (!message.has("type") || message.get("type").isJsonNull()) {
            System.out.println("[Server] Received message without protocol type field");
            sendProtocolMessage(STEP_AUTH_ERROR, "Invalid message format - missing protocol type", null);
            return;
        }
        
        String protocolStep = message.get("type").getAsString();
        System.out.println("[Server] Processing protocol step: " + protocolStep);
        
        switch (protocolStep) {
            case AUTH_REQUEST:
                handleAuthRequest(message);
                break;
            case CHALLENGE_RESPONSE:
                handleChallengeResponse(message);
                break;
            default:
                System.out.println("[Server] Unknown protocol step: " + protocolStep);
                sendProtocolMessage(STEP_AUTH_ERROR, "Unknown protocol step: " + protocolStep, null);
                break;
        }
    }

    /**
     * Handle authentication request (Step 1 of protocol)
     * Expected message format: {"type": "auth_request", "user_id": "hex_encoded_user_id"}
     * @param message the authentication request message
     */
    private void handleAuthRequest(JsonObject message) {
        try {
            // Check if user_id is provided
            if (!message.has("user_id") || message.get("user_id").isJsonNull()) {
                System.out.println("[Server] Auth request missing user_id");
                sendProtocolMessage(STEP_AUTH_ERROR, "Missing user_id in auth_request", null);
                resetState();
                return;
            }
            
            String hexUserId = message.get("user_id").getAsString();
            System.out.println("[Server] Received hex-encoded user ID in auth request: " + hexUserId);
            
            // Decode hex user ID
            String userId;
            try {
                userId = hexToString(hexUserId);
                System.out.println("[Server] Decoded user ID: " + userId);
            } catch (Exception e) {
                System.err.println("[Server] Failed to decode hex user ID: " + e.getMessage());
                sendProtocolMessage(STEP_AUTH_ERROR, "Invalid hexadecimal user ID format", null);
                resetState();
                return;
            }
            
            processAuthRequestForUser(userId);
            
        } catch (Exception e) {
            System.err.println("[Server] Auth request error: " + e.getMessage());
            sendProtocolMessage(STEP_AUTH_ERROR, "Auth request processing failed", null);
            resetState();
        }
    }
    
    /**
     * Process authentication request for a specific user
     * @param userId the decoded user ID
     */
    private void processAuthRequestForUser(String userId) {
        try {
            System.out.println("[Server] Processing auth request for user: " + userId);
            
            // Update state
            serverState = "ID_VERIFICATION";
            currentUserId = userId;

            Long userIdLong;
            try {
                userIdLong = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                System.out.println("[Server] Invalid user ID format: " + userId);
                sendProtocolMessage(STEP_AUTH_ERROR, "Invalid user ID format", null);
                resetState();
                return;
            }

            // Check if user exists in database
            if (!userService.userExists(userIdLong)) {
                System.out.println("[Server] Auth request failed - user not found: " + userIdLong);
                sendProtocolMessage(STEP_AUTH_ERROR, "User not found or inactive", null);
                resetState();
                return;
            }

            // Retrieve symmetric key for user
            Optional<String> symKeyOpt = userService.getSymmetricKey(userIdLong);
            if (!symKeyOpt.isPresent()) {
                System.out.println("[Server] Auth request failed - symmetric key not found for user: " + userIdLong);
                sendProtocolMessage(STEP_AUTH_ERROR, "User authentication data not available", null);
                resetState();
                return;
            }

            currentSymmetricKey = symKeyOpt.get();
            System.out.println("[Server] Retrieved symmetric key for user: " + userId);

            // Generate challenge
            currentChallenge = cryptoService.generateChallenge();
            System.out.println("[Server] Generated challenge for user: " + userId);

            // Encrypt challenge with user's symmetric key
            try {
                String encryptedChallenge = cryptoService.encryptChallenge(currentChallenge, currentSymmetricKey);
                
                // Convert encrypted challenge to hexadecimal
                String hexEncryptedChallenge = stringToHex(encryptedChallenge);
                
                // Send challenge (Step 2 of protocol)
                JsonObject challengeData = new JsonObject();
                challengeData.addProperty("challenge", hexEncryptedChallenge);
                sendProtocolMessage(STEP_CHALLENGE, "Challenge generated", challengeData);
                
                System.out.println("[Server] Sent encrypted challenge (hex) to user: " + userId);
                
                // Set up timeout for challenge response
                serverState = "WAITING_CHALLENGE_RESPONSE";
                startTimeout("challenge response");
                
            } catch (Exception e) {
                System.err.println("[Server] Auth request failed - encryption error for user " + userId + ": " + e.getMessage());
                sendProtocolMessage(STEP_AUTH_ERROR, "Challenge encryption failed", null);
                resetState();
            }

        } catch (Exception e) {
            System.err.println("[Server] Auth request error for user " + currentUserId + ": " + e.getMessage());
            sendProtocolMessage(STEP_AUTH_ERROR, "Auth request processing failed", null);
            resetState();
        }
    }
    
    /**
     * Handle challenge response (Step 3 of protocol)
     * Expected message format: {"type": "challenge_response", "response": "hex_encoded_response"}
     * @param message the response message
     */
    private void handleChallengeResponse(JsonObject message) {
        if (!"WAITING_CHALLENGE_RESPONSE".equals(serverState)) {
            System.err.println("[Server] Received challenge response in wrong state: " + serverState);
            sendProtocolMessage(STEP_AUTH_ERROR, "Unexpected challenge response", null);
            resetState();
            return;
        }
        
        try {
            // Check for response field
            if (!message.has("response") || message.get("response").isJsonNull()) {
                System.out.println("[Server] Challenge response missing response field");
                sendProtocolMessage(STEP_AUTH_ERROR, "Missing response in challenge_response", null);
                resetState();
                return;
            }
            
            String encrypted_hexReceivedDigest = message.get("response").getAsString();
            System.out.println("[Server] Received challenge response (hex) from user: " + currentUserId);
            
            // Convert encrypted hex response back to Base64 digest string
            String receivedDigest;
            try {
                String hexReceivedDigest = cryptoService.decryptChallenge(encrypted_hexReceivedDigest, currentSymmetricKey);
                receivedDigest = hexToString(hexReceivedDigest);
                System.out.println("[Server] Successfully converted hex response to digest string for user: " + currentUserId);
            } catch (Exception e) {
                System.err.println("[Server] Failed to decode hex response for user: " + currentUserId + ": " + e.getMessage());
                sendProtocolMessage(STEP_AUTH_ERROR, "Invalid hexadecimal response format", null);
                resetState();
                return;
            }
            
            // Verify we have the original challenge stored
            if (currentChallenge == null) {
                System.err.println("[Server] No stored challenge found for user: " + currentUserId);
                sendProtocolMessage(STEP_AUTH_ERROR, "Challenge verification failed - no stored challenge", null);
                resetState();
                return;
            }
            
            // Compute the expected digest of the original challenge
            String expectedDigest;
            try {
                expectedDigest = cryptoService.computeChallengeDigest(currentChallenge);
                System.out.println("[Server] Computed expected digest for challenge verification for user: " + currentUserId);
            } catch (Exception e) {
                System.err.println("[Server] Failed to compute challenge digest for user: " + currentUserId + ": " + e.getMessage());
                sendProtocolMessage(STEP_AUTH_ERROR, "Challenge digest computation failed", null);
                resetState();
                return;
            }
            
            // Compare the received digest with the expected digest
            if (!expectedDigest.equals(receivedDigest)) {
                System.err.println("[Server] Challenge verification failed for user: " + currentUserId);
                System.err.println("[Server] Expected digest: " + expectedDigest);
                System.err.println("[Server] Received digest: " + receivedDigest);
                sendProtocolMessage(STEP_AUTH_ERROR, "Challenge verification failed", null);
                resetState();
                return;
            }
            
            // Challenge verification successful (Step 4 of protocol)
            System.out.println("[Server] Challenge response verified successfully for user: " + currentUserId);
            serverState = "AUTHENTICATED";
            
            // Send success response with hex-encoded data
            JsonObject successData = new JsonObject();
            successData.addProperty("user_id", stringToHex(currentUserId));
            successData.addProperty("timestamp", stringToHex(String.valueOf(System.currentTimeMillis())));
            sendProtocolMessage(STEP_AUTH_SUCCESS, "Authentication successful", successData);
            
            System.out.println("[Server] Authentication completed successfully for user: " + currentUserId);
            
            // Clear sensitive data
            currentChallenge = null;
            currentSymmetricKey = null;
            
        } catch (Exception e) {
            System.err.println("[Server] Error processing challenge response: " + e.getMessage());
            sendProtocolMessage(STEP_AUTH_ERROR, "Challenge response processing failed", null);
            resetState();
        }
    }
    
    /**
     * Start a timeout timer for the current operation
     * @param operation description of the operation being timed
     */
    private void startTimeout(String operation) {
        // Cancel any existing timeout
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(true);
        }
        
        // Start new timeout
        timeoutTask = timeoutExecutor.schedule(() -> {
            System.out.println("[Server] Timeout waiting for " + operation + " from user: " + currentUserId);
            sendProtocolMessage(STEP_TIMEOUT, "Timeout waiting for " + operation, null);
            resetState();
        }, PROTOCOL_TIMEOUT, TimeUnit.MILLISECONDS);
        
        System.out.println("[Server] Started " + PROTOCOL_TIMEOUT + "ms timeout for " + operation);
    }

    /**
     * Send a protocol message with structured format
     * @param protocolStep the protocol step type
     * @param message the message content
     * @param data additional data payload (can be null)
     */
    private void sendProtocolMessage(String protocolStep, String message, JsonObject data) {
        if (currentClient != null) {
            if (data != null) {
                data.addProperty("type", protocolStep);
                data.addProperty("timestamp", stringToHex(String.valueOf(System.currentTimeMillis()))); // Encode timestamp in hex
            }
            
            currentClient.sendJson(data);
            System.out.println("[Server] Sent protocol message - type: " + protocolStep);
        }
    }
    
    /**
     * Cleanup method called when the application shuts down
     */
    @PreDestroy
    public void cleanup() {
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdown();
            try {
                if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    timeoutExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                timeoutExecutor.shutdownNow();
            }
        }
    }
}