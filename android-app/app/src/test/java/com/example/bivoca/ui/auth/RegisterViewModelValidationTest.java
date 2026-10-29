package com.example.bivoca.ui.auth;

import com.example.bivoca.R;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RegisterViewModelValidationTest {
    @Test public void optionalFullNameAndValidFormPass() {
        assertNull(RegisterViewModel.validate("", "test_user", "user@example.com",
                "password8", "password8"));
    }

    @Test public void rejectsMissingOrInvalidUsername() {
        assertEquals(RegisterViewModel.Field.USERNAME,
                RegisterViewModel.validate("", "", "user@example.com", "password8", "password8").field);
        assertEquals(R.string.register_username_invalid,
                RegisterViewModel.validate("", "tên", "user@example.com", "password8", "password8").message);
    }

    @Test public void rejectsInvalidEmail() {
        assertEquals(RegisterViewModel.Field.EMAIL,
                RegisterViewModel.validate("", "test_user", "invalid", "password8", "password8").field);
    }

    @Test public void enforcesPasswordLengthAndUtf8ByteLimit() {
        assertEquals(R.string.register_password_short,
                RegisterViewModel.validate("", "test_user", "user@example.com", "short", "short").message);
        String longPassword = "á".repeat(37);
        assertEquals(R.string.register_password_long,
                RegisterViewModel.validate("", "test_user", "user@example.com",
                        longPassword, longPassword).message);
    }

    @Test public void rejectsMismatchedConfirmation() {
        assertEquals(RegisterViewModel.Field.CONFIRM_PASSWORD,
                RegisterViewModel.validate("", "test_user", "user@example.com",
                        "password8", "password9").field);
    }
}
