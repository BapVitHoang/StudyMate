package com.hcmute.studymate.service;

import com.hcmute.studymate.ml.LocalEmbeddingEngine;
import com.hcmute.studymate.ml.LocalReranker;
import com.hcmute.studymate.ml.ModelManager;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.NoteChunk;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.repository.ChunkRepository;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.ListCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HybridSearchService {
    public interface HybridCallback {
        void onSuccess(List<Note> notes, boolean usedSemantic);

        void onError(Exception exception);
    }

    public interface PassageCallback {
        void onSuccess(List<RetrievedChunk> passages);

        void onError(Exception exception);
    }

    private final SearchService keywordSearchService;
    private final ChunkRepository chunkRepository;
    private final ModelManager modelManager;
    private final LocalEmbeddingEngine embeddingEngine;
    private final LocalReranker reranker;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HybridSearchService(SearchService keywordSearchService,
                               ChunkRepository chunkRepository,
                               ModelManager modelManager,
                               LocalEmbeddingEngine embeddingEngine,
                               LocalReranker reranker) {
        this.keywordSearchService = keywordSearchService;
        this.chunkRepository = chunkRepository;
        this.modelManager = modelManager;
        this.embeddingEngine = embeddingEngine;
        this.reranker = reranker;
    }

    public List<Note> filterNotesKeyword(List<Note> notes, String keyword, String category) {
        return keywordSearchService.filterNotes(notes, keyword, category);
    }

    public void searchHybridAsync(String userId, List<Note> allNotes, String keyword, String category,
                                  HybridCallback callback) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty() || normalized.length() < 2 || !modelManager.isReady()) {
            callback.onSuccess(filterNotesKeyword(allNotes, keyword, category), false);
            return;
        }

        executor.execute(() -> {
            try {
                List<Note> result = searchHybridSync(userId, allNotes, normalized, category);
                callback.onSuccess(result, true);
            } catch (Exception exception) {
                callback.onSuccess(filterNotesKeyword(allNotes, keyword, category), false);
            }
        });
    }

    public void retrievePassagesAsync(String userId, String question, PassageCallback callback) {
        retrievePassagesAsync(userId, question, Constants.RAG_PASSAGE_COUNT, callback);
    }

    public void retrievePassagesAsync(String userId, String question, int limit, PassageCallback callback) {
        retrievePassagesAsync(userId, question, limit, Constants.VECTOR_TOP_K, callback);
    }

    public void retrievePassagesAsync(String userId, String question, int limit, int vectorTopK,
                                      PassageCallback callback) {
        executor.execute(() -> {
            try {
                callback.onSuccess(retrievePassagesSync(userId, question, limit, vectorTopK));
            } catch (Exception exception) {
                callback.onError(exception);
            }
        });
    }

    public void retrievePassagesForNotesAsync(String userId, List<String> noteIds, int limit,
                                              PassageCallback callback) {
        executor.execute(() -> {
            try {
                callback.onSuccess(retrievePassagesForNotesSync(userId, noteIds, limit));
            } catch (Exception exception) {
                callback.onError(exception);
            }
        });
    }

    public List<RetrievedChunk> retrievePassagesForNotesSync(String userId, List<String> noteIds, int limit)
            throws Exception {
        if (noteIds == null || noteIds.isEmpty()) {
            return new ArrayList<>();
        }
        final Object lock = new Object();
        final List<NoteChunk>[] result = new List[]{null};
        final Exception[] error = new Exception[1];
        chunkRepository.getChunksForUser(userId, new ListCallback<NoteChunk>() {
            @Override
            public void onSuccess(List<NoteChunk> items) {
                synchronized (lock) {
                    result[0] = items == null ? new ArrayList<>() : items;
                    lock.notifyAll();
                }
            }

            @Override
            public void onError(Exception exception) {
                synchronized (lock) {
                    error[0] = exception;
                    lock.notifyAll();
                }
            }
        });
        synchronized (lock) {
            while (result[0] == null && error[0] == null) {
                lock.wait();
            }
        }
        if (error[0] != null) {
            throw error[0];
        }
        java.util.HashSet<String> wanted = new java.util.HashSet<>();
        for (String noteId : noteIds) {
            if (noteId != null && !noteId.trim().isEmpty()) {
                wanted.add(noteId);
            }
        }
        List<RetrievedChunk> passages = new ArrayList<>();
        for (NoteChunk chunk : result[0]) {
            if (chunk == null || chunk.getNoteId() == null || !wanted.contains(chunk.getNoteId())) {
                continue;
            }
            passages.add(new RetrievedChunk(chunk, 1.0, "note-filter"));
            if (passages.size() >= Math.max(limit, 1)) {
                break;
            }
        }
        return passages;
    }

    public List<Note> searchHybridSync(String userId, List<Note> allNotes, String keyword, String category)
            throws Exception {
        if (!modelManager.isReady()) {
            modelManager.ensureReadySync();
        }

        List<Note> keywordHits = filterNotesKeyword(allNotes, keyword, category);
        float[] queryVector = embeddingEngine.embed(keyword);
        List<NoteChunk> vectorChunks = awaitNearest(userId, queryVector, Constants.VECTOR_TOP_K);
        if (category != null && !Constants.CATEGORY_ALL.equals(category)) {
            List<NoteChunk> filtered = new ArrayList<>();
            for (NoteChunk chunk : vectorChunks) {
                if (category.equals(chunk.getCategory())) {
                    filtered.add(chunk);
                }
            }
            vectorChunks = filtered;
        }

        Map<String, Double> rrfScores = new HashMap<>();
        for (int i = 0; i < keywordHits.size(); i++) {
            String noteId = keywordHits.get(i).getId();
            if (noteId == null) {
                continue;
            }
            double score = 1.0 / (Constants.RRF_K + i + 1);
            rrfScores.put(noteId, rrfScores.getOrDefault(noteId, 0.0) + score);
        }
        Map<String, NoteChunk> bestChunkByNote = new LinkedHashMap<>();
        for (int i = 0; i < vectorChunks.size(); i++) {
            NoteChunk chunk = vectorChunks.get(i);
            String noteId = chunk.getNoteId();
            if (noteId == null) {
                continue;
            }
            double score = 1.0 / (Constants.RRF_K + i + 1);
            rrfScores.put(noteId, rrfScores.getOrDefault(noteId, 0.0) + score);
            if (!bestChunkByNote.containsKey(noteId)) {
                bestChunkByNote.put(noteId, chunk);
            }
        }

        List<Map.Entry<String, Double>> ranked = new ArrayList<>(rrfScores.entrySet());
        ranked.sort((left, right) -> Double.compare(right.getValue(), left.getValue()));

        List<LocalReranker.ScoredPassage> candidates = new ArrayList<>();
        for (int i = 0; i < Math.min(ranked.size(), Constants.VECTOR_TOP_K); i++) {
            String noteId = ranked.get(i).getKey();
            NoteChunk chunk = bestChunkByNote.get(noteId);
            String passageText = chunk != null ? chunk.getText() : findNoteText(allNotes, noteId);
            if (passageText == null || passageText.trim().isEmpty()) {
                continue;
            }
            candidates.add(new LocalReranker.ScoredPassage(noteId, passageText, ranked.get(i).getValue()));
        }

        List<String> orderedNoteIds = new ArrayList<>();
        if (reranker.isReady() && !candidates.isEmpty()) {
            List<LocalReranker.ScoredPassage> reranked =
                    reranker.rerank(keyword, candidates, Constants.RERANK_TOP_N);
            for (LocalReranker.ScoredPassage passage : reranked) {
                orderedNoteIds.add(passage.id);
            }
        } else {
            for (LocalReranker.ScoredPassage candidate : candidates) {
                orderedNoteIds.add(candidate.id);
                if (orderedNoteIds.size() >= Constants.RERANK_TOP_N) {
                    break;
                }
            }
        }

        Map<String, Note> noteById = new HashMap<>();
        for (Note note : allNotes) {
            if (note.getId() != null) {
                noteById.put(note.getId(), note);
            }
        }
        List<Note> result = new ArrayList<>();
        for (String noteId : orderedNoteIds) {
            Note note = noteById.get(noteId);
            if (note != null) {
                result.add(note);
            }
        }
        if (result.isEmpty()) {
            return keywordHits;
        }
        return result;
    }

    public List<RetrievedChunk> retrievePassagesSync(String userId, String question, int limit)
            throws Exception {
        return retrievePassagesSync(userId, question, limit, Constants.VECTOR_TOP_K);
    }

    public List<RetrievedChunk> retrievePassagesSync(String userId, String question, int limit, int vectorTopK)
            throws Exception {
        if (!modelManager.isReady()) {
            modelManager.ensureReadySync();
        }
        float[] queryVector = embeddingEngine.embed(question);
        List<NoteChunk> vectorChunks = awaitNearest(userId, queryVector,
                Math.max(vectorTopK, limit));
        List<LocalReranker.ScoredPassage> candidates = new ArrayList<>();
        for (NoteChunk chunk : vectorChunks) {
            candidates.add(new LocalReranker.ScoredPassage(
                    chunk.getId() == null ? chunk.getNoteId() : chunk.getId(),
                    chunk.getText(),
                    0));
        }
        List<LocalReranker.ScoredPassage> reranked =
                reranker.rerank(question, candidates, Math.max(limit, 1));

        Map<String, NoteChunk> byId = new HashMap<>();
        for (NoteChunk chunk : vectorChunks) {
            String key = chunk.getId() == null ? chunk.getNoteId() : chunk.getId();
            byId.put(key, chunk);
        }

        List<RetrievedChunk> passages = new ArrayList<>();
        for (LocalReranker.ScoredPassage scored : reranked) {
            NoteChunk chunk = byId.get(scored.id);
            if (chunk == null) {
                continue;
            }
            passages.add(new RetrievedChunk(chunk, scored.score, "hybrid-rerank"));
            if (passages.size() >= limit) {
                break;
            }
        }
        return passages;
    }

    private List<NoteChunk> awaitNearest(String userId, float[] queryVector, int limit) throws Exception {
        final Object lock = new Object();
        final List<NoteChunk>[] result = new List[]{null};
        final Exception[] error = new Exception[1];
        chunkRepository.findNearest(userId, queryVector, limit, new ListCallback<NoteChunk>() {
            @Override
            public void onSuccess(List<NoteChunk> items) {
                synchronized (lock) {
                    result[0] = items;
                    lock.notifyAll();
                }
            }

            @Override
            public void onError(Exception exception) {
                synchronized (lock) {
                    error[0] = exception;
                    lock.notifyAll();
                }
            }
        });
        synchronized (lock) {
            while (result[0] == null && error[0] == null) {
                lock.wait();
            }
        }
        if (error[0] != null) {
            throw error[0];
        }
        return result[0] == null ? new ArrayList<>() : result[0];
    }

    private String findNoteText(List<Note> notes, String noteId) {
        for (Note note : notes) {
            if (noteId.equals(note.getId())) {
                String title = note.getTitle() == null ? "" : note.getTitle();
                String content = note.getContent() == null ? "" : note.getContent();
                return (title + "\n" + content).trim().toLowerCase(Locale.US);
            }
        }
        return "";
    }
}
