package com.example.bivoca.data.repository;

import com.example.bivoca.data.api.AuthApi;
import com.example.bivoca.data.model.LoginRequest;
import com.example.bivoca.data.model.LoginResponse;
import com.example.bivoca.data.model.RegisterRequest;
import com.example.bivoca.data.model.User;
import com.example.bivoca.data.model.GoogleChallengeResponse;
import com.example.bivoca.data.model.GoogleLoginRequest;
import com.example.bivoca.data.model.GoogleLinkRequest;
import com.example.bivoca.data.model.ForgotPasswordRequest;
import com.example.bivoca.data.model.MessageResponse;
import com.google.gson.Gson;

import org.junit.Test;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AuthRepositoryRegistrationErrorTest {
    @Test public void readsBackendConflictCode() {
        Response<?> response = Response.error(409, ResponseBody.create(
                MediaType.parse("application/json"),
                "{\"error\":{\"code\":\"EMAIL_ALREADY_EXISTS\"}}"));
        assertEquals("EMAIL_ALREADY_EXISTS", AuthRepository.registrationErrorCode(response));
    }

    @Test public void malformedErrorBodyDoesNotCrashRegistration() {
        Response<?> response = Response.error(409, ResponseBody.create(
                MediaType.parse("text/plain"), "unavailable"));
        assertNull(AuthRepository.registrationErrorCode(response));
    }

    @Test public void successfulRegistrationUsesRealApiCallback() {
        LoginResponse body = new Gson().fromJson("{}", LoginResponse.class);
        Result result = registerWith(Response.success(201, body), null);
        assertNotNull(result.success);
        assertNull(result.error);
    }

    @Test public void conflictCodesHaveDistinctVietnameseMessages() {
        Result email = registerWith(error(409, "EMAIL_ALREADY_EXISTS"), null);
        Result username = registerWith(error(409, "USERNAME_ALREADY_EXISTS"), null);
        assertEquals("Email đã được sử dụng.", email.error);
        assertEquals("Tên đăng nhập đã được sử dụng.", username.error);
        assertNull(email.success);
        assertNull(username.success);
    }

    @Test public void networkFailureAndUnavailableServerAreReported() {
        Result network = registerWith(null, new IOException("offline"));
        Result server = registerWith(error(503, "SERVICE_UNAVAILABLE"), null);
        assertEquals("Không kết nối được máy chủ.", network.error);
        assertEquals("Máy chủ tạm thời không hoạt động. Vui lòng thử lại sau.", server.error);
    }

    @Test public void googleRouteNotFoundIsNotShownAsTechnicalBackendText() {
        Response<?> response = errorBody(404, "ROUTE_NOT_FOUND", "Route not found");
        assertEquals("Máy chủ hiện chưa hỗ trợ đăng nhập Google. Vui lòng cập nhật Backend.",
                AuthRepository.googleChallengeError(response));
        assertEquals("Máy chủ hiện chưa hỗ trợ đăng nhập Google. Vui lòng cập nhật Backend.",
                AuthRepository.googleLoginError(response));
    }

    @Test public void googleErrorsAreMappedToClearVietnameseMessages() {
        assertEquals("Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.",
                AuthRepository.googleLoginError(errorBody(401, "GOOGLE_TOKEN_INVALID", "technical")));
        assertEquals("Email này đã có tài khoản BiVoca. Hãy đăng nhập bằng mật khẩu để liên kết Google.",
                AuthRepository.googleLoginError(errorBody(409, "GOOGLE_ACCOUNT_LINK_REQUIRED", "technical")));
        assertEquals("Đăng nhập Google chưa được cấu hình đúng trên máy chủ.",
                AuthRepository.googleLoginError(errorBody(503, "GOOGLE_AUTH_NOT_CONFIGURED", "technical")));
    }

    private static Response<LoginResponse> error(int status, String code) {
        return Response.error(status, ResponseBody.create(MediaType.parse("application/json"),
                "{\"error\":{\"code\":\"" + code + "\"}}"));
    }

    private static Response<?> errorBody(int status, String code, String message) {
        return Response.error(status, ResponseBody.create(MediaType.parse("application/json"),
                "{\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}"));
    }

    private static Result registerWith(Response<LoginResponse> response, Throwable failure) {
        AuthApi api = new AuthApi() {
            @Override public Call<LoginResponse> register(RegisterRequest request) {
                assertEquals("test_user", request.getUsername());
                assertNull(request.getFullName());
                assertFalse(new Gson().toJson(request).contains("fullName"));
                assertFalse(new Gson().toJson(request).contains("confirmPassword"));
                return new ImmediateCall<>(response, failure);
            }
            @Override public Call<LoginResponse> login(LoginRequest request) { throw new AssertionError(); }
            @Override public Call<GoogleChallengeResponse> googleChallenge() { throw new AssertionError(); }
            @Override public Call<LoginResponse> googleLogin(GoogleLoginRequest request) { throw new AssertionError(); }
            @Override public Call<LoginResponse> linkGoogle(GoogleLinkRequest request) { throw new AssertionError(); }
            @Override public Call<MessageResponse> forgotPassword(ForgotPasswordRequest request) { throw new AssertionError(); }
            @Override public Call<User> getMe() { throw new AssertionError(); }
        };
        Result result = new Result();
        new AuthRepository(api).register("test_user", "user@example.com", "password8", null,
                new AuthRepository.RegisterCallback() {
                    @Override public void onSuccess(LoginResponse value) { result.success = value; }
                    @Override public void onError(String message) { result.error = message; }
                });
        return result;
    }

    private static final class Result {
        LoginResponse success;
        String error;
    }

    private static final class ImmediateCall<T> implements Call<T> {
        final Response<T> response;
        final Throwable failure;
        boolean canceled;
        ImmediateCall(Response<T> response, Throwable failure) {
            this.response = response;
            this.failure = failure;
        }
        @Override public void enqueue(Callback<T> callback) {
            if (failure != null) callback.onFailure(this, failure);
            else callback.onResponse(this, response);
        }
        @Override public Response<T> execute() throws IOException {
            if (failure instanceof IOException) throw (IOException) failure;
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
