package com.example.dijitalraf.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Splits a display/full name into first and last name fields for storage.
 * <p>
 * Heuristic: normalize Unicode whitespace, then if there are three or more tokens,
 * treat the <em>last</em> token as the family name and everything before it as given name(s).
 * This matches many Turkish and European forms ("Mehmet Ali Yılmaz" → "Mehmet Ali", "Yılmaz").
 * Single-token names use the full string as first name only. Two tokens split as first / last.
 * </p>
 */
public final class ProfileNameSplitter {

    private ProfileNameSplitter() {}

    /**
     * @return a two-element array: {@code [0]} first name, {@code [1]} last name (never null entries)
     */
    @NonNull
    public static String[] splitForStorage(@Nullable String fullName) {
        String[] out = new String[] {"", ""};
        if (fullName == null) {
            return out;
        }
        String trimmed = fullName.trim();
        if (trimmed.isEmpty()) {
            return out;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 1) {
            out[0] = parts[0];
            return out;
        }
        if (parts.length == 2) {
            out[0] = parts[0];
            out[1] = parts[1];
            return out;
        }
        StringBuilder first = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                first.append(' ');
            }
            first.append(parts[i]);
        }
        out[0] = first.toString();
        out[1] = parts[parts.length - 1];
        return out;
    }
}
