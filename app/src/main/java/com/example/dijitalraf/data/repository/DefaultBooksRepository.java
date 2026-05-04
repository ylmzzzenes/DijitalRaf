package com.example.dijitalraf.data.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.core.constants.DatabasePaths;
import com.example.dijitalraf.core.utils.ListenerRegistration;
import com.example.dijitalraf.data.model.BookQuote;
import com.example.dijitalraf.data.remote.firebase.FirebaseBooksDataSource;
import com.example.dijitalraf.ui.home.Kitap;
import com.example.dijitalraf.ui.home.KitapAlinti;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Firebase Realtime Database implementation of {@link BooksRepository}. Snapshot parsing runs
 * off the main thread to keep UI responsive for large libraries.
 */
public final class DefaultBooksRepository implements BooksRepository {

    private final FirebaseBooksDataSource booksDataSource;
    private final ExecutorService parseExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DatabaseReference kitaplarRef;
    private ValueEventListener kitaplarListener;
    private Listener listener;

    public DefaultBooksRepository(@NonNull FirebaseBooksDataSource booksDataSource) {
        this.booksDataSource = booksDataSource;
    }

    @Override
    public synchronized void startListening(@NonNull Listener listener) {
        this.listener = listener;
        if (kitaplarListener != null) {
            return;
        }
        listener.onLoading(true);
        FirebaseUser currentUser = booksDataSource.getCurrentUser();
        if (currentUser == null) {
            listener.onBooks(new ArrayList<>());
            listener.onLoading(false);
            return;
        }
        kitaplarRef = booksDataSource.currentUserBooksRef();
        if (kitaplarRef == null) {
            listener.onBooks(new ArrayList<>());
            listener.onLoading(false);
            return;
        }
        kitaplarListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final Listener l = DefaultBooksRepository.this.listener;
                if (l == null) {
                    return;
                }
                parseExecutor.execute(() -> {
                    List<Kitap> list = parseSnapshot(snapshot);
                    mainHandler.post(() -> {
                        Listener cb = DefaultBooksRepository.this.listener;
                        if (cb != null) {
                            cb.onBooks(list);
                            cb.onLoading(false);
                        }
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mainHandler.post(() -> {
                    Listener cb = DefaultBooksRepository.this.listener;
                    if (cb != null) {
                        cb.onError(error.getMessage());
                        cb.onBooks(new ArrayList<>());
                        cb.onLoading(false);
                    }
                });
            }
        };
        kitaplarRef.addValueEventListener(kitaplarListener);
    }

    @NonNull
    private static List<Kitap> parseSnapshot(@NonNull DataSnapshot snapshot) {
        List<Kitap> list = new ArrayList<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            Kitap kitap = child.getValue(Kitap.class);
            if (kitap != null) {
                kitap.setId(child.getKey());
                list.add(kitap);
            }
        }
        return list;
    }

    @Override
    public synchronized void stopListening() {
        if (kitaplarRef != null && kitaplarListener != null) {
            kitaplarRef.removeEventListener(kitaplarListener);
        }
        kitaplarListener = null;
        kitaplarRef = null;
        listener = null;
        parseExecutor.shutdown();
    }

    @Nullable
    private DatabaseReference bookRefForCurrentUser(@NonNull String bookId) {
        return booksDataSource.currentUserBookRef(bookId);
    }

    @NonNull
    @Override
    public ListenerRegistration observeBook(@NonNull String bookId, @NonNull BookListener listener) {
        DatabaseReference ref = bookRefForCurrentUser(bookId);
        if (ref == null) {
            listener.onError("Book reference not available");
            return () -> { };
        }
        ValueEventListener valueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    listener.onNotFound();
                    return;
                }
                Kitap kitap = snapshot.getValue(Kitap.class);
                if (kitap == null) {
                    listener.onError("Book detail could not be loaded");
                    return;
                }
                kitap.setId(bookId);
                listener.onBook(kitap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        };
        ref.addValueEventListener(valueListener);
        return () -> ref.removeEventListener(valueListener);
    }

