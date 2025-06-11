package com.accessauth.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "sym_key", nullable = false)
    private String symKey;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "active")
    private Boolean active = true;
    
    // Constructors
    public User() {}
    
    public User(String symKey) {
        this.symKey = symKey;
    }
    
    public User(String symKey, String username, String email) {
        this.symKey = symKey;
        this.username = username;
        this.email = email;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return id;
    }
    
    public void setUserId(Long userId) {
        this.id = userId;
    }
    
    public String getSymKey() {
        return symKey;
    }
    
    public void setSymKey(String symKey) {
        this.symKey = symKey;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userId='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", active=" + active +
                '}';
    }
}