package com.example.dijitalraf.data.remote.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.core.constants.DatabasePaths;
import com.example.dijitalraf.ui.home.Kitap;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

/**
 * Raw Firebase Realtime Database access for book data.
 */
public final class FirebaseBooksDataSource {

    private final FirebaseAuth auth;
    private final FirebaseDatabase database;

    public FirebaseBooksDataSource(@NonNull FirebaseAuth auth, @NonNull FirebaseDatabase database) {
        this.auth = auth;
        this.database = database;
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    @Nullable
    public DatabaseReference currentUserBooksRef() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return null;
        }
        return database.getReference(DatabasePaths.BOOKS).child(user.getUid());
    }

    @Nullable
    public DatabaseReference currentUserBookRef(@NonNull String bookId) {
        DatabaseReference booksRef = currentUserBooksRef();
        if (booksRef == null || bookId.trim().isEmpty()) {
            return null;
        }
        return booksRef.child(bookId);
    }

    @NonNull
    public Task<Void> addBook(@NonNull Map<String, Object> values) {
        DatabaseReference booksRef = currentUserBooksRef();
        if (booksRef == null) {
            return Tasks.forException(new IllegalStateException("User session not found"));
        }
        return booksRef.push().setValue(values);
    }

    @NonNull
    public Task<Void> setBook(@NonNull String bookId, @NonNull Kitap kitap) {
        DatabaseReference ref = currentUserBookRef(bookId);
        if (ref == null) {
            return Tasks.forException(new IllegalStateException("Book reference not available"));
        }
        return ref.setValue(kitap);
    }

    @NonNull
    public Task<Void> removeBook(@NonNull String bookId) {
        DatabaseReference ref = currentUserBookRef(bookId);
        if (ref == null) {
            return Tasks.forException(new IllegalStateException("Book reference not available"));
        }
        return ref.removeValue();
    }
}
