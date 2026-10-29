package com.example.bivoca.data.api;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthInterceptorTest {

    @Test
    public void invalidatesSessionOnlyForUnauthorizedAuthenticatedRequest() {
        assertTrue(AuthInterceptor.shouldInvalidateSession(401, "Bearer expired-token"));
        assertFalse(AuthInterceptor.shouldInvalidateSession(401, null));
        assertFalse(AuthInterceptor.shouldInvalidateSession(401, ""));
    }

    @Test
    public void doesNotInvalidateSessionForForbiddenOrServerErrors() {
        assertFalse(AuthInterceptor.shouldInvalidateSession(403, "Bearer token"));
        assertFalse(AuthInterceptor.shouldInvalidateSession(500, "Bearer token"));
        assertFalse(AuthInterceptor.shouldInvalidateSession(503, "Bearer token"));
    }
}
