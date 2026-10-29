package com.example.bivoca.ui.auth;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.example.bivoca.data.model.LoginResponse;
import com.example.bivoca.data.repository.AuthRepository;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RegisterViewModelStateTest {
    private PendingRepository repository;
    private RegisterViewModel model;

    @Before public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable runnable) { runnable.run(); }
            @Override public void postToMainThread(Runnable runnable) { runnable.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        repository = new PendingRepository();
        model = new RegisterViewModel(repository);
    }

    @After public void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test public void invalidFormDoesNotCallBackend() {
        model.register("", "test_user", "invalid", "password8", "password8");
        assertEquals(RegisterViewModel.Field.EMAIL, model.getValidationIssue().getValue().field);
        assertEquals(0, repository.calls);
        assertFalse(model.getLoading().getValue());
    }

    @Test public void loadingPreventsDuplicateRequestAndFailureAllowsRetry() {
        model.register("", "test_user", "user@example.com", "password8", "password8");
        model.register("", "test_user", "user@example.com", "password8", "password8");
        assertEquals(1, repository.calls);
        assertTrue(model.getLoading().getValue());
        assertNull(repository.fullName);
        repository.callback.onError("Không kết nối được máy chủ.");
        assertFalse(model.getLoading().getValue());
        assertEquals("Không kết nối được máy chủ.", model.getError().getValue());
        model.register("", "test_user", "user@example.com", "password8", "password8");
        assertEquals(2, repository.calls);
    }

    @Test public void successfulResponseEndsLoadingAndPublishesResult() {
        model.register("A", "test_user", "user@example.com", "password8", "password8");
        assertEquals("A", repository.fullName);
        LoginResponse response = new Gson().fromJson("{}", LoginResponse.class);
        repository.callback.onSuccess(response);
        assertFalse(model.getLoading().getValue());
        assertNotNull(model.getRegisterResult().getValue());
    }

    private static final class PendingRepository extends AuthRepository {
        int calls;
        String fullName;
        RegisterCallback callback;

        @Override public void register(String username, String email, String password,
                                       String fullName, RegisterCallback callback) {
            calls++;
            this.fullName = fullName;
            this.callback = callback;
        }
    }
}
