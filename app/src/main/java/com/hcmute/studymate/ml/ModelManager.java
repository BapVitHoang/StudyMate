package com.hcmute.studymate.ml;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Downloads and caches ONNX models + BERT vocab under {@code files/models/}.
 */
public class ModelManager {
    private static final String TAG = "ModelManager";
    private static final String MODELS_DIR = "models";
    private static final String VOCAB_FILE = "vocab.txt";
    private static final String EMBED_FILE = "minilm_embed_int8.onnx";
    private static final String RERANK_FILE = "minilm_rerank_int8.onnx";

    public interface ReadyCallback {
        void onReady(boolean ready, Exception error);
    }

    private final Context appContext;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean preparing = new AtomicBoolean(false);
    private final AtomicBoolean ready = new AtomicBoolean(false);

    private String vocabUrl;
    private String embedModelUrl;
    private String rerankModelUrl;
    private BertWordPieceTokenizer tokenizer;

    public ModelManager(Context context) {
        this.appContext = context.getApplicationContext();
        loadUrlConfig();
    }

    public boolean isReady() {
        return ready.get() && tokenizer != null
                && embedModelFile().exists()
                && rerankModelFile().exists()
                && vocabFile().exists();
    }

    public BertWordPieceTokenizer getTokenizer() {
        return tokenizer;
    }

    public File embedModelFile() {
        return new File(modelsDir(), EMBED_FILE);
    }

    public File rerankModelFile() {
        return new File(modelsDir(), RERANK_FILE);
    }

    public File vocabFile() {
        return new File(modelsDir(), VOCAB_FILE);
    }

    public void ensureReadyAsync(ReadyCallback callback) {
        if (isReady()) {
            callback.onReady(true, null);
            return;
        }
        if (!preparing.compareAndSet(false, true)) {
            executor.execute(() -> {
                while (preparing.get()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                callback.onReady(isReady(), isReady() ? null : new IllegalStateException("Models are not ready"));
            });
            return;
        }

        executor.execute(() -> {
            Exception error = null;
            try {
                ensureReadySync();
            } catch (Exception exception) {
                error = exception;
                Log.e(TAG, "Failed to prepare RAG models", exception);
            } finally {
                preparing.set(false);
                callback.onReady(isReady(), error);
            }
        });
    }

    public synchronized void ensureReadySync() throws IOException {
        if (!modelsDir().exists() && !modelsDir().mkdirs()) {
            throw new IOException("Unable to create models directory");
        }

        ensureFile(vocabFile(), vocabUrl, "models/" + VOCAB_FILE, "vocab.txt");
        ensureFile(embedModelFile(), embedModelUrl, "models/" + EMBED_FILE, "embedding ONNX model");
        ensureFile(rerankModelFile(), rerankModelUrl, "models/" + RERANK_FILE, "rerank ONNX model");

        tokenizer = new BertWordPieceTokenizer(vocabFile());
        ready.set(true);
    }

    private void ensureFile(File target, String url, String assetPath, String label) throws IOException {
        if (target.exists() && target.length() > 0) {
            return;
        }
        if (copyAssetIfPresent(assetPath, target)) {
            Log.i(TAG, "Copied " + label + " from assets/" + assetPath);
            return;
        }
        if (url == null || url.trim().isEmpty()) {
            throw new IOException(label + " is missing. Place files under assets/models/ "
                    + "(from scripts/export_rag_models.py), set URLs in assets/rag_model_urls.json, "
                    + "or copy into " + target.getAbsolutePath());
        }
        downloadToFile(url.trim(), target);
    }

    private boolean copyAssetIfPresent(String assetPath, File target) {
        try (InputStream input = appContext.getAssets().open(assetPath)) {
            File temp = new File(target.getAbsolutePath() + ".tmp");
            try (FileOutputStream output = new FileOutputStream(temp)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                output.flush();
            }
            if (target.exists() && !target.delete()) {
                return false;
            }
            return temp.renameTo(target);
        } catch (IOException ignored) {
            return false;
        }
    }

    private void downloadToFile(String url, File target) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Download failed (" + response.code() + ") for " + url);
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty download body for " + url);
            }
            File temp = new File(target.getAbsolutePath() + ".tmp");
            try (InputStream input = body.byteStream();
                 FileOutputStream output = new FileOutputStream(temp)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                output.flush();
            }
            if (target.exists() && !target.delete()) {
                throw new IOException("Unable to replace existing file: " + target.getName());
            }
            if (!temp.renameTo(target)) {
                throw new IOException("Unable to finalize download: " + target.getName());
            }
        }
    }

    private File modelsDir() {
        return new File(appContext.getFilesDir(), MODELS_DIR);
    }

    private void loadUrlConfig() {
        vocabUrl = "";
        embedModelUrl = "";
        rerankModelUrl = "";
        try (InputStream stream = appContext.getAssets().open("rag_model_urls.json")) {
            byte[] bytes = readAll(stream);
            JSONObject json = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            vocabUrl = json.optString("vocabUrl", "");
            embedModelUrl = json.optString("embedModelUrl", "");
            rerankModelUrl = json.optString("rerankModelUrl", "");
        } catch (Exception exception) {
            Log.w(TAG, "Unable to read rag_model_urls.json", exception);
        }
        if (isBlank(vocabUrl)) {
            vocabUrl = com.hcmute.studymate.utils.Constants.DEFAULT_VOCAB_URL;
        }
        if (isBlank(embedModelUrl)) {
            embedModelUrl = com.hcmute.studymate.utils.Constants.DEFAULT_EMBED_MODEL_URL;
        }
        if (isBlank(rerankModelUrl)) {
            rerankModelUrl = com.hcmute.studymate.utils.Constants.DEFAULT_RERANK_MODEL_URL;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        byte[] buffer = new byte[4096];
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
