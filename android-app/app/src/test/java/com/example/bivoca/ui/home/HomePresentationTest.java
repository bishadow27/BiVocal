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
    @Test public void currentUserPreviewPreservesRealRankAndAvoidsDuplicate() {
        LeaderboardUser first = user(1,2), second=user(2,3), me=user(7,55);
        List<LeaderboardUser> selected=HomePresentation.preview(Arrays.asList(first,second,me),55);
        assertEquals(3,selected.size());
        assertEquals(7,selected.get(2).getRank());
        assertEquals(2,HomePresentation.preview(Arrays.asList(first,second,me),2).size());
    }
    @Test public void missingUserDoesNotGetFabricatedRanking() {
        assertTrue(HomePresentation.preview(null,55).isEmpty());
        assertTrue(HomePresentation.preview(Collections.emptyList(),55).isEmpty());
        assertEquals(1,HomePresentation.preview(Collections.singletonList(user(1,2)),55).size());
    }
    private LeaderboardUser user(int rank,int id) {
        return new LeaderboardUser(rank,id,"test",null,null,100,2,1,1);
    }
}
