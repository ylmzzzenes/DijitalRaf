package com.example.dijitalraf.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class FavoritesHelper {

    private static final String PREFS = "dijital_raf_prefs";
    private static final String KEY_FAVORITES = "favorite_book_ids";

    private FavoritesHelper() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Set<String> getFavoriteIds(Context context) {
        Set<String> stored = prefs(context).getStringSet(KEY_FAVORITES, null);
        if (stored == null) {
            return new HashSet<>();
        }
        return new HashSet<>(stored);
    }

    public static boolean isFavorite(Context context, String bookId) {
        if (bookId == null) {
            return false;
        }
        return getFavoriteIds(context).contains(bookId);
    }

    public static void add(Context context, String bookId) {
        if (bookId == null) {
            return;
        }
        Set<String> next = getFavoriteIds(context);
        next.add(bookId);
        prefs(context).edit().putStringSet(KEY_FAVORITES, next).apply();
    }

    public static void remove(Context context, String bookId) {
        if (bookId == null) {
            return;
        }
        Set<String> next = getFavoriteIds(context);
        next.remove(bookId);
        prefs(context).edit().putStringSet(KEY_FAVORITES, next).apply();
    }

    public static void toggle(Context context, String bookId) {
        if (isFavorite(context, bookId)) {
            remove(context, bookId);
        } else {
            add(context, bookId);
        }
    }
}
