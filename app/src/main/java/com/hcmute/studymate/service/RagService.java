package com.hcmute.studymate.service;

import com.hcmute.studymate.model.RagAnswer;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.repository.HybridRagRepository;
import com.hcmute.studymate.utils.DataCallback;

import java.util.List;

public class RagService {
    private final HybridSearchService hybridSearchService;
    private final HybridRagRepository ragRepository;

    public RagService(HybridSearchService hybridSearchService, HybridRagRepository ragRepository) {
        this.hybridSearchService = hybridSearchService;
        this.ragRepository = ragRepository;
    }

    public void ask(String userId, String question, String locale, DataCallback<RagAnswer> callback) {
        if (question == null || question.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Question is required"));
            return;
        }
        hybridSearchService.retrievePassagesAsync(userId, question.trim(),
                new HybridSearchService.PassageCallback() {
                    @Override
                    public void onSuccess(List<RetrievedChunk> passages) {
                        if (passages == null || passages.isEmpty()) {
                            callback.onError(new IllegalStateException(
                                    "No relevant notes found. Save and index notes first."));
                            return;
                        }
                        ragRepository.answerFromNotes(question.trim(), passages, locale, callback);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);
                    }
                });
    }
}
