package com.hcmute.studymate.repository;

import com.hcmute.studymate.model.NoteChunk;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.List;

public interface ChunkRepository {
    void replaceChunksForNote(String userId, String noteId, List<NoteChunk> chunks, OperationCallback callback);

    void deleteChunksForNote(String userId, String noteId, OperationCallback callback);

    void getChunksForUser(String userId, ListCallback<NoteChunk> callback);

    void getChunksForNote(String userId, String noteId, ListCallback<NoteChunk> callback);

    void findNearest(String userId, float[] queryEmbedding, int limit, ListCallback<NoteChunk> callback);
}
