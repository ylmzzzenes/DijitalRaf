package com.example.dijitalraf.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dijitalraf.R;
import com.example.dijitalraf.ui.home.HomeActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String RTDB_URL =
            "https://dijitalraf-ec149-default-rtdb.europe-west1.firebasedatabase.app";

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnGoogleSignIn;
    private TextView tvGoToRegister;
    private FirebaseAuth mAuth;
    @Nullable
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    Toast.makeText(LoginActivity.this, R.string.google_sign_in_cancelled, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (result.getData() == null) {
                    return;
                }
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account == null || account.getIdToken() == null) {
                        Toast.makeText(LoginActivity.this, R.string.google_sign_in_token_missing, Toast.LENGTH_LONG)
                                .show();
                        return;
                    }
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(
                            LoginActivity.this,
                            AuthUiMessages.forGoogleSignIn(e, LoginActivity.this),
                            Toast.LENGTH_LONG
                    ).show();
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

        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void initComponents() {
        mAuth = FirebaseAuth.getInstance();
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        if (GoogleSignInHelper.hasWebClientIdConfigured(this)) {
            googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInHelper.buildSignInOptions(this));
        }
    }

    private void registerEventHandlers() {
        btnLogin.setOnClickListener(v -> loginUser());

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
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = task.getResult() != null ? task.getResult().getUser() : null;
                Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                syncGoogleUserToRtdb(user, () -> {
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                });
            } else {
                Toast.makeText(
                        LoginActivity.this,
                        AuthUiMessages.forFirebaseAuth(task.getException(), LoginActivity.this),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void syncGoogleUserToRtdb(@Nullable FirebaseUser user, Runnable onDone) {
        if (user == null) {
            onDone.run();
            return;
        }
        DatabaseReference ref = FirebaseDatabase.getInstance(RTDB_URL).getReference("users").child(user.getUid());
        ref.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                onDone.run();
                return;
            }
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("uid", user.getUid());
            userMap.put("email", user.getEmail() != null ? user.getEmail() : "");
            userMap.put("fullName", user.getDisplayName() != null ? user.getDisplayName() : "");
            userMap.put("createdAt", System.currentTimeMillis());
            ref.setValue(userMap).addOnCompleteListener(t -> onDone.run());
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_email_empty));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_password_empty));
            etPassword.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(
                                LoginActivity.this,
                                AuthUiMessages.forFirebaseAuth(task.getException(), LoginActivity.this),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}
