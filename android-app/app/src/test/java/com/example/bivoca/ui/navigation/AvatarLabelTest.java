package com.example.bivoca.ui.navigation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AvatarLabelTest {
    @Test public void derivesInitialFromCurrentUsername() {
        assertEquals("D", AvatarLabel.initial(" duyphuc "));
        assertEquals("Đ", AvatarLabel.initial("đạt"));
    }

    @Test public void usesBrandFallbackWhenUsernameMissing() {
        assertEquals("B", AvatarLabel.initial(null));
        assertEquals("B", AvatarLabel.initial("   "));
    }
}
