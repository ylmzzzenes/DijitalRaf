package com.example.dijitalraf.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Kitap bazlı kişisel notları yalnızca cihazda saklar (Firebase ile senkronize edilmez).
 */
public final class BookLocalNotesStore {

    private static final String PREFS_NAME = "dijitalraf_book_personal_notes";

    private BookLocalNotesStore() {
    }

    @NonNull
    private static String storageKey(@NonNull String uid, @NonNull String bookId) {
        return uid + "::" + bookId;
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static String getNote(@NonNull Context context, @NonNull String uid, @NonNull String bookId) {
        String raw = prefs(context).getString(storageKey(uid, bookId), "");
        return raw != null ? raw : "";
    }

    public static void saveNote(@NonNull Context context, @NonNull String uid, @NonNull String bookId,
                                @NonNull String note) {
        prefs(context).edit().putString(storageKey(uid, bookId), note).apply();
    }

    public static void deleteNote(@NonNull Context context, @NonNull String uid, @NonNull String bookId) {
        prefs(context).edit().remove(storageKey(uid, bookId)).apply();
    }
}
