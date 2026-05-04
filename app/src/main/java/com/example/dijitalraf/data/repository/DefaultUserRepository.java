package com.example.dijitalraf.data.repository;

import androidx.annotation.NonNull;

import com.example.dijitalraf.data.remote.firebase.FirebaseUserDataSource;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;

import java.util.Map;

/**
 * Default Firebase-backed user profile repository.
 */
public final class DefaultUserRepository implements UserRepository {

    private final FirebaseUserDataSource dataSource;

    public DefaultUserRepository(@NonNull FirebaseUserDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @NonNull
    @Override
    public Task<DataSnapshot> getUser(@NonNull String uid) {
        return dataSource.getUser(uid);
    }

    @NonNull
    @Override
    public Task<Void> setUser(@NonNull String uid, @NonNull Map<String, Object> values) {
        return dataSource.setUser(uid, values);
    }

    @NonNull
    @Override
    public Task<Void> updateUser(@NonNull String uid, @NonNull Map<String, Object> updates) {
        return dataSource.updateUser(uid, updates);
    }
}
