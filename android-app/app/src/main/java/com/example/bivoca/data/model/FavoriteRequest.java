package com.example.bivoca.data.model;

public class FavoriteRequest {
    private final boolean isFavorite;

    public FavoriteRequest(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public boolean isFavorite() {
        return isFavorite;
    }
}
