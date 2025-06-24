package com.accessauth.socket;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import com.accessauth.service.CryptoService;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class Client {
    private SecureSocket socket;
    private String clientId;
    private String userId;
    private String symmetricKey; // You'll need to set this for your user
    private CryptoService cryptoService = new CryptoService();
    
    public Client(String host, int port, String clientId, String userId, String symmetricKey) throws Exception {
        this.socket = new SecureSocket(host, port, "src/main/resources/keystore.jks", "password");
        this.clientId = clientId;
        this.userId = userId;
        this.symmetricKey = symmetricKey;
        
        // Start listening for messages from server
        socket.startJsonListening(this::handleServerMessage);
        
        System.out.println("[" + clientId + "] Connected to protocol server!");
    }
    

    public static String fetchSymmetricKeyFromServer(String userId, String serverUrl) throws Exception {
        URL url = new URL(serverUrl + "/api/keys/" + userId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Failed to fetch symmetric key: HTTP code " + responseCode);
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }


    // Helper method to convert string to hexadecimal
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
    
    // Helper method to convert hexadecimal to string
    private String hexToString(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hexadecimal string");
        }
        
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            String hexPair = hex.substring(i, i + 2);
            bytes[i / 2] = (byte) Integer.parseInt(hexPair, 16);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    private void handleServerMessage(JsonObject message) {
        try {
            System.out.println("[" + clientId + "] Received message: " + message.toString());
            
            if (!message.has("type") || message.get("type").isJsonNull()) {
                System.err.println("[" + clientId + "] Message missing type field");
                return;
            }
            
            String messageType = message.get("type").getAsString();
            
            // Decode timestamp if present
            if (message.has("timestamp") && !message.get("timestamp").isJsonNull()) {
                String hexTimestamp = message.get("timestamp").getAsString();
                try {
                    String decodedTimestamp = hexToString(hexTimestamp);
                    System.out.println("[" + clientId + "] Timestamp (decoded): " + decodedTimestamp);
                } catch (Exception e) {
                    System.err.println("[" + clientId + "] Failed to decode timestamp: " + e.getMessage());
                }
            }
            
            switch (messageType) {
                case "challenge":
                    handleChallenge(message);
                    break;
                    
                case "auth_success":
                    handleAuthSuccess(message);
                    break;
                    
                case "auth_error":
                    handleAuthError(message);
                    break;
                    
                case "timeout":
                    handleTimeout(message);
                    break;
                    
                default:
                    System.out.println("[" + clientId + "] Unknown message type: " + messageType);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[" + clientId + "] Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleChallenge(JsonObject message) {
        System.out.println("[" + clientId + "] Processing challenge message");
        
        // Get hex-encoded challenge directly from the message
        String hexEncryptedChallenge = null;
        if (message.has("challenge") && !message.get("challenge").isJsonNull()) {
            hexEncryptedChallenge = message.get("challenge").getAsString();
        }
        
        if (hexEncryptedChallenge == null) {
            System.err.println("[" + clientId + "] No challenge found in message");
            return;
        }
        
        System.out.println("[" + clientId + "] Received encrypted challenge (hex): " + hexEncryptedChallenge);
        
        try {
            // Step 1: Decode the hex-encoded encrypted challenge
            String encryptedChallenge = hexToString(hexEncryptedChallenge);
            System.out.println("[" + clientId + "] Decoded encrypted challenge from hex");
            
            // Step 2: Decrypt the challenge using the symmetric key
            String decryptedChallenge = cryptoService.decryptChallenge(encryptedChallenge, symmetricKey);
            System.out.println("[" + clientId + "] Successfully decrypted challenge");
            
            // Step 3: Compute digest of the decrypted challenge (this is the response)
            String challengeDigest = cryptoService.computeChallengeDigest(decryptedChallenge);
            System.out.println("[" + clientId + "] Computed challenge digest");
            
            // Step 4: Convert the digest to hex for transmission
            String hexDigestResponse = stringToHex(challengeDigest);
            String encrypted_hexDigestResponse = cryptoService.encryptChallenge(hexDigestResponse, symmetricKey);
            
            // Step 5: Send challenge response with the digest
            JsonObject challengeResponse = new JsonObject();
            challengeResponse.addProperty("type", "challenge_response");
            challengeResponse.addProperty("response", encrypted_hexDigestResponse);
            
            socket.sendJson(challengeResponse);
            System.out.println("[" + clientId + "] Sent challenge response (hex digest): " + hexDigestResponse);
            
        } catch (Exception e) {
            System.err.println("[" + clientId + "] Error processing challenge: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleAuthSuccess(JsonObject message) {
        System.out.println("[" + clientId + "] AUTHENTICATION SUCCESS!");
        
        // Decode user_id if present
        if (message.has("user_id") && !message.get("user_id").isJsonNull()) {
            String hexUserId = message.get("user_id").getAsString();
            try {
                String decodedUserId = hexToString(hexUserId);
                System.out.println("[" + clientId + "] Authenticated User ID: " + decodedUserId);
            } catch (Exception e) {
                System.err.println("[" + clientId + "] Failed to decode user ID: " + e.getMessage());
            }
        }
        
        // Decode timestamp if present
        if (message.has("timestamp") && !message.get("timestamp").isJsonNull()) {
            String hexTimestamp = message.get("timestamp").getAsString();
            try {
                String decodedTimestamp = hexToString(hexTimestamp);
                System.out.println("[" + clientId + "] Success timestamp: " + decodedTimestamp);
            } catch (Exception e) {
                System.err.println("[" + clientId + "] Failed to decode timestamp: " + e.getMessage());
            }
        }
    }

    private void handleAuthError(JsonObject message) {
        String errorInfo = "Authentication failed";
        
        // Try to get additional error information if available
        if (message.has("error") && !message.get("error").isJsonNull()) {
            errorInfo = message.get("error").getAsString();
        }
        
        System.err.println("[" + clientId + "] AUTHENTICATION ERROR: " + errorInfo);
    }

    private void handleTimeout(JsonObject message) {
        String timeoutInfo = "Protocol timeout occurred";
        
        // Try to get additional timeout information if available
        if (message.has("timeout_reason") && !message.get("timeout_reason").isJsonNull()) {
            timeoutInfo = message.get("timeout_reason").getAsString();
        }
        
        System.err.println("[" + clientId + "] TIMEOUT: " + timeoutInfo);
    }
    
    public void startAuthentication() {
        System.out.println("[" + clientId + "] Starting authentication for user: " + userId);
        
        // Send authentication request with hex-encoded user ID
        String hexEncodedUserId = stringToHex(userId);
        
        JsonObject authRequest = new JsonObject();
        authRequest.addProperty("type", "auth_request");
        authRequest.addProperty("user_id", hexEncodedUserId);
        
        socket.sendJson(authRequest);
        System.out.println("[" + clientId + "] Sent auth request - User ID (original): " + userId);
        System.out.println("[" + clientId + "] Sent auth request - User ID (hex): " + hexEncodedUserId);
    }
    
    public void close() throws Exception {
        socket.close();
        System.out.println("[" + clientId + "] Connection closed");
    }
    
    public static void main(String[] args) {
        try {
            // This should match the key stored in your database for this user
            String testUserId = "2";
            String serverUrl = "http://localhost:8080"; // or your actual backend host
            String symmetricKey = fetchSymmetricKeyFromServer(testUserId, serverUrl);
            
            Client client = new Client("localhost", 12345, "TestClient", testUserId, symmetricKey);
            
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