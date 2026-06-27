package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.UserProfile;
import com.hcmute.studymate.service.AuthService;
import com.hcmute.studymate.utils.AuthCallback;
import com.hcmute.studymate.utils.OperationCallback;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void signIn(String email, String password, AuthCallback callback) {
        authService.signIn(email, password, callback);
    }

    public void register(String email, String password, String displayName, AuthCallback callback) {
        authService.register(email, password, displayName, callback);
    }

    public void signOut(OperationCallback callback) {
        authService.signOut(callback);
    }

    public UserProfile getCurrentUser() {
        return authService.getCurrentUser();
    }

    public void getCurrentUserProfile(AuthCallback callback) {
        authService.getCurrentUserProfile(callback);
    }

    public boolean isLoggedIn() {
        return authService.isLoggedIn();
    }

    public String getCurrentUserId() {
        return authService.getCurrentUserId();
    }
}
