package com.example.dijitalraf.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Kitap detayındaki kullanıcı etkileşimlerini (not, puan, favori, okundu) cihazda tek kayıt
 * altında saklar; kitap kimliği başına en fazla bir kayıt vardır.
 */
public final class BookDetailUserActionsStore {

    private static final String PREFS_NAME = "book_detail_user_actions";

    private static final String JSON_NOTE = "note";
    private static final String JSON_STARS = "stars";
    private static final String JSON_FAVORITE = "favorite";
    private static final String JSON_READ = "read";

    private BookDetailUserActionsStore() {
    }

    @NonNull
    private static String prefsKey(@NonNull String bookId) {
        String safe = bookId.trim().replace('.', '_').replace('#', '_').replace('$', '_');
        return "book_" + safe;
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Aynı {@code bookId} için mevcut kaydı tamamen değiştirir (duplicate oluşmaz).
     */
    public static void save(@NonNull Context context, @NonNull String bookId, @NonNull Snapshot snapshot) {
        if (TextUtils.isEmpty(bookId)) {
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put(JSON_NOTE, snapshot.note != null ? snapshot.note : "");
            o.put(JSON_STARS, Math.max(0, Math.min(5, snapshot.stars)));
            o.put(JSON_FAVORITE, snapshot.favorite);
            o.put(JSON_READ, snapshot.read);
            prefs(context).edit().putString(prefsKey(bookId), o.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    @Nullable
    public static Snapshot load(@NonNull Context context, @NonNull String bookId) {
        if (TextUtils.isEmpty(bookId)) {
            return null;
        }
        String raw = prefs(context).getString(prefsKey(bookId), null);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(raw);
            Snapshot s = new Snapshot();
            s.note = o.optString(JSON_NOTE, "");
            s.stars = Math.max(0, Math.min(5, o.optInt(JSON_STARS, 0)));
            s.favorite = o.optBoolean(JSON_FAVORITE, false);
            s.read = o.optBoolean(JSON_READ, false);
            return s;
        } catch (JSONException e) {
            return null;
        }
    }

    public static final class Snapshot {
        @NonNull
        public String note = "";
        public int stars;
        public boolean favorite;
        public boolean read;
    }
}
