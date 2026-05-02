package com.example.dijitalraf.ui.auth;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.dijitalraf.R;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

/**
 * Tek tip {@link GoogleSignInOptions} (giriş ve çıkış aynı yapılandırmayı kullanmalı).
 */
public final class GoogleSignInHelper {

    private GoogleSignInHelper() {
    }

    public static boolean hasWebClientIdConfigured(@NonNull Context context) {
        String id = context.getString(R.string.default_web_client_id);
        return id != null && !id.trim().isEmpty();
    }

    @NonNull
    public static GoogleSignInOptions buildSignInOptions(@NonNull Context context) {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile();
        String id = context.getString(R.string.default_web_client_id);
        if (id != null && !id.trim().isEmpty()) {
            builder.requestIdToken(id.trim());
        }
        return builder.build();
    }
}
