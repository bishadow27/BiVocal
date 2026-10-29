package com.example.bivoca.data.api;

import com.example.bivoca.utils.SessionManager;
import com.example.bivoca.utils.AuthSessionExpiry;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final SessionManager sessionManager;
    private final android.content.Context appContext;

    public AuthInterceptor(android.content.Context context, SessionManager sessionManager) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = sessionManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request originalRequest = chain.request();

        String token = sessionManager.getAccessToken();

        if (token == null || token.trim().isEmpty()) {
            return chain.proceed(originalRequest);
        }

        Request authenticatedRequest =
                originalRequest.newBuilder()
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
                        .build();

        okhttp3.Response response = chain.proceed(authenticatedRequest);
        if (shouldInvalidateSession(response.code(), authenticatedRequest.header("Authorization"))) {
            AuthSessionExpiry.handleRejectedToken(appContext, sessionManager, token);
        }
        return response;
    }

    static boolean shouldInvalidateSession(int responseCode, String authorizationHeader) {
        return responseCode == 401
                && authorizationHeader != null
                && authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)
                && authorizationHeader.length() > 7;
    }
}
