package com.example.dijitalraf.domain.usecase;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.ui.home.Kitap;
import com.example.dijitalraf.ui.home.LibraryFilterSpec;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure book filtering rules used by library-like screens.
 */
public final class BookFilterUseCase {

    private static final Pattern YEAR_PATTERN = Pattern.compile("(19|20)\\d{2}");

    private BookFilterUseCase() {
    }

    @NonNull
    private static Locale searchLocale() {
        if ("tr".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
            return Locale.forLanguageTag("tr-TR");
        }
        return Locale.getDefault();
    }

    @NonNull
    private static String normTr(@Nullable String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase(searchLocale()).trim();
    }

    public static boolean matchesQuickSearch(@NonNull Kitap k, @NonNull String queryRaw) {
        String q = normTr(queryRaw);
        if (q.isEmpty()) {
            return true;
        }
        String title = normTr(k.getKitapAdi());
        String author = normTr(k.getYazar());
        return title.contains(q) || author.contains(q);
    }

    public static boolean matches(@NonNull Kitap k, @NonNull LibraryFilterSpec s) {
        if (!matchesQuickSearch(k, s.quickSearch)) {
            return false;
        }
        String ap = normTr(s.authorContains);
        if (!ap.isEmpty() && !normTr(k.getYazar()).contains(ap)) {
            return false;
        }
        String ge = normTr(s.genreExact);
        if (!ge.isEmpty() && !normTr(k.getTur()).equals(ge)) {
            return false;
        }
        if (s.readOkundu != null && k.isOkundu() != s.readOkundu) {
            return false;
        }
        if (s.favorite != null && k.isFavorite() != s.favorite) {
            return false;
        }
        if (s.minStars != null) {
            int stars = k.getYildiz();
            if (stars < s.minStars) {
                return false;
            }
        }
        if (s.yearExact != null) {
            Integer y = extractPublicationYear(k.getYayinTarihi());
            if (y == null || !y.equals(s.yearExact)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public static Integer extractPublicationYear(@Nullable String yayinTarihi) {
        if (TextUtils.isEmpty(yayinTarihi)) {
            return null;
        }
        Matcher m = YEAR_PATTERN.matcher(yayinTarihi.trim());
        if (m.find()) {
            try {
                return Integer.parseInt(m.group());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
