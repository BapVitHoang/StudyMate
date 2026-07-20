package com.hcmute.studymate.service;

import com.hcmute.studymate.model.SummarizeRequest;
import com.hcmute.studymate.repository.AiSummaryRepository;
import com.hcmute.studymate.utils.SummaryCallback;

public class SummaryService {
    private final AiSummaryRepository aiSummaryRepository;

    public SummaryService(AiSummaryRepository aiSummaryRepository) {
        this.aiSummaryRepository = aiSummaryRepository;
    }

    public void summarize(SummarizeRequest request, SummaryCallback callback) {
        if (request == null) {
            callback.onError(new IllegalArgumentException("Summarize request is required"));
            return;
        }
        boolean hasContent = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasNoteId = request.getNoteId() != null && !request.getNoteId().trim().isEmpty();
        if (!hasContent && !hasNoteId) {
            callback.onError(new IllegalArgumentException("Note id or content is required"));
            return;
        }
        aiSummaryRepository.summarize(request, callback);
    }
}
