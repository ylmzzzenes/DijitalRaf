package com.example.dijitalraf.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.data.remote.firebase.FirebaseAuthDataSource;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

/**
 * Default Firebase-backed auth repository.
 */
public final class DefaultAuthRepository implements AuthRepository {

    private final FirebaseAuthDataSource dataSource;

    public DefaultAuthRepository(@NonNull FirebaseAuthDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Nullable
    @Override
    public FirebaseUser getCurrentUser() {
        return dataSource.getCurrentUser();
    }

    @NonNull
    @Override
    public Task<AuthResult> signInWithEmail(@NonNull String email, @NonNull String password) {
        return dataSource.signInWithEmail(email, password);
    }

    @NonNull
    @Override
    public Task<AuthResult> signInWithCredential(@NonNull AuthCredential credential) {
        return dataSource.signInWithCredential(credential);
    }

    @NonNull
    @Override
    public Task<AuthResult> registerWithEmail(@NonNull String email, @NonNull String password) {
        return dataSource.createUserWithEmail(email, password);
    }

    @NonNull
    @Override
    public Task<Void> sendPasswordResetEmail(@NonNull String email) {
        return dataSource.sendPasswordResetEmail(email);
    }

    @Override
    public void signOut() {
        dataSource.signOut();
    }

    @NonNull
    @Override
    public Task<Void> deleteCurrentUser() {
        return dataSource.deleteCurrentUser();
    }
}
