package com.example.dijitalraf.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class BooksViewModel extends ViewModel {

    private static final String DATABASE_URL =
            "https://dijitalraf-ec149-default-rtdb.europe-west1.firebasedatabase.app";

    private final MutableLiveData<List<Kitap>> books = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);

    private DatabaseReference kitaplarRef;
    private ValueEventListener kitaplarListener;

    public LiveData<List<Kitap>> getBooks() {
        return books;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void persistKitap(@NonNull Kitap kitap) {
        if (kitaplarRef == null || kitap.getId() == null) {
            return;
        }
        kitap.setUpdatedAt(System.currentTimeMillis());
        kitaplarRef.child(kitap.getId()).setValue(kitap);
    }

    @Nullable
    private DatabaseReference bookRefForCurrentUser(@NonNull String bookId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return null;
        }
        return FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("books")
                .child(user.getUid())
                .child(bookId);
    }

    /** Kişisel notu Realtime Database'e yazar (tüm cihazlarda güncellenir). */
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

    /** Yıldız puanını (0–5) Realtime Database'e yazar. */
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

    /** Alıntı ekler; yol {@code books/{uid}/{bookId}/quotes/{pushId}}. */
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

    public void startListening() {
        if (kitaplarListener != null) {
            return;
        }
        loading.setValue(true);
        FirebaseUser currentUser =FirebaseAuth.getInstance().getCurrentUser();

        if(currentUser == null)
        {
            books.setValue(new ArrayList<>());
            loading.setValue(false);
            return;

        }

        String uid = currentUser.getUid();

        kitaplarRef = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference("books")
                .child(uid);

        kitaplarListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Kitap> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Kitap kitap = child.getValue(Kitap.class);
                    if (kitap != null) {
                        kitap.setId(child.getKey());
                        list.add(kitap);
                    }
                }
                books.setValue(list);
                loading.setValue(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                books.setValue(new ArrayList<>());
                loading.setValue(false);
            }
        };
        kitaplarRef.addValueEventListener(kitaplarListener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (kitaplarRef != null && kitaplarListener != null) {
            kitaplarRef.removeEventListener(kitaplarListener);
        }
    }
}
