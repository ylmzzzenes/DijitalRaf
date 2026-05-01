package com.example.dijitalraf.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dijitalraf.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.firebase.auth.FirebaseAuth;

public class KitapEkleActivity extends AppCompatActivity {

    private static final String TAG = "KitapEkleActivity";

    private MaterialToolbar toolbar;
    private TextInputEditText etKitapAdi, etYazar, etTur;
    private TextInputLayout tilKitapAdi, tilYazar, tilTur;
    private MaterialButton btnKaydet, btnAra;
    private TextView tvPreviewTitle, tvPreviewAuthor;
    private Chip chipPreviewGenre;
    private LinearProgressIndicator progressApi;
    private DatabaseReference kitaplarRef;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kitap_ekle);

        initViews();
        setupToolbar();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        kitaplarRef = FirebaseDatabase
                .getInstance("https://dijitalraf-ec149-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("books")
                .child(uid);
        client = new OkHttpClient();

        TextWatcher previewWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        etKitapAdi.addTextChangedListener(previewWatcher);
        etYazar.addTextChangedListener(previewWatcher);
        etTur.addTextChangedListener(previewWatcher);

        updatePreview();

        btnKaydet.setOnClickListener(v -> kitapKaydet());
        btnAra.setOnClickListener(v -> kitapAra());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etKitapAdi = findViewById(R.id.etKitapAdi);
        etYazar = findViewById(R.id.etYazar);
        etTur = findViewById(R.id.etTur);
        tilKitapAdi = findViewById(R.id.tilKitapAdi);
        tilYazar = findViewById(R.id.tilYazar);
        tilTur = findViewById(R.id.tilTur);
        btnKaydet = findViewById(R.id.btnKaydet);
        btnAra = findViewById(R.id.btnAra);
        tvPreviewTitle = findViewById(R.id.tvPreviewTitle);
        tvPreviewAuthor = findViewById(R.id.tvPreviewAuthor);
        chipPreviewGenre = findViewById(R.id.chipPreviewGenre);
        progressApi = findViewById(R.id.progressApi);
    }

    private void updatePreview() {
        String title = etKitapAdi.getText() != null ? etKitapAdi.getText().toString().trim() : "";
        String author = etYazar.getText() != null ? etYazar.getText().toString().trim() : "";
        String genre = etTur.getText() != null ? etTur.getText().toString().trim() : "";

        tvPreviewTitle.setText(title.isEmpty() ? getString(R.string.hint_book_title) : title);
        tvPreviewAuthor.setText(author.isEmpty() ? getString(R.string.hint_author) : author);
        if (genre.isEmpty()) {
            chipPreviewGenre.setVisibility(View.GONE);
        } else {
            chipPreviewGenre.setVisibility(View.VISIBLE);
            chipPreviewGenre.setText(genre);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setApiLoading(boolean loading) {
        btnAra.setEnabled(!loading);
        progressApi.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            progressApi.setIndeterminate(true);
        }
    }

    private void kitapAra() {
        String query = etKitapAdi.getText() != null ? etKitapAdi.getText().toString().trim() : "";

        if (query.isEmpty()) {
            tilKitapAdi.setError(getString(R.string.error_enter_title_first));
            etKitapAdi.requestFocus();
            return;
        }
        tilKitapAdi.setError(null);

        setApiLoading(true);

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://www.googleapis.com/books/v1/volumes?q=" + encodedQuery;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setApiLoading(false);
                    Toast.makeText(KitapEkleActivity.this, getString(R.string.api_error, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> {
                        setApiLoading(false);
                        Toast.makeText(KitapEkleActivity.this, R.string.result_not_found, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String jsonData = response.body().string();

                try {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray items = jsonObject.optJSONArray("items");

                    if (items == null || items.length() == 0) {
                        runOnUiThread(() -> {
                            setApiLoading(false);
                            Toast.makeText(KitapEkleActivity.this, R.string.book_not_found, Toast.LENGTH_SHORT).show();
                        });
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
                        setApiLoading(false);
                        etKitapAdi.setText(finalTitle);
                        etYazar.setText(finalAuthor);
                        etTur.setText(finalCategory);
                        updatePreview();
                        Toast.makeText(KitapEkleActivity.this, R.string.book_info_loaded, Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        setApiLoading(false);
                        Toast.makeText(KitapEkleActivity.this, R.string.data_parse_error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void kitapKaydet() {
        String kitapAdi = etKitapAdi.getText() != null ? etKitapAdi.getText().toString().trim() : "";
        String yazar = etYazar.getText() != null ? etYazar.getText().toString().trim() : "";
        String tur = etTur.getText() != null ? etTur.getText().toString().trim() : "";

        Log.d(TAG, "Kaydet başladı");

        tilKitapAdi.setError(null);
        tilYazar.setError(null);
        tilTur.setError(null);

        if (kitapAdi.isEmpty()) {
            tilKitapAdi.setError(getString(R.string.field_required));
            etKitapAdi.requestFocus();
            return;
        }
        if (yazar.isEmpty()) {
            tilYazar.setError(getString(R.string.field_required));
            etYazar.requestFocus();
            return;
        }
        if (tur.isEmpty()) {
            tilTur.setError(getString(R.string.field_required));
            etTur.requestFocus();
            return;
        }

        Map<String, Object> kitap = new HashMap<>();
        kitap.put("uid",FirebaseAuth.getInstance().getCurrentUser().getUid());
        kitap.put("kitapAdi", kitapAdi);
        kitap.put("yazar", yazar);
        kitap.put("tur", tur);
        kitap.put("createdAt",System.currentTimeMillis());

        kitaplarRef.push().setValue(kitap)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(KitapEkleActivity.this, R.string.book_added, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(KitapEkleActivity.this, getString(R.string.error_generic, e.getMessage()),
                                Toast.LENGTH_LONG).show()
                );
    }
}
