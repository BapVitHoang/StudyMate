package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.Concept;
import com.hcmute.studymate.model.ConceptEdge;
import com.hcmute.studymate.model.LearningGap;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.service.KnowledgeGraphService;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.List;

public class KnowledgeGraphController {
    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    public void extractFromNote(String userId, Note note, String locale, OperationCallback callback) {
        knowledgeGraphService.extractFromNote(userId, note, locale, callback);
    }

    public void rebuildGaps(String userId, OperationCallback callback) {
        knowledgeGraphService.rebuildGaps(userId, callback);
    }

    public void listConcepts(String userId, ListCallback<Concept> callback) {
        knowledgeGraphService.listConcepts(userId, callback);
    }

    public void listEdges(String userId, ListCallback<ConceptEdge> callback) {
        knowledgeGraphService.listEdges(userId, callback);
    }

    public void listGaps(String userId, ListCallback<LearningGap> callback) {
        knowledgeGraphService.listGaps(userId, callback);
    }

    public void studyNext(String userId, DataCallback<List<LearningGap>> callback) {
        knowledgeGraphService.studyNext(userId, callback);
    }
}
