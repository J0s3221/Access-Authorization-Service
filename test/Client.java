import com.google.gson.*;

public class Client {
    private SecureSocket socket;
    private String clientId;
    
    public Client(String host, int port, String clientId) throws Exception {
        this.socket = new SecureSocket(host, port, "../backend/src/main/resources/keystore.jks", "password");
        this.clientId = clientId;
        
        // Start listening for messages from server
        socket.startJsonListening(this::handleServerMessage);
        
        System.out.println("[" + clientId + "] Connected to protocol server!");
    }
    
    private void handleServerMessage(JsonObject message) {
        try {
            String messageType = message.get("type").getAsString();
            
            switch (messageType) {
                case "layer_message":
                    handleLayerMessage(message);
                    break;
                    
                case "success":
                    handleSuccess(message);
                    break;
                    
                case "error":
                    handleError(message);
                    break;
                    
                default:
                    System.out.println("[" + clientId + "] Unknown message: " + messageType);
                    System.out.println("Full message: " + message);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[" + clientId + "] Error handling message: " + e.getMessage());
        }
    }
    
    private void handleLayerMessage(JsonObject message) {
        String layerMessage = message.get("message").getAsString();
        System.out.println("[" + clientId + "] Received: " + layerMessage);
        
        // Send acknowledgment back to server
        JsonObject response = new JsonObject();
        response.addProperty("type", "layer_ack");
        response.addProperty("message", "OK");
        socket.sendJson(response);
        
        System.out.println("[" + clientId + "] Sent acknowledgment");
    }
    
    private void handleSuccess(JsonObject message) {
        String successMessage = message.get("message").getAsString();
        System.out.println("[" + clientId + "] SUCCESS: " + successMessage);
    }
    
    private void handleError(JsonObject message) {
        String errorMessage = message.get("message").getAsString();
        System.err.println("[" + clientId + "] ERROR: " + errorMessage);
    }
    
    public void startProtocol() {
        System.out.println("[" + clientId + "] Starting protocol...");
        
        JsonObject protocolStart = new JsonObject();
        protocolStart.addProperty("type", "protocol_start");
        protocolStart.addProperty("user_id", "user123");
        
        socket.sendJson(protocolStart);
        System.out.println("[" + clientId + "] Sent protocol_start message");
    }
    
    public void close() throws Exception {
        socket.close();
        System.out.println("[" + clientId + "] Connection closed");
    }
    
    public static void main(String[] args) {
        try {
            Client client = new Client("localhost", 12345, "TestClient");
            
            // Wait a moment for connection to establish
            Thread.sleep(1000);
            
            // Start the protocol
            client.startProtocol();
            
            // Keep the client running to receive all protocol messages
            System.out.println("[TestClient] Waiting for protocol messages...");
            Thread.sleep(15000); // Wait 15 seconds for protocol to complete
            
            client.close();
            
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}