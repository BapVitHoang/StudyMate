package com.hcmute.studymate.service;

import com.hcmute.studymate.model.UserProfile;
import com.hcmute.studymate.repository.AuthRepository;
import com.hcmute.studymate.utils.AuthCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.regex.Pattern;

public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final AuthRepository authRepository;

    public AuthService(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public void signIn(String email, String password, AuthCallback callback) {
        if (!validateEmailAndPassword(email, password, callback)) {
            return;
        }
        authRepository.signIn(email.trim(), password, callback);
    }

    public void register(String email, String password, String displayName, AuthCallback callback) {
        if (!validateEmailAndPassword(email, password, callback)) {
            return;
        }
        if (isBlank(displayName)) {
            callback.onError(new IllegalArgumentException("Display name is required"));
            return;
        }
        authRepository.register(email.trim(), password, displayName.trim(), callback);
    }

    public void signOut(OperationCallback callback) {
        authRepository.signOut(callback);
    }

    public UserProfile getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    public void getCurrentUserProfile(AuthCallback callback) {
        authRepository.getCurrentUserProfile(callback);
    }

    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }

    public String getCurrentUserId() {
        return authRepository.getCurrentUserId();
    }

    private boolean validateEmailAndPassword(String email, String password, AuthCallback callback) {
        if (isBlank(email) || isBlank(password)) {
            callback.onError(new IllegalArgumentException("Email and password are required"));
            return false;
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            callback.onError(new IllegalArgumentException("Enter a valid email address"));
            return false;
        }
        if (password.length() < 6) {
            callback.onError(new IllegalArgumentException("Password must be at least 6 characters"));
            return false;
        }
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
