package com.example.bivoca.data.repository;

import com.example.bivoca.data.api.GamificationApi;
import com.example.bivoca.data.model.GamificationStats;
import com.example.bivoca.data.model.LeaderboardUser;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class LeaderboardRepositoryTest {

    @Test
    public void emptyResponseReturnsEmptyList() {
        final List<?>[] result = new List<?>[1];
        final String[] error = new String[1];
        LeaderboardRepository repository = repositoryWith(Response.success(Collections.emptyList()), null);

        repository.getLeaderboard(50, new LeaderboardRepository.LeaderboardCallback() {
            @Override
            public void onSuccess(List<LeaderboardUser> users) {
                result[0] = users;
            }

            @Override
            public void onError(String message) {
                error[0] = message;
            }
        });

        assertNotNull(result[0]);
        assertEquals(0, result[0].size());
        assertNull(error[0]);
    }

    @Test
    public void unauthorizedAndForbiddenResponsesAreReportedWithoutSuccess() {
        assertHttpErrorReported(401);
        assertHttpErrorReported(403);
    }

    @Test
    public void networkFailureIsReportedAsConnectionError() {
        final String[] error = new String[1];
        LeaderboardRepository repository = repositoryWith(null, new IOException("offline"));

        repository.getLeaderboard(50, new LeaderboardRepository.LeaderboardCallback() {
            @Override
            public void onSuccess(List<LeaderboardUser> users) {
                throw new AssertionError("Network failure must not produce success");
            }

            @Override
            public void onError(String message) {
                error[0] = message;
            }
        });

        assertEquals("Không kết nối được máy chủ.", error[0]);
    }

    private static void assertHttpErrorReported(int statusCode) {
        final String[] error = new String[1];
        LeaderboardRepository repository = repositoryWith(
                Response.error(statusCode, ResponseBody.create(MediaType.parse("text/plain"), new byte[0])),
                null);

        repository.getLeaderboard(50, new LeaderboardRepository.LeaderboardCallback() {
            @Override
            public void onSuccess(List<LeaderboardUser> users) {
                throw new AssertionError("HTTP error must not produce success");
            }

            @Override
            public void onError(String message) {
                error[0] = message;
            }
        });

        assertNotNull(error[0]);
        assertEquals(true, error[0].contains("HTTP " + statusCode));
    }

    private static LeaderboardRepository repositoryWith(Response<List<LeaderboardUser>> response,
                                                          Throwable failure) {
        GamificationApi api = new GamificationApi() {
            @Override
            public Call<GamificationStats> getMyStats() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Call<List<LeaderboardUser>> getLeaderboard(Integer limit) {
                return new ImmediateCall<>(response, failure);
            }

            @Override
            public Call<List<LeaderboardUser>> getLeaderboard() {
                return new ImmediateCall<>(response, failure);
            }
        };
        return new LeaderboardRepository(api);
    }

    private static final class ImmediateCall<T> implements Call<T> {
        private final Response<T> response;
        private final Throwable failure;
        private boolean canceled;

        private ImmediateCall(Response<T> response, Throwable failure) {
            this.response = response;
            this.failure = failure;
        }

        @Override
        public void enqueue(Callback<T> callback) {
            if (failure != null) {
                callback.onFailure(this, failure);
            } else {
                callback.onResponse(this, response);
            }
        }

        @Override
        public Response<T> execute() throws IOException {
            if (failure instanceof IOException) throw (IOException) failure;
            if (failure != null) throw new IOException(failure);
            return response;
        }

        @Override public void cancel() { canceled = true; }
        @Override public boolean isExecuted() { return true; }
        @Override public boolean isCanceled() { return canceled; }
        @Override public Call<T> clone() { return new ImmediateCall<>(response, failure); }
        @Override public Request request() { return new Request.Builder().url("http://localhost/").build(); }
        @Override public Timeout timeout() { return Timeout.NONE; }
    }
}
