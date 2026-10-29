package com.example.bivoca.ui.navigation;

/** Pure navigation rules kept separate so they can be unit tested without Android. */
public final class NavigationRoutePolicy {

    private NavigationRoutePolicy() {
    }

    public static NavigationRoute selectedMenuFor(NavigationRoute screen) {
        return screen == NavigationRoute.FLASHCARDS ? NavigationRoute.TOPICS : screen;
    }

    public static boolean shouldReuseCurrentScreen(NavigationRoute screen,
                                                   NavigationRoute destination) {
        return screen == destination;
    }
}
