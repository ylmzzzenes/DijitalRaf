package com.example.dijitalraf.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Raf kitap listesinden (cihazda tutulan / Belleğe yüklenen) istatistikleri türetir.
 */
public final class BookStatisticsCalculator {

    private BookStatisticsCalculator() {
    }

    public static final class GenreStat implements Comparable<GenreStat> {
        @NonNull
        public final String label;
        public final int countRead;

        public GenreStat(@NonNull String label, int countRead) {
            this.label = label;
            this.countRead = countRead;
        }

        @Override
        public int compareTo(GenreStat o) {
            int c = Integer.compare(o.countRead, countRead);
            if (c != 0) {
                return c;
            }
            return label.compareToIgnoreCase(o.label);
        }
    }

    public static final class Snapshot {
        public int totalBooks;
        public int readBooks;
        public int favoriteBooks;
        public int ratedCount;
        public int starsSum;
        public double averageStars;
        @NonNull
        public final List<GenreStat> topReadGenres = new ArrayList<>();
    }

    @NonNull
    public static Snapshot compute(@NonNull List<Kitap> books) {
        Snapshot s = new Snapshot();
        Map<String, Integer> genreRead = new HashMap<>();
        for (Kitap k : books) {
            if (k == null || k.getId() == null) {
                continue;
            }
            s.totalBooks++;
            if (k.isOkundu()) {
                s.readBooks++;
                String genre = normalizeGenreLabel(k.getTur());
                genreRead.merge(genre, 1, Integer::sum);
            }
            if (k.isFavorite()) {
                s.favoriteBooks++;
            }
            int stars = k.getYildiz();
            if (stars > 0) {
                s.ratedCount++;
                s.starsSum += stars;
            }
        }
        if (s.ratedCount > 0) {
            s.averageStars = s.starsSum / (double) s.ratedCount;
        } else {
            s.averageStars = 0d;
        }
        for (Map.Entry<String, Integer> e : genreRead.entrySet()) {
            s.topReadGenres.add(new GenreStat(e.getKey(), e.getValue()));
        }
        Collections.sort(s.topReadGenres);
        int max = 5;
        while (s.topReadGenres.size() > max) {
            s.topReadGenres.remove(s.topReadGenres.size() - 1);
        }
        return s;
    }

    @NonNull
    public static String formatAverage(double avg, int ratedCount) {
        if (ratedCount <= 0) {
            return "—";
        }
        return String.format(Locale.getDefault(), "%.1f", avg);
    }

    @NonNull
    private static String normalizeGenreLabel(@Nullable String tur) {
        if (tur == null || tur.trim().isEmpty()) {
            return "";
        }
        return tur.trim();
    }
}
