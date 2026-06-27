package com.hcmute.studymate.utils;

import com.hcmute.studymate.model.SummaryResult;

public interface SummaryCallback {
    void onSuccess(SummaryResult result);

    void onError(Exception exception);
}
