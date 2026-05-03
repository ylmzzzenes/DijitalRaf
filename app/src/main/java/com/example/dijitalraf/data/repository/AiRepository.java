package com.example.dijitalraf.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.data.AiService;
import com.example.dijitalraf.ui.home.Kitap;

import org.json.JSONArray;

import java.util.List;

/**
 * LLM / OpenRouter access. Keeps UI layers depending on this interface instead of {@link AiService}
 * directly so networking can be swapped or tested.
 */
public interface AiRepository {

    void generateBookRecommendations(
            @NonNull String apiKey,
            @Nullable List<Kitap> books,
            @NonNull AiService.LlmCallback callback);

    void sendChatMessage(
            @NonNull String apiKey,
            @NonNull JSONArray messages,
            @NonNull AiService.LlmCallback callback);
}
