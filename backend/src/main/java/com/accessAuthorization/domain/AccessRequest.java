package com.accessAuthorization.domain;

public class AccessRequest {
    private String id;
    private String message;
    private String cipher;

    // Construtores
    public AccessRequest() {}
    
    public AccessRequest(String id, String message, String cipher) {
        this.id = id;
        this.message = message;
        this.cipher = cipher;
    }

    // Getters e setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCipher() { return cipher; }
    public void setCipher(String cipher) { this.cipher = cipher; }

    // Método de domínio
    public boolean isValid(String publicKey) {
        // Lógica de validação da assinatura aqui
        return true;
    }
}

