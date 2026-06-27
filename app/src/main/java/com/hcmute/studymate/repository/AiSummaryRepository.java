package com.hcmute.studymate.repository;

import com.hcmute.studymate.utils.SummaryCallback;

public interface AiSummaryRepository {
    void summarize(String content, SummaryCallback callback);
}
