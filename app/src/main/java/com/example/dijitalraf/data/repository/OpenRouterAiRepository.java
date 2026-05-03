package com.example.dijitalraf.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.data.AiService;
import com.example.dijitalraf.ui.home.Kitap;

import org.json.JSONArray;

import java.util.List;

public final class OpenRouterAiRepository implements AiRepository {

    private final AiService delegate;

    public OpenRouterAiRepository(@NonNull Context context) {
        this.delegate = new AiService(context);
    }

    @Override
    public void generateBookRecommendations(
            @NonNull String apiKey,
            @Nullable List<Kitap> books,
            @NonNull AiService.LlmCallback callback) {
        delegate.generateBookRecommendations(apiKey, books, callback);
    }

    @Override
    public void sendChatMessage(
            @NonNull String apiKey,
            @NonNull JSONArray messages,
            @NonNull AiService.LlmCallback callback) {
        delegate.sendChatMessage(apiKey, messages, callback);
    }
}
