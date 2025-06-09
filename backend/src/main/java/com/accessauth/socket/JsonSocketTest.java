package com.accessauth.socket;

import java.util.concurrent.CountDownLatch;

public class JsonSocketTest {
    public static void main(String[] args) {
        CountDownLatch latch = new CountDownLatch(1);
        
        // Start server in background
        new Thread(() -> {
            try {
                JsonServerDemo server = new JsonServerDemo(12345);
                latch.countDown(); // Signal server is ready
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        
        try {
            latch.await(); // Wait for server to start
            Thread.sleep(1000); // Give server a moment
            
            // Start multiple clients
            JsonClientDemo client1 = new JsonClientDemo("localhost", 12345, "Alice");
            Thread.sleep(500);
            
            JsonClientDemo client2 = new JsonClientDemo("localhost", 12345, "Bob");
            Thread.sleep(500);
            
            // Let them communicate
            Thread.sleep(5000);
            
            client1.close();
            client2.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}