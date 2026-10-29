package com.example.bivoca.data.api;

import com.example.bivoca.data.model.LoginRequest;
import com.example.bivoca.data.model.LoginResponse;
import com.example.bivoca.data.model.RegisterRequest;
import com.example.bivoca.data.model.User;
import com.example.bivoca.data.model.GoogleChallengeResponse;
import com.example.bivoca.data.model.GoogleLoginRequest;
import com.example.bivoca.data.model.GoogleLinkRequest;
import com.example.bivoca.data.model.ForgotPasswordRequest;
import com.example.bivoca.data.model.MessageResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("auth/login")
    Call<LoginResponse> login(
            @Body LoginRequest request
    );

    @POST("auth/register")
    Call<LoginResponse> register(
            @Body RegisterRequest request
    );

    @POST("auth/google/challenge")
    Call<GoogleChallengeResponse> googleChallenge();

    @POST("auth/google")
    Call<LoginResponse> googleLogin(@Body GoogleLoginRequest request);

    @POST("auth/google/link")
    Call<LoginResponse> linkGoogle(@Body GoogleLinkRequest request);

    @POST("auth/forgot-password")
    Call<MessageResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @GET("auth/me")
    Call<User> getMe();
}
