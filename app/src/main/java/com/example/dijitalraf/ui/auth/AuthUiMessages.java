package com.example.dijitalraf.ui.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException;
import androidx.credentials.exceptions.NoCredentialException;

import com.example.dijitalraf.R;
import com.google.firebase.auth.FirebaseAuthException;

import java.util.Locale;

/**
 * Firebase Auth ve Google Credential Manager hatalarını kullanıcıya anlaşılır metne çevirir.
 */
public final class AuthUiMessages {

    private AuthUiMessages() {
    }

    @NonNull
    public static String forGoogleCredential(@NonNull GetCredentialException e, @NonNull Context context) {
        if (e instanceof GetCredentialCancellationException) {
            return context.getString(R.string.google_sign_in_cancelled);
        }
        if (e instanceof GetCredentialProviderConfigurationException) {
            return context.getString(R.string.google_sign_in_error_developer);
        }
        if (e instanceof NoCredentialException) {
            return context.getString(R.string.google_sign_in_no_account);
        }
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.toLowerCase(Locale.ROOT).contains("network")) {
            return context.getString(R.string.auth_error_network);
        }
        return context.getString(R.string.google_sign_in_error_generic, msg);
    }

    @NonNull
    public static String forFirebaseAuth(@Nullable Exception e, @NonNull Context context) {
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            if ("ERROR_INVALID_CREDENTIAL".equals(code)
                    || "ERROR_WRONG_PASSWORD".equals(code)
                    || "ERROR_USER_NOT_FOUND".equals(code)) {
                return context.getString(R.string.auth_error_invalid_credentials);
            }
            if ("ERROR_USER_DISABLED".equals(code)) {
                return context.getString(R.string.auth_error_user_disabled);
            }
            if ("ERROR_TOO_MANY_REQUESTS".equals(code)) {
                return context.getString(R.string.auth_error_too_many_requests);
            }
            if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) {
                return context.getString(R.string.auth_error_network);
            }
            if ("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL".equals(code)) {
                return context.getString(R.string.auth_error_account_exists_different);
            }
        }
        String msg = e != null && e.getMessage() != null ? e.getMessage() : "";
        return context.getString(R.string.auth_error_generic, msg);
    }

    /** {@link com.google.firebase.auth.FirebaseAuth#sendPasswordResetEmail(String)} hataları. */
    @NonNull
    public static String forPasswordReset(@Nullable Exception e, @NonNull Context context) {
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return context.getString(R.string.auth_error_invalid_email_reset);
            }
            if ("ERROR_USER_NOT_FOUND".equals(code)) {
                return context.getString(R.string.auth_error_user_not_found_reset);
            }
            if ("ERROR_TOO_MANY_REQUESTS".equals(code)) {
                return context.getString(R.string.auth_error_too_many_requests);
            }
            if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) {
                return context.getString(R.string.auth_error_network);
            }
        }
        String msg = e != null && e.getMessage() != null ? e.getMessage() : "";
        return context.getString(R.string.password_reset_send_failed, msg);
    }
}
