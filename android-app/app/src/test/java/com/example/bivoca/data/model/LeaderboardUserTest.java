package com.example.bivoca.data.model;

import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.*;

public class LeaderboardUserTest {

    @Test
    public void testBackendLeaderboardJsonMapsExactFields() {
        String json = "{\"rank\":2,\"userId\":43,\"username\":\"student_b\","
                + "\"fullName\":null,\"avatarUrl\":null,\"totalXp\":120,"
                + "\"level\":2,\"currentStreak\":3,\"longestStreak\":5}";

        LeaderboardUser user = new Gson().fromJson(json, LeaderboardUser.class);

        assertEquals(2, user.getRank());
        assertEquals(43, user.getUserId());
        assertEquals("student_b", user.getUsername());
        assertEquals("student_b", user.getDisplayName());
        assertEquals(120, user.getTotalXp());
        assertEquals(2, user.getLevel());
        assertEquals(3, user.getCurrentStreak());
        assertEquals(5, user.getLongestStreak());
    }

    @Test
    public void testLeaderboardUserFields() {
        LeaderboardUser user = new LeaderboardUser(
                1,
                42,
                "nguyen_van_a",
                "Nguyễn Văn A",
                "https://example.com/avatar.png",
                350,
                4,
                7,
                12
        );

        assertEquals(1, user.getRank());
        assertEquals(42, user.getUserId());
        assertEquals("nguyen_van_a", user.getUsername());
        assertEquals("Nguyễn Văn A", user.getFullName());
        assertEquals("Nguyễn Văn A", user.getDisplayName());
        assertEquals("https://example.com/avatar.png", user.getAvatarUrl());
        assertEquals(350, user.getTotalXp());
        assertEquals(4, user.getLevel());
        assertEquals(7, user.getCurrentStreak());
        assertEquals(12, user.getLongestStreak());
    }

    @Test
    public void testDisplayNameFallbackWhenFullNameIsNull() {
        LeaderboardUser user = new LeaderboardUser(
                2,
                43,
                "student_b",
                null,
                null,
                120,
                2,
                3,
                5
        );

        assertEquals("student_b", user.getDisplayName());
        assertNull(user.getFullName());
        assertNull(user.getAvatarUrl());
    }

    @Test
    public void testDisplayNameFallbackWhenFullNameIsEmpty() {
        LeaderboardUser user = new LeaderboardUser(
                3,
                44,
                "student_c",
                "   ",
                null,
                50,
                1,
                1,
                1
        );

        assertEquals("student_c", user.getDisplayName());
    }
}
