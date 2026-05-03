package com.example.dijitalraf.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.dijitalraf.data.FirebaseRtdb;
import com.example.dijitalraf.data.repository.BooksRepository;
import com.example.dijitalraf.data.repository.DefaultBooksRepository;
import com.example.dijitalraf.ui.util.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Presentation logic for the signed-in user's book shelf. Delegates I/O to {@link BooksRepository}
 * and exposes {@link LiveData} for UI.
 */
public class BooksViewModel extends AndroidViewModel {

    private final BooksRepository repository;

    private final MutableLiveData<List<Kitap>> books = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Event<String>> booksError = new MutableLiveData<>();

    public BooksViewModel(@NonNull Application application) {
        super(application);
        this.repository = new DefaultBooksRepository(FirebaseRtdb.URL);
    }

    public LiveData<List<Kitap>> getBooks() {
        return books;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /** One-shot sync failures (e.g. permission / network). Observe with {@link Event#getContentIfNotHandled()}. */
    public LiveData<Event<String>> getBooksError() {
        return booksError;
    }

    public void persistKitap(@NonNull Kitap kitap) {
        repository.persistKitap(kitap);
    }

    public void updateBookNote(@NonNull String bookId, @NonNull String note) {
        repository.updateBookNote(bookId, note);
    }

    public void updateBookYildiz(@NonNull String bookId, int yildiz) {
        repository.updateBookYildiz(bookId, yildiz);
    }

    public void updateBookFavorite(@NonNull String bookId, boolean favorite) {
        repository.updateBookFavorite(bookId, favorite);
    }

    public void updateBookOkundu(@NonNull String bookId, boolean okundu) {
        repository.updateBookOkundu(bookId, okundu);
    }

    public void addBookQuote(@NonNull String bookId, @NonNull String text) {
        repository.addBookQuote(bookId, text);
    }

    public void updateBookQuote(@NonNull String bookId, @NonNull String quoteId, @NonNull String text) {
        repository.updateBookQuote(bookId, quoteId, text);
    }

    public void deleteBookQuote(@NonNull String bookId, @NonNull String quoteId) {
        repository.deleteBookQuote(bookId, quoteId);
    }

    public void startListening() {
        repository.startListening(new BooksRepository.Listener() {
            @Override
            public void onLoading(boolean value) {
                loading.postValue(value);
            }

            @Override
            public void onBooks(@NonNull List<Kitap> list) {
                books.postValue(list);
            }

            @Override
            public void onError(@NonNull String message) {
                booksError.postValue(new Event<>(message));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.stopListening();
    }
}
