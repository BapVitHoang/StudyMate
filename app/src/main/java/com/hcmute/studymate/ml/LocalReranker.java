package com.hcmute.studymate.ml;

import android.util.Log;

import com.hcmute.studymate.utils.Constants;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

/**
 * Local cross-encoder reranker (ms-marco-MiniLM-L-6-v2 ONNX).
 */
public class LocalReranker {
    private static final String TAG = "LocalReranker";

    public static final class ScoredPassage {
        public final String id;
        public final String text;
        public final double score;

        public ScoredPassage(String id, String text, double score) {
            this.id = id;
            this.text = text;
            this.score = score;
        }
    }

    private final ModelManager modelManager;
    private final OrtEnvironment environment;
    private OrtSession session;

    public LocalReranker(ModelManager modelManager) {
        this.modelManager = modelManager;
        this.environment = OrtEnvironment.getEnvironment();
    }

    public boolean isReady() {
        return modelManager.isReady();
    }

    public List<ScoredPassage> rerank(String query, List<ScoredPassage> passages, int topN)
            throws Exception {
        if (passages == null || passages.isEmpty()) {
            return new ArrayList<>();
        }
        ensureSession();
        BertWordPieceTokenizer tokenizer = modelManager.getTokenizer();
        List<ScoredPassage> scored = new ArrayList<>();
        for (ScoredPassage passage : passages) {
            double score = scorePair(tokenizer, query, passage.text);
            scored.add(new ScoredPassage(passage.id, passage.text, score));
        }
        scored.sort((left, right) -> Double.compare(right.score, left.score));
        int limit = Math.min(Math.max(topN, 1), scored.size());
        return new ArrayList<>(scored.subList(0, limit));
    }

    private double scorePair(BertWordPieceTokenizer tokenizer, String query, String passage)
            throws OrtException {
        BertWordPieceTokenizer.EncodedText encoded =
                tokenizer.encodePair(safe(query), safe(passage), Constants.RERANK_MAX_SEQ_LEN);

        Map<String, OnnxTensor> inputs = new HashMap<>();
        OnnxTensor inputIds = null;
        OnnxTensor attentionMask = null;
        OnnxTensor tokenTypeIds = null;
        try {
            inputIds = OnnxTensor.createTensor(environment, LongBuffer.wrap(encoded.inputIds),
                    new long[]{1, encoded.inputIds.length});
            attentionMask = OnnxTensor.createTensor(environment, LongBuffer.wrap(encoded.attentionMask),
                    new long[]{1, encoded.attentionMask.length});
            inputs.put(resolveInputName(session, "input_ids", 0), inputIds);
            inputs.put(resolveInputName(session, "attention_mask", 1), attentionMask);
            String tokenTypeName = findInputName(session, "token_type_ids");
            if (tokenTypeName != null) {
                tokenTypeIds = OnnxTensor.createTensor(environment, LongBuffer.wrap(encoded.tokenTypeIds),
                        new long[]{1, encoded.tokenTypeIds.length});
                inputs.put(tokenTypeName, tokenTypeIds);
            }

            try (OrtSession.Result result = session.run(inputs)) {
                Object raw = result.get(0).getValue();
                return extractScore(raw);
            }
        } finally {
            closeQuietly(inputIds);
            closeQuietly(attentionMask);
            closeQuietly(tokenTypeIds);
        }
    }

    private double extractScore(Object raw) {
        if (raw instanceof float[][]) {
            float[][] matrix = (float[][]) raw;
            if (matrix.length > 0 && matrix[0].length > 0) {
                if (matrix[0].length == 1) {
                    return matrix[0][0];
                }
                // Softmax-style relevance when 2-class logits are returned.
                float neg = matrix[0][0];
                float pos = matrix[0][1];
                return pos - neg;
            }
        }
        if (raw instanceof float[]) {
            float[] values = (float[]) raw;
            if (values.length == 1) {
                return values[0];
            }
            if (values.length >= 2) {
                return values[1] - values[0];
            }
        }
        throw new IllegalStateException("Unsupported reranker output type: "
                + (raw == null ? "null" : raw.getClass().getName()));
    }

    private synchronized void ensureSession() throws OrtException {
        if (!modelManager.isReady()) {
            throw new IllegalStateException("Rerank model is not ready");
        }
        if (session == null) {
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            session = environment.createSession(modelManager.rerankModelFile().getAbsolutePath(), options);
            Log.i(TAG, "Loaded rerank ONNX session");
        }
    }

    private String resolveInputName(OrtSession ortSession, String preferred, int fallbackIndex) {
        String found = findInputName(ortSession, preferred);
        if (found != null) {
            return found;
        }
        String[] names = ortSession.getInputNames().toArray(new String[0]);
        if (fallbackIndex >= 0 && fallbackIndex < names.length) {
            return names[fallbackIndex];
        }
        throw new IllegalStateException("ONNX model is missing input: " + preferred);
    }

    private String findInputName(OrtSession ortSession, String preferred) {
        for (String name : ortSession.getInputNames()) {
            if (preferred.equalsIgnoreCase(name)) {
                return name;
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    private void closeQuietly(OnnxTensor tensor) {
        if (tensor == null) {
            return;
        }
        try {
            tensor.close();
        } catch (Exception ignored) {
        }
    }

    public synchronized void close() {
        if (session != null) {
            try {
                session.close();
            } catch (OrtException ignored) {
            }
            session = null;
        }
    }
}
