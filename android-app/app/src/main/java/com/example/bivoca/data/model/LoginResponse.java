package com.example.bivoca.data.model;

public class LoginResponse {

    private User user;
    private String accessToken;

    public User getUser() {
        return user;
    }

    public String getAccessToken() {
        return accessToken;
    }
}