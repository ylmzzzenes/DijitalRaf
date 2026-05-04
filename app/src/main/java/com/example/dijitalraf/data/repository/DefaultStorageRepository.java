package com.example.dijitalraf.data.repository;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.dijitalraf.data.remote.firebase.FirebaseStorageDataSource;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

/**
 * Default Firebase-backed storage repository.
 */
public final class DefaultStorageRepository implements StorageRepository {

    private final FirebaseStorageDataSource dataSource;

    public DefaultStorageRepository(@NonNull FirebaseStorageDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @NonNull
    @Override
    public StorageReference profileAvatarRef(@NonNull String uid) {
        return dataSource.profileAvatarRef(uid);
    }

    @NonNull
    @Override
    public UploadTask uploadProfileAvatar(@NonNull String uid, @NonNull Uri imageUri) {
        return dataSource.uploadProfileAvatar(uid, imageUri);
    }

    @NonNull
    @Override
    public Task<Uri> getDownloadUrl(@NonNull StorageReference reference) {
        return dataSource.getDownloadUrl(reference);
    }
}
