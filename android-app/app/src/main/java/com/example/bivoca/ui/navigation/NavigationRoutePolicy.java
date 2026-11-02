package com.example.bivoca.ui.navigation;

/** Pure navigation rules kept separate so they can be unit tested without Android. */
public final class NavigationRoutePolicy {

    private NavigationRoutePolicy() {
    }

    public static NavigationRoute selectedMenuFor(NavigationRoute screen) {
        if (screen == NavigationRoute.NEW_TODAY) return NavigationRoute.HOME;
        return screen == NavigationRoute.FLASHCARDS || screen == NavigationRoute.TOPIC_DETAIL
                ? NavigationRoute.TOPICS : screen;
    }

    /** Existing ViewModel classes are retained independently for distinct learning contexts. */
    public static String flashcardSessionKey(boolean review, boolean fresh, String topicCode, int limit) {
        if (review) return "learning:review";
        if (fresh) return "learning:new"; // Home remaining/goal is a load option, not a new session.
        return "learning:browse:" + (topicCode == null ? "<none>" : topicCode);
    }

    public static String quizSessionKey(String topicCode) {
        return topicCode == null ? "learning:quiz:all" : "learning:quiz:topic:" + topicCode;
    }

    public static boolean shouldReuseCurrentScreen(NavigationRoute screen,
                                                   NavigationRoute destination) {
        return screen == destination;
    }
}
