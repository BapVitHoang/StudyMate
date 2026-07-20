package com.hcmute.studymate.service;

import com.hcmute.studymate.model.QuizQuestion;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.model.ReviewItem;
import com.hcmute.studymate.repository.CloudQuizRepository;
import com.hcmute.studymate.repository.FirestoreQuizRepository;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.Arrays;
import java.util.List;

public class QuizService {
    private final HybridSearchService hybridSearchService;
    private final CloudQuizRepository cloudQuizRepository;
    private final FirestoreQuizRepository quizRepository;
    private final SrsService srsService;

    public QuizService(HybridSearchService hybridSearchService,
                       CloudQuizRepository cloudQuizRepository,
                       FirestoreQuizRepository quizRepository,
                       SrsService srsService) {
        this.hybridSearchService = hybridSearchService;
        this.cloudQuizRepository = cloudQuizRepository;
        this.quizRepository = quizRepository;
        this.srsService = srsService;
    }

    public void generateQuiz(String userId, String topic, List<String> noteIds, int count,
                             String locale, DataCallback<List<QuizQuestion>> callback) {
        HybridSearchService.PassageCallback passageCallback = new HybridSearchService.PassageCallback() {
            @Override
            public void onSuccess(List<RetrievedChunk> passages) {
                if (passages == null || passages.isEmpty()) {
                    callback.onError(new IllegalStateException("No passages available for quiz."));
                    return;
                }
                cloudQuizRepository.generate(
                        passages,
                        count <= 0 ? Constants.DEFAULT_QUIZ_COUNT : count,
                        Arrays.asList(QuizQuestion.TYPE_MCQ, QuizQuestion.TYPE_SHORT),
                        locale,
                        new DataCallback<List<QuizQuestion>>() {
                            @Override
                            public void onSuccess(List<QuizQuestion> questions) {
                                long now = System.currentTimeMillis();
                                quizRepository.saveQuestions(userId, questions, new OperationCallback() {
                                    @Override
                                    public void onSuccess() {
                                        for (QuizQuestion question : questions) {
                                            ReviewItem item = srsService.fromQuestion(question, now);
                                            quizRepository.saveReviewItem(userId, item, new OperationCallback() {
                                                @Override
                                                public void onSuccess() {
                                                }

                                                @Override
                                                public void onError(Exception exception) {
                                                }
                                            });
                                        }
                                        callback.onSuccess(questions);
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        callback.onSuccess(questions);
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception exception) {
                                callback.onError(exception);
                            }
                        });
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        };

        if (noteIds != null && !noteIds.isEmpty()) {
            hybridSearchService.retrievePassagesForNotesAsync(
                    userId, noteIds, Constants.QUIZ_PASSAGE_COUNT, passageCallback);
        } else {
            String query = topic == null || topic.trim().isEmpty() ? "important concepts" : topic;
            hybridSearchService.retrievePassagesAsync(
                    userId, query, Constants.QUIZ_PASSAGE_COUNT, passageCallback);
        }
    }

    public void loadDueReviews(String userId, ListCallback<ReviewItem> callback) {
        quizRepository.listDueReviews(userId, System.currentTimeMillis(), callback);
    }

    public void reviewCard(String userId, ReviewItem item, int quality, OperationCallback callback) {
        srsService.applyReview(item, quality, System.currentTimeMillis());
        quizRepository.saveReviewItem(userId, item, callback);
    }

    public SrsService getSrsService() {
        return srsService;
    }
}
