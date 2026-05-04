package com.example.dijitalraf.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dijitalraf.R;
import com.example.dijitalraf.core.constants.DatabasePaths;
import com.example.dijitalraf.data.EmailValidation;
import com.example.dijitalraf.data.repository.AuthRepository;
import com.example.dijitalraf.data.repository.UserRepository;
import com.example.dijitalraf.di.AppContainer;
import com.example.dijitalraf.ui.home.HomeActivity;
import com.example.dijitalraf.ui.util.UiMessages;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnGoogleSignIn;
    private TextView tvForgotPassword;
    private TextView tvGoToRegister;
    private AuthRepository authRepository;
    private UserRepository userRepository;
    @Nullable
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    UiMessages.snackbar(LoginActivity.this, R.string.google_sign_in_cancelled, Snackbar.LENGTH_SHORT);
                    return;
                }
                if (result.getData() == null) {
                    return;
                }
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account == null || account.getIdToken() == null) {
                        UiMessages.snackbar(
                                LoginActivity.this,
                                R.string.google_sign_in_token_missing,
                                Snackbar.LENGTH_LONG);
                        return;
                    }
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    UiMessages.snackbar(
                            LoginActivity.this,
                            AuthUiMessages.forGoogleSignIn(e, LoginActivity.this),
                            Snackbar.LENGTH_LONG);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        initComponents();
        registerEventHandlers();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (authRepository.getCurrentUser() != null) {
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void initComponents() {
        AppContainer appContainer = AppContainer.from(this);
        authRepository = appContainer.getAuthRepository();
        userRepository = appContainer.getUserRepository();
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        if (GoogleSignInHelper.hasWebClientIdConfigured(this)) {
            googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInHelper.buildSignInOptions(this));
        }
    }

    private void registerEventHandlers() {
        btnLogin.setOnClickListener(v -> loginUser());

        tvForgotPassword.setOnClickListener(v -> sendPasswordResetEmail());

        btnGoogleSignIn.setOnClickListener(v -> onGoogleSignInClicked());

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void onGoogleSignInClicked() {
        if (!GoogleSignInHelper.hasWebClientIdConfigured(this)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.google_sign_in_setup_title)
                    .setMessage(R.string.google_sign_in_setup_message)
                    .setPositiveButton(R.string.dialog_close, (d, w) -> d.dismiss())
                    .show();
            return;
        }
        if (googleSignInClient == null) {
            googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInHelper.buildSignInOptions(this));
        }
        googleSignInClient.signOut().addOnCompleteListener(this, unused ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        authRepository.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = task.getResult() != null ? task.getResult().getUser() : null;
                syncGoogleUserToRtdb(user, () -> UiMessages.snackbarShortThenRun(
                        LoginActivity.this,
                        R.string.login_success,
                        () -> {
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        }));
            } else {
                UiMessages.snackbar(
                        LoginActivity.this,
                        AuthUiMessages.forFirebaseAuth(task.getException(), LoginActivity.this),
                        Snackbar.LENGTH_LONG);
            }
        });
    }

    private void syncGoogleUserToRtdb(@Nullable FirebaseUser user, Runnable onDone) {
        if (user == null) {
            onDone.run();
            return;
        }
        userRepository.getUser(user.getUid()).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                onDone.run();
                return;
            }
            String display = user.getDisplayName() != null ? user.getDisplayName() : "";
            String[] nameParts = splitFullNameForGoogleProfile(display);
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("uid", user.getUid());
            userMap.put(DatabasePaths.FIELD_EMAIL, user.getEmail() != null ? user.getEmail() : "");
            userMap.put(DatabasePaths.FIELD_FULL_NAME, display);
            userMap.put(DatabasePaths.FIELD_FIRST_NAME, nameParts[0]);
            userMap.put(DatabasePaths.FIELD_LAST_NAME, nameParts[1]);
            userMap.put(DatabasePaths.FIELD_CREATED_AT, System.currentTimeMillis());
            if (user.getPhotoUrl() != null) {
                userMap.put(DatabasePaths.FIELD_PHOTO_URL, user.getPhotoUrl().toString());
            }
            userRepository.setUser(user.getUid(), userMap).addOnCompleteListener(t -> onDone.run());
        });
    }

    @NonNull
    private static String[] splitFullNameForGoogleProfile(@Nullable String fullName) {
        String[] out = new String[] {"", ""};
        if (fullName == null || fullName.trim().isEmpty()) {
            return out;
        }
        String t = fullName.trim();
        int sp = t.indexOf(' ');
        if (sp < 0) {
            out[0] = t;
            return out;
        }
        out[0] = t.substring(0, sp).trim();
        out[1] = t.substring(sp + 1).trim();
        return out;
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        tilEmail.setError(null);
        tilPassword.setError(null);

        Integer emailIssue = EmailValidation.validateForForm(email);
        if (emailIssue != null) {
            tilEmail.setError(getString(emailIssue));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_password_empty));
            etPassword.requestFocus();
            return;
        }

        authRepository.signInWithEmail(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        UiMessages.snackbarShortThenRun(
                                LoginActivity.this,
                                R.string.login_success,
                                () -> {
                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    startActivity(intent);
                                    finish();
                                });
                    } else {
                        UiMessages.snackbar(
                                LoginActivity.this,
                                AuthUiMessages.forFirebaseAuth(task.getException(), LoginActivity.this),
                                Snackbar.LENGTH_LONG);
                    }
                });
    }

    private void sendPasswordResetEmail() {
        String email = etEmail.getText().toString().trim();
        tilEmail.setError(null);

        Integer emailIssue = EmailValidation.validateForForm(email);
        if (emailIssue != null) {
            tilEmail.setError(getString(emailIssue));
            etEmail.requestFocus();
            return;
        }

        tvForgotPassword.setEnabled(false);
        btnLogin.setEnabled(false);
        authRepository.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    tvForgotPassword.setEnabled(true);
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        UiMessages.snackbar(
                                LoginActivity.this,
                                R.string.password_reset_email_sent,
                                Snackbar.LENGTH_LONG);
                    } else {
                        UiMessages.snackbar(
                                LoginActivity.this,
                                AuthUiMessages.forPasswordReset(task.getException(), LoginActivity.this),
                                Snackbar.LENGTH_LONG);
                    }
                });
    }
}
