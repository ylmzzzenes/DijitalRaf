package com.example.dijitalraf.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.data.model.BookMetadata;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Book metadata repository backed by Open Library lookup endpoints.
 */
public final class DefaultOpenLibraryRepository implements OpenLibraryRepository {

    private static final String TAG = "OpenLibraryRepository";
    private static final String OPEN_LIBRARY_SEARCH = "https://openlibrary.org/search.json";
    private static final String OPEN_LIBRARY_ROOT = "https://openlibrary.org";
    private static final String USER_AGENT = "DijitalRaf/1.0 (Android)";

    private final OkHttpClient client;

    public DefaultOpenLibraryRepository(@NonNull OkHttpClient client) {
        this.client = client;
    }

    @Override
    public void searchBooks(@NonNull String query, @NonNull OpenLibraryRepository.Callback callback) {
        Request request = buildOpenLibrarySearchRequest(query);
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                JSONObject doc;
                try (Response r = response) {
                    if (!r.isSuccessful() || r.body() == null) {
                        callback.onNotFound();
                        return;
                    }

                    JSONObject root = new JSONObject(r.body().string());
                    doc = pickBestOpenLibraryDoc(root.optJSONArray("docs"));

                    if (doc == null) {
                        String normalized = normalizeTurkishCharacters(query);
                        if (!normalized.equals(query)) {
                            doc = searchNormalizedSync(normalized);
                        }
                    }

                    if (doc == null) {
                        callback.onNotFound();
                        return;
                    }

                    callback.onSuccess(toMetadata(doc));
                } catch (Exception e) {
                    Log.e(TAG, "Book metadata parse/search failed", e);
                    callback.onError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                }
            }
        });
    }

    @Nullable
    private JSONObject searchNormalizedSync(@NonNull String normalized) {
        try (Response response = client.newCall(buildOpenLibrarySearchRequest(normalized)).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JSONObject root = new JSONObject(response.body().string());
                return pickBestOpenLibraryDoc(root.optJSONArray("docs"));
            }
        } catch (Exception e) {
            Log.w(TAG, "Normalized Open Library search failed", e);
        }
        return null;
    }

    @NonNull
    private BookMetadata toMetadata(@NonNull JSONObject doc) {
        String title = buildOpenLibraryTitle(doc);
        String author = firstOpenLibraryAuthor(doc);
        String category = primarySubjectFromOpenLibraryDoc(doc);
        String coverUrl = coverUrlFromOpenLibraryDoc(doc);
        int pages = doc.optInt("number_of_pages_median", 0);
        String pagesStr = pages > 0 ? String.valueOf(pages) : "";
        String published = publishYearFromOpenLibraryDoc(doc);

        OpenLibraryWorkExtras extras = OpenLibraryWorkExtras.empty();
        String workKey = doc.optString("key", "");
        if (!workKey.isEmpty()) {
            extras = fetchOpenLibraryWorkExtrasSync(workKey);
        }
        if (category.isEmpty() && !extras.subject.isEmpty()) {
            category = extras.subject;
        }

        return new BookMetadata(title, author, category, coverUrl, extras.description, pagesStr, published);
    }

    @NonNull
    private static Request buildOpenLibrarySearchRequest(@NonNull String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = OPEN_LIBRARY_SEARCH + "?q=" + encoded + "&limit=15";
        return new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();
    }

    @Nullable
    private static JSONObject pickBestOpenLibraryDoc(@Nullable JSONArray docs) {
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
    private static String buildOpenLibraryTitle(@NonNull JSONObject doc) {
        String t = doc.optString("title", "").trim();
        String sub = doc.optString("subtitle", "").trim();
        if (!sub.isEmpty()) {
            t = t.isEmpty() ? sub : t + ": " + sub;
        }
        return t;
    }

    @NonNull
    private static String firstOpenLibraryAuthor(@NonNull JSONObject doc) {
        JSONArray a = doc.optJSONArray("author_name");
        if (a != null && a.length() > 0) {
            return a.optString(0, "").trim();
        }
        return "";
    }

    @NonNull
    private static String primarySubjectFromOpenLibraryDoc(@NonNull JSONObject doc) {
        String s = firstStringFromJsonArray(doc, "subject");
        if (!s.isEmpty()) {
            return s;
        }
        return firstStringFromJsonArray(doc, "subject_facet");
    }

    private static boolean docHasAnySubjectHint(@NonNull JSONObject doc) {
        JSONArray a = doc.optJSONArray("subject");
        if (a != null && a.length() > 0) {
            return true;
        }
        JSONArray b = doc.optJSONArray("subject_facet");
        return b != null && b.length() > 0;
    }

    @NonNull
    private static String firstStringFromJsonArray(@NonNull JSONObject doc, @NonNull String key) {
        JSONArray arr = doc.optJSONArray(key);
        if (arr != null && arr.length() > 0) {
            return arr.optString(0, "").trim();
        }
        return "";
    }

    private static boolean hasOpenLibraryIsbn(@NonNull JSONObject doc) {
        JSONArray isbn = doc.optJSONArray("isbn");
        return isbn != null && isbn.length() > 0 && !isbn.optString(0, "").trim().isEmpty();
    }

    @NonNull
    private static String coverUrlFromOpenLibraryDoc(@NonNull JSONObject doc) {
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
    private static String publishYearFromOpenLibraryDoc(@NonNull JSONObject doc) {
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

    @NonNull
    private OpenLibraryWorkExtras fetchOpenLibraryWorkExtrasSync(@NonNull String workKey) {
        if (!workKey.startsWith("/works/")) {
            return OpenLibraryWorkExtras.empty();
        }
        String url = OPEN_LIBRARY_ROOT + workKey + ".json";
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
            Log.w(TAG, "Open Library work details failed: " + workKey, e);
            return OpenLibraryWorkExtras.empty();
        }
    }

    @NonNull
    private static String extractOpenLibraryDescription(@NonNull JSONObject work) {
        Object raw = work.opt("description");
        if (raw instanceof String) {
            return ((String) raw).trim();
        }
        if (raw instanceof JSONObject) {
            return ((JSONObject) raw).optString("value", "").trim();
        }
        return "";
    }

    @NonNull
    private static String normalizeTurkishCharacters(@NonNull String text) {
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
}
