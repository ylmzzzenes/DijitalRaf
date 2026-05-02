package com.example.dijitalraf.ui.home;

import androidx.annotation.Nullable;

/**
 * Kütüphane listesi için arama ve filtre durumu (Activity ViewModel üzerinde tutulur).
 */
public class LibraryFilterSpec {

    /** Başlık veya yazarda hızlı arama (Türkçe büyük/küçük harf duyarsız). */
    public String quickSearch = "";
    public String authorContains = "";
    /** Boş değilse tür tam eşleşmesi (Türkçe). */
    public String genreExact = "";
    @Nullable
    public Boolean readOkundu = null;
    @Nullable
    public Boolean favorite = null;
    /** null: tümü; 1–5: bu değer ve üzeri yıldız (0 yıldızlılar 1+ seçildiğinde elenir). */
    @Nullable
    public Integer minStars = null;
    @Nullable
    public Integer yearExact = null;

    public LibraryFilterSpec copy() {
        LibraryFilterSpec n = new LibraryFilterSpec();
        n.quickSearch = quickSearch;
        n.authorContains = authorContains;
        n.genreExact = genreExact;
        n.readOkundu = readOkundu;
        n.favorite = favorite;
        n.minStars = minStars;
        n.yearExact = yearExact;
        return n;
    }

    public boolean hasActiveFilters() {
        return !quickSearch.trim().isEmpty()
                || !authorContains.trim().isEmpty()
                || !genreExact.trim().isEmpty()
                || readOkundu != null
                || favorite != null
                || minStars != null
                || yearExact != null;
    }
}
