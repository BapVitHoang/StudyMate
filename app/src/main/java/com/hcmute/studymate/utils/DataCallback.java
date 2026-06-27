package com.hcmute.studymate.utils;

public interface DataCallback<T> {
    void onSuccess(T data);

    void onError(Exception exception);
}
