package com.example.dijitalraf.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

/**
 * Authentication repository boundary. Implementation will be introduced gradually.
 */
public interface AuthRepository {

    @Nullable
    FirebaseUser getCurrentUser();

    @NonNull
    Task<AuthResult> signInWithEmail(@NonNull String email, @NonNull String password);

    @NonNull
    Task<AuthResult> signInWithCredential(@NonNull AuthCredential credential);

    @NonNull
    Task<AuthResult> registerWithEmail(@NonNull String email, @NonNull String password);

    @NonNull
    Task<Void> sendPasswordResetEmail(@NonNull String email);

    void signOut();

    /**
     * Deletes the Firebase Authentication account for the current session.
     */
    @NonNull
    Task<Void> deleteCurrentUser();
}
