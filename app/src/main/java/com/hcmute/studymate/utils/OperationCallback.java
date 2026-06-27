package com.hcmute.studymate.utils;

public interface OperationCallback {
    void onSuccess();

    void onError(Exception exception);
}
