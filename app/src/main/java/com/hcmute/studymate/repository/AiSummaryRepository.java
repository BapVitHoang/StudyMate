package com.hcmute.studymate.repository;

import com.hcmute.studymate.model.SummarizeRequest;
import com.hcmute.studymate.utils.SummaryCallback;

public interface AiSummaryRepository {
    void summarize(SummarizeRequest request, SummaryCallback callback);
}
