package com.example.dijitalraf.data.repository;

import androidx.annotation.NonNull;
import com.example.dijitalraf.ui.home.Kitap;

import java.util.List;

/**
 * Book shelf data: Realtime Database reads/writes. UI layer talks to {@link com.example.dijitalraf.ui.home.BooksViewModel};
 * the ViewModel delegates here.
 */
public interface BooksRepository {

    interface Listener {
        void onLoading(boolean loading);

        void onBooks(@NonNull List<Kitap> books);

        /** User-visible message; empty list should accompany failures when appropriate. */
        void onError(@NonNull String message);
    }

    void startListening(@NonNull Listener listener);

    void stopListening();

    void persistKitap(@NonNull Kitap kitap);

    void updateBookNote(@NonNull String bookId, @NonNull String note);

    void updateBookYildiz(@NonNull String bookId, int yildiz);

    void updateBookFavorite(@NonNull String bookId, boolean favorite);

    void updateBookOkundu(@NonNull String bookId, boolean okundu);

    void addBookQuote(@NonNull String bookId, @NonNull String text);

    void updateBookQuote(@NonNull String bookId, @NonNull String quoteId, @NonNull String text);

    void deleteBookQuote(@NonNull String bookId, @NonNull String quoteId);
}
