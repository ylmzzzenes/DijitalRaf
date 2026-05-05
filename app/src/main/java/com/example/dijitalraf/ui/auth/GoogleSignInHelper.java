package com.example.dijitalraf.ui.auth;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.CredentialOption;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialResponse;

import com.example.dijitalraf.R;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.security.SecureRandom;

/**
 * Credential Manager tabanlı Google ile giriş yardımcıları.
 */
public final class GoogleSignInHelper {

    private GoogleSignInHelper() {
    }

    @NonNull
    public static String trimServerClientIdOrEmpty(@NonNull Context context) {
        String id = context.getString(R.string.default_web_client_id);
        return id != null ? id.trim() : "";
    }

    public static boolean hasWebClientIdConfigured(@NonNull Context context) {
        return !trimServerClientIdOrEmpty(context).isEmpty();
    }

    @NonNull
    public static CredentialOption buildSignInWithGoogleOption(@NonNull String serverClientId) {
        return new GetSignInWithGoogleOption.Builder(serverClientId)
                .setNonce(generateNonce())
                .build();
    }

    @Nullable
    public static String extractIdToken(@NonNull GetCredentialResponse response) {
        if (!(response.getCredential() instanceof CustomCredential)) {
            return null;
        }
        CustomCredential credential = (CustomCredential) response.getCredential();
        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            return null;
        }
        try {
            return GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @NonNull
    private static String generateNonce() {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        return Base64.encodeToString(raw, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
}
