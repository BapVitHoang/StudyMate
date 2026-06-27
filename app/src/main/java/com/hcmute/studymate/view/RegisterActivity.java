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

public class RegisterActivity extends AppCompatActivity {
    private AuthController authController;
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton registerButton;
    private ProgressBar loadingProgress;
    private TextView loginLinkText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authController = AppContainer.authController();
        if (authController.isLoggedIn()) {
            openNotes();
            return;
        }

        setContentView(R.layout.activity_register);
        nameLayout = findViewById(R.id.registerNameLayout);
        emailLayout = findViewById(R.id.registerEmailLayout);
        passwordLayout = findViewById(R.id.registerPasswordLayout);
        nameInput = findViewById(R.id.registerNameInput);
        emailInput = findViewById(R.id.registerEmailInput);
        passwordInput = findViewById(R.id.registerPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        loadingProgress = findViewById(R.id.registerLoadingProgress);
        loginLinkText = findViewById(R.id.loginLinkText);

        registerButton.setOnClickListener(view -> register());
        loginLinkText.setOnClickListener(view -> finish());
        nameInput.addTextChangedListener(validationWatcher);
        emailInput.addTextChangedListener(validationWatcher);
        passwordInput.addTextChangedListener(validationWatcher);
        updateRegisterButton();
    }

    private void register() {
        String displayName = readInput(nameInput);
        String email = readInput(emailInput);
        String password = readInput(passwordInput);
        if (!validateInputs(true)) {
            return;
        }

        setLoading(true);
        authController.register(email, password, displayName, new AuthCallback() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                setLoading(false);
                openNotes();
            }

            @Override
            public void onError(Exception exception) {
                setLoading(false);
                showError("Could not create account", exception);
            }
        });
    }

    private boolean validateInputs(boolean showErrors) {
        boolean hasName = !readInput(nameInput).isEmpty();
        boolean hasEmail = !readInput(emailInput).isEmpty();
        boolean hasPassword = !readInput(passwordInput).isEmpty();
        boolean passwordLongEnough = readInput(passwordInput).length() >= 6;

        if (showErrors) {
            nameLayout.setError(hasName ? null : "Display name is required");
            emailLayout.setError(hasEmail ? null : "Email is required");
            passwordLayout.setError(!hasPassword ? "Password is required"
                    : passwordLongEnough ? null : "Use at least 6 characters");
        } else {
            if (hasName) {
                nameLayout.setError(null);
            }
            if (hasEmail) {
                emailLayout.setError(null);
            }
            if (hasPassword && passwordLongEnough) {
                passwordLayout.setError(null);
            }
        }
        return hasName && hasEmail && hasPassword && passwordLongEnough;
    }

    private void updateRegisterButton() {
        registerButton.setEnabled(validateInputs(false));
    }

    private void setLoading(boolean loading) {
        loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!loading && validateInputs(false));
        nameInput.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        loginLinkText.setEnabled(!loading);
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
                ? "Please check your connection and Firebase setup."
                : exception.getMessage();
        Toast.makeText(this, prefix + ". " + detail, Toast.LENGTH_LONG).show();
    }

    private final TextWatcher validationWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateRegisterButton();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };
}
