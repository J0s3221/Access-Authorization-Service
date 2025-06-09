package com.accessauth.controller;

import com.google.gson.*;
import com.accessauth.service.AccessAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.concurrent.*;
import com.accessauth.socket.*;

@Component
public class AccessAuthorizationController {
    private SecureSocketServer server;

    @Autowired
    private AccessAuthorizationService authService;

    @Value("${protocol.server.port:12345}")
    private int serverPort;

    private static final long PROTOCOL_TIMEOUT = 8000;

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
        BlockingQueue<JsonObject> messageQueue = new LinkedBlockingQueue<>();

        // Start listening ONCE for this client
        clientSocket.startJsonListening(message -> {
            try {
                messageQueue.put(message);
            } catch (InterruptedException e) {
                System.err.println("[Server] Failed to enqueue message: " + e.getMessage());
            }
        });

        try {
            JsonObject initialMessage = waitForMessage(messageQueue, PROTOCOL_TIMEOUT);
            if (initialMessage == null || !"protocol_start".equals(initialMessage.get("type").getAsString())) {
                sendMessage(clientSocket, "error", "Expected protocol_start message");
                return;
            }

            System.out.println("[Server] Starting protocol loop");

            for (int layer = 1; layer <= 5; layer++) {
                if (!executeLayer(clientSocket, messageQueue, layer)) {
                    System.out.println("[Server] Protocol failed at layer " + layer);
                    return;
                }
            }

            System.out.println("[Server] Protocol completed successfully");
            sendMessage(clientSocket, "success", "Protocol completed");

        } catch (Exception e) {
            System.err.println("[Server] Client error: " + e.getMessage());
        }
    }

    private boolean executeLayer(SecureSocket clientSocket, BlockingQueue<JsonObject> queue, int layer) {
        try {
            System.out.println("[Server] Executing Layer " + layer);
            sendMessage(clientSocket, "layer_message", "Hi from layer " + layer);

            JsonObject response = waitForMessage(queue, PROTOCOL_TIMEOUT);
            if (response == null) {
                System.out.println("[Server] Layer " + layer + " timeout - no response received");
                return false;
            }

            String responseType = response.get("type") != null ? response.get("type").getAsString() : "";
            if (!"layer_ack".equals(responseType)) {
                System.out.println("[Server] Layer " + layer + " failed - expected 'layer_ack' but got: " + responseType);
                return false;
            }

            System.out.println("[Server] Layer " + layer + " completed successfully - received acknowledgment");
            return true;

        } catch (Exception e) {
            System.err.println("[Server] Layer " + layer + " error: " + e.getMessage());
            return false;
        }
    }

    private JsonObject waitForMessage(BlockingQueue<JsonObject> queue, long timeoutMs) {
        try {
            return queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.err.println("[Server] Interrupted while waiting for message: " + e.getMessage());
            return null;
        }
    }

    private void sendMessage(SecureSocket clientSocket, String type, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("message", message);
        clientSocket.sendJson(json);
        System.out.println("[Server] Sent message - type: " + type + ", message: " + message);
    }
}
