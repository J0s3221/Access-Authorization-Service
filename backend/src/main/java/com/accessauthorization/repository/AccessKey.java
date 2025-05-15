package com.accessauthorization.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AccessKey {
    @Id
    private String id;  
    private String publicKey;

    // Constructors
    public AccessKey() {}  

    public AccessKey(String id, String publicKey) {
        this.id = id;
        this.publicKey = publicKey;
    }

    // Getters and setters
    public String getId() { return id; }
    public String getPublicKey() { return publicKey; }

    public void setId(String id) { this.id = id; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
}
