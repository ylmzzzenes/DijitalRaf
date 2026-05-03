package com.example.dijitalraf.data.repository;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

/**
 * Storage repository boundary.
 */
public interface StorageRepository {

    @NonNull
    StorageReference profileAvatarRef(@NonNull String uid);

    @NonNull
    UploadTask uploadProfileAvatar(@NonNull String uid, @NonNull Uri imageUri);

    @NonNull
    Task<Uri> getDownloadUrl(@NonNull StorageReference reference);
}
