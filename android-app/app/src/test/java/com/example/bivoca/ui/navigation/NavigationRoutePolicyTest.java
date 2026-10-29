package com.example.bivoca.ui.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationRoutePolicyTest {

    @Test
    public void normalFlashcardsHighlightTopicsWithoutTreatingTopicsAsCurrentScreen() {
        assertEquals(NavigationRoute.TOPICS,
                NavigationRoutePolicy.selectedMenuFor(NavigationRoute.FLASHCARDS));
        assertFalse(NavigationRoutePolicy.shouldReuseCurrentScreen(
                NavigationRoute.FLASHCARDS, NavigationRoute.TOPICS));
    }

    @Test
    public void reviewTodayIsItsOwnCurrentDestination() {
        assertEquals(NavigationRoute.REVIEW_TODAY,
                NavigationRoutePolicy.selectedMenuFor(NavigationRoute.REVIEW_TODAY));
        assertTrue(NavigationRoutePolicy.shouldReuseCurrentScreen(
                NavigationRoute.REVIEW_TODAY, NavigationRoute.REVIEW_TODAY));
    }

    @Test
    public void exactTopLevelDestinationIsReused() {
        for (NavigationRoute route : new NavigationRoute[]{
                NavigationRoute.HOME,
                NavigationRoute.TOPICS,
                NavigationRoute.QUIZ,
                NavigationRoute.PROGRESS,
                NavigationRoute.LEADERBOARD,
                NavigationRoute.PROFILE,
                NavigationRoute.SETTINGS
        }) {
            assertEquals(route, NavigationRoutePolicy.selectedMenuFor(route));
            assertTrue(NavigationRoutePolicy.shouldReuseCurrentScreen(route, route));
        }
    }
}
