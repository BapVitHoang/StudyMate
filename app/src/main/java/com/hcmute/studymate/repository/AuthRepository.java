package com.hcmute.studymate.repository;

import com.hcmute.studymate.model.UserProfile;
import com.hcmute.studymate.utils.AuthCallback;
import com.hcmute.studymate.utils.OperationCallback;

public interface AuthRepository {
    void signIn(String email, String password, AuthCallback callback);

    void register(String email, String password, String displayName, AuthCallback callback);

    void signOut(OperationCallback callback);

    UserProfile getCurrentUser();

    void getCurrentUserProfile(AuthCallback callback);

    boolean isLoggedIn();

    String getCurrentUserId();
}
