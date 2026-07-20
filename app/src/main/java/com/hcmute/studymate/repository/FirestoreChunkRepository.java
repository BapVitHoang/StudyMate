package com.hcmute.studymate.repository;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.VectorQuery;
import com.google.firebase.firestore.VectorValue;
import com.google.firebase.firestore.WriteBatch;
import com.hcmute.studymate.ml.VectorMath;
import com.hcmute.studymate.model.NoteChunk;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreChunkRepository implements ChunkRepository {
    private static final String TAG = "FirestoreChunkRepo";
    private static final String USERS_COLLECTION = "users";
    private static final String CHUNKS_COLLECTION = "noteChunks";

    private final FirebaseFirestore firestore;

    public FirestoreChunkRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreChunkRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void replaceChunksForNote(String userId, String noteId, List<NoteChunk> chunks,
                                     OperationCallback callback) {
        if (isBlank(userId) || isBlank(noteId)) {
            callback.onError(new IllegalArgumentException("User id and note id are required"));
            return;
        }

        CollectionReference chunksRef = chunksRef(userId);
        chunksRef.whereEqualTo("noteId", noteId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = firestore.batch();
                    for (QueryDocumentSnapshot document : snapshot) {
                        batch.delete(document.getReference());
                    }
                    if (chunks != null) {
                        for (NoteChunk chunk : chunks) {
                            if (chunk == null || isBlank(chunk.getText())) {
                                continue;
                            }
                            String chunkId = isBlank(chunk.getId())
                                    ? noteId + "_" + chunk.getChunkIndex()
                                    : chunk.getId();
                            chunk.setId(chunkId);
                            batch.set(chunksRef.document(chunkId), toFirestoreMap(chunk));
                        }
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void deleteChunksForNote(String userId, String noteId, OperationCallback callback) {
        if (isBlank(userId) || isBlank(noteId)) {
            callback.onError(new IllegalArgumentException("User id and note id are required"));
            return;
        }
        chunksRef(userId).whereEqualTo("noteId", noteId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }
                    WriteBatch batch = firestore.batch();
                    for (QueryDocumentSnapshot document : snapshot) {
                        batch.delete(document.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getChunksForUser(String userId, ListCallback<NoteChunk> callback) {
        if (isBlank(userId)) {
            callback.onError(new IllegalArgumentException("User id is required"));
            return;
        }
        chunksRef(userId).get()
                .addOnSuccessListener(snapshot -> {
                    List<NoteChunk> chunks = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        NoteChunk chunk = fromDocument(document);
                        if (chunk != null) {
                            chunks.add(chunk);
                        }
                    }
                    callback.onSuccess(chunks);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getChunksForNote(String userId, String noteId, ListCallback<NoteChunk> callback) {
        if (isBlank(userId) || isBlank(noteId)) {
            callback.onError(new IllegalArgumentException("User id and note id are required"));
            return;
        }
        chunksRef(userId).whereEqualTo("noteId", noteId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<NoteChunk> chunks = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        NoteChunk chunk = fromDocument(document);
                        if (chunk != null) {
                            chunks.add(chunk);
                        }
                    }
                    callback.onSuccess(chunks);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void findNearest(String userId, float[] queryEmbedding, int limit,
                            ListCallback<NoteChunk> callback) {
        if (isBlank(userId) || queryEmbedding == null || queryEmbedding.length == 0) {
            callback.onError(new IllegalArgumentException("User id and query embedding are required"));
            return;
        }
        int safeLimit = Math.max(1, Math.min(limit, Constants.EXAM_PREP_VECTOR_TOP_K));
        tryVectorNearest(userId, queryEmbedding, safeLimit, new ListCallback<NoteChunk>() {
            @Override
            public void onSuccess(List<NoteChunk> items) {
                if (items == null || items.isEmpty()) {
                    cosineFallback(userId, queryEmbedding, safeLimit, callback);
                    return;
                }
                callback.onSuccess(items);
            }

            @Override
            public void onError(Exception exception) {
                Log.w(TAG, "Firestore vector KNN unavailable, using cosine fallback", exception);
                cosineFallback(userId, queryEmbedding, safeLimit, callback);
            }
        });
    }

    private void tryVectorNearest(String userId, float[] queryEmbedding, int limit,
                                  ListCallback<NoteChunk> callback) {
        try {
            float[] normalized = VectorMath.l2Normalize(queryEmbedding);
            double[] doubles = new double[normalized.length];
            for (int i = 0; i < normalized.length; i++) {
                doubles[i] = normalized[i];
            }
            VectorValue vectorValue = FieldValue.vector(doubles);
            VectorQuery vectorQuery = chunksRef(userId).findNearest(
                    "embedding",
                    vectorValue,
                    limit,
                    VectorQuery.DistanceMeasure.COSINE);
            vectorQuery.get()
                    .addOnSuccessListener(snapshot -> {
                        List<NoteChunk> chunks = new ArrayList<>();
                        for (QueryDocumentSnapshot document : snapshot) {
                            NoteChunk chunk = fromDocument(document);
                            if (chunk != null) {
                                chunks.add(chunk);
                            }
                        }
                        callback.onSuccess(chunks);
                    })
                    .addOnFailureListener(callback::onError);
        } catch (Exception exception) {
            callback.onError(exception);
        }
    }

    private void cosineFallback(String userId, float[] queryEmbedding, int limit,
                                ListCallback<NoteChunk> callback) {
        getChunksForUser(userId, new ListCallback<NoteChunk>() {
            @Override
            public void onSuccess(List<NoteChunk> items) {
                float[] query = VectorMath.l2Normalize(queryEmbedding);
                List<Scored> scored = new ArrayList<>();
                for (NoteChunk chunk : items) {
                    float[] vector = VectorMath.toFloatArray(chunk.getEmbedding());
                    if (vector.length == 0 || vector.length != query.length) {
                        continue;
                    }
                    double score = VectorMath.cosineSimilarity(query, VectorMath.l2Normalize(vector));
                    scored.add(new Scored(chunk, score));
                }
                Collections.sort(scored, Comparator.comparingDouble((Scored item) -> item.score).reversed());
                List<NoteChunk> result = new ArrayList<>();
                for (int i = 0; i < Math.min(limit, scored.size()); i++) {
                    result.add(scored.get(i).chunk);
                }
                callback.onSuccess(result);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    private Map<String, Object> toFirestoreMap(NoteChunk chunk) {
        Map<String, Object> data = new HashMap<>();
        data.put("noteId", chunk.getNoteId());
        data.put("chunkIndex", chunk.getChunkIndex());
        data.put("text", chunk.getText());
        data.put("title", chunk.getTitle());
        data.put("category", chunk.getCategory());
        data.put("tags", chunk.getTags() == null ? new ArrayList<>() : chunk.getTags());
        data.put("contentHash", chunk.getContentHash());
        data.put("updatedAt", chunk.getUpdatedAt());
        data.put("modelId", chunk.getModelId());
        List<Double> embedding = chunk.getEmbedding() == null ? new ArrayList<>() : chunk.getEmbedding();
        data.put("embeddingValues", embedding);
        try {
            data.put("embedding", FieldValue.vector(embedding));
        } catch (Exception exception) {
            Log.w(TAG, "FieldValue.vector unavailable, storing list only", exception);
            data.put("embedding", embedding);
        }
        return data;
    }

    private NoteChunk fromDocument(QueryDocumentSnapshot document) {
        NoteChunk chunk = new NoteChunk();
        chunk.setId(document.getId());
        chunk.setNoteId(document.getString("noteId"));
        Long index = document.getLong("chunkIndex");
        chunk.setChunkIndex(index == null ? 0 : index.intValue());
        chunk.setText(document.getString("text"));
        chunk.setTitle(document.getString("title"));
        chunk.setCategory(document.getString("category"));
        Object tags = document.get("tags");
        if (tags instanceof List) {
            List<String> tagList = new ArrayList<>();
            for (Object tag : (List<?>) tags) {
                if (tag != null) {
                    tagList.add(String.valueOf(tag));
                }
            }
            chunk.setTags(tagList);
        }
        chunk.setContentHash(document.getString("contentHash"));
        chunk.setModelId(document.getString("modelId"));
        Long updatedAt = document.getLong("updatedAt");
        chunk.setUpdatedAt(updatedAt == null ? 0L : updatedAt);
        chunk.setEmbedding(readEmbedding(document));
        return chunk;
    }

    private List<Double> readEmbedding(QueryDocumentSnapshot document) {
        Object embeddingValues = document.get("embeddingValues");
        List<Double> fromList = asDoubleList(embeddingValues);
        if (!fromList.isEmpty()) {
            return fromList;
        }

        Object embedding = document.get("embedding");
        if (embedding instanceof VectorValue) {
            try {
                double[] array = ((VectorValue) embedding).toArray();
                List<Double> values = new ArrayList<>();
                for (double value : array) {
                    values.add(value);
                }
                return values;
            } catch (Exception exception) {
                Log.w(TAG, "Unable to read VectorValue.toArray()", exception);
            }
            try {
                return ((VectorValue) embedding).toList();
            } catch (Exception exception) {
                Log.w(TAG, "Unable to read VectorValue.toList()", exception);
            }
        }
        return asDoubleList(embedding);
    }

    private List<Double> asDoubleList(Object value) {
        List<Double> values = new ArrayList<>();
        if (!(value instanceof List)) {
            return values;
        }
        for (Object item : (List<?>) value) {
            if (item instanceof Number) {
                values.add(((Number) item).doubleValue());
            }
        }
        return values;
    }

    private CollectionReference chunksRef(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CHUNKS_COLLECTION);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class Scored {
        private final NoteChunk chunk;
        private final double score;

        private Scored(NoteChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
