package com.example.dijitalraf.theme;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Persists UI theme (follow system / light / dark) and applies it via {@link AppCompatDelegate}.
 */
public final class NightModePreference {

    private static final String PREFS = "dijitalraf_app_prefs";
    private static final String KEY_NIGHT_MODE = "night_mode";

    public static final String FOLLOW_SYSTEM = "follow_system";
    public static final String LIGHT = "light";
    public static final String DARK = "dark";

    private NightModePreference() {}

    @NonNull
    public static String getSavedOrDefault(@NonNull Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String v = p.getString(KEY_NIGHT_MODE, null);
        if (v == null || v.isEmpty()) {
            return FOLLOW_SYSTEM;
        }
        return v;
    }

    public static void setAndApply(@NonNull Context context, @NonNull String mode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_NIGHT_MODE, mode)
                .apply();
        AppCompatDelegate.setDefaultNightMode(toDelegateMode(mode));
    }

    /** Call from {@link android.app.Application#onCreate()} before activities are shown. */
    public static void applyStoredNightMode(@NonNull Context context) {
        AppCompatDelegate.setDefaultNightMode(toDelegateMode(getSavedOrDefault(context)));
    }

    static int toDelegateMode(@NonNull String mode) {
        if (LIGHT.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (DARK.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
