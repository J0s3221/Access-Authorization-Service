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
import java.util.Map;
import java.util.Optional;

import com.accessauth.socket.*;

@Component
public class AccessAuthorizationController {
    private SecureSocketServer server;
    
    // Track client states and timeouts
    private final Map<SecureSocket, ClientState> clientStates = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(10);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CryptoService cryptoService;

    @Value("${protocol.server.port:12345}")
    private int serverPort;

    private static final long PROTOCOL_TIMEOUT = 8000; // 8 seconds
    
    // Client state tracking
    private static class ClientState {
        private String currentLayer;
        private String userId;
        private String challenge;
        private long lastActivity;
        private ScheduledFuture<?> timeoutTask;
        
        public ClientState() {
            this.lastActivity = System.currentTimeMillis();
        }
        
        public void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
        
        public boolean isTimedOut() {
            return (System.currentTimeMillis() - lastActivity) > PROTOCOL_TIMEOUT;
        }
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

                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (Exception e) {
                System.err.println("[Server] Error: " + e.getMessage());
            }
        }).start();
    }

    private void handleClient(SecureSocket clientSocket) {
        // Initialize client state
        ClientState clientState = new ClientState();
        clientStates.put(clientSocket, clientState);
        
        System.out.println("[Server] Client connected, initialized state tracking");
        
        // Start listening for incoming messages
        clientSocket.startJsonListening(message -> {
            try {
                handleIncomingMessage(clientSocket, message);
            } catch (Exception e) {
                System.err.println("[Server] Error handling message: " + e.getMessage());
                sendMessage(clientSocket, "error", "Message processing failed");
                cleanupClient(clientSocket);
            }
        });
        
        // Set up connection cleanup on disconnect
        // Note: This depends on your SecureSocket implementation having a disconnect callback
        // You might need to adapt this based on your actual SecureSocket API
    }
    
    /**
     * Clean up client state and cancel any pending timeouts
     */
    private void cleanupClient(SecureSocket clientSocket) {
        ClientState state = clientStates.get(clientSocket);
        if (state != null) {
            if (state.timeoutTask != null && !state.timeoutTask.isDone()) {
                state.timeoutTask.cancel(true);
            }
            clientStates.remove(clientSocket);
            ClientState clientState = new ClientState();
            clientStates.put(clientSocket, clientState);
            System.out.println("[Server] Cleaned up client state");
        }
    }

    /**
     * Handle incoming JSON messages from client
     * @param clientSocket the client socket
     * @param message the received JSON message
     */
    private void handleIncomingMessage(SecureSocket clientSocket, JsonObject message) {
        ClientState clientState = clientStates.get(clientSocket);
        if (clientState == null) {
            System.err.println("[Server] No client state found for socket");
            return;
        }
        
        // Update client activity
        clientState.updateActivity();
        
        // Cancel any existing timeout task
        if (clientState.timeoutTask != null && !clientState.timeoutTask.isDone()) {
            clientState.timeoutTask.cancel(true);
        }
        
        System.out.println("[Server] Received message: " + message.toString());
        
        // Check if message contains "id" key - this starts the authentication process
        if (message.has("id") && !message.get("id").isJsonNull()) {
            String userId = message.get("id").getAsString();
            System.out.println("[Server] Processing ID handler for user: " + userId);
            idHandler(clientSocket, userId);
        } 
        // Check if this is a challenge response
        else if (message.has("challenge_response") && !message.get("challenge_response").isJsonNull()) {
            handleChallengeResponse(clientSocket, message);
        }
        // Handle other message types as needed
        else {
            System.out.println("[Server] Received message without recognized fields");
            sendMessage(clientSocket, "info", "Message received but no recognized action found");
        }
    }

    /**
     * Handle ID verification and challenge generation for a specific user
     * @param clientSocket the client socket
     * @param userId the user ID to process
     */
    private void idHandler(SecureSocket clientSocket, String userId) {
        ClientState clientState = clientStates.get(clientSocket);
        if (clientState == null) {
            System.err.println("[Server] No client state found for ID handler");
            return;
        }
        
        try {
            System.out.println("[Server] Executing ID Handler for user: " + userId);
            
            // Update client state
            clientState.currentLayer = "ID_VERIFICATION";
            clientState.userId = userId;

            Long userIdLong;
            try {
                userIdLong = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                System.out.println("[Server] Invalid user ID format: " + userId);
                sendMessage(clientSocket, "error", "Invalid user ID format");
                cleanupClient(clientSocket);
                return;
            }


            // Check if user exists in database
            if (!userService.userExists(userIdLong)) {
                System.out.println("[Server] ID Handler failed - user not found: " + userIdLong);
                sendMessage(clientSocket, "error", "User not found or inactive");
                cleanupClient(clientSocket);
                return;
            }

            Optional<String> symKeyOpt = userService.getSymmetricKey(userIdLong);
            if (!symKeyOpt.isPresent()) {
                System.out.println("[Server] ID Handler failed - symmetric key not found for user: " + userIdLong);
                sendMessage(clientSocket, "error", "User authentication data not available");
                cleanupClient(clientSocket);
                return;
            }

            String symKey = symKeyOpt.get();
            System.out.println("[Server] Retrieved symmetric key for user: " + userId);

            // Generate challenge
            String challenge = cryptoService.generateChallenge();
            clientState.challenge = challenge; // Store challenge for verification
            System.out.println("[Server] Generated challenge for user: " + userId);

            // Encrypt challenge with user's symmetric key
            try {
                String encryptedChallenge = cryptoService.encryptChallenge(challenge, symKey);
                
                // Send encrypted challenge to client
                JsonObject challengeMessage = new JsonObject();
                challengeMessage.addProperty("type", "challenge");
                challengeMessage.addProperty("challenge", encryptedChallenge);
                clientSocket.sendJson(challengeMessage);
                
                System.out.println("[Server] Sent encrypted challenge to user: " + userId);
                
                // Set up timeout for challenge response
                clientState.currentLayer = "WAITING_CHALLENGE_RESPONSE";
                startTimeout(clientSocket, "challenge response");
                
            } catch (Exception e) {
                System.err.println("[Server] ID Handler failed - encryption error for user " + userId + ": " + e.getMessage());
                sendMessage(clientSocket, "error", "Challenge encryption failed");
                cleanupClient(clientSocket);
            }

        } catch (Exception e) {
            System.err.println("[Server] ID Handler error for user " + userId + ": " + e.getMessage());
            sendMessage(clientSocket, "error", "ID Handler processing failed");
            cleanupClient(clientSocket);
        }
    }
    
    /**
     * Handle challenge response from client
     * @param clientSocket the client socket
     * @param message the response message
     */
    private void handleChallengeResponse(SecureSocket clientSocket, JsonObject message) {
        ClientState clientState = clientStates.get(clientSocket);
        if (clientState == null) {
            System.err.println("[Server] No client state found for challenge response");
            return;
        }
        
        if (!"WAITING_CHALLENGE_RESPONSE".equals(clientState.currentLayer)) {
            System.err.println("[Server] Received challenge response in wrong state: " + clientState.currentLayer);
            sendMessage(clientSocket, "error", "Unexpected challenge response");
            cleanupClient(clientSocket);
            return;
        }
        
        try {
            String challengeResponse = message.get("challenge_response").getAsString();
            System.out.println("[Server] Received challenge response from user: " + clientState.userId);
            
            // Here you would verify the challenge response
            // For now, we'll assume it's correct and move to next layer
            // You might want to decrypt and verify the response matches the original challenge
            
            System.out.println("[Server] Challenge response verified for user: " + clientState.userId);
            clientState.currentLayer = "AUTHENTICATED";
            
            // Send success response
            sendMessage(clientSocket, "auth_success", "Authentication successful");
            
            // Continue with next layer or complete the protocol
            // For now, we'll just complete
            System.out.println("[Server] Authentication completed successfully for user: " + clientState.userId);
            
        } catch (Exception e) {
            System.err.println("[Server] Error processing challenge response: " + e.getMessage());
            sendMessage(clientSocket, "error", "Challenge response processing failed");
            cleanupClient(clientSocket);
        }
    }
    
    /**
     * Start a timeout timer for the current operation
     * @param clientSocket the client socket
     * @param operation description of the operation being timed
     */
    private void startTimeout(SecureSocket clientSocket, String operation) {
        ClientState clientState = clientStates.get(clientSocket);
        if (clientState == null) return;
        
        // Cancel any existing timeout
        if (clientState.timeoutTask != null && !clientState.timeoutTask.isDone()) {
            clientState.timeoutTask.cancel(true);
        }
        
        // Start new timeout
        clientState.timeoutTask = timeoutExecutor.schedule(() -> {
            System.out.println("[Server] Timeout waiting for " + operation + " from user: " + clientState.userId);
            sendMessage(clientSocket, "timeout", "Timeout waiting for " + operation);
            cleanupClient(clientSocket);
        }, PROTOCOL_TIMEOUT, TimeUnit.MILLISECONDS);
        
        System.out.println("[Server] Started " + PROTOCOL_TIMEOUT + "ms timeout for " + operation);
    }

    private void sendMessage(SecureSocket clientSocket, String type, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("message", message);
        clientSocket.sendJson(json);
        System.out.println("[Server] Sent message - type: " + type + ", message: " + message);
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