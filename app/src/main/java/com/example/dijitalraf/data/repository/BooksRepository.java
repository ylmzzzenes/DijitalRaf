package com.example.dijitalraf.data.repository;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.example.dijitalraf.core.utils.ListenerRegistration;
import com.example.dijitalraf.data.model.BookQuote;
import com.example.dijitalraf.ui.home.Kitap;

import java.util.List;
import java.util.Map;

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

    interface BookListener {
        void onBook(@NonNull Kitap kitap);

        void onNotFound();

        void onError(@NonNull String message);
    }

    interface QuotesListener {
        void onQuotes(@NonNull List<BookQuote> quotes);

        void onError(@NonNull String message);
    }

    void startListening(@NonNull Listener listener);

    void stopListening();

    @NonNull
    ListenerRegistration observeBook(@NonNull String bookId, @NonNull BookListener listener);

    @NonNull
    ListenerRegistration observeBookQuotes(@NonNull String bookId, @NonNull QuotesListener listener);

    void persistKitap(@NonNull Kitap kitap);

    @NonNull
    Task<Void> addBook(@NonNull Map<String, Object> values);

    @NonNull
    Task<Void> deleteBook(@NonNull String bookId);

    @NonNull
    Task<Void> restoreBook(@NonNull String bookId, @NonNull Kitap kitap);

    void updateBookNote(@NonNull String bookId, @NonNull String note);

    void updateBookYildiz(@NonNull String bookId, int yildiz);

    void updateBookFavorite(@NonNull String bookId, boolean favorite);

    void updateBookOkundu(@NonNull String bookId, boolean okundu);

    void addBookQuote(@NonNull String bookId, @NonNull String text);

    void updateBookQuote(@NonNull String bookId, @NonNull String quoteId, @NonNull String text);

    void deleteBookQuote(@NonNull String bookId, @NonNull String quoteId);
}
