package com.example.bivoca.ui.home;

import com.example.bivoca.data.model.LeaderboardUser;
import java.util.ArrayList;
import java.util.List;

/** Presentation only; does not change SRS levels, ranks or scores. */
public final class HomePresentation {
    private HomePresentation() { }
    public static int[] srsGroups(int[] levels) {
        if (levels == null || levels.length != 6) return null;
        for (int level : levels) if (level < 0) return null;
        // Presentation: fresh 0–1, practicing 2–3, good 4, mastered 5. Only reviewed cards enter these API buckets.
        return new int[]{levels[0] + levels[1], levels[2] + levels[3], levels[4], levels[5]};
    }
    public static List<LeaderboardUser> preview(List<LeaderboardUser> users, int currentUserId) {
        List<LeaderboardUser> result = new ArrayList<>();
        if (users == null) return result;
        for (int i = 0; i < Math.min(2, users.size()); i++) result.add(users.get(i));
        for (LeaderboardUser user : users) {
            if (user.getUserId() == currentUserId) {
                boolean present = false;
                for (LeaderboardUser selected : result) if (selected.getUserId() == currentUserId) present = true;
                if (!present) result.add(user);
                break;
            }
        }
        return result;
    }
}

