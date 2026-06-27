package com.hcmute.studymate.service;

import com.hcmute.studymate.repository.AiSummaryRepository;
import com.hcmute.studymate.utils.SummaryCallback;

public class SummaryService {
    private final AiSummaryRepository aiSummaryRepository;

    public SummaryService(AiSummaryRepository aiSummaryRepository) {
        this.aiSummaryRepository = aiSummaryRepository;
    }

    public void summarize(String content, SummaryCallback callback) {
        if (content == null || content.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Content is required"));
            return;
        }
        aiSummaryRepository.summarize(content, callback);
    }
}
