package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.service.IndexingService;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.List;

public class IndexingController {
    private final IndexingService indexingService;

    public IndexingController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    public void indexNoteAsync(String userId, Note note) {
        indexingService.indexNoteAsync(userId, note);
    }

    public void deleteNoteChunksAsync(String userId, String noteId) {
        indexingService.deleteNoteChunksAsync(userId, noteId);
    }

    public void reindexAllAsync(String userId, List<Note> notes, OperationCallback callback) {
        indexingService.reindexAllAsync(userId, notes, callback);
    }
}
