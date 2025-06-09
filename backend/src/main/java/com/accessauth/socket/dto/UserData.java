package com.accessauth.socket.dto;

public class UserData {
    private String username;
    private int age;
    private String email;
    
    public UserData(String username, int age, String email) {
        this.username = username;
        this.age = age;
        this.email = email;
    }
    
    // Getters
    public String getUsername() { return username; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
    
    // Setters (for JSON deserialization)
    public void setUsername(String username) { this.username = username; }
    public void setAge(int age) { this.age = age; }
    public void setEmail(String email) { this.email = email; }
    
    @Override
    public String toString() {
        return "UserData{" +
                "username='" + username + '\'' +
                ", age=" + age +
                ", email='" + email + '\'' +
                '}';
    }
}