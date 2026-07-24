package com.hcmute.studymate.ai;

import android.util.Log;

import com.hcmute.studymate.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Calls Gemini Generative Language API directly from the device.
 * Used so the app can run AI features without Firebase Blaze / Cloud Functions.
 *
 * Security note: the API key is embedded via BuildConfig from local.properties.
 * Fine for coursework/demo; do not ship a production Play Store build this way.
 */
public final class GeminiApiClient {
    private static final String TAG = "GeminiApiClient";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * Prefer lighter / newer models first: free-tier keys often get 404 on retired
     * 2.x models and 503 on overloaded premium Flash endpoints.
     */
    private static final String[] MODEL_CANDIDATES = {
            "gemini-3.1-flash-lite",
            "gemini-3.5-flash",
            "gemini-flash-latest",
            "gemini-3-flash-preview"
    };

    private static final int MAX_ATTEMPTS_PER_MODEL = 3;

    public interface JsonCallback {
        void onSuccess(JSONObject json);

        void onError(Exception exception);
    }

    private static final GeminiApiClient INSTANCE = new GeminiApiClient();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private GeminiApiClient() {
    }

    public static GeminiApiClient getInstance() {
        return INSTANCE;
    }

    public boolean isConfigured() {
        String key = BuildConfig.GEMINI_API_KEY;
        return key != null && !key.trim().isEmpty()
                && !"YOUR_GEMINI_API_KEY".equals(key.trim());
    }

    public void generateJsonAsync(String prompt, double temperature, JsonCallback callback) {
        executor.execute(() -> {
            try {
                callback.onSuccess(generateJsonSync(prompt, temperature));
            } catch (Exception exception) {
                callback.onError(exception);
            }
        });
    }

    public JSONObject generateJsonSync(String prompt, double temperature) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY missing. Add it to local.properties then Sync Gradle.");
        }
        String apiKey = BuildConfig.GEMINI_API_KEY.trim();

        JSONObject part = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray().put(part));
        JSONObject generationConfig = new JSONObject()
                .put("temperature", temperature)
                .put("responseMimeType", "application/json");
        JSONObject body = new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig", generationConfig);
        String bodyJson = body.toString();

        Exception lastError = null;
        for (String model : MODEL_CANDIDATES) {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_MODEL; attempt++) {
                try {
                    return callModel(apiKey, model, bodyJson);
                } catch (IOException exception) {
                    lastError = exception;
                    String message = exception.getMessage() == null ? "" : exception.getMessage();
                    boolean retryable = message.contains("Gemini HTTP 503")
                            || message.contains("Gemini HTTP 429")
                            || message.contains("Gemini HTTP 500");
                    boolean notFound = message.contains("Gemini HTTP 404");
                    Log.w(TAG, "Model " + model + " attempt " + attempt + " failed: " + truncate(message, 180));
                    if (notFound) {
                        break; // try next model
                    }
                    if (retryable && attempt < MAX_ATTEMPTS_PER_MODEL) {
                        Thread.sleep(700L * attempt);
                        continue;
                    }
                    if (retryable) {
                        break; // exhausted retries for this model, try next
                    }
                    throw exception;
                }
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new IllegalStateException("No Gemini model candidates configured");
    }

    private JSONObject callModel(String apiKey, String model, String bodyJson) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent";

        // Auth keys (AQ.*) require x-goog-api-key header.
        Request request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(bodyJson, JSON))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String raw = responseBody == null ? "" : responseBody.string();
            if (!response.isSuccessful()) {
                throw new IOException("Gemini HTTP " + response.code() + ": " + truncate(raw, 400));
            }
            return extractJsonPayload(raw);
        }
    }

    private JSONObject extractJsonPayload(String rawResponse) throws Exception {
        JSONObject root = new JSONObject(rawResponse);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            throw new IllegalStateException("Gemini returned no candidates");
        }
        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
        if (content == null) {
            throw new IllegalStateException("Gemini candidate missing content");
        }
        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.length() == 0) {
            throw new IllegalStateException("Gemini candidate missing parts");
        }
        String text = parts.getJSONObject(0).optString("text", "").trim();
        if (text.isEmpty()) {
            throw new IllegalStateException("Gemini returned empty text");
        }
        return parseJsonObject(text);
    }

    public static JSONObject parseJsonObject(String text) throws Exception {
        try {
            return new JSONObject(text);
        } catch (Exception ignored) {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return new JSONObject(text.substring(start, end + 1));
            }
            throw new IllegalStateException("Gemini returned invalid JSON");
        }
    }

    public static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "…";
    }

    public static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
