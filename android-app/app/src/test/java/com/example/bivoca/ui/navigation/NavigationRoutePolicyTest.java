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
    public void detailHighlightsTopicsButTopicsNavigatesBackToList() {
        assertEquals(NavigationRoute.TOPICS,
                NavigationRoutePolicy.selectedMenuFor(NavigationRoute.TOPIC_DETAIL));
        assertFalse(NavigationRoutePolicy.shouldReuseCurrentScreen(
                NavigationRoute.TOPIC_DETAIL, NavigationRoute.TOPICS));
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
    @Test public void newTodayHighlightsHomeButIsNotTheHomeScreen() {
        assertEquals(NavigationRoute.HOME,NavigationRoutePolicy.selectedMenuFor(NavigationRoute.NEW_TODAY));
        assertFalse(NavigationRoutePolicy.shouldReuseCurrentScreen(NavigationRoute.NEW_TODAY,NavigationRoute.HOME));
    }
    @Test public void learningModeAndTopicKeysDoNotCollide() {
        String browse=NavigationRoutePolicy.flashcardSessionKey(false,false,"FOOD",10);
        assertEquals(browse,NavigationRoutePolicy.flashcardSessionKey(false,false,"FOOD",20));
        assertFalse(browse.equals(NavigationRoutePolicy.flashcardSessionKey(false,false,"WORK",10)));
        assertFalse(browse.equals(NavigationRoutePolicy.flashcardSessionKey(true,false,"FOOD",10)));
        assertFalse(browse.equals(NavigationRoutePolicy.flashcardSessionKey(false,true,"FOOD",10)));
        assertEquals(NavigationRoutePolicy.flashcardSessionKey(true,false,"FOOD",10),NavigationRoutePolicy.flashcardSessionKey(true,true,"WORK",20));
    }
    @Test public void returningFromHomeWithChangedRemainingDoesNotReplaceNewQueue() {
        assertEquals(NavigationRoutePolicy.flashcardSessionKey(false,true,null,1),NavigationRoutePolicy.flashcardSessionKey(false,true,null,20));
        assertEquals(NavigationRoutePolicy.flashcardSessionKey(false,true,"FOOD",10),NavigationRoutePolicy.flashcardSessionKey(false,true,null,20));
    }
    @Test public void quizAllAndTopicSessionsAreSeparateAndStable() {
        assertEquals(NavigationRoutePolicy.quizSessionKey("FOOD"),NavigationRoutePolicy.quizSessionKey("FOOD"));
        assertFalse(NavigationRoutePolicy.quizSessionKey(null).equals(NavigationRoutePolicy.quizSessionKey("FOOD")));
        assertFalse(NavigationRoutePolicy.quizSessionKey("FOOD").equals(NavigationRoutePolicy.quizSessionKey("WORK")));
    }
}