    @NonNull
    @Override
    public ListenerRegistration observeBookQuotes(@NonNull String bookId, @NonNull QuotesListener listener) {
        DatabaseReference bookRef = bookRefForCurrentUser(bookId);
        if (bookRef == null) {
            listener.onError("Book reference not available");
            return () -> { };
        }
        DatabaseReference quotesRef = bookRef.child(DatabasePaths.QUOTES);
        ValueEventListener valueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<BookQuote> rows = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    KitapAlinti quote = child.getValue(KitapAlinti.class);
                    String key = child.getKey();
                    if (quote == null || key == null || quote.getText() == null) {
                        continue;
                    }
                    String text = quote.getText().trim();
                    if (!text.isEmpty()) {
                        rows.add(new BookQuote(key, text, quote.getCreatedAt()));
                    }
                }
                rows.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
                listener.onQuotes(rows);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        };
        quotesRef.addValueEventListener(valueListener);
        return () -> quotesRef.removeEventListener(valueListener);
    }

    @Override
    public void persistKitap(@NonNull Kitap kitap) {
        if (kitaplarRef == null || kitap.getId() == null) {
            return;
        }
        kitap.setUpdatedAt(System.currentTimeMillis());
        kitaplarRef.child(kitap.getId()).setValue(kitap);
    }

    @NonNull
    @Override
    public Task<Void> addBook(@NonNull Map<String, Object> values) {
        return booksDataSource.addBook(values);
    }

    @NonNull
    @Override
    public Task<Void> deleteBook(@NonNull String bookId) {
        return booksDataSource.removeBook(bookId);
    }

    @NonNull
    @Override
    public Task<Void> restoreBook(@NonNull String bookId, @NonNull Kitap kitap) {
        return booksDataSource.setBook(bookId, kitap);
    }

    @Override
    public void updateBookNote(@NonNull String bookId, @NonNull String note) {
        DatabaseReference ref = bookRefForCurrentUser(bookId);
        if (ref == null) {
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put(DatabasePaths.FIELD_BOOK_NOTE, note);
        updates.put(DatabasePaths.FIELD_UPDATED_AT, System.currentTimeMillis());
        ref.updateChildren(updates);
    }

    @Override
    public void updateBookYildiz(@NonNull String bookId, int yildiz) {
        DatabaseReference ref = bookRefForCurrentUser(bookId);
        if (ref == null) {
            return;
        }
        int v = Math.max(0, Math.min(5, yildiz));
        Map<String, Object> updates = new HashMap<>();
        updates.put(DatabasePaths.FIELD_BOOK_STARS, v);
        updates.put(DatabasePaths.FIELD_UPDATED_AT, System.currentTimeMillis());
        ref.updateChildren(updates);
    }

    @Override
    public void updateBookFavorite(@NonNull String bookId, boolean favorite) {
        DatabaseReference ref = bookRefForCurrentUser(bookId);
        if (ref == null) {
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put(DatabasePaths.FIELD_BOOK_FAVORITE, favorite);
        updates.put(DatabasePaths.FIELD_UPDATED_AT, System.currentTimeMillis());
        ref.updateChildren(updates);
    }

    @Override
    public void updateBookOkundu(@NonNull String bookId, boolean okundu) {
        DatabaseReference ref = bookRefForCurrentUser(bookId);
        if (ref == null) {
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put(DatabasePaths.FIELD_BOOK_READ, okundu);
        updates.put(DatabasePaths.FIELD_UPDATED_AT, System.currentTimeMillis());
        ref.updateChildren(updates);
    }

    @Override
    public void addBookQuote(@NonNull String bookId, @NonNull String text) {
        DatabaseReference ref = bookRefForCurrentUser(bookId);
        if (ref == null) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        String key = ref.child("quotes").push().getKey();
        if (key == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Object> updates = new HashMap<>();
        updates.put(DatabasePaths.QUOTES + "/" + key + "/" + DatabasePaths.FIELD_QUOTE_TEXT, trimmed);
        updates.put(DatabasePaths.QUOTES + "/" + key + "/" + DatabasePaths.FIELD_CREATED_AT, now);
        updates.put(DatabasePaths.QUOTES + "/" + key + "/" + DatabasePaths.FIELD_UPDATED_AT, now);
        updates.put(DatabasePaths.FIELD_UPDATED_AT, now);
        ref.updateChildren(updates);
    }

    @Override
    public void updateBookQuote(@NonNull String bookId, @NonNull String quoteId, @NonNull String text) {
        DatabaseReference ref = bookRefForCurrentUser(bookId);
        if (ref == null) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Object> updates = new HashMap<>();
        updates.put(DatabasePaths.QUOTES + "/" + quoteId + "/" + DatabasePaths.FIELD_QUOTE_TEXT, trimmed);
        updates.put(DatabasePaths.QUOTES + "/" + quoteId + "/" + DatabasePaths.FIELD_UPDATED_AT, now);
        updates.put(DatabasePaths.FIELD_UPDATED_AT, now);
        ref.updateChildren(updates);
    }

    @Override
    public void deleteBookQuote(@NonNull String bookId, @NonNull String quoteId) {
        DatabaseReference ref = bookRefForCurrentUser(bookId);
        if (ref == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Object> updates = new HashMap<>();
        updates.put(DatabasePaths.QUOTES + "/" + quoteId, null);
        updates.put(DatabasePaths.FIELD_UPDATED_AT, now);
        ref.updateChildren(updates);
    }
}
