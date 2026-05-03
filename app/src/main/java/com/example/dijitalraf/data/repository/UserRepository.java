package com.example.dijitalraf.data.repository;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;

import java.util.Map;

/**
 * User profile repository boundary.
 */
public interface UserRepository {

    @NonNull
    Task<DataSnapshot> getUser(@NonNull String uid);

    @NonNull
    Task<Void> setUser(@NonNull String uid, @NonNull Map<String, Object> values);

    @NonNull
    Task<Void> updateUser(@NonNull String uid, @NonNull Map<String, Object> updates);
}
