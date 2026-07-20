package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.RagAnswer;
import com.hcmute.studymate.service.RagService;
import com.hcmute.studymate.utils.DataCallback;

public class RagController {
    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    public void ask(String userId, String question, String locale, DataCallback<RagAnswer> callback) {
        ragService.ask(userId, question, locale, callback);
    }
}
