package com.hcmute.studymate.controller;

import com.hcmute.studymate.service.SummaryService;
import com.hcmute.studymate.utils.SummaryCallback;

public class SummaryController {
    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    public void summarize(String content, SummaryCallback callback) {
        summaryService.summarize(content, callback);
    }
}
