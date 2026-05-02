package com.example.dijitalraf.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.dijitalraf.R;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Kayıt / giriş için e-posta doğrulama: boşluk, biçim (regex) ve alan adı yapısı.
 */
public final class EmailValidation {

    /** Yaygın ASCII e-posta deseni (RFC 5322 basitleştirilmiş). */
    private static final Pattern EMAIL_SYNTAX = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}$"
    );

    private EmailValidation() {}

    /**
     * @return geçerliyse {@code null}, aksi halde hata için string kaynağı
     */
    @Nullable
    @StringRes
    public static Integer validateForForm(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return R.string.error_email_empty;
        }
        String email = raw.trim();
        if (email.length() > 254) {
            return R.string.error_email_invalid_format;
        }
        if (email.contains(" ") || email.indexOf('@') != email.lastIndexOf('@')) {
            return R.string.error_email_invalid_format;
        }
        if (!EMAIL_SYNTAX.matcher(email).matches()) {
            return R.string.error_email_invalid_format;
        }
        if (!hasPlausibleDomain(email)) {
            return R.string.error_email_invalid_domain;
        }
        return null;
    }

    public static boolean isValid(@Nullable String raw) {
        return validateForForm(raw) == null;
    }

    private static boolean hasPlausibleDomain(@NonNull String email) {
        int at = email.lastIndexOf('@');
        if (at < 1 || at >= email.length() - 1) {
            return false;
        }
        String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
        if (!domain.contains(".")) {
            return false;
        }
        if (domain.startsWith(".") || domain.endsWith(".") || domain.contains("..")) {
            return false;
        }
        String[] labels = domain.split("\\.");
        if (labels.length < 2) {
            return false;
        }
        String tld = labels[labels.length - 1];
        if (tld.length() < 2 || tld.length() > 63) {
            return false;
        }
        if (!tld.matches("[a-zA-Z]+")) {
            return false;
        }
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63) {
                return false;
            }
            if (label.startsWith("-") || label.endsWith("-")) {
                return false;
            }
            if (!label.matches("[a-zA-Z0-9-]+")) {
                return false;
            }
        }
        if ("localhost".equals(domain) || domain.endsWith(".local")) {
            return false;
        }
        return true;
    }
}
