package com.hcmute.studymate.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hcmute.studymate.R;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.model.UserProfile;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.AuthCallback;

public class LoginActivity extends AppCompatActivity {
    private AuthController authController;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private ProgressBar loadingProgress;
    private TextView registerLinkText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authController = AppContainer.authController();
        if (authController.isLoggedIn()) {
            openNotes();
            return;
        }

        setContentView(R.layout.activity_login);
        emailLayout = findViewById(R.id.loginEmailLayout);
        passwordLayout = findViewById(R.id.loginPasswordLayout);
        emailInput = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);
        loginButton = findViewById(R.id.loginButton);
        loadingProgress = findViewById(R.id.loginLoadingProgress);
        registerLinkText = findViewById(R.id.registerLinkText);

        loginButton.setOnClickListener(view -> signIn());
        registerLinkText.setOnClickListener(view -> startActivity(new Intent(this, RegisterActivity.class)));
        emailInput.addTextChangedListener(validationWatcher);
        passwordInput.addTextChangedListener(validationWatcher);
        updateLoginButton();
    }

    private void signIn() {
        String email = readInput(emailInput);
        String password = readInput(passwordInput);
        if (!validateInputs(true)) {
            return;
        }

        setLoading(true);
        authController.signIn(email, password, new AuthCallback() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                setLoading(false);
                openNotes();
            }

            @Override
            public void onError(Exception exception) {
                setLoading(false);
                showError("Could not sign in", exception);
            }
        });
    }

    private boolean validateInputs(boolean showErrors) {
        boolean hasEmail = !readInput(emailInput).isEmpty();
        boolean hasPassword = !readInput(passwordInput).isEmpty();

        if (showErrors) {
            emailLayout.setError(hasEmail ? null : "Email is required");
            passwordLayout.setError(hasPassword ? null : "Password is required");
        } else {
            if (hasEmail) {
                emailLayout.setError(null);
            }
            if (hasPassword) {
                passwordLayout.setError(null);
            }
        }
        return hasEmail && hasPassword;
    }

    private void updateLoginButton() {
        loginButton.setEnabled(validateInputs(false));
    }

    private void setLoading(boolean loading) {
        loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading && validateInputs(false));
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        registerLinkText.setEnabled(!loading);
    }

    private void openNotes() {
        Intent intent = new Intent(this, NoteListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String readInput(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void showError(String prefix, Exception exception) {
        String detail = exception == null || exception.getMessage() == null
                ? "Please check your email, password, and Firebase setup."
                : exception.getMessage();
        Toast.makeText(this, prefix + ". " + detail, Toast.LENGTH_LONG).show();
    }

    private final TextWatcher validationWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateLoginButton();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };
}
