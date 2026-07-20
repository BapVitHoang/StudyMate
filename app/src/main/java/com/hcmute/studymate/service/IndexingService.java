package com.hcmute.studymate.service;

import android.util.Log;

import com.hcmute.studymate.ml.ContentHasher;
import com.hcmute.studymate.ml.LocalEmbeddingEngine;
import com.hcmute.studymate.ml.ModelManager;
import com.hcmute.studymate.ml.NoteChunker;
import com.hcmute.studymate.ml.VectorMath;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.NoteChunk;
import com.hcmute.studymate.repository.ChunkRepository;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IndexingService {
    private static final String TAG = "IndexingService";

    private final ChunkRepository chunkRepository;
    private final ModelManager modelManager;
    private final LocalEmbeddingEngine embeddingEngine;
    private final NoteChunker noteChunker;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public IndexingService(ChunkRepository chunkRepository,
                           ModelManager modelManager,
                           LocalEmbeddingEngine embeddingEngine,
                           NoteChunker noteChunker) {
        this.chunkRepository = chunkRepository;
        this.modelManager = modelManager;
        this.embeddingEngine = embeddingEngine;
        this.noteChunker = noteChunker;
    }

    public void indexNoteAsync(String userId, Note note) {
        if (userId == null || note == null || note.getId() == null) {
            return;
        }
        executor.execute(() -> {
            try {
                indexNoteSync(userId, note);
            } catch (Exception exception) {
                Log.w(TAG, "Indexing skipped/failed for note " + note.getId(), exception);
            }
        });
    }

    public void deleteNoteChunksAsync(String userId, String noteId) {
        if (userId == null || noteId == null) {
            return;
        }
        executor.execute(() -> chunkRepository.deleteChunksForNote(userId, noteId, new OperationCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Deleted chunks for note " + noteId);
            }

            @Override
            public void onError(Exception exception) {
                Log.w(TAG, "Failed deleting chunks for note " + noteId, exception);
            }
        }));
    }

    public void reindexAllAsync(String userId, List<Note> notes, OperationCallback callback) {
        if (userId == null) {
            callback.onError(new IllegalArgumentException("User id is required"));
            return;
        }
        executor.execute(() -> {
            try {
                if (!modelManager.isReady()) {
                    modelManager.ensureReadySync();
                }
                List<Note> safeNotes = notes == null ? new ArrayList<>() : notes;
                for (Note note : safeNotes) {
                    if (note == null || note.getId() == null) {
                        continue;
                    }
                    indexNoteSync(userId, note, true);
                }
                callback.onSuccess();
            } catch (Exception exception) {
                callback.onError(exception);
            }
        });
    }

    public void indexNoteSync(String userId, Note note) throws Exception {
        indexNoteSync(userId, note, false);
    }

    public void indexNoteSync(String userId, Note note, boolean force) throws Exception {
        if (!modelManager.isReady()) {
            modelManager.ensureReadySync();
        }
        if (!embeddingEngine.isReady()) {
            throw new IllegalStateException("Embedding engine is not ready");
        }

        String contentHash = ContentHasher.sha256(
                safe(note.getTitle()) + "\n" + safe(note.getContent()) + "\n" + safe(note.getCategory()));

        if (!force && shouldSkipReindex(userId, note.getId(), contentHash)) {
            Log.i(TAG, "Skip reindex for unchanged note " + note.getId());
            return;
        }

        List<String> texts = noteChunker.chunk(note);
        List<NoteChunk> chunks = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < texts.size(); i++) {
            float[] embedding = embeddingEngine.embed(texts.get(i));
            NoteChunk chunk = new NoteChunk();
            chunk.setId(note.getId() + "_" + i);
            chunk.setNoteId(note.getId());
            chunk.setChunkIndex(i);
            chunk.setText(texts.get(i));
            chunk.setTitle(note.getTitle());
            chunk.setCategory(note.getCategory());
            chunk.setTags(note.getTags());
            chunk.setContentHash(contentHash);
            chunk.setEmbedding(VectorMath.toDoubleList(embedding));
            chunk.setUpdatedAt(now);
            chunk.setModelId(Constants.EMBED_MODEL_ID);
            chunks.add(chunk);
        }

        awaitReplace(userId, note.getId(), chunks);
    }

    private boolean shouldSkipReindex(String userId, String noteId, String contentHash) throws Exception {
        List<NoteChunk> existing = awaitChunksForNote(userId, noteId);
        if (existing == null || existing.isEmpty()) {
            return false;
        }
        for (NoteChunk chunk : existing) {
            if (chunk == null) {
                continue;
            }
            if (!contentHash.equals(chunk.getContentHash())) {
                return false;
            }
            if (!Constants.EMBED_MODEL_ID.equals(chunk.getModelId())) {
                return false;
            }
        }
        return true;
    }

    private List<NoteChunk> awaitChunksForNote(String userId, String noteId) throws Exception {
        final Object lock = new Object();
        final List<NoteChunk>[] result = new List[]{null};
        final Exception[] error = new Exception[1];
        chunkRepository.getChunksForNote(userId, noteId, new ListCallback<NoteChunk>() {
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
        return result[0];
    }

    private void awaitReplace(String userId, String noteId, List<NoteChunk> chunks) throws Exception {
        final Exception[] error = new Exception[1];
        final Object lock = new Object();
        final boolean[] done = new boolean[]{false};
        chunkRepository.replaceChunksForNote(userId, noteId, chunks, new OperationCallback() {
            @Override
            public void onSuccess() {
                synchronized (lock) {
                    done[0] = true;
                    lock.notifyAll();
                }
            }

            @Override
            public void onError(Exception exception) {
                synchronized (lock) {
                    error[0] = exception;
                    done[0] = true;
                    lock.notifyAll();
                }
            }
        });
        synchronized (lock) {
            while (!done[0]) {
                lock.wait();
            }
        }
        if (error[0] != null) {
            throw error[0];
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
