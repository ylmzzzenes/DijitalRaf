package com.example.dijitalraf.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dijitalraf.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
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

public class KitapEkleActivity extends AppCompatActivity {

    private static final String TAG = "KitapEkleActivity";

    private static final String OPEN_LIBRARY_SEARCH =
            "https://openlibrary.org/search.json";
    private static final String USER_AGENT = "DijitalRaf/1.0 (Android)";

    private MaterialToolbar toolbar;
    private TextInputEditText etKitapAdi, etYazar, etTur;
    private TextInputLayout tilKitapAdi, tilYazar, tilTur;
    private MaterialButton btnKaydet, btnAra;
    private TextView tvPreviewTitle, tvPreviewAuthor;
    private Chip chipPreviewGenre;
    private LinearProgressIndicator progressApi;
    private DatabaseReference kitaplarRef;
    private OkHttpClient client;

    private String selectedImageUrl = "";
    private String apiAciklama = "";
    private String apiSayfaSayisi = "";
    private String apiYayinTarihi = "";

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
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview();
            }
            @Override public void afterTextChanged(Editable s) {}
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

        if (TextUtils.isEmpty(query)) {
            tilKitapAdi.setError(getString(R.string.error_enter_title_first));
            Toast.makeText(this, R.string.error_search_empty, Toast.LENGTH_SHORT).show();
            etKitapAdi.requestFocus();
            return;
        }

        selectedImageUrl = "";
        apiAciklama = "";
        apiSayfaSayisi = "";
        apiYayinTarihi = "";
        tilKitapAdi.setError(null);
        setApiLoading(true);

        searchBookFromOpenLibrary(query);
    }

    /**
     * Open Library araması; OkHttp callback arka planda çalışır, UI güncellemeleri runOnUiThread ile yapılır.
     */
    private void searchBookFromOpenLibrary(final String queryTrimmed) {
        Request request = buildOpenLibrarySearchRequest(queryTrimmed);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    setApiLoading(false);
                    Toast.makeText(
                            KitapEkleActivity.this,
                            getString(R.string.api_error, e.getMessage()),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                JSONObject doc = null;
                try (Response r = response) {
                    if (!r.isSuccessful() || r.body() == null) {
                        runOnUiThread(() -> {
                            setApiLoading(false);
                            Toast.makeText(
                                    KitapEkleActivity.this,
                                    R.string.result_not_found,
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                        return;
                    }

                    String jsonData = r.body().string();
                    JSONObject root = new JSONObject(jsonData);
                    JSONArray docs = root.optJSONArray("docs");
                    doc = pickBestOpenLibraryDoc(docs);

                    if (doc == null) {
                        String normalized = normalizeTurkishCharacters(queryTrimmed);
                        if (!normalized.equals(queryTrimmed)) {
                            try (Response r2 = client.newCall(buildOpenLibrarySearchRequest(normalized)).execute()) {
                                if (r2.isSuccessful() && r2.body() != null) {
                                    JSONObject root2 = new JSONObject(r2.body().string());
                                    doc = pickBestOpenLibraryDoc(root2.optJSONArray("docs"));
                                }
                            } catch (IOException e) {
                                Log.w(TAG, "İkinci arama isteği başarısız", e);
                            }
                        }
                    }

                    if (doc == null) {
                        runOnUiThread(() -> {
                            setApiLoading(false);
                            Toast.makeText(
                                    KitapEkleActivity.this,
                                    R.string.open_library_no_results,
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                        return;
                    }

                    final String title = buildOpenLibraryTitle(doc);
                    final String author = firstOpenLibraryAuthor(doc);
                    String category = primarySubjectFromOpenLibraryDoc(doc);
                    final String coverUrl = coverUrlFromOpenLibraryDoc(doc);
                    int pages = doc.optInt("number_of_pages_median", 0);
                    final String pagesStr = pages > 0 ? String.valueOf(pages) : "";
                    final String published = publishYearFromOpenLibraryDoc(doc);

                    String workKey = doc.optString("key", "");
                    OpenLibraryWorkExtras workExtras = OpenLibraryWorkExtras.empty();
                    if (!workKey.isEmpty()) {
                        workExtras = fetchOpenLibraryWorkExtrasSync(workKey);
                    }
                    if (category.isEmpty() && !workExtras.subject.isEmpty()) {
                        category = workExtras.subject;
                    }

                    final String finalDescription = workExtras.description;
                    final String finalTitle = title;
                    final String finalAuthor = author;
                    final String finalCategory = category;
                    final String finalCoverUrl = coverUrl;
                    final String finalPagesStr = pagesStr;
                    final String finalPublished = published;

                    runOnUiThread(() -> {
                        setApiLoading(false);

                        etKitapAdi.setText(finalTitle);
                        etYazar.setText(finalAuthor);
                        etTur.setText(finalCategory);

                        selectedImageUrl = finalCoverUrl;
                        apiAciklama = finalDescription;
                        apiSayfaSayisi = finalPagesStr;
                        apiYayinTarihi = finalPublished;

                        updatePreview();
                        Toast.makeText(KitapEkleActivity.this, R.string.book_info_loaded, Toast.LENGTH_SHORT).show();

                        Log.d(TAG, "Open Library seçimi: " + finalTitle + " | kapak: " + selectedImageUrl);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Open Library parse/akış hatası", e);
                    runOnUiThread(() -> {
                        setApiLoading(false);
                        Toast.makeText(
                                KitapEkleActivity.this,
                                R.string.data_parse_error,
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }
            }
        });
    }

    @NonNull
    private Request buildOpenLibrarySearchRequest(@NonNull String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = OPEN_LIBRARY_SEARCH + "?q=" + encoded + "&limit=15";
        return new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();
    }

    @Nullable
    private JSONObject pickBestOpenLibraryDoc(@Nullable JSONArray docs) {
        if (docs == null || docs.length() == 0) {
            return null;
        }
        JSONObject best = null;
        int bestScore = -1;
        for (int i = 0; i < docs.length(); i++) {
            JSONObject d = docs.optJSONObject(i);
            if (d == null) {
                continue;
            }
            int score = 0;
            JSONArray authors = d.optJSONArray("author_name");
            if (authors != null && authors.length() > 0) {
                score += 2;
            }
            if (d.optInt("cover_i", 0) > 0 || hasOpenLibraryIsbn(d)) {
                score += 2;
            }
            if (!d.optString("title", "").trim().isEmpty()) {
                score += 1;
            }
            if (docHasAnySubjectHint(d)) {
                score += 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = d;
            }
        }
        return best != null ? best : docs.optJSONObject(0);
    }

    @NonNull
    private String buildOpenLibraryTitle(@NonNull JSONObject doc) {
        String t = doc.optString("title", "").trim();
        String sub = doc.optString("subtitle", "").trim();
        if (!sub.isEmpty()) {
            t = t.isEmpty() ? sub : t + ": " + sub;
        }
        return t;
    }

    @NonNull
    private String firstOpenLibraryAuthor(@NonNull JSONObject doc) {
        JSONArray a = doc.optJSONArray("author_name");
        if (a != null && a.length() > 0) {
            return a.optString(0, "").trim();
        }
        return "";
    }

    /** Arama dokümanından tür: subject, yoksa subject_facet. */
    @NonNull
    private String primarySubjectFromOpenLibraryDoc(@NonNull JSONObject doc) {
        String s = firstStringFromJsonArray(doc, "subject");
        if (!s.isEmpty()) {
            return s;
        }
        return firstStringFromJsonArray(doc, "subject_facet");
    }

    private boolean docHasAnySubjectHint(@NonNull JSONObject doc) {
        JSONArray a = doc.optJSONArray("subject");
        if (a != null && a.length() > 0) {
            return true;
        }
        JSONArray b = doc.optJSONArray("subject_facet");
        return b != null && b.length() > 0;
    }

    @NonNull
    private String firstStringFromJsonArray(@NonNull JSONObject doc, @NonNull String key) {
        JSONArray arr = doc.optJSONArray(key);
        if (arr != null && arr.length() > 0) {
            return arr.optString(0, "").trim();
        }
        return "";
    }

    private boolean hasOpenLibraryIsbn(@NonNull JSONObject doc) {
        JSONArray isbn = doc.optJSONArray("isbn");
        return isbn != null && isbn.length() > 0 && !isbn.optString(0, "").trim().isEmpty();
    }

    @NonNull
    private String coverUrlFromOpenLibraryDoc(@NonNull JSONObject doc) {
        int coverI = doc.optInt("cover_i", 0);
        if (coverI > 0) {
            return "https://covers.openlibrary.org/b/id/" + coverI + "-L.jpg";
        }
        JSONArray isbn = doc.optJSONArray("isbn");
        if (isbn != null && isbn.length() > 0) {
            String isbnStr = isbn.optString(0, "").trim();
            if (!isbnStr.isEmpty()) {
                return "https://covers.openlibrary.org/b/isbn/" + isbnStr + "-L.jpg";
            }
        }
        return "";
    }

    @NonNull
    private String publishYearFromOpenLibraryDoc(@NonNull JSONObject doc) {
        if (doc.has("first_publish_year") && !doc.isNull("first_publish_year")) {
            try {
                return String.valueOf(doc.getInt("first_publish_year"));
            } catch (Exception ignored) {
            }
        }
        JSONArray py = doc.optJSONArray("publish_year");
        if (py != null && py.length() > 0) {
            return py.optString(0, "").trim();
        }
        return "";
    }

    /**
     * Work JSON: açıklama + subjects (tür) tek istekte; arka plan iş parçacığında çağrılır.
     */
    @NonNull
    private OpenLibraryWorkExtras fetchOpenLibraryWorkExtrasSync(@NonNull String workKey) {
        if (!workKey.startsWith("/works/")) {
            return OpenLibraryWorkExtras.empty();
        }
        String url = "https://openlibrary.org" + workKey + ".json";
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();
        try (Response wr = client.newCall(req).execute()) {
            if (!wr.isSuccessful() || wr.body() == null) {
                return OpenLibraryWorkExtras.empty();
            }
            JSONObject work = new JSONObject(wr.body().string());
            String description = extractOpenLibraryDescription(work);
            String subject = firstStringFromJsonArray(work, "subjects");
            if (subject.isEmpty()) {
                subject = firstStringFromJsonArray(work, "subject");
            }
            return new OpenLibraryWorkExtras(description, subject);
        } catch (Exception e) {
            Log.w(TAG, "Work detayı alınamadı: " + workKey, e);
            return OpenLibraryWorkExtras.empty();
        }
    }

    private static final class OpenLibraryWorkExtras {
        @NonNull final String description;
        @NonNull final String subject;

        OpenLibraryWorkExtras(@NonNull String description, @NonNull String subject) {
            this.description = description;
            this.subject = subject;
        }

        static OpenLibraryWorkExtras empty() {
            return new OpenLibraryWorkExtras("", "");
        }
    }

    @NonNull
    private String extractOpenLibraryDescription(@NonNull JSONObject work) {
        Object raw = work.opt("description");
        if (raw instanceof String) {
            return ((String) raw).trim();
        }
        if (raw instanceof JSONObject) {
            return ((JSONObject) raw).optString("value", "").trim();
        }
        return "";
    }

    private String normalizeTurkishCharacters(String text) {
        return text
                .replace("ç", "c")
                .replace("Ç", "C")
                .replace("ğ", "g")
                .replace("Ğ", "G")
                .replace("ı", "i")
                .replace("İ", "I")
                .replace("ö", "o")
                .replace("Ö", "O")
                .replace("ş", "s")
                .replace("Ş", "S")
                .replace("ü", "u")
                .replace("Ü", "U");
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
        kitap.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        kitap.put("kitapAdi", kitapAdi);
        kitap.put("yazar", yazar);
        kitap.put("tur", tur);
        kitap.put("imageUrl", selectedImageUrl);
        kitap.put("createdAt", System.currentTimeMillis());

        if (apiAciklama != null && !apiAciklama.trim().isEmpty()) {
            kitap.put("aciklama", apiAciklama.trim());
        }
        if (apiSayfaSayisi != null && !apiSayfaSayisi.trim().isEmpty()) {
            kitap.put("sayfaSayisi", apiSayfaSayisi.trim());
        }
        if (apiYayinTarihi != null && !apiYayinTarihi.trim().isEmpty()) {
            kitap.put("yayinTarihi", apiYayinTarihi.trim());
        }

        kitaplarRef.push().setValue(kitap)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(KitapEkleActivity.this, R.string.book_added, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                KitapEkleActivity.this,
                                getString(R.string.error_generic, e.getMessage()),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }
}