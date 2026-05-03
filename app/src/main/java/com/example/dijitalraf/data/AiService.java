package com.example.dijitalraf.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.R;
import com.example.dijitalraf.ui.home.Kitap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OpenRouter üzerinden tek LLM altyapısı: kitap önerileri ve sohbet.
 * API anahtarı yalnızca çağıran katmandan ({@link com.example.dijitalraf.BuildConfig}) geçirilir; burada saklanmaz ve loglanmaz.
 */
public final class AiService {

    public interface LlmCallback {
        void onSuccess(@NonNull String content);

        void onError(@NonNull String userMessage);
    }

    private static final String OPENROUTER_CHAT_COMPLETIONS_URL =
            "https://openrouter.ai/api/v1/chat/completions";

    private static final String HTTP_REFERER = "https://github.com/ylmzzzenes/DijitalRaf";
    private static final String APP_TITLE = "DijitalRaf";

    private static final String[] MODEL_CHAIN = {
            "google/gemma-4-26b-a4b-it:free",
            "google/gemma-4-31b-it:free",
            "openai/gpt-oss-20b:free",
            "qwen/qwen3-next-80b-a3b-instruct:free",
    };

    private static final int MAX_RETRIES_PER_MODEL = 4;
    private static final long BASE_BACKOFF_MS = 1500L;

    private static final int RECOMMENDATION_MAX_TOKENS = 480;
    private static final double RECOMMENDATION_TEMPERATURE = 0.45;

