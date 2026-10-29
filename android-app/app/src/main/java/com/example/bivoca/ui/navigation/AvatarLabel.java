package com.example.bivoca.ui.navigation;

import java.util.Locale;

/** Creates the account-specific letter shown when no local avatar image exists. */
public final class AvatarLabel {
    private AvatarLabel() { }

    public static String initial(String username) {
        if (username == null || username.trim().isEmpty()) return "B";
        String trimmed = username.trim();
        int end = trimmed.offsetByCodePoints(0, 1);
        return trimmed.substring(0, end).toUpperCase(Locale.ROOT);
    }
}
