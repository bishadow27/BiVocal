package com.example.bivoca.data.repository;

import android.content.Context;

import com.example.bivoca.data.api.AuthApi;
import com.example.bivoca.data.api.AuthenticatedRetrofitClient;
import com.example.bivoca.data.api.RetrofitClient;
import com.example.bivoca.data.model.LoginRequest;
import com.example.bivoca.data.model.LoginResponse;
import com.example.bivoca.data.model.RegisterRequest;
import com.example.bivoca.data.model.GoogleChallengeResponse;
import com.example.bivoca.data.model.GoogleLoginRequest;
import com.example.bivoca.data.model.GoogleLinkRequest;
import com.example.bivoca.data.model.ForgotPasswordRequest;
import com.example.bivoca.data.model.MessageResponse;
import com.google.gson.JsonParser;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final AuthApi authApi;

    public AuthRepository() {

        this(RetrofitClient
                .getInstance()
                .create(AuthApi.class));
    }

    AuthRepository(AuthApi authApi) {
        this.authApi = authApi;
    }

    public static AuthRepository authenticated(Context context) {
        return new AuthRepository(AuthenticatedRetrofitClient.getInstance(context)
                .create(AuthApi.class));
    }

    // =========================
    // Login callback
    // =========================

    public interface LoginCallback {

        void onSuccess(LoginResponse response);

        void onError(String message);
    }

    // =========================
    // Register callback
    // =========================

    public interface RegisterCallback {

        void onSuccess(LoginResponse response);

        void onError(String message);
    }

    public interface ResultCallback<T> {
        void onSuccess(T value);
        void onError(String message);
    }

    public void requestGoogleChallenge(ResultCallback<GoogleChallengeResponse> callback) {
        authApi.googleChallenge().enqueue(new Callback<GoogleChallengeResponse>() {
            @Override public void onResponse(Call<GoogleChallengeResponse> call, Response<GoogleChallengeResponse> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body());
                else callback.onError(googleChallengeError(response));
            }
            @Override public void onFailure(Call<GoogleChallengeResponse> call, Throwable throwable) {
                callback.onError("Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.");
            }
        });
    }

    public void loginWithGoogle(String idToken, GoogleChallengeResponse challenge, LoginCallback callback) {
        authApi.googleLogin(new GoogleLoginRequest(idToken, challenge.getChallengeId(), challenge.getNonce()))
                .enqueue(new Callback<LoginResponse>() {
                    @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body());
                        else callback.onError(googleLoginError(response));
                    }
                    @Override public void onFailure(Call<LoginResponse> call, Throwable throwable) {
                        callback.onError("Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.");
                    }
                });
    }

    public void forgotPassword(String email, ResultCallback<String> callback) {
        authApi.forgotPassword(new ForgotPasswordRequest(email)).enqueue(new Callback<MessageResponse>() {
            @Override public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body().getMessage());
                else callback.onError(response.code() == 503
                        ? "Dịch vụ gửi email khôi phục chưa được cấu hình."
                        : "Không thể gửi yêu cầu khôi phục.");
            }
            @Override public void onFailure(Call<MessageResponse> call, Throwable throwable) {
                callback.onError("Không thể kết nối đến máy chủ. Vui lòng kiểm tra Internet và thử lại.");
            }
        });
    }

    public void linkGoogle(String idToken, GoogleChallengeResponse challenge, String password,
                           LoginCallback callback) {
        authApi.linkGoogle(new GoogleLinkRequest(idToken, challenge.getChallengeId(),
                challenge.getNonce(), password)).enqueue(new Callback<LoginResponse>() {
            @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body());
                else if (response.code() == 401) callback.onError("Mật khẩu xác minh không đúng.");
                else if (response.code() == 409) callback.onError("Tài khoản Google không thể liên kết với tài khoản này.");
                else callback.onError("Không thể liên kết tài khoản Google.");
            }
            @Override public void onFailure(Call<LoginResponse> call, Throwable throwable) {
                callback.onError("Không thể kết nối đến máy chủ. Vui lòng thử lại.");
            }
        });
    }

    static String googleChallengeError(Response<?> response) {
        if (response.code() == 404) {
            return "Máy chủ hiện chưa hỗ trợ đăng nhập Google. Vui lòng cập nhật Backend.";
        }
        if (response.code() == 429) {
            return "Bạn đã thử đăng nhập Google quá nhiều lần. Vui lòng thử lại sau.";
        }
        if (response.code() >= 500) {
            return "Máy chủ tạm thời không thể bắt đầu đăng nhập Google.";
        }
        return "Không thể bắt đầu đăng nhập Google.";
    }

    static String googleLoginError(Response<?> response) {
        String code = apiErrorCode(response);
        if (response.code() == 404) {
            return "Máy chủ hiện chưa hỗ trợ đăng nhập Google. Vui lòng cập nhật Backend.";
        }
        if (response.code() == 409 && "GOOGLE_ACCOUNT_LINK_REQUIRED".equals(code)) {
            return "Email này đã có tài khoản BiVoca. Hãy đăng nhập bằng mật khẩu để liên kết Google.";
        }
        if (response.code() == 401 && ("GOOGLE_TOKEN_INVALID".equals(code)
                || "GOOGLE_CHALLENGE_INVALID".equals(code))) {
            return "Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.";
        }
        if (response.code() == 503 && "GOOGLE_AUTH_NOT_CONFIGURED".equals(code)) {
            return "Đăng nhập Google chưa được cấu hình đúng trên máy chủ.";
        }
        if (response.code() == 429) {
            return "Bạn đã thử đăng nhập Google quá nhiều lần. Vui lòng thử lại sau.";
        }
        if (response.code() >= 500) {
            return "Máy chủ tạm thời không thể xử lý đăng nhập Google.";
        }
        return "Đăng nhập Google không thành công.";
    }

    private static String apiErrorCode(Response<?> response) {
        if (response.errorBody() == null) return null;
        try {
            return JsonParser.parseString(response.errorBody().string()).getAsJsonObject()
                    .getAsJsonObject("error").get("code").getAsString();
        } catch (IOException | RuntimeException ignored) { return null; }
    }

    // =========================
    // Login
    // =========================

    public void login(
            String email,
            String password,
            LoginCallback callback
    ) {

        LoginRequest request =
                new LoginRequest(
                        email,
                        password
                );

        authApi.login(request)
                .enqueue(new Callback<LoginResponse>() {

                    @Override
                    public void onResponse(
                            Call<LoginResponse> call,
                            Response<LoginResponse> response
                    ) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            callback.onSuccess(
                                    response.body()
                            );

                            return;
                        }

                        if (response.code() == 401) {

                            callback.onError(
                                    "Email hoặc mật khẩu không đúng."
                            );

                        } else {

                            callback.onError(
                                    "Đăng nhập thất bại. HTTP "
                                            + response.code()
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<LoginResponse> call,
                            Throwable throwable
                    ) {

                        callback.onError(
                                "Không kết nối được máy chủ."
                        );
                    }
                });
    }

    // =========================
    // Register
    // =========================

    public void register(
            String username,
            String email,
            String password,
            String fullName,
            RegisterCallback callback
    ) {

        RegisterRequest request =
                new RegisterRequest(
                        username,
                        email,
                        password,
                        fullName
                );

        authApi.register(request)
                .enqueue(new Callback<LoginResponse>() {

                    @Override
                    public void onResponse(
                            Call<LoginResponse> call,
                            Response<LoginResponse> response
                    ) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            callback.onSuccess(
                                    response.body()
                            );

                            return;
                        }

                        String errorCode = registrationErrorCode(response);
                        if (response.code() == 409 && "EMAIL_ALREADY_EXISTS".equals(errorCode)) {
                            callback.onError("Email đã được sử dụng.");
                        } else if (response.code() == 409 && "USERNAME_ALREADY_EXISTS".equals(errorCode)) {
                            callback.onError("Tên đăng nhập đã được sử dụng.");
                        } else if (response.code() == 409) {
                            callback.onError("Email hoặc tên đăng nhập đã tồn tại.");
                        } else if (response.code() == 400) {

                            callback.onError(
                                    "Thông tin đăng ký chưa hợp lệ."
                            );

                        } else if (response.code() == 502 || response.code() == 503 || response.code() == 504) {
                            callback.onError("Máy chủ tạm thời không hoạt động. Vui lòng thử lại sau.");
                        } else {

                            callback.onError(
                                    "Đăng ký thất bại. HTTP "
                                            + response.code()
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<LoginResponse> call,
                            Throwable throwable
                    ) {

                        callback.onError(
                                "Không kết nối được máy chủ."
                        );
                    }
                });
    }

    static String registrationErrorCode(Response<?> response) {
        if (response.errorBody() == null) return null;
        try {
            return JsonParser.parseString(response.errorBody().string())
                    .getAsJsonObject().getAsJsonObject("error")
                    .get("code").getAsString();
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }
}
