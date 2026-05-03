package com.example.dijitalraf.data.remote.ai;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.data.AiService;
import com.example.dijitalraf.ui.home.Kitap;

import org.json.JSONArray;

import java.util.List;

/**
 * Remote AI gateway for OpenRouter-backed requests.
 */
public final class OpenRouterDataSource {

    private final AiService aiService;

    public OpenRouterDataSource(@NonNull Context context) {
        this(new AiService(context));
    }

    public OpenRouterDataSource(@NonNull AiService aiService) {
        this.aiService = aiService;
    }

    public void generateBookRecommendations(
            @NonNull String apiKey,
            @Nullable List<Kitap> books,
            @NonNull AiService.LlmCallback callback) {
        aiService.generateBookRecommendations(apiKey, books, callback);
    }

    public void sendChatMessage(
            @NonNull String apiKey,
            @NonNull JSONArray messages,
            @NonNull AiService.LlmCallback callback) {
        aiService.sendChatMessage(apiKey, messages, callback);
    }
}
