package com.example.bivoca;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bivoca.data.api.AuthApi;
import com.example.bivoca.data.api.RetrofitClient;
import com.example.bivoca.data.model.LoginRequest;
import com.example.bivoca.data.model.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BIVOCA";

    private EditText editEmail;
    private EditText editPassword;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        buttonLogin.setOnClickListener(view -> {

            String email =
                    editEmail.getText().toString().trim();

            String password =
                    editPassword.getText().toString();

            if (email.isEmpty()) {
                editEmail.setError("Nhập email");
                return;
            }

            if (password.isEmpty()) {
                editPassword.setError("Nhập mật khẩu");
                return;
            }

            testLogin(email, password);
        });
    }

    private void testLogin(String email, String password) {

        buttonLogin.setEnabled(false);

        AuthApi authApi = RetrofitClient
                .getInstance()
                .create(AuthApi.class);

        LoginRequest request =
                new LoginRequest(email, password);

        authApi.login(request)
                .enqueue(new Callback<LoginResponse>() {

                    @Override
                    public void onResponse(
                            Call<LoginResponse> call,
                            Response<LoginResponse> response
                    ) {

                        buttonLogin.setEnabled(true);

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getUser() != null) {

                            String username =
                                    response.body()
                                            .getUser()
                                            .getUsername();

                            Toast.makeText(
                                    MainActivity.this,
                                    "Đăng nhập thành công: "
                                            + username,
                                    Toast.LENGTH_LONG
                            ).show();

                            Log.d(
                                    TAG,
                                    "HTTP "
                                            + response.code()
                                            + " - Login OK - "
                                            + username
                            );

                        } else {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Đăng nhập thất bại. HTTP "
                                            + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();

                            Log.e(
                                    TAG,
                                    "Login failed - HTTP "
                                            + response.code()
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<LoginResponse> call,
                            Throwable throwable
                    ) {

                        buttonLogin.setEnabled(true);

                        Toast.makeText(
                                MainActivity.this,
                                "Không kết nối được backend",
                                Toast.LENGTH_LONG
                        ).show();

                        Log.e(
                                TAG,
                                "Backend connection error",
                                throwable
                        );
                    }
                });
    }
}