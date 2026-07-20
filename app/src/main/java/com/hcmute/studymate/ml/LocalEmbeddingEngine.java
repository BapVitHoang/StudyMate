package com.hcmute.studymate.ml;

import android.util.Log;

import com.hcmute.studymate.utils.Constants;

import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

/**
 * Local sentence embedding with all-MiniLM-L6-v2 ONNX (mean-pool + L2 normalize).
 */
public class LocalEmbeddingEngine {
    private static final String TAG = "LocalEmbeddingEngine";

    private final ModelManager modelManager;
    private final OrtEnvironment environment;
    private OrtSession session;

    public LocalEmbeddingEngine(ModelManager modelManager) {
        this.modelManager = modelManager;
        this.environment = OrtEnvironment.getEnvironment();
    }

    public synchronized boolean isReady() {
        return modelManager.isReady();
    }

    public float[] embed(String text) throws Exception {
        ensureSession();
        BertWordPieceTokenizer tokenizer = modelManager.getTokenizer();
        BertWordPieceTokenizer.EncodedText encoded = tokenizer.encode(text, Constants.EMBED_MAX_SEQ_LEN);

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
                float[] embedding = extractEmbedding(raw, encoded.attentionMask);
                return VectorMath.l2Normalize(embedding);
            }
        } finally {
            closeQuietly(inputIds);
            closeQuietly(attentionMask);
            closeQuietly(tokenTypeIds);
        }
    }

    private synchronized void ensureSession() throws OrtException {
        if (!modelManager.isReady()) {
            throw new IllegalStateException("Embedding model is not ready");
        }
        if (session == null) {
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            session = environment.createSession(modelManager.embedModelFile().getAbsolutePath(), options);
            Log.i(TAG, "Loaded embedding ONNX session");
        }
    }

    private float[] extractEmbedding(Object raw, long[] attentionMask) {
        if (raw instanceof float[][]) {
            float[][] matrix = (float[][]) raw;
            if (matrix.length == 1) {
                return matrix[0];
            }
        }
        if (raw instanceof float[][][]) {
            float[][][] hidden = (float[][][]) raw;
            return meanPool(hidden[0], attentionMask);
        }
        if (raw instanceof float[]) {
            return (float[]) raw;
        }
        throw new IllegalStateException("Unsupported embedding output type: "
                + (raw == null ? "null" : raw.getClass().getName()));
    }

    private float[] meanPool(float[][] tokenVectors, long[] attentionMask) {
        int hidden = tokenVectors[0].length;
        float[] pooled = new float[hidden];
        float count = 0f;
        int limit = Math.min(tokenVectors.length, attentionMask.length);
        for (int i = 0; i < limit; i++) {
            if (attentionMask[i] == 0) {
                continue;
            }
            count += 1f;
            for (int h = 0; h < hidden; h++) {
                pooled[h] += tokenVectors[i][h];
            }
        }
        if (count < 1f) {
            return pooled;
        }
        for (int h = 0; h < hidden; h++) {
            pooled[h] /= count;
        }
        return pooled;
    }

    private String resolveInputName(OrtSession ortSession, String preferred, int fallbackIndex)
            throws OrtException {
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
