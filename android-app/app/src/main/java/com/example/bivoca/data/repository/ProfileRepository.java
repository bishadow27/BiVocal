package com.example.bivoca.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.bivoca.data.api.AuthApi;
import com.example.bivoca.data.api.AuthenticatedRetrofitClient;
import com.example.bivoca.data.api.GamificationApi;
import com.example.bivoca.data.model.GamificationStats;
import com.example.bivoca.data.model.User;

import java.io.IOException;
import java.net.SocketTimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepository {

    private final AuthApi authApi;
    private final GamificationApi gamificationApi;

    public ProfileRepository(Context context) {
        this.authApi = AuthenticatedRetrofitClient.getInstance(context).create(AuthApi.class);
        this.gamificationApi = AuthenticatedRetrofitClient.getInstance(context).create(GamificationApi.class);
    }

    // Constructor phục vụ test
    ProfileRepository(AuthApi authApi, GamificationApi gamificationApi) {
        this.authApi = authApi;
        this.gamificationApi = gamificationApi;
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface StatsCallback {
        void onSuccess(GamificationStats stats);
        void onError(String message);
    }

    public Call<User> getUserProfile(UserCallback callback) {
        Call<User> call = authApi.getMe();
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> request, @NonNull Response<User> response) {
                if (request.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.errorBody() != null) response.errorBody().close();
                if (response.code() == 401) {
                    callback.onError("Phiên đăng nhập đã hết hạn.");
                } else {
                    callback.onError("Không tải được thông tin cá nhân (HTTP " + response.code() + ").");
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> request, @NonNull Throwable t) {
                if (request.isCanceled()) return;
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Máy chủ phản hồi quá lâu. Hãy thử lại.");
                } else if (t instanceof IOException) {
                    callback.onError("Không kết nối được máy chủ.");
                } else {
                    callback.onError("Lỗi khi tải thông tin cá nhân.");
                }
            }
        });
        return call;
    }

    public Call<GamificationStats> getGamificationStats(StatsCallback callback) {
        Call<GamificationStats> call = gamificationApi.getMyStats();
        call.enqueue(new Callback<GamificationStats>() {
            @Override
            public void onResponse(@NonNull Call<GamificationStats> request, @NonNull Response<GamificationStats> response) {
                if (request.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.errorBody() != null) response.errorBody().close();
                if (response.code() == 401) {
                    callback.onError("Phiên đăng nhập đã hết hạn.");
                } else {
                    callback.onError("Không tải được thành tích học tập (HTTP " + response.code() + ").");
                }
            }

            @Override
            public void onFailure(@NonNull Call<GamificationStats> request, @NonNull Throwable t) {
                if (request.isCanceled()) return;
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Máy chủ phản hồi quá lâu. Hãy thử lại.");
                } else if (t instanceof IOException) {
                    callback.onError("Không kết nối được máy chủ.");
                } else {
                    callback.onError("Lỗi khi tải thành tích học tập.");
                }
            }
        });
        return call;
    }
}
