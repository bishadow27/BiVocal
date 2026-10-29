package com.example.bivoca;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;

/** Verifies that historical M8 Room classes are absent from the installed application. */
@RunWith(AndroidJUnit4.class)
public class OnlineOnlyArchitectureTest {
    @Test public void roomLearningCacheIsNotPackaged() {
        assertClassAbsent("com.example.bivoca.data.local.LearningDatabase");
        assertClassAbsent("com.example.bivoca.data.local.RoomLearningCache");
        assertClassAbsent("com.example.bivoca.data.repository.NetworkFirstLoader");
    }

    private static void assertClassAbsent(String name) {
        try {
            Class.forName(name);
            fail(name + " must not be packaged in the Online-Only application");
        } catch (ClassNotFoundException expected) {
            // Expected: source is intentionally excluded from the Online-Only APK.
        }
    }
}
