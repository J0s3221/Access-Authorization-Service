package com.accessauth.socket;

import com.google.gson.*;

public class Client {
    private SecureSocket socket;
    private String clientId;
    private String userId;
    
    public Client(String host, int port, String clientId, String userId) throws Exception {
        this.socket = new SecureSocket(host, port, "src/main/resources/keystore.jks", "password");
        this.clientId = clientId;
        this.userId = userId;
        
        // Start listening for messages from server
        socket.startJsonListening(this::handleServerMessage);
        
        System.out.println("[" + clientId + "] Connected to protocol server!");
    }
    
    private void handleServerMessage(JsonObject message) {
        try {
            System.out.println("[" + clientId + "] Received message: " + message.toString());
            
            String messageType = message.get("type").getAsString();
            
            switch (messageType) {
                case "challenge":
                    handleChallenge(message);
                    break;
                    
                case "auth_success":
                    handleAuthSuccess(message);
                    break;
                    
                case "error":
                    handleError(message);
                    break;
                    
                case "timeout":
                    handleTimeout(message);
                    break;
                    
                case "info":
                    handleInfo(message);
                    break;
                    
                default:
                    System.out.println("[" + clientId + "] Unknown message type: " + messageType);
                    System.out.println("Full message: " + message);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[" + clientId + "] Error handling message: " + e.getMessage());
        }
    }
    
    private void handleChallenge(JsonObject message) {
        String encryptedChallenge = message.get("challenge").getAsString();
        System.out.println("[" + clientId + "] Received encrypted challenge: " + encryptedChallenge);
        
        // In a real implementation, you would:
        // 1. Decrypt the challenge using your symmetric key
        // 2. Process the challenge (maybe add some data or transform it)
        // 3. Encrypt the response with your symmetric key
        
        // For simulation, we'll send a dummy response
        String dummyResponse = "dummy_challenge_response_" + System.currentTimeMillis();
        
        JsonObject challengeResponse = new JsonObject();
        challengeResponse.addProperty("challenge_response", dummyResponse);
        
        socket.sendJson(challengeResponse);
        System.out.println("[" + clientId + "] Sent challenge response: " + dummyResponse);
    }
    
    private void handleAuthSuccess(JsonObject message) {
        String successMessage = message.get("message").getAsString();
        System.out.println("[" + clientId + "] AUTHENTICATION SUCCESS: " + successMessage);
    }
    
    private void handleError(JsonObject message) {
        String errorMessage = message.get("message").getAsString();
        System.err.println("[" + clientId + "] ERROR: " + errorMessage);
    }
    
    private void handleTimeout(JsonObject message) {
        String timeoutMessage = message.get("message").getAsString();
        System.err.println("[" + clientId + "] TIMEOUT: " + timeoutMessage);
    }
    
    private void handleInfo(JsonObject message) {
        String infoMessage = message.get("message").getAsString();
        System.out.println("[" + clientId + "] INFO: " + infoMessage);
    }
    
    public void startAuthentication() {
        System.out.println("[" + clientId + "] Starting authentication for user: " + userId);
        
        // Send user ID to start the authentication process
        JsonObject idMessage = new JsonObject();
        idMessage.addProperty("id", userId);
        
        socket.sendJson(idMessage);
        System.out.println("[" + clientId + "] Sent user ID: " + userId);
    }
    
    public void close() throws Exception {
        socket.close();
        System.out.println("[" + clientId + "] Connection closed");
    }
    
    public static void main(String[] args) {
        try {
            // You can change the userId here to test different users
            String testUserId = "2"; // Make sure this user exists in your database
            Client client = new Client("192.168.1.113", 12345, "TestClient", testUserId);
            
            // Wait a moment for connection to establish
            Thread.sleep(1000);
            
            // Start the authentication process
            client.startAuthentication();
            
            // Keep the client running to receive all authentication messages
            System.out.println("[TestClient] Waiting for authentication messages...");
            Thread.sleep(15000); // Wait 15 seconds for authentication to complete
            
            client.close();
            
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}