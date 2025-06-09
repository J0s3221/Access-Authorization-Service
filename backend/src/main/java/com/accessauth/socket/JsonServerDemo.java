package com.accessauth.socket;

import com.google.gson.*;
import com.accessauth.socket.dto.UserData;
import com.accessauth.socket.dto.GameMove;
import java.util.Map;
import java.util.HashMap;

public class JsonServerDemo {
    private SecureSocketServer server;
    private Map<String, SecureSocket> connectedClients;
    private long startTime;
    
    public JsonServerDemo(int port) throws Exception {
        this.server = new SecureSocketServer(port, "src/main/resources/keystore.jks", "password");
        this.connectedClients = new HashMap<>();
        this.startTime = System.currentTimeMillis();
    }
    
    public void start() throws Exception {
        server.start();
        System.out.println("JSON Server started, waiting for connections...");
        
        while (server.isRunning()) {
            try {
                SecureSocket clientSocket = server.acceptConnection();
                String clientId = "Client_" + System.currentTimeMillis();
                connectedClients.put(clientId, clientSocket);
                
                System.out.println("New client connected");
                
                // Start handling this client
                handleClient(clientSocket, clientId);
                
            } catch (Exception e) {
                System.err.println("Error accepting client: " + e.getMessage());
            }
        }
    }
    
    private void handleClient(SecureSocket clientSocket, String clientId) {
        // Send welcome message
        JsonObject welcome = new JsonObject();
        welcome.addProperty("type", "welcome");
        welcome.addProperty("message", "Welcome to JSON Server!");
        clientSocket.sendJson(welcome);
        
        // Start listening to client messages
        clientSocket.startJsonListening(message -> handleClientMessage(message, clientSocket, clientId));
    }
    
    private void handleClientMessage(JsonObject message, SecureSocket clientSocket, String clientId) {
        try {
            String messageType = message.get("type").getAsString();
            
            switch (messageType) {
                case "user_registration":
                    handleUserRegistration(message, clientSocket, clientId);
                    break;
                    
                case "game_move":
                    handleGameMove(message, clientSocket, clientId);
                    break;
                    
                case "heartbeat":
                    handleHeartbeat(message, clientSocket, clientId);
                    break;
                    
                case "get_server_stats":
                    handleServerStatsRequest(message, clientSocket, clientId);
                    break;
                    
                case "pong":
                    handlePong(message, clientSocket, clientId);
                    break;
                    
                default:
                    System.out.println("[Server] Unknown message from " + clientId + ": " + messageType);
                    sendError(clientSocket, "Unknown message type: " + messageType);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[Server] Error handling message from " + clientId + ": " + e.getMessage());
            sendError(clientSocket, "Error processing message");
        }
    }
    
    private void handleUserRegistration(JsonObject message, SecureSocket clientSocket, String clientId) {
        JsonObject userData = message.getAsJsonObject("user_data");
        String username = userData.get("username").getAsString();
        
        System.out.println("[Server] User registration: " + username);
        
        JsonObject response = new JsonObject();
        response.addProperty("type", "user_info_response");
        response.addProperty("status", "success");
        response.addProperty("message", "User " + username + " registered successfully");
        
        clientSocket.sendJson(response);
    }
    
    private void handleGameMove(JsonObject message, SecureSocket clientSocket, String clientId) {
        if (message.has("move_data")) {
            JsonObject moveData = message.getAsJsonObject("move_data");
            String player = moveData.get("player").getAsString();
            String move = moveData.get("move").getAsString();
            
            System.out.println("[Server] Game move from " + player + ": " + move);
        }

    }
    
    private void handleHeartbeat(JsonObject message, SecureSocket clientSocket, String clientId) {
        String status = message.get("status").getAsString();
        System.out.println("[Server] Heartbeat from " + clientId + ": " + status);
        
        // Send ping back
        JsonObject ping = new JsonObject();
        ping.addProperty("type", "ping");
        ping.addProperty("server_time", System.currentTimeMillis());
        
        clientSocket.sendJson(ping);
    }
    
    private void handleServerStatsRequest(JsonObject message, SecureSocket clientSocket, String clientId) {
        System.out.println("[Server] Stats requested by " + clientId);
        
        JsonObject stats = new JsonObject();
        stats.addProperty("type", "server_stats");
        stats.addProperty("connected_clients", connectedClients.size());
        stats.addProperty("uptime_seconds", (System.currentTimeMillis() - startTime) / 1000);
        
        clientSocket.sendJson(stats);
    }
    
    private void handlePong(JsonObject message, SecureSocket clientSocket, String clientId) {
        long timestamp = message.get("timestamp").getAsLong();
        long latency = System.currentTimeMillis() - timestamp;
        System.out.println("[Server] Pong from " + clientId + ", latency: " + latency + "ms");
    }
    
    private void sendError(SecureSocket clientSocket, String errorMessage) {
        JsonObject error = new JsonObject();
        error.addProperty("type", "error");
        error.addProperty("message", errorMessage);
        
        clientSocket.sendJson(error);
    }
    
    private void broadcastToOthers(JsonObject message, String excludeClientId) {
        connectedClients.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(excludeClientId))
            .forEach(entry -> entry.getValue().sendJson(message));
    }
    
    public static void main(String[] args) {
        try {
            JsonServerDemo server = new JsonServerDemo(12345);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}