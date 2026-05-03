package com.example.dijitalraf.locale;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Persists UI language (tr / en / de) and applies it via AppCompat per-app locales.
 */
public final class LanguagePreference {

    private static final String PREFS = "dijitalraf_app_prefs";
    private static final String KEY_LANGUAGE = "language_tag";

    public static final String TAG_TURKISH = "tr";
    public static final String TAG_ENGLISH = "en";
    public static final String TAG_GERMAN = "de";

    private LanguagePreference() {}

    @NonNull
    public static String getSavedOrDefault(@NonNull Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String v = p.getString(KEY_LANGUAGE, null);
        if (v == null || v.isEmpty()) {
            return TAG_TURKISH;
        }
        return v;
    }

    public static void setAndApply(@NonNull Context context, @NonNull String languageTag) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, languageTag)
                .apply();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
    }

    /** Call from {@link android.app.Application#onCreate()} before any UI. */
    public static void applyStoredLocale(@NonNull Context context) {
        String tag = getSavedOrDefault(context);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
    }
}
