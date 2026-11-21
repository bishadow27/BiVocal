package com.example.bivoca.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.bivoca.R;

public class QuenMatKhauActivity extends AppCompatActivity {

    private EditText editEmail;
    private TextView textFeedback;
    private ForgotPasswordViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quen_mat_khau);

        ((ImageView) findViewById(R.id.authMascot)).setImageResource(R.drawable.bivocal_brand_horizontal);
        ((TextView) findViewById(R.id.authScreenTitle)).setText(R.string.auth_recovery_title);

        View root = findViewById(R.id.forgotRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        editEmail = findViewById(R.id.forgotEditEmail);
        textFeedback = findViewById(R.id.forgotFeedback);
        Button buttonSubmit = findViewById(R.id.forgotButtonSubmit);
        ProgressBar progressBar = findViewById(R.id.forgotProgressBar);
        TextView textBack = findViewById(R.id.forgotBackLogin);

        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);
        viewModel.getFeedback().observe(this, feedback -> {
            if (feedback == null) {
                textFeedback.setVisibility(View.GONE);
                return;
            }
            if (feedback.messageText != null) textFeedback.setText(feedback.messageText);
            else textFeedback.setText(feedback.message);
            textFeedback.setTextColor(getColor(feedback.validationError
                    ? R.color.forgot_error : R.color.forgot_notice));
            textFeedback.setVisibility(View.VISIBLE);
        });
        viewModel.getLoading().observe(this, loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            buttonSubmit.setEnabled(!busy);
            buttonSubmit.setText(busy ? R.string.forgot_sending : R.string.forgot_submit);
            progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        });

        editEmail.setOnFocusChangeListener((view, focused) ->
                findViewById(R.id.forgotEmailFieldContainer).setSelected(focused));
        editEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                viewModel.clearFeedback();
            }
            @Override public void afterTextChanged(Editable text) { }
        });
        buttonSubmit.setOnClickListener(view -> viewModel.submit(editEmail.getText().toString()));
        textBack.setOnClickListener(view -> finish());
    }
}
