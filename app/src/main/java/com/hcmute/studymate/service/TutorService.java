package com.hcmute.studymate.service;

import com.hcmute.studymate.model.RagAnswer;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.model.TutorMessage;
import com.hcmute.studymate.model.TutorReply;
import com.hcmute.studymate.model.TutorSession;
import com.hcmute.studymate.repository.CloudTutorRepository;
import com.hcmute.studymate.repository.FirestoreTutorSessionRepository;
import com.hcmute.studymate.repository.LocalExtractiveRagRepository;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TutorService {
    private final HybridSearchService hybridSearchService;
    private final CloudTutorRepository cloudTutorRepository;
    private final LocalExtractiveRagRepository localExtractiveRagRepository;
    private final FirestoreTutorSessionRepository sessionRepository;

    public TutorService(HybridSearchService hybridSearchService,
                        CloudTutorRepository cloudTutorRepository,
                        LocalExtractiveRagRepository localExtractiveRagRepository,
                        FirestoreTutorSessionRepository sessionRepository) {
        this.hybridSearchService = hybridSearchService;
        this.cloudTutorRepository = cloudTutorRepository;
        this.localExtractiveRagRepository = localExtractiveRagRepository;
        this.sessionRepository = sessionRepository;
    }

    public void sendMessage(String userId, TutorSession session, String userText, String locale,
                            DataCallback<TutorReply> callback) {
        if (session == null) {
            callback.onError(new IllegalArgumentException("Session required"));
            return;
        }
        if (userText == null || userText.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Message required"));
            return;
        }

        TutorMessage userMessage = new TutorMessage(
                TutorMessage.ROLE_USER, userText.trim(), System.currentTimeMillis());
        session.getMessages().add(userMessage);
        if (session.getTitle() == null || session.getTitle().trim().isEmpty()) {
            session.setTitle(truncate(userText.trim(), 48));
        }

        String retrievalQuery = buildRetrievalQuery(session);
        hybridSearchService.retrievePassagesAsync(
                userId, retrievalQuery, Constants.TUTOR_PASSAGE_COUNT,
                new HybridSearchService.PassageCallback() {
                    @Override
                    public void onSuccess(List<RetrievedChunk> passages) {
                        if (passages == null || passages.isEmpty()) {
                            callback.onError(new IllegalStateException(
                                    "No relevant notes found. Save and index notes first."));
                            return;
                        }
                        List<TutorMessage> history = trimHistory(session.getMessages());
                        cloudTutorRepository.chat(history, passages, locale, new DataCallback<TutorReply>() {
                            @Override
                            public void onSuccess(TutorReply data) {
                                appendAssistant(session, data);
                                persist(userId, session);
                                callback.onSuccess(data);
                            }

                            @Override
                            public void onError(Exception exception) {
                                localExtractiveRagRepository.answerFromNotes(
                                        userText.trim(), passages, locale, new DataCallback<RagAnswer>() {
                                            @Override
                                            public void onSuccess(RagAnswer answer) {
                                                TutorReply fallback = new TutorReply();
                                                fallback.setAnswer(answer.getAnswer());
                                                fallback.setCitations(answer.getCitations());
                                                fallback.setSource(Constants.RAG_SOURCE_LOCAL);
                                                fallback.setUsedFallback(true);
                                                fallback.setGeneratedAt(System.currentTimeMillis());
                                                fallback.setSuggestedFollowUps(Arrays.asList(
                                                        "Can you explain that with an example?",
                                                        "What else should I review on this topic?"
                                                ));
                                                appendAssistant(session, fallback);
                                                persist(userId, session);
                                                callback.onSuccess(fallback);
                                            }

                                            @Override
                                            public void onError(Exception localException) {
                                                callback.onError(localException);
                                            }
                                        });
                            }
                        });
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);
                    }
                });
    }

    public void saveSession(String userId, TutorSession session, OperationCallback callback) {
        sessionRepository.saveSession(userId, session, callback);
    }

    private void persist(String userId, TutorSession session) {
        sessionRepository.saveSession(userId, session, new OperationCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(Exception exception) {
            }
        });
    }

    private void appendAssistant(TutorSession session, TutorReply reply) {
        session.getMessages().add(new TutorMessage(
                TutorMessage.ROLE_ASSISTANT,
                reply.getAnswer(),
                System.currentTimeMillis()));
        session.setUpdatedAt(System.currentTimeMillis());
    }

    private List<TutorMessage> trimHistory(List<TutorMessage> messages) {
        if (messages == null || messages.size() <= Constants.TUTOR_MAX_HISTORY) {
            return messages == null ? new ArrayList<>() : new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(
                messages.size() - Constants.TUTOR_MAX_HISTORY, messages.size()));
    }

    private String buildRetrievalQuery(TutorSession session) {
        List<TutorMessage> messages = session.getMessages();
        StringBuilder builder = new StringBuilder();
        int start = Math.max(0, messages.size() - 4);
        for (int i = start; i < messages.size(); i++) {
            TutorMessage message = messages.get(i);
            if (message == null || message.getContent() == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(message.getContent());
        }
        return builder.toString().trim();
    }

    private String truncate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}
