package com.example.dijitalraf.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.domain.usecase.BookFilterUseCase;

public final class BookFilterUtils {

    private BookFilterUtils() {
    }

    public static boolean matchesQuickSearch(@NonNull Kitap k, @NonNull String queryRaw) {
        return BookFilterUseCase.matchesQuickSearch(k, queryRaw);
    }

    public static boolean matches(@NonNull Kitap k, @NonNull LibraryFilterSpec s) {
        return BookFilterUseCase.matches(k, s);
    }

    @Nullable
    public static Integer extractPublicationYear(@Nullable String yayinTarihi) {
        return BookFilterUseCase.extractPublicationYear(yayinTarihi);
    }
}
