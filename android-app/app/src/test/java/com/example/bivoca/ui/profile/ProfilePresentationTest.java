package com.example.bivoca.ui.profile;

import com.example.bivoca.data.model.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ProfilePresentationTest {
    private final Gson gson=new Gson();
    private LearningHistory history(String days){return gson.fromJson("{\"days\":"+days+"}",LearningHistory.class);}
    @Test public void realDisplayNameAndInitialsPreserveVietnamese() {
        User user=gson.fromJson("{\"id\":7,\"username\":\"sample_user\",\"fullName\":\"Nguyễn Duy Phúc\"}",User.class);
        assertEquals("Nguyễn Duy Phúc",ProfilePresentation.displayName(user));
        assertEquals("NP",ProfilePresentation.initials(ProfilePresentation.displayName(user)));
        assertEquals("Thảo",ProfilePresentation.displayName(gson.fromJson("{\"username\":\"Thảo\"}",User.class)));
    }
    @Test public void usesServerLevelProgressAndRejectsInvalidFields() {
        GamificationStats stats=gson.fromJson("{\"totalXp\":170,\"level\":2,\"xpIntoLevel\":70,\"xpPerLevel\":100}",GamificationStats.class);
        assertTrue(ProfilePresentation.hasLevelProgress(stats));assertEquals(70,stats.getXpIntoLevel());
        assertFalse(ProfilePresentation.hasLevelProgress(gson.fromJson("{\"totalXp\":170,\"level\":0,\"xpIntoLevel\":0,\"xpPerLevel\":0}",GamificationStats.class)));
        assertFalse(ProfilePresentation.hasLevelProgress(gson.fromJson("{\"level\":2,\"xpIntoLevel\":101,\"xpPerLevel\":100}",GamificationStats.class)));
    }
    @Test public void currentRankUsesUserIdAndBackendRankNeverListIndex() {
        List<LeaderboardUser> board=Arrays.asList(new LeaderboardUser(1,8,"a","A",null,300,4,0,0),new LeaderboardUser(13,7,"b","B",null,170,2,0,0));
        assertEquals(Integer.valueOf(13),ProfilePresentation.rank(board,7));assertNull(ProfilePresentation.rank(board,99));assertNull(ProfilePresentation.rank(Collections.emptyList(),7));
    }
    @Test public void mapsRollingHistoryToActualCalendarWeekAndFutureDays() {
        LearningHistory h=history("[{\"date\":\"2026-11-01\",\"newWords\":4,\"reviews\":1,\"status\":\"COMPLETED\"},{\"date\":\"2026-11-02\",\"newWords\":1,\"reviews\":0,\"status\":\"NOT_COMPLETED\"},{\"date\":\"2026-11-03\",\"newWords\":28,\"reviews\":4,\"status\":\"COMPLETED\"}]");
        List<ProfilePresentation.Day> week=ProfilePresentation.week(h);assertEquals(7,week.size());
        assertEquals("2026-11-02",week.get(0).date);assertEquals(ProfilePresentation.DayState.ACTIVE,week.get(0).state);
        assertEquals(ProfilePresentation.DayState.COMPLETED,week.get(1).state);assertEquals(ProfilePresentation.DayState.FUTURE,week.get(6).state);
        assertEquals("2026-11-08",week.get(6).date);
    }
    @Test public void noDataAndIncompleteCoverageAreNeverInventedActivity() {
        List<ProfilePresentation.Day> week=ProfilePresentation.week(history("[{\"date\":\"2026-11-02\",\"newWords\":0,\"reviews\":0,\"status\":\"NO_DATA\"},{\"date\":\"2026-11-03\",\"newWords\":0,\"reviews\":0,\"status\":\"NO_ACTIVITY\"}]") );
        assertEquals(ProfilePresentation.DayState.UNKNOWN,week.get(0).state);assertEquals(ProfilePresentation.DayState.INACTIVE,week.get(1).state);
        assertTrue(ProfilePresentation.week(history("[]")).isEmpty());
        assertTrue(ProfilePresentation.week(history("[{\"date\":\"2026-11-03\",\"status\":\"NO_ACTIVITY\"}]")).isEmpty());
        assertTrue(ProfilePresentation.week(history("[{\"date\":\"2026-02-30\",\"status\":\"NO_ACTIVITY\"}]")).isEmpty());
    }
    @Test public void dateMappingUsesServerDatesAcrossYearBoundary() {
        List<ProfilePresentation.Day> week=ProfilePresentation.week(history("[{\"date\":\"2026-12-28\",\"status\":\"NO_ACTIVITY\"}]") );
        assertEquals(7,week.size());assertEquals("2027-01-03",week.get(6).date);
    }
    @Test public void sectionFailuresRetainOnlyTheirOwnValuesAndDoNotChangeOtherSections() {
        ProfileOverviewState initial=ProfileOverviewState.empty();
        ProfileOverviewState ready=initial.withGoal(ProfileOverviewState.Section.success(gson.fromJson("{\"dailyGoal\":20}",LearningGoal.class)));
        ProfileOverviewState failed=ready.withRank(ProfileOverviewState.Section.failure("Unavailable",ready.rank));
        assertEquals(20,failed.goal.value.getDailyGoal());assertNull(initial.goal.value);assertEquals("Unavailable",failed.rank.error);
        assertFalse(failed.rank.loading);assertNull(failed.stats.value);
        assertTrue(ProfileOverviewState.Section.loading(failed.goal).loading);
        assertSame(failed.goal.value,ProfileOverviewState.Section.loading(failed.goal).value);
    }
}
