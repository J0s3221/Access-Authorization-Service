package com.accessAuthorization.domain;

public class Challenge {
    private String id;
    private String challenge;
    private String pubkey;
    
    // Construtores
    public Challenge() {}

    public Challenge(String pubkey) {
        this.pubkey = pubkey;

        // cria o resto das cenas
    }

    // Getters e setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChallenge() { return challenge; }
    public void setChallenge(String challenge) { this.challenge = challenge; }

    public String getPubkey() { return pubkey; }
    public void setPubkey(String pubkey) { this.pubkey = pubkey; }

    public void hashChallenge(){} // hash the challenge and returns the digest
}
