package com.example.dijitalraf.auth;

import androidx.annotation.Nullable;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

/**
 * E-posta/şifre ile giriş yapan ve henüz e-postayı doğrulamamış kullanıcıları tespit eder.
 * Google vb. sağlayıcılar bu kontrole dahil edilmez.
 */
public final class EmailVerificationHelper {

    private EmailVerificationHelper() {}

    public static boolean mustVerifyEmail(@Nullable FirebaseUser user) {
        if (user == null) {
            return false;
        }
        if (user.isEmailVerified()) {
            return false;
        }
        for (UserInfo info : user.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }
}
