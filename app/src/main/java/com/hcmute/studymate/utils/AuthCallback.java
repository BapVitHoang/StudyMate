package com.hcmute.studymate.utils;

import com.hcmute.studymate.model.UserProfile;

public interface AuthCallback {
    void onSuccess(UserProfile userProfile);

    void onError(Exception exception);
}