    private static final int CHAT_MAX_TOKENS = 1200;
    private static final double CHAT_TEMPERATURE = 0.7;

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AiService(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Raf içeriğine göre kısa, yapılandırılmış öneri üretir (tek tur).
     */
    public void generateBookRecommendations(
            @NonNull String apiKey,
            @Nullable List<Kitap> books,
            @NonNull LlmCallback callback) {
        if (!isApiKeyPresent(apiKey)) {
            postError(callback, appContext.getString(R.string.error_openrouter_key_missing));
            return;
        }
        try {
            JSONArray messages = buildRecommendationMessages(books);
            enqueueCompletion(
                    apiKey.trim(),
                    messages,
                    RECOMMENDATION_MAX_TOKENS,
                    RECOMMENDATION_TEMPERATURE,
                    0,
                    0,
                    callback);
        } catch (JSONException e) {
            postError(callback, appContext.getString(R.string.ai_error_prepare_request, e.getMessage()));
        }
    }

    /**
     * Sohbet: {@code messages} içinde system / user / assistant sırası korunmalıdır.
     */
    public void sendChatMessage(
            @NonNull String apiKey,
            @NonNull JSONArray messages,
            @NonNull LlmCallback callback) {
        if (!isApiKeyPresent(apiKey)) {
            postError(callback, appContext.getString(R.string.error_openrouter_key_missing));
            return;
        }
        if (messages.length() == 0) {
            postError(callback, appContext.getString(R.string.ai_error_chat_messages_empty));
            return;
        }
        enqueueCompletion(
                apiKey.trim(),
                messages,
                CHAT_MAX_TOKENS,
                CHAT_TEMPERATURE,
                0,
                0,
                callback);
    }

    private static boolean isApiKeyPresent(@NonNull String apiKey) {
        return apiKey.trim().length() > 0;
    }

    private void postError(@NonNull LlmCallback callback, @NonNull String msg) {
        mainHandler.post(() -> callback.onError(msg));
    }

    private void enqueueCompletion(
            @NonNull String apiKey,
            @NonNull JSONArray messages,
            int maxTokens,
            double temperature,
            int modelIndex,
            int retryIndex,
            @NonNull LlmCallback callback) {

        if (modelIndex >= MODEL_CHAIN.length) {
            postError(callback, appContext.getString(R.string.ai_error_service_busy));
            return;
        }

        final JSONObject body;
        try {
            body = buildRequestBody(MODEL_CHAIN[modelIndex], messages, maxTokens, temperature);
        } catch (JSONException e) {
            postError(callback, appContext.getString(R.string.ai_error_prepare_request, e.getMessage()));
            return;
        }

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(OPENROUTER_CHAT_COMPLETIONS_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", HTTP_REFERER)
                .addHeader("X-Title", APP_TITLE)
                .post(requestBody)
                .build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError(mapNetworkError(e)));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                final int code = response.code();
                final long retryAfterMs = parseRetryAfterMs(response);
                String responseBody;
                try {
                    responseBody = response.body() != null ? response.body().string() : "";
                } catch (IOException e) {
                    mainHandler.post(() -> callback.onError(mapNetworkError(e)));
                    return;
                } finally {
                    response.close();
                }

                if (code == 429) {
                    long delayMs = backoffDelayMs(retryIndex, retryAfterMs);
                    if (retryIndex + 1 < MAX_RETRIES_PER_MODEL) {
                        mainHandler.postDelayed(
                                () -> enqueueCompletion(
                                        apiKey, messages, maxTokens, temperature,
                                        modelIndex, retryIndex + 1, callback),
                                delayMs);
                        return;
                    }
                    if (modelIndex + 1 < MODEL_CHAIN.length) {
                        mainHandler.postDelayed(
                                () -> enqueueCompletion(
                                        apiKey, messages, maxTokens, temperature,
                                        modelIndex + 1, 0, callback),
                                Math.min(delayMs, 2000L));
                        return;
                    }
                    postError(callback, appContext.getString(R.string.ai_error_rate_limit));
                    return;
                }

                if (code == 401) {
                    postError(callback, appContext.getString(R.string.ai_error_unauthorized));
                    return;
                }

                if (code == 404 && modelIndex + 1 < MODEL_CHAIN.length) {
                    mainHandler.post(() -> enqueueCompletion(
                            apiKey, messages, maxTokens, temperature,
                            modelIndex + 1, 0, callback));
                    return;
                }

                if (code < 200 || code >= 300) {
                    String snippet = responseBody.length() > 400
                            ? responseBody.substring(0, 400) + "…"
                            : responseBody;
                    postError(callback, appContext.getString(R.string.ai_error_http, code, snippet));
                    return;
                }

                String content;
                try {
                    content = parseAssistantContent(responseBody);
                } catch (Exception e) {
                    postError(callback, appContext.getString(R.string.ai_error_parse, e.getMessage()));
                    return;
                }

                if (content.isEmpty()) {
                    postError(callback, appContext.getString(R.string.ai_error_empty_reply));
                    return;
                }

                mainHandler.post(() -> callback.onSuccess(content));
            }
        });
    }

    @NonNull
    private static String parseAssistantContent(@NonNull String responseBody) throws JSONException {
        JSONObject json = new JSONObject(responseBody);
        JSONArray choices = json.getJSONArray("choices");
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String raw = message.optString("content", "");
        return raw != null ? raw.trim() : "";
    }

    @NonNull
    private String mapNetworkError(@NonNull IOException e) {
        if (e instanceof SocketTimeoutException) {
            return appContext.getString(R.string.ai_error_timeout);
        }
        if (e instanceof UnknownHostException) {
            return appContext.getString(R.string.ai_error_no_network);
        }
        String msg = e.getMessage();
        if (msg != null && (msg.toLowerCase().contains("timeout")
                || msg.toLowerCase().contains("timed out"))) {
            return appContext.getString(R.string.ai_error_timeout);
        }
        return appContext.getString(R.string.ai_error_network,
                msg != null ? msg : e.getClass().getSimpleName());
    }

    private JSONArray buildRecommendationMessages(@Nullable List<Kitap> books) throws JSONException {
        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", appContext.getString(R.string.ai_recommendation_system_prompt));
        messages.put(system);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", buildRecommendationUserPrompt(books));
        messages.put(user);
        return messages;
    }

    @NonNull
    private String buildRecommendationUserPrompt(@Nullable List<Kitap> kitaplar) {
        StringBuilder sb = new StringBuilder();
        sb.append(appContext.getString(R.string.ai_recommendation_shelf_header));
        if (kitaplar == null || kitaplar.isEmpty()) {
            sb.append(appContext.getString(R.string.ai_recommendation_shelf_empty_marker));
            sb.append(appContext.getString(R.string.ai_recommendation_suggest_five_general));
        } else {
            for (Kitap kitap : kitaplar) {
                sb.append(appContext.getString(
                        R.string.ai_recommendation_shelf_line,
                        nullSafe(kitap.getKitapAdi()),
                        nullSafe(kitap.getYazar()),
                        nullSafe(kitap.getTur())));
            }
            sb.append(appContext.getString(R.string.ai_recommendation_suggest_five_from_shelf));
        }
        sb.append(appContext.getString(R.string.ai_recommendation_format_instructions));
        return sb.toString();
    }

    @NonNull
    private String nullSafe(@Nullable String value) {
        return value == null || value.trim().isEmpty()
                ? appContext.getString(R.string.ai_unknown)
                : value.trim();
    }

    private static long parseRetryAfterMs(Response response) {
        String ra = response.header("Retry-After");
        if (ra == null || ra.isEmpty()) {
            return -1L;
        }
        try {
            long seconds = Long.parseLong(ra.trim());
            return Math.min(seconds * 1000L, 120_000L);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private static long backoffDelayMs(int retryIndex, long retryAfterHeaderMs) {
        if (retryAfterHeaderMs > 0L) {
            return retryAfterHeaderMs;
        }
        return Math.min(BASE_BACKOFF_MS * (1L << retryIndex), 60_000L);
    }

    private static JSONObject buildRequestBody(
            @NonNull String model,
            @NonNull JSONArray messages,
            int maxTokens,
            double temperature) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        return body;
    }
}
