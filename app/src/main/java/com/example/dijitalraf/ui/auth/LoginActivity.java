package com.example.dijitalraf.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.dijitalraf.R;
import com.example.dijitalraf.core.constants.DatabasePaths;
import com.example.dijitalraf.data.EmailValidation;
import com.example.dijitalraf.data.ProfileNameSplitter;
import com.example.dijitalraf.data.repository.AuthRepository;
import com.example.dijitalraf.data.repository.UserRepository;
import com.example.dijitalraf.di.AppContainer;
import com.example.dijitalraf.ui.home.HomeActivity;
import com.example.dijitalraf.ui.util.UiMessages;
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

    public static final String EXTRA_SNACKBAR_MESSAGE =
            "com.example.dijitalraf.ui.auth.LoginActivity.EXTRA_SNACKBAR_MESSAGE";

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnGoogleSignIn;
    private TextView tvForgotPassword;
    private TextView tvGoToRegister;
    private AuthRepository authRepository;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        initComponents();
        maybeShowSnackMessageFromIntent();
        registerEventHandlers();
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
    }

    private void registerEventHandlers() {
        clearErrorOnTextChanged(etEmail, tilEmail);
        clearErrorOnTextChanged(etPassword, tilPassword);

        btnLogin.setOnClickListener(v -> loginUser());
        tvForgotPassword.setOnClickListener(v -> sendPasswordResetEmail());
        btnGoogleSignIn.setOnClickListener(v -> onGoogleSignInClicked());
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private static void clearErrorOnTextChanged(
            @NonNull TextInputEditText editText,
            @NonNull TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op.
            }
        });
    }

    private void maybeShowSnackMessageFromIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        String message = intent.getStringExtra(EXTRA_SNACKBAR_MESSAGE);
        if (message == null || message.isEmpty()) {
            return;
        }
        intent.removeExtra(EXTRA_SNACKBAR_MESSAGE);
        getWindow().getDecorView().post(() ->
                UiMessages.snackbar(LoginActivity.this, message, Snackbar.LENGTH_LONG));
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

    private void onGoogleSignInClicked() {
        if (!GoogleSignInHelper.hasWebClientIdConfigured(this)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.google_sign_in_setup_title)
                    .setMessage(R.string.google_sign_in_setup_message)
                    .setPositiveButton(R.string.dialog_close, (d, w) -> d.dismiss())
                    .show();
            return;
        }

        String serverClientId = GoogleSignInHelper.trimServerClientIdOrEmpty(this);
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(GoogleSignInHelper.buildSignInWithGoogleOption(serverClientId))
                .build();

        CredentialManager credentialManager = CredentialManager.create(this);
        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                getMainExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(@NonNull GetCredentialResponse result) {
                        String idToken = GoogleSignInHelper.extractIdToken(result);
                        if (idToken == null) {
                            UiMessages.snackbar(
                                    LoginActivity.this,
                                    R.string.google_sign_in_token_missing,
                                    Snackbar.LENGTH_LONG);
                            return;
                        }
                        firebaseAuthWithGoogle(idToken);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        UiMessages.snackbar(
                                LoginActivity.this,
                                AuthUiMessages.forGoogleCredential(e, LoginActivity.this),
                                Snackbar.LENGTH_LONG);
                    }
                });
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
            String[] nameParts = ProfileNameSplitter.splitForStorage(display);
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
