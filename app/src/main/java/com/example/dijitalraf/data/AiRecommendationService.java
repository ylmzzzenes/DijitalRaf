package com.example.dijitalraf.data;

import android.os.Handler;
import android.os.Looper;

import com.example.dijitalraf.ui.home.Kitap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

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

    private static final String MODEL =
            "google/gemma-4-31b-it:free";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void getRecommendations(String apiKey, List<Kitap> kitaplar, AiCallback callback)
    {
      try
      {
       String prompt = buildPrompt(kitaplar);

       JSONObject body = new JSONObject();
          body.put("model", MODEL);

       JSONArray messages = new JSONArray();

       JSONObject systemMessage = new JSONObject();
       systemMessage.put("role", "system");
       systemMessage.put("content","Sen Türkçe konuşan profesyonel bir kitap öneri asistanısın." +
               "Kullanıcının kitap zevkine göre kısa, net ve mantıklı öneriler ver."
       );

       JSONObject userMessage = new JSONObject();
       userMessage.put("role","user");
       userMessage.put("content",prompt);

          messages.put(systemMessage);
          messages.put(userMessage);

       body.put("messages",messages);
       body.put("temperature",0.7);
       body.put("max_tokens",500);



       RequestBody requestBody = RequestBody.create(
               body.toString(),
               MediaType.parse("application/json")
       );

       Request request = new Request.Builder()
               .url(OPENROUTER_URL)
               .addHeader("Authorization","Bearer" + apiKey)
               .addHeader("Content-Type","application/json")
               .addHeader("HTTP-Referer","https://github.com/ylmzzzenes/DijitalRaf")
               .addHeader("X-Title","DijitalRaf")
               .post(requestBody)
               .build();

       client.newCall(request).enqueue(new Callback(){
          @Override
          public void onFailure(Call call, IOException e) {
              mainHandler.post(() -> callback.onError(e.getMessage()));
          }

          @Override
           public void onResponse(Call call, Response response) throws IOException{
              String responseBody = response.body() != null ? response.body().string() : "";

              if(!response.isSuccessful()){
                  mainHandler.post(() ->
                          callback.onError("API hatası" + response.code() + " _ " + responseBody));
                  return;
              }

              try{
                  JSONObject json = new JSONObject(responseBody);
                  JSONArray choices = json.getJSONArray("choices");
                  JSONObject message = choices
                          .getJSONObject(0)
                          .getJSONObject("message");

                  String content = message.getString("content");

                  mainHandler.post(() -> callback.onSuccess(content));

              } catch (Exception e) {
                  mainHandler.post(() ->
                          callback.onError("Cevap parse edilemedi" + e.getMessage()));
              }
          }
       });




      } catch (Exception e) {
          callback.onError("İstek hazırlanamadı" + e.getMessage());
      }
    }

    private String buildPrompt(List<Kitap> kitaplar){
        StringBuilder sb = new StringBuilder();
        sb.append("Kullanıcının dijital rafındaki kitaplar şunlar:\n\n");
        if(kitaplar == null || kitaplar.isEmpty()){
        sb.append("Kullanıcının henüz kitabı yok. \n");
        sb.append("Genel okuyucuya uygun 5 başlangıç kitabı öner.\n");
        }
        else{
            for(Kitap kitap : kitaplar){
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

    private String nullSafe(String value){
        return value == null || value.trim().isEmpty() ? "Bilinmiyor" : value.trim();
    }
}

