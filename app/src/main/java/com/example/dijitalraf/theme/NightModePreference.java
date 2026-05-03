package com.example.dijitalraf.theme;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Persists UI theme (light or dark only) and applies it via {@link AppCompatDelegate}.
 */
public final class NightModePreference {

    private static final String PREFS = "dijitalraf_app_prefs";
    private static final String KEY_NIGHT_MODE = "night_mode";

    /** Legacy value still read from preferences for one-time migration to {@link #LIGHT}. */
    private static final String LEGACY_FOLLOW_SYSTEM = "follow_system";

    public static final String LIGHT = "light";
    public static final String DARK = "dark";

    private NightModePreference() {}

    @NonNull
    private static String normalizeStored(@Nullable String v) {
        if (v == null || v.isEmpty() || LEGACY_FOLLOW_SYSTEM.equals(v)) {
            return LIGHT;
        }
        if (DARK.equals(v)) {
            return DARK;
        }
        if (LIGHT.equals(v)) {
            return LIGHT;
        }
        return LIGHT;
    }

    @NonNull
    public static String getSavedOrDefault(@NonNull Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return normalizeStored(p.getString(KEY_NIGHT_MODE, null));
    }

    public static void setAndApply(@NonNull Context context, @NonNull String mode) {
        String normalized = DARK.equals(mode) ? DARK : LIGHT;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_NIGHT_MODE, normalized)
                .apply();
        AppCompatDelegate.setDefaultNightMode(toDelegateMode(normalized));
    }

    /** Call from {@link android.app.Application#onCreate()} before activities are shown. */
    public static void applyStoredNightMode(@NonNull Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = p.getString(KEY_NIGHT_MODE, null);
        String mode = normalizeStored(raw);
        if (!mode.equals(raw)) {
            p.edit().putString(KEY_NIGHT_MODE, mode).apply();
        }
        AppCompatDelegate.setDefaultNightMode(toDelegateMode(mode));
    }

    static int toDelegateMode(@NonNull String mode) {
        if (DARK.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_NO;
    }
}
