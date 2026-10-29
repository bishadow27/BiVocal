package com.example.bivoca.ui.home;
import com.example.bivoca.data.model.LeaderboardUser;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;
public class HomePresentationTest {
    @Test public void allReviewedLevelsBelongToExactlyOneGroup() {
        int[] groups = HomePresentation.srsGroups(new int[]{3,5,7,11,13,17});
        assertArrayEquals(new int[]{8,18,13,17}, groups);
        assertEquals(56, java.util.Arrays.stream(groups).sum());
    }
    @Test public void noReviewDataStaysZeroWithoutInventedProgress() {
        assertArrayEquals(new int[]{0,0,0,0}, HomePresentation.srsGroups(new int[6]));
    }
    @Test public void incompleteOrNegativeLevelsAreUnavailable() {
        assertNull(HomePresentation.srsGroups(null));
        assertNull(HomePresentation.srsGroups(new int[5]));
        assertNull(HomePresentation.srsGroups(new int[]{0,0,0,-1,0,0}));
    }
    @Test public void topThreePreservesApiOrderAndRealRanksIncludingTies() {
        LeaderboardUser first=user(1,2), second=user(1,3), third=user(3,4), me=user(8,55);
        List<LeaderboardUser> users=Arrays.asList(first,second,third,me);
        assertEquals(Arrays.asList(first,second,third),HomePresentation.topThree(users));
        assertEquals(1,HomePresentation.topThree(users).get(1).getRank());
        assertSame(me,HomePresentation.findUser(users,55));
        assertNull(HomePresentation.findUser(HomePresentation.topThree(users),55));
        assertSame(second,HomePresentation.findUser(HomePresentation.topThree(users),3));
    }
    @Test public void missingUserDoesNotGetFabricatedRanking() {
        assertTrue(HomePresentation.topThree(null).isEmpty());
        assertTrue(HomePresentation.topThree(Collections.emptyList()).isEmpty());
        assertEquals(1,HomePresentation.topThree(Collections.singletonList(user(1,2))).size());
        assertNull(HomePresentation.findUser(null,55));
        assertNull(HomePresentation.findUser(Collections.singletonList(user(1,2)),55));
    }
    private LeaderboardUser user(int rank,int id) {
        return new LeaderboardUser(rank,id,"test",null,null,100,2,1,1);
    }
}
