package com.example.usermanager.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")  // Mapping to the 'users' table in the database
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String publicKey;

    // No-argument constructor for JPA
    public User() {
    }

    // Constructor with arguments
    public User(String name, String publicKey) {
        this.name = name;
        this.publicKey = publicKey;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String toString() {
        return String.format("User{id=%d, name='%s', publicKey='%s'}", id, name, publicKey);
    }
}
