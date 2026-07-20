package com.hcmute.studymate.ai;

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
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String MODEL = "gemini-2.0-flash";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent";

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

        Request request = new Request.Builder()
                .url(ENDPOINT + "?key=" + apiKey)
                .post(RequestBody.create(body.toString(), JSON))
                .header("Content-Type", "application/json")
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
