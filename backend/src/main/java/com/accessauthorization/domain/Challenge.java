package com.accessauthorization.domain;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Challenge {
    private String id;
    private String challenge;
    private String pubkey;
    
    // Construtores
    public Challenge() {}

    public Challenge(String pubkey) {
        this.pubkey = pubkey;

        this.id = UUID.randomUUID().toString();
        this.challenge = generateChallenge();
    }

    private String generateChallenge() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32]; // 256-bit challenge
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashChallenge() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(this.challenge.getBytes());
            
            // Convert hash to Base64 or Hex (your choice)
            String hashed = Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
            
            // Overwrite or store it somewhere
            return hashed;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public String decypher(String cypher) {
        // uses public key to decypher the cypher 
        
        // place holder
        String a = "yes";
        return a;
    }

    // Getters e setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChallenge() { return challenge; }
    public void setChallenge(String challenge) { this.challenge = challenge; }

    public String getPubkey() { return pubkey; }
    public void setPubkey(String pubkey) { this.pubkey = pubkey; }

}
