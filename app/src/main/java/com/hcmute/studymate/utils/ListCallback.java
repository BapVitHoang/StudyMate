package com.hcmute.studymate.utils;

import java.util.List;

public interface ListCallback<T> {
    void onSuccess(List<T> items);

    void onError(Exception exception);
}
