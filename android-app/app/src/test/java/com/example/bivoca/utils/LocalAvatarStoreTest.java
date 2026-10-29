package com.example.bivoca.utils;

import org.junit.Test;
import java.io.File;
import static org.junit.Assert.*;

public class LocalAvatarStoreTest {
    @Test public void fileNameIsStableAndIsolatedPerUser() {
        File root = new File("avatars");
        assertEquals("user_12.jpg", LocalAvatarStore.avatarFile(root, 12).getName());
        assertNotEquals(LocalAvatarStore.avatarFile(root, 12), LocalAvatarStore.avatarFile(root, 13));
    }
    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingSessionOwner() {
        LocalAvatarStore.avatarFile(new File("avatars"), -1);
    }
    @Test public void samplesLargeImages() {
        assertTrue(LocalAvatarStore.sampleSize(8000, 6000, 1024) >= 4);
        assertEquals(1, LocalAvatarStore.sampleSize(800, 600, 1024));
    }
}
