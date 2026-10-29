package com.example.bivoca.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.bivoca.data.model.LoginResponse;
import com.example.bivoca.data.model.User;

public class SessionManager {

    private static final String PREF_NAME = "bivoca_session";
    private static final Object SESSION_LOCK = new Object();

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void saveLogin(LoginResponse response) {

        if (response == null || response.getUser() == null) {
            return;
        }

        User user = response.getUser();

        synchronized (SESSION_LOCK) {
            preferences.edit()
                    .putString(KEY_ACCESS_TOKEN, response.getAccessToken())
                    .putInt(KEY_USER_ID, user.getId())
                    .putString(KEY_USERNAME, user.getUsername())
                    .apply();
            AuthSessionExpiry.reset();
        }
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public int getUserId() {
        return preferences.getInt(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, "");
    }

    public boolean isLoggedIn() {
        String token = getAccessToken();
        return token != null && !token.isEmpty();
    }

    public void logout() {
        synchronized (SESSION_LOCK) {
            preferences.edit().clear().commit();
        }
    }

    /** Clears the session only when the rejected request used the current token. */
    public boolean clearSessionIfTokenMatches(String rejectedToken) {
        if (rejectedToken == null || rejectedToken.isEmpty()) {
            return false;
        }
        synchronized (SESSION_LOCK) {
            String currentToken = getAccessToken();
            if (!rejectedToken.equals(currentToken)) {
                return false;
            }
            preferences.edit().clear().commit();
            return true;
        }
    }
}
