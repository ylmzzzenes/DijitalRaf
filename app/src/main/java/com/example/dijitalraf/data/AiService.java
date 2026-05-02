package com.example.dijitalraf.data;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    /** Endpoint sürümle birlikte kodda; anahtar {@code local.properties} + BuildConfig ile gelir. */
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

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Raf içeriğine göre kısa, yapılandırılmış Türkçe öneri üretir (tek tur).
     */
    public void generateBookRecommendations(
            @NonNull String apiKey,
            @Nullable List<Kitap> books,
            @NonNull LlmCallback callback) {
        if (!isApiKeyPresent(apiKey)) {
            postError(callback, ERR_API_KEY_MISSING);
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
            postError(callback, ERR_PREPARE_REQUEST + e.getMessage());
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
            postError(callback, ERR_API_KEY_MISSING);
            return;
        }
        if (messages.length() == 0) {
            postError(callback, ERR_EMPTY_MESSAGES);
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
            postError(callback, ERR_ALL_MODELS_EXHAUSTED);
            return;
        }

        final JSONObject body;
        try {
            body = buildRequestBody(MODEL_CHAIN[modelIndex], messages, maxTokens, temperature);
        } catch (JSONException e) {
            postError(callback, ERR_PREPARE_REQUEST + e.getMessage());
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
                    postError(callback, ERR_RATE_LIMIT);
                    return;
                }

                if (code == 401) {
                    postError(callback, ERR_UNAUTHORIZED);
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
                    postError(callback, ERR_HTTP + code + " — " + snippet);
                    return;
                }

                String content;
                try {
                    content = parseAssistantContent(responseBody);
                } catch (Exception e) {
                    postError(callback, ERR_PARSE + e.getMessage());
                    return;
                }

                if (content.isEmpty()) {
                    postError(callback, ERR_EMPTY_MODEL_REPLY);
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
    private static String mapNetworkError(@NonNull IOException e) {
        if (e instanceof SocketTimeoutException) {
            return ERR_TIMEOUT;
        }
        if (e instanceof UnknownHostException) {
            return ERR_NO_NETWORK;
        }
        String msg = e.getMessage();
        if (msg != null && (msg.toLowerCase().contains("timeout")
                || msg.toLowerCase().contains("timed out"))) {
            return ERR_TIMEOUT;
        }
        return ERR_NETWORK_GENERIC + (msg != null ? msg : e.getClass().getSimpleName());
    }

    private static JSONArray buildRecommendationMessages(@Nullable List<Kitap> books) throws JSONException {
        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content",
                "Sen Türkçe konuşan bir kitap öneri asistanısın. Yalnızca istenen yapıda, kısa ve net yanıt ver. "
                        + "Giriş paragrafı veya özet cümlesi yazma; doğrudan listeye başla.");
        messages.put(system);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", buildRecommendationUserPrompt(books));
        messages.put(user);
        return messages;
    }

    @NonNull
    private static String buildRecommendationUserPrompt(@Nullable List<Kitap> kitaplar) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kullanıcının dijital rafı:\n\n");
        if (kitaplar == null || kitaplar.isEmpty()) {
            sb.append("(Raf boş.)\n\n");
            sb.append("Genel okuyucuya uygun tam 5 kitap öner.\n");
        } else {
            for (Kitap kitap : kitaplar) {
                sb.append("- ");
                sb.append(nullSafe(kitap.getKitapAdi()));
                sb.append(" | Yazar: ");
                sb.append(nullSafe(kitap.getYazar()));
                sb.append(" | Tür: ");
                sb.append(nullSafe(kitap.getTur()));
                sb.append("\n");
            }
            sb.append("\nBuna göre tam 5 kitap öner.\n");
        }
        sb.append("Her öneri için yalnızca şu üç satırı kullan (başlık ekleme):\n");
        sb.append("Kitap: …\nYazar: …\nNeden: …\n");
        sb.append("Öneriler arasında boş satır bırak. Toplam uzunluğu makul tut.");
        return sb.toString();
    }

    @NonNull
    private static String nullSafe(@Nullable String value) {
        return value == null || value.trim().isEmpty() ? "Bilinmiyor" : value.trim();
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

    private static final String ERR_API_KEY_MISSING =
            "OpenRouter API anahtarı yapılandırılmamış. local.properties içinde OPENROUTER_API_KEY tanımlayıp projeyi yeniden derleyin.";

    private static final String ERR_EMPTY_MESSAGES = "Sohbet mesajları boş; istek gönderilemedi.";

    private static final String ERR_PREPARE_REQUEST = "İstek hazırlanamadı: ";

    private static final String ERR_ALL_MODELS_EXHAUSTED =
            "Servis şu an yoğun. Birkaç dakika sonra tekrar deneyin.";

    private static final String ERR_RATE_LIMIT =
            "Çok fazla istek (429). Ücretsiz model kotası dolmuş olabilir; bir süre sonra tekrar deneyin.";

    private static final String ERR_UNAUTHORIZED =
            "API anahtarı geçersiz (401). openrouter.ai/keys üzerinden anahtarı kontrol edip BuildConfig ile yeniden derleyin.";

    private static final String ERR_HTTP = "API hatası ";

    private static final String ERR_PARSE = "Cevap işlenemedi: ";

    private static final String ERR_EMPTY_MODEL_REPLY =
            "Model boş veya geçersiz bir cevap döndü. Tekrar deneyin.";

    private static final String ERR_TIMEOUT =
            "Bağlantı zaman aşımına uğradı. Ağınızı kontrol edip tekrar deneyin.";

    private static final String ERR_NO_NETWORK =
            "İnternet bağlantısı yok veya sunucu adı çözülemedi. Bağlantınızı kontrol edin.";

    private static final String ERR_NETWORK_GENERIC = "Ağ hatası: ";
}
