package com.example.dijitalraf.data.remote.firebase;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.dijitalraf.core.constants.FirebaseConstants;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

/**
 * Raw Firebase Storage access.
 */
public final class FirebaseStorageDataSource {

    private final FirebaseStorage storage;

    public FirebaseStorageDataSource() {
        this(FirebaseStorage.getInstance());
    }

    public FirebaseStorageDataSource(@NonNull FirebaseStorage storage) {
        this.storage = storage;
    }

    @NonNull
    public StorageReference profileAvatarRef(@NonNull String uid) {
        return storage.getReference()
                .child(FirebaseConstants.STORAGE_PROFILES_ROOT)
                .child(uid)
                .child(FirebaseConstants.STORAGE_AVATAR_FILE_NAME);
    }

    @NonNull
    public UploadTask uploadProfileAvatar(@NonNull String uid, @NonNull Uri imageUri) {
        return profileAvatarRef(uid).putFile(imageUri);
    }

    @NonNull
    public Task<Uri> getDownloadUrl(@NonNull StorageReference reference) {
        return reference.getDownloadUrl();
    }
}
