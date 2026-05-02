package com.example.dijitalraf.ui.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dijitalraf.R;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.FirebaseAuthException;

/**
 * Firebase Auth ve Google Sign-In hatalarını kullanıcıya anlaşılır Türkçe metne çevirir.
 */
public final class AuthUiMessages {

    private AuthUiMessages() {
    }

    @NonNull
    public static String forGoogleSignIn(@NonNull ApiException e, @NonNull Context context) {
        switch (e.getStatusCode()) {
            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                return context.getString(R.string.google_sign_in_cancelled);
            case ConnectionResult.DEVELOPER_ERROR:
                return context.getString(R.string.google_sign_in_error_developer);
            case GoogleSignInStatusCodes.NETWORK_ERROR:
                return context.getString(R.string.auth_error_network);
            default:
                return context.getString(R.string.google_sign_in_error_generic, e.getStatusCode());
        }
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
