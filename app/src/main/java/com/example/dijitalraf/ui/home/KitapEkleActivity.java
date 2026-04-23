package com.example.dijitalraf.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dijitalraf.R;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class KitapEkleActivity extends AppCompatActivity {

    private static final String TAG = "KitapEkleActivity";

    private EditText etKitapAdi, etYazar, etTur;
    private Button btnKaydet, btnAra;
    private FirebaseFirestore db;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kitap_ekle);

        db = FirebaseFirestore.getInstance();
        client = new OkHttpClient();

        etKitapAdi = findViewById(R.id.etKitapAdi);
        etYazar = findViewById(R.id.etYazar);
        etTur = findViewById(R.id.etTur);
        btnKaydet = findViewById(R.id.btnKaydet);
        btnAra = findViewById(R.id.btnAra);

        btnKaydet.setOnClickListener(v -> {
            Toast.makeText(KitapEkleActivity.this, "Kaydet butonuna basıldı", Toast.LENGTH_SHORT).show();
            kitapKaydet();
        });

        btnAra.setOnClickListener(v -> kitapAra());
    }

    private void kitapAra() {
        String query = etKitapAdi.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Önce kitap adı girin", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://www.googleapis.com/books/v1/volumes?q=" + query.replace(" ", "+");

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(KitapEkleActivity.this, "API hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() ->
                            Toast.makeText(KitapEkleActivity.this, "Sonuç alınamadı", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String jsonData = response.body().string();

                try {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray items = jsonObject.optJSONArray("items");

                    if (items == null || items.length() == 0) {
                        runOnUiThread(() ->
                                Toast.makeText(KitapEkleActivity.this, "Kitap bulunamadı", Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    JSONObject firstBook = items.getJSONObject(0);
                    JSONObject volumeInfo = firstBook.getJSONObject("volumeInfo");

                    String title = volumeInfo.optString("title", "");
                    String author = "";
                    String category = "";

                    JSONArray authors = volumeInfo.optJSONArray("authors");
                    if (authors != null && authors.length() > 0) {
                        author = authors.getString(0);
                    }

                    JSONArray categories = volumeInfo.optJSONArray("categories");
                    if (categories != null && categories.length() > 0) {
                        category = categories.getString(0);
                    }

                    final String finalTitle = title;
                    final String finalAuthor = author;
                    final String finalCategory = category;

                    runOnUiThread(() -> {
                        etKitapAdi.setText(finalTitle);
                        etYazar.setText(finalAuthor);
                        etTur.setText(finalCategory);
                        Toast.makeText(KitapEkleActivity.this, "Kitap bilgileri getirildi", Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(KitapEkleActivity.this, "Veri işleme hatası", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void kitapKaydet() {
        String kitapAdi = etKitapAdi.getText().toString().trim();
        String yazar = etYazar.getText().toString().trim();
        String tur = etTur.getText().toString().trim();

        Log.d(TAG, "Kaydet başladı");
        Log.d(TAG, "kitapAdi: " + kitapAdi);
        Log.d(TAG, "yazar: " + yazar);
        Log.d(TAG, "tur: " + tur);

        if (kitapAdi.isEmpty() || yazar.isEmpty() || tur.isEmpty()) {
            Toast.makeText(this, "Tüm alanları doldurun", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> kitap = new HashMap<>();
        kitap.put("kitapAdi", kitapAdi);
        kitap.put("yazar", yazar);
        kitap.put("tur", tur);

        db.collection("kitaplar")
                .add(kitap)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Firestore kayıt başarılı: " + documentReference.getId());
                    Toast.makeText(KitapEkleActivity.this, "Kitap eklendi", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore hata: ", e);
                    Toast.makeText(KitapEkleActivity.this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}