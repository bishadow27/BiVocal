package com.example.bivoca.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bivoca.data.api.AuthenticatedRetrofitClient;
import com.example.bivoca.data.api.GamificationApi;
import com.example.bivoca.data.model.LeaderboardUser;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardRepository {

    private final GamificationApi api;

    public LeaderboardRepository(Context context) {
        this.api = AuthenticatedRetrofitClient.getInstance(context).create(GamificationApi.class);
    }

    LeaderboardRepository(GamificationApi api) {
        this.api = api;
    }

    public interface LeaderboardCallback {
        void onSuccess(List<LeaderboardUser> users);
        void onError(String message);
    }

    public Call<List<LeaderboardUser>> getLeaderboard(@Nullable Integer limit, LeaderboardCallback callback) {
        Call<List<LeaderboardUser>> call = limit != null && limit > 0
                ? api.getLeaderboard(limit)
                : api.getLeaderboard();

        call.enqueue(new Callback<List<LeaderboardUser>>() {
            @Override
            public void onResponse(@NonNull Call<List<LeaderboardUser>> request, @NonNull Response<List<LeaderboardUser>> response) {
                if (request.isCanceled()) return;
                if (response.isSuccessful()) {
                    List<LeaderboardUser> body = response.body();
                    callback.onSuccess(body != null ? body : Collections.emptyList());
                    return;
                }
                if (response.errorBody() != null) response.errorBody().close();
                callback.onError("Không tải được bảng xếp hạng (HTTP " + response.code() + ").");
            }

            @Override
            public void onFailure(@NonNull Call<List<LeaderboardUser>> request, @NonNull Throwable t) {
                if (request.isCanceled()) return;
                callback.onError(t instanceof IOException ? "Không kết nối được máy chủ." : "Lỗi tải bảng xếp hạng.");
            }
        });
        return call;
    }
}
