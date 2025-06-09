package com.accessauth.socket.dto;

public class GameMove {
    private String player;
    private String move;
    private int timestamp;
    
    public GameMove(String player, String move) {
        this.player = player;
        this.move = move;
        this.timestamp = (int) (System.currentTimeMillis() / 1000);
    }
    
    // Default constructor for JSON deserialization
    public GameMove() {}
    
    // Getters
    public String getPlayer() { return player; }
    public String getMove() { return move; }
    public int getTimestamp() { return timestamp; }
    
    // Setters (for JSON deserialization)
    public void setPlayer(String player) { this.player = player; }
    public void setMove(String move) { this.move = move; }
    public void setTimestamp(int timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return "GameMove{" +
                "player='" + player + '\'' +
                ", move='" + move + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}