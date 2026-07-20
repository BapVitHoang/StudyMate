package com.hcmute.studymate.service;

import com.hcmute.studymate.model.ExamPrepResult;
import com.hcmute.studymate.model.ExamPrepSection;
import com.hcmute.studymate.model.RagCitation;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.repository.CloudExamPrepRepository;
import com.hcmute.studymate.repository.FirestoreExamPrepRepository;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExamPrepService {
    private final HybridSearchService hybridSearchService;
    private final CloudExamPrepRepository cloudExamPrepRepository;
    private final FirestoreExamPrepRepository examPrepRepository;

    public ExamPrepService(HybridSearchService hybridSearchService,
                           CloudExamPrepRepository cloudExamPrepRepository,
                           FirestoreExamPrepRepository examPrepRepository) {
        this.hybridSearchService = hybridSearchService;
        this.cloudExamPrepRepository = cloudExamPrepRepository;
        this.examPrepRepository = examPrepRepository;
    }

    public void synthesize(String userId, String mode, String topic, List<String> noteIds,
                           String locale, DataCallback<ExamPrepResult> callback) {
        HybridSearchService.PassageCallback passageCallback = new HybridSearchService.PassageCallback() {
            @Override
            public void onSuccess(List<RetrievedChunk> passages) {
                if (passages == null || passages.isEmpty()) {
                    callback.onError(new IllegalStateException(
                            "No passages found. Index notes or pick different notes."));
                    return;
                }
                cloudExamPrepRepository.synthesize(mode, topic, passages, locale,
                        new DataCallback<ExamPrepResult>() {
                            @Override
                            public void onSuccess(ExamPrepResult data) {
                                examPrepRepository.save(userId, data, new OperationCallback() {
                                    @Override
                                    public void onSuccess() {
                                        callback.onSuccess(data);
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        callback.onSuccess(data);
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception exception) {
                                callback.onSuccess(buildLocalFallback(mode, topic, passages));
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
                    userId, noteIds, Constants.EXAM_PREP_PASSAGE_COUNT, passageCallback);
        } else {
            String query = topic == null || topic.trim().isEmpty() ? "exam review key concepts" : topic;
            hybridSearchService.retrievePassagesAsync(
                    userId, query, Constants.EXAM_PREP_PASSAGE_COUNT,
                    Constants.EXAM_PREP_VECTOR_TOP_K, passageCallback);
        }
    }

    public void listSaved(String userId, ListCallback<ExamPrepResult> callback) {
        examPrepRepository.list(userId, callback);
    }

    private ExamPrepResult buildLocalFallback(String mode, String topic, List<RetrievedChunk> passages) {
        ExamPrepResult result = new ExamPrepResult();
        result.setTitle(topic == null || topic.isEmpty() ? "Offline exam outline" : topic);
        result.setMode(mode);
        result.setTopic(topic);
        result.setSource(Constants.RAG_SOURCE_LOCAL);
        result.setGeneratedAt(System.currentTimeMillis());
        List<ExamPrepSection> sections = new ArrayList<>();
        Set<String> coverage = new LinkedHashSet<>();
        List<RagCitation> citations = new ArrayList<>();
        int index = 1;
        for (RetrievedChunk passage : passages) {
            if (passage.getChunk() == null) {
                continue;
            }
            coverage.add(passage.getChunk().getNoteId());
            citations.add(new RagCitation(
                    passage.getChunk().getNoteId(),
                    passage.getChunk().getTitle(),
                    truncate(passage.getChunk().getText(), 160)));
            List<String> bullets = new ArrayList<>();
            bullets.add(truncate(passage.getChunk().getText(), 220));
            sections.add(new ExamPrepSection(
                    "Source " + index + ": " + safe(passage.getChunk().getTitle()), bullets));
            index++;
            if (sections.size() >= 6) {
                break;
            }
        }
        result.setSections(sections);
        result.setCitations(citations);
        result.setCoverageNoteIds(new ArrayList<>(coverage));
        return result;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "Note" : value;
    }
}
