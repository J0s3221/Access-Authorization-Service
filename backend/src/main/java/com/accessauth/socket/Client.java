package com.accessauth.socket;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;

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
    
    // Helper method to convert string to hexadecimal
    private String stringToHex(String input) {
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
    
    // Helper method to convert hexadecimal to string
    private String hexToString(String hex) {
        try {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < hex.length(); i += 2) {
                String hexPair = hex.substring(i, i + 2);
                int value = Integer.parseInt(hexPair, 16);
                result.append((char) value);
            }
            return result.toString();
        } catch (Exception e) {
            System.err.println("[" + clientId + "] Error decoding hex string: " + e.getMessage());
            return hex; // Return original if decoding fails
        }
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
        // First decode the hex-encoded message
        String hexMessage = message.get("message").getAsString();
        String decodedMessage = hexToString(hexMessage);
        System.out.println("[" + clientId + "] Received message (decoded): " + decodedMessage);
        
        // Get challenge from data object
        String encryptedChallenge = null;
        if (message.has("data") && message.get("data").isJsonObject()) {
            JsonObject data = message.getAsJsonObject("data");
            if (data.has("challenge")) {
                encryptedChallenge = data.get("challenge").getAsString();
            }
        }
        
        if (encryptedChallenge == null) {
            System.err.println("[" + clientId + "] No challenge found in message data");
            return;
        }
        
        System.out.println("[" + clientId + "] Received encrypted challenge (hex): " + encryptedChallenge);
        
        // Try to decode the hex-encoded challenge
        String decodedChallenge = hexToString(encryptedChallenge);
        System.out.println("[" + clientId + "] Decoded challenge: " + decodedChallenge);
        
        // In a real implementation, you would:
        // 1. Decrypt the challenge using your symmetric key
        // 2. Process the challenge (maybe add some data or transform it)
        // 3. Encrypt the response with your symmetric key
        
        // For simulation, we'll send a dummy response encoded in hex
        String dummyResponse = "dummy_challenge_response_" + System.currentTimeMillis();
        String hexEncodedResponse = stringToHex(dummyResponse);
        
        JsonObject challengeResponse = new JsonObject();
        challengeResponse.addProperty("challenge_response", hexEncodedResponse);
        
        socket.sendJson(challengeResponse);
        System.out.println("[" + clientId + "] Sent challenge response (original): " + dummyResponse);
        System.out.println("[" + clientId + "] Sent challenge response (hex): " + hexEncodedResponse);
    }
    
    private void handleAuthSuccess(JsonObject message) {
        String successMessage = message.get("message").getAsString();
        // Try to decode if it's hex encoded
        String decodedMessage = hexToString(successMessage);
        System.out.println("[" + clientId + "] AUTHENTICATION SUCCESS (hex): " + successMessage);
        System.out.println("[" + clientId + "] AUTHENTICATION SUCCESS (decoded): " + decodedMessage);
        
        // Also decode any data if present
        if (message.has("data") && message.get("data").isJsonObject()) {
            JsonObject data = message.getAsJsonObject("data");
            System.out.println("[" + clientId + "] Success data received:");
            if (data.has("user_id")) {
                String hexUserId = data.get("user_id").getAsString();
                String decodedUserId = hexToString(hexUserId);
                System.out.println("[" + clientId + "] User ID (hex): " + hexUserId + " -> (decoded): " + decodedUserId);
            }
            if (data.has("timestamp")) {
                String hexTimestamp = data.get("timestamp").getAsString();
                String decodedTimestamp = hexToString(hexTimestamp);
                System.out.println("[" + clientId + "] Timestamp (hex): " + hexTimestamp + " -> (decoded): " + decodedTimestamp);
            }
        }
    }
    
    private void handleError(JsonObject message) {
        String errorMessage = message.get("message").getAsString();
        // Try to decode if it's hex encoded
        String decodedMessage = hexToString(errorMessage);
        System.err.println("[" + clientId + "] ERROR (hex): " + errorMessage);
        System.err.println("[" + clientId + "] ERROR (decoded): " + decodedMessage);
    }
    
    private void handleTimeout(JsonObject message) {
        String timeoutMessage = message.get("message").getAsString();
        // Try to decode if it's hex encoded
        String decodedMessage = hexToString(timeoutMessage);
        System.err.println("[" + clientId + "] TIMEOUT (hex): " + timeoutMessage);
        System.err.println("[" + clientId + "] TIMEOUT (decoded): " + decodedMessage);
    }
    
    private void handleInfo(JsonObject message) {
        String infoMessage = message.get("message").getAsString();
        // Try to decode if it's hex encoded
        String decodedMessage = hexToString(infoMessage);
        System.out.println("[" + clientId + "] INFO (hex): " + infoMessage);
        System.out.println("[" + clientId + "] INFO (decoded): " + decodedMessage);
    }
    
    public void startAuthentication() {
        System.out.println("[" + clientId + "] Starting authentication for user: " + userId);
        
        // Send user ID encoded in hex to start the authentication process
        String hexEncodedUserId = stringToHex(userId);
        
        JsonObject idMessage = new JsonObject();
        idMessage.addProperty("id", hexEncodedUserId);
        
        socket.sendJson(idMessage);
        System.out.println("[" + clientId + "] Sent user ID (original): " + userId);
        System.out.println("[" + clientId + "] Sent user ID (hex): " + hexEncodedUserId);
    }
    
    public void close() throws Exception {
        socket.close();
        System.out.println("[" + clientId + "] Connection closed");
    }
    
    public static void main(String[] args) {
        try {
            // You can change the userId here to test different users
            String testUserId = "2"; // Make sure this user exists in your database
            Client client = new Client("192.168.12.133", 12345, "TestClient", testUserId);
            
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