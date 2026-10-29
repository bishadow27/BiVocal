package com.example.bivoca.data.model;

public class GoogleChallengeResponse {
    private String challengeId;
    private String nonce;
    private int expiresInSeconds;
    public String getChallengeId() { return challengeId; }
    public String getNonce() { return nonce; }
    public int getExpiresInSeconds() { return expiresInSeconds; }
}
