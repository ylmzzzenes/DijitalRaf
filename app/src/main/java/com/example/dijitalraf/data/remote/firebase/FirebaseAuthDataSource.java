package com.example.dijitalraf.data.remote.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Raw Firebase Authentication access.
 */
public final class FirebaseAuthDataSource {

    private final FirebaseAuth auth;

    public FirebaseAuthDataSource() {
        this(FirebaseAuth.getInstance());
    }

    public FirebaseAuthDataSource(@NonNull FirebaseAuth auth) {
        this.auth = auth;
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    @NonNull
    public Task<AuthResult> signInWithEmail(@NonNull String email, @NonNull String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    @NonNull
    public Task<AuthResult> signInWithCredential(@NonNull AuthCredential credential) {
        return auth.signInWithCredential(credential);
    }

    @NonNull
    public Task<AuthResult> createUserWithEmail(@NonNull String email, @NonNull String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    @NonNull
    public Task<Void> sendPasswordResetEmail(@NonNull String email) {
        return auth.sendPasswordResetEmail(email);
    }

    public void signOut() {
        auth.signOut();
    }
}
