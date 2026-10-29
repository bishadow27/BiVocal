package com.example.bivoca.data.api;

import android.content.Context;

import com.example.bivoca.utils.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthenticatedRetrofitClient {

    private static volatile Retrofit retrofit;

    private AuthenticatedRetrofitClient() {
        // Không cho tạo object từ bên ngoài
    }

    public static Retrofit getInstance(Context context) {
        if (retrofit == null) {
            synchronized (AuthenticatedRetrofitClient.class) {
                if (retrofit == null) {
                    Context appContext = context.getApplicationContext();
                    SessionManager sessionManager = new SessionManager(appContext);
                    AuthInterceptor authInterceptor = new AuthInterceptor(appContext, sessionManager);

                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    // Chỉ log request cơ bản. Không dùng BODY để tránh log token/password.
                    logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(authInterceptor)
                            .addInterceptor(logging)
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(ApiBaseUrl.current())
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }
}
