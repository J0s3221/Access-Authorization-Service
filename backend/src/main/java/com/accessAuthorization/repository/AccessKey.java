package com.accessAuthorization.repository;

import jakarta.persistence.*;

@Entity
public class AccessKey {

    @Id
    private String id;
    private String publicKey;

    public String getId() { return id; }
    public String getPublicKey() { return publicKey; }

    public void setId(String id) { this.id = id; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
}
