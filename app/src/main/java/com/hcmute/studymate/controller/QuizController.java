package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.QuizQuestion;
import com.hcmute.studymate.model.ReviewItem;
import com.hcmute.studymate.service.QuizService;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.List;

public class QuizController {
    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    public void generateQuiz(String userId, String topic, List<String> noteIds, int count,
                             String locale, DataCallback<List<QuizQuestion>> callback) {
        quizService.generateQuiz(userId, topic, noteIds, count, locale, callback);
    }

    public void loadDueReviews(String userId, ListCallback<ReviewItem> callback) {
        quizService.loadDueReviews(userId, callback);
    }

    public void reviewCard(String userId, ReviewItem item, int quality, OperationCallback callback) {
        quizService.reviewCard(userId, item, quality, callback);
    }
}
