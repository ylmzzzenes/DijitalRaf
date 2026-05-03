package com.example.dijitalraf.data.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.ui.home.Kitap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

    private final String databaseUrl;
    private final ExecutorService parseExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DatabaseReference kitaplarRef;
    private ValueEventListener kitaplarListener;
    private Listener listener;

    public DefaultBooksRepository(@NonNull String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    @Override
    public synchronized void startListening(@NonNull Listener listener) {
        this.listener = listener;
        if (kitaplarListener != null) {
            return;
        }
        listener.onLoading(true);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            listener.onBooks(new ArrayList<>());
            listener.onLoading(false);
            return;
        }
        String uid = currentUser.getUid();
        kitaplarRef = FirebaseDatabase.getInstance(databaseUrl).getReference("books").child(uid);
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return null;
        }
        return FirebaseDatabase.getInstance(databaseUrl)
                .getReference("books")
                .child(user.getUid())
                .child(bookId);
    }

    @Override
    public void persistKitap(@NonNull Kitap kitap) {
        if (kitaplarRef == null || kitap.getId() == null) {
            return;
        }
        kitap.setUpdatedAt(System.currentTimeMillis());
        kitaplarRef.child(kitap.getId()).setValue(kitap);
    }

    @Override
    public void updateBookNote(@NonNull String bookId, @NonNull String note) {
        DatabaseReference ref = bookRefForCurrentUser(bookId);
        if (ref == null) {
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("note", note);
        updates.put("updatedAt", System.currentTimeMillis());
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
        updates.put("yildiz", v);
        updates.put("updatedAt", System.currentTimeMillis());
        ref.updateChildren(updates);
    }

    @Override
    public void updateBookFavorite(@NonNull String bookId, boolean favorite) {
        DatabaseReference ref = bookRefForCurrentUser(bookId);
        if (ref == null) {
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("favorite", favorite);
        updates.put("updatedAt", System.currentTimeMillis());
        ref.updateChildren(updates);
    }

    @Override
    public void updateBookOkundu(@NonNull String bookId, boolean okundu) {
        DatabaseReference ref = bookRefForCurrentUser(bookId);
        if (ref == null) {
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("okundu", okundu);
        updates.put("updatedAt", System.currentTimeMillis());
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
        updates.put("quotes/" + key + "/text", trimmed);
        updates.put("quotes/" + key + "/createdAt", now);
        updates.put("quotes/" + key + "/updatedAt", now);
        updates.put("updatedAt", now);
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
        updates.put("quotes/" + quoteId + "/text", trimmed);
        updates.put("quotes/" + quoteId + "/updatedAt", now);
        updates.put("updatedAt", now);
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
        updates.put("quotes/" + quoteId, null);
        updates.put("updatedAt", now);
        ref.updateChildren(updates);
    }
}
