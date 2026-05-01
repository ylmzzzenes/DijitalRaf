package com.example.dijitalraf.data;

import android.os.Handler;
import android.os.Looper;

import com.example.dijitalraf.ui.home.Kitap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AiRecommendationService {

    public interface AiCallback {
        void onSuccess(String result);

        void onError(String error);
    }

    private static final String OPENROUTER_URL =
            "https://openrouter.ai/api/v1/chat/completions";

    /**
     * Ücretsiz modeller sırayla denenir; biri kota/endpoint hatası verirse diğerine geçilir.
     */
    private static final String[] MODEL_CHAIN = {
            "google/gemma-4-26b-a4b-it:free",
            "google/gemma-4-31b-it:free",
            "openai/gpt-oss-20b:free",
            "qwen/qwen3-next-80b-a3b-instruct:free",
    };

    private static final int MAX_RETRIES_PER_MODEL = 4;
    private static final long BASE_BACKOFF_MS = 1500L;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void getRecommendations(String apiKey, List<Kitap> kitaplar, AiCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            mainHandler.post(() -> callback.onError("OpenRouter API anahtarı yapılandırılmamış."));
            return;
        }
        try {
            String prompt = buildPrompt(kitaplar);
            enqueueChat(apiKey.trim(), prompt, 0, 0, callback);
        } catch (Exception e) {
            callback.onError("İstek hazırlanamadı" + e.getMessage());
        }
    }

    private void enqueueChat(String apiKey, String prompt, int modelIndex, int retryIndex,
                             AiCallback callback) {
        if (modelIndex >= MODEL_CHAIN.length) {
            mainHandler.post(() -> callback.onError(
                    "Öneri servisi şu an çok yoğun (kotayı aştınız). Birkaç dakika sonra tekrar deneyin."));
            return;
        }

        final JSONObject body;
        try {
            body = buildRequestBody(MODEL_CHAIN[modelIndex], prompt);
        } catch (JSONException e) {
            mainHandler.post(() -> callback.onError("İstek hazırlanamadı: " + e.getMessage()));
            return;
        }

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(OPENROUTER_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://github.com/ylmzzzenes/DijitalRaf")
                .addHeader("X-Title", "DijitalRaf")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                final int code = response.code();
                final long retryAfterMs = parseRetryAfterMs(response);
                String responseBody;
                try {
                    responseBody = response.body() != null ? response.body().string() : "";
                } catch (IOException e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                    return;
                } finally {
                    response.close();
                }

                if (code == 429) {
                    long delayMs = backoffDelayMs(retryIndex, retryAfterMs);
                    if (retryIndex + 1 < MAX_RETRIES_PER_MODEL) {
                        mainHandler.postDelayed(
                                () -> enqueueChat(apiKey, prompt, modelIndex, retryIndex + 1, callback),
                                delayMs);
                        return;
                    }
                    if (modelIndex + 1 < MODEL_CHAIN.length) {
                        mainHandler.postDelayed(
                                () -> enqueueChat(apiKey, prompt, modelIndex + 1, 0, callback),
                                Math.min(delayMs, 2000L));
                        return;
                    }
                    mainHandler.post(() -> callback.onError(
                            "Çok fazla istek (429). Ücretsiz model kotası doldu; bir süre sonra tekrar deneyin."));
                    return;
                }

                if (code == 401) {
                    mainHandler.post(() -> callback.onError(
                            "OpenRouter API anahtarı geçersiz veya istekte yok (401). "
                                    + "local.properties dosyasında OPENROUTER_API_KEY=yeni_anahtar "
                                    + "tanımlayıp projeyi yeniden derleyin (openrouter.ai/keys)."));
                    return;
                }

                if (code == 404 && modelIndex + 1 < MODEL_CHAIN.length) {
                    mainHandler.post(() -> enqueueChat(apiKey, prompt, modelIndex + 1, 0, callback));
                    return;
                }

                if (code < 200 || code >= 300) {
                    mainHandler.post(() ->
                            callback.onError("API hatası " + code + " — " + responseBody));
                    return;
                }

                try {
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray choices = json.getJSONArray("choices");
                    JSONObject message = choices
                            .getJSONObject(0)
                            .getJSONObject("message");
                    String content = message.getString("content");
                    mainHandler.post(() -> callback.onSuccess(content));
                } catch (Exception e) {
                    mainHandler.post(() ->
                            callback.onError("Cevap parse edilemedi: " + e.getMessage()));
                }
            }
        });
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

    private static JSONObject buildRequestBody(String model, String prompt) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("model", model);

        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Sen Türkçe konuşan profesyonel bir kitap öneri asistanısın. "
                + "Kullanıcının kitap zevkine göre kısa, net ve mantıklı öneriler ver.");
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(systemMessage);
        messages.put(userMessage);

        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 500);
        return body;
    }

    private String buildPrompt(List<Kitap> kitaplar) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kullanıcının dijital rafındaki kitaplar şunlar:\n\n");
        if (kitaplar == null || kitaplar.isEmpty()) {
            sb.append("Kullanıcının henüz kitabı yok. \n");
            sb.append("Genel okuyucuya uygun 5 başlangıç kitabı öner.\n");
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
        }
        sb.append("\nBuna göre 5 kitap öner.");
        sb.append("\nHer öneriyi şu formatta ver:");
        sb.append("\nKitap: ...");
        sb.append("\nYazar: ...");
        sb.append("\nNeden: ...");
        sb.append("\nCevap Türkçe olsun. Gereksiz uzun açıklama yapma.");

        return sb.toString();
    }

    private String nullSafe(String value) {
        return value == null || value.trim().isEmpty() ? "Bilinmiyor" : value.trim();
    }
}
