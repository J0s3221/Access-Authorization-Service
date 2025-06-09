package com.accessauth.socket;

import com.google.gson.*;
import com.accessauth.socket.dto.UserData;
import com.accessauth.socket.dto.GameMove;

public class JsonClientDemo {
    private SecureSocket socket;
    private String clientId;
    
    public JsonClientDemo(String host, int port, String clientId) throws Exception {
        this.socket = new SecureSocket(host, port, "src/main/resources/keystore.jks", "password");
        this.clientId = clientId;
        
        // Start continuous JSON listening with switch case handling
        socket.startJsonListening(this::handleIncomingMessage);
        
        System.out.println("[" + clientId + "] Connected to server!");
    }
    
    // Main message handler with switch case
    private void handleIncomingMessage(JsonObject message) {
        try {
            String messageType = message.get("type").getAsString();
            
            switch (messageType) {
                case "welcome":
                    handleWelcome(message);
                    break;
                    
                case "user_info_response":
                    handleUserInfoResponse(message);
                    break;
                    
                case "game_move":
                    handleGameMove(message);
                    break;
                    
                case "ping":
                    handlePing(message);
                    break;
                    
                case "server_stats":
                    handleServerStats(message);
                    break;
                    
                case "error":
                    handleError(message);
                    break;
                    
                default:
                    System.out.println("[" + clientId + "] Unknown message type: " + messageType);
                    System.out.println("Full message: " + message);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[" + clientId + "] Error handling message: " + e.getMessage());
            System.err.println("Message was: " + message);
        }
    }
    
    private void handleWelcome(JsonObject message) {
        String welcomeMsg = message.get("message").getAsString();
        System.out.println("[" + clientId + "] Server says: " + welcomeMsg);
        
        // Respond with user registration
        sendUserRegistration();
    }
    
    private void handleUserInfoResponse(JsonObject message) {
        String status = message.get("status").getAsString();
        System.out.println("[" + clientId + "] User registration " + status);
        
        if ("success".equals(status)) {
            // Send a game move after successful registration
            sendGameMove();
        }
    }
    
    private void handleGameMove(JsonObject message) {
        String player = message.get("player").getAsString();
        String move = message.get("move").getAsString();
        System.out.println("[" + clientId + "] Game move from " + player + ": " + move);
    }
    
    private void handlePing(JsonObject message) {
        System.out.println("[" + clientId + "] Received ping");
        
        // Send pong response
        JsonObject pong = new JsonObject();
        pong.addProperty("type", "pong");
        pong.addProperty("client_id", clientId);
        pong.addProperty("timestamp", System.currentTimeMillis());
        socket.sendJson(pong);
    }
    
    private void handleServerStats(JsonObject message) {
        int connectedClients = message.get("connected_clients").getAsInt();
        long uptime = message.get("uptime_seconds").getAsLong();
        System.out.println("[" + clientId + "] Server stats - Clients: " + connectedClients + ", Uptime: " + uptime + "s");
    }
    
    private void handleError(JsonObject message) {
        String error = message.get("message").getAsString();
        System.err.println("[" + clientId + "] Server error: " + error);
    }
    
    // Methods to send different types of messages
    private void sendUserRegistration() {
        UserData user = new UserData(clientId, 25, clientId + "@example.com");
        
        JsonObject message = new JsonObject();
        message.addProperty("type", "user_registration");
        message.add("user_data", new Gson().toJsonTree(user));
        
        socket.sendJson(message);
        System.out.println("[" + clientId + "] Sent user registration");
    }
    
    private void sendGameMove() {
        GameMove move = new GameMove(clientId, "attack_dragon");
        
        JsonObject message = new JsonObject();
        message.addProperty("type", "game_move");
        message.add("move_data", new Gson().toJsonTree(move));
        
        socket.sendJson(message);
        System.out.println("[" + clientId + "] Sent game move");
    }
    
    public void sendHeartbeat() {
        JsonObject heartbeat = new JsonObject();
        heartbeat.addProperty("type", "heartbeat");
        heartbeat.addProperty("client_id", clientId);
        heartbeat.addProperty("status", "alive");
        
        socket.sendJson(heartbeat);
    }
    
    public void requestServerStats() {
        JsonObject request = new JsonObject();
        request.addProperty("type", "get_server_stats");
        request.addProperty("client_id", clientId);
        
        socket.sendJson(request);
        System.out.println("[" + clientId + "] Requested server stats");
    }
    
    public void close() throws Exception {
        socket.close();
    }
    
    public static void main(String[] args) {
        try {
            JsonClientDemo client = new JsonClientDemo("localhost", 12345, "Player1");
            
            // Simulate some activity
            Thread.sleep(2000);
            client.requestServerStats();
            
            Thread.sleep(3000);
            client.sendHeartbeat();
            
            // Keep running
            Thread.sleep(10000);
            
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}