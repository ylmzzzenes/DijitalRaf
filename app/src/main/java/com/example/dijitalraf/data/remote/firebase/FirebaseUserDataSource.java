package com.example.dijitalraf.data.remote.firebase;

import androidx.annotation.NonNull;

import com.example.dijitalraf.core.constants.DatabasePaths;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

/**
 * Raw Realtime Database access for user profile data.
 */
public final class FirebaseUserDataSource {

    private final FirebaseDatabase database;

    public FirebaseUserDataSource(@NonNull FirebaseDatabase database) {
        this.database = database;
    }

    @NonNull
    public DatabaseReference userRef(@NonNull String uid) {
        return database.getReference(DatabasePaths.USERS).child(uid);
    }

    @NonNull
    public Task<DataSnapshot> getUser(@NonNull String uid) {
        return userRef(uid).get();
    }

    @NonNull
    public Task<Void> setUser(@NonNull String uid, @NonNull Map<String, Object> values) {
        return userRef(uid).setValue(values);
    }

    @NonNull
    public Task<Void> updateUser(@NonNull String uid, @NonNull Map<String, Object> updates) {
        return userRef(uid).updateChildren(updates);
    }
}
