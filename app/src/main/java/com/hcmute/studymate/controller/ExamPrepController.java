package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.ExamPrepResult;
import com.hcmute.studymate.service.ExamPrepService;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;

import java.util.List;

public class ExamPrepController {
    private final ExamPrepService examPrepService;

    public ExamPrepController(ExamPrepService examPrepService) {
        this.examPrepService = examPrepService;
    }

    public void synthesize(String userId, String mode, String topic, List<String> noteIds,
                           String locale, DataCallback<ExamPrepResult> callback) {
        examPrepService.synthesize(userId, mode, topic, noteIds, locale, callback);
    }

    public void listSaved(String userId, ListCallback<ExamPrepResult> callback) {
        examPrepService.listSaved(userId, callback);
    }
}
