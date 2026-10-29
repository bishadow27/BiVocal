package com.example.bivoca.data.model;

public class GoogleLoginRequest {
    private final String idToken;
    private final String challengeId;
    private final String nonce;
    public GoogleLoginRequest(String idToken, String challengeId, String nonce) {
        this.idToken = idToken; this.challengeId = challengeId; this.nonce = nonce;
    }
}
