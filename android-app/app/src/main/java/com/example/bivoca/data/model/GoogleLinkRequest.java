package com.example.bivoca.data.model;

public class GoogleLinkRequest {
    private final String idToken;
    private final String challengeId;
    private final String nonce;
    private final String password;

    public GoogleLinkRequest(String idToken, String challengeId, String nonce, String password) {
        this.idToken = idToken;
        this.challengeId = challengeId;
        this.nonce = nonce;
        this.password = password;
    }
}
