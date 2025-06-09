package com.accessauth.socket;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;

public class SecureSocketServer {
    private SSLServerSocket serverSocket;
    private SSLContext sslContext;
    private boolean isRunning = false;
    
    public SecureSocketServer(int port, String keystorePath, String password) throws Exception {
        this.sslContext = createSSLContext(keystorePath, password);
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        this.serverSocket = (SSLServerSocket) factory.createServerSocket(port);
    }

    // Accept a connection and return a SecureSocket
    public SecureSocket acceptConnection() throws IOException {
        if (!isRunning) {
            throw new IllegalStateException("Server not started");
        }
        SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
        return new SecureSocket(clientSocket);
    }
    
    // Start the server
    public void start() {
        isRunning = true;
        System.out.println("Server started on port " + serverSocket.getLocalPort());
    }
    
    // Stop the server
    public void stop() throws IOException {
        isRunning = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    private static SSLContext createSSLContext(String keystorePath, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream is = new FileInputStream(keystorePath)) {
            keyStore.load(is, password.toCharArray());
        }
        
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, password.toCharArray());
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keyStore);
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        
        return sslContext;
    }
}