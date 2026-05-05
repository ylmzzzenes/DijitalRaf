package com.example.dijitalraf.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dijitalraf.R;
import com.example.dijitalraf.data.EmailValidation;
import com.example.dijitalraf.data.ProfileNameSplitter;
import com.example.dijitalraf.core.constants.DatabasePaths;
import com.example.dijitalraf.data.repository.AuthRepository;
import com.example.dijitalraf.data.repository.UserRepository;
import com.example.dijitalraf.di.AppContainer;
import com.example.dijitalraf.ui.home.HomeActivity;
import com.example.dijitalraf.ui.util.UiMessages;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilFullName, tilEmail, tilPassword, tilConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvGoToLogin;
    private AuthRepository authRepository;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        initComponents();
        registerEventHandlers();
    }

    private void initComponents() {
        AppContainer appContainer = AppContainer.from(this);
        authRepository = appContainer.getAuthRepository();
        userRepository = appContainer.getUserRepository();

        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
    }

    private void registerEventHandlers() {
        btnRegister.setOnClickListener(v -> registerUser());

        tvGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError(getString(R.string.error_full_name_empty));
            etFullName.requestFocus();
            return;
        }

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

        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_short));
            etPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_confirm_empty));
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            etConfirmPassword.requestFocus();
            return;
        }

        authRepository.registerWithEmail(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = authRepository.getCurrentUser();

                        if (firebaseUser == null) {
                            UiMessages.snackbar(
                                    RegisterActivity.this,
                                    R.string.error_user_profile_unavailable,
                                    Snackbar.LENGTH_LONG);
                            return;
                        }

                        String uid = firebaseUser.getUid();

                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("uid", uid);
                        userMap.put(DatabasePaths.FIELD_FULL_NAME, fullName);
                        String[] nameParts = ProfileNameSplitter.splitForStorage(fullName);
                        userMap.put(DatabasePaths.FIELD_FIRST_NAME, nameParts[0]);
                        userMap.put(DatabasePaths.FIELD_LAST_NAME, nameParts[1]);
                        userMap.put(DatabasePaths.FIELD_EMAIL, email);
                        userMap.put(DatabasePaths.FIELD_CREATED_AT, System.currentTimeMillis());

                        userRepository.setUser(uid, userMap)
                                .addOnSuccessListener(unused -> firebaseUser.sendEmailVerification()
                                        .addOnCompleteListener(sendTask -> {
                                            if (sendTask.isSuccessful()) {
                                                UiMessages.snackbar(
                                                        RegisterActivity.this,
                                                        R.string.email_verification_sent,
                                                        Snackbar.LENGTH_LONG);
                                                Intent intent = new Intent(
                                                        RegisterActivity.this,
                                                        HomeActivity.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                String msg = sendTask.getException() != null
                                                        && sendTask.getException().getMessage() != null
                                                        ? sendTask.getException().getMessage()
                                                        : "";
                                                authRepository.signOut();
                                                Intent loginIntent = new Intent(
                                                        RegisterActivity.this,
                                                        LoginActivity.class);
                                                loginIntent.putExtra(
                                                        LoginActivity.EXTRA_SNACKBAR_MESSAGE,
                                                        getString(
                                                                R.string.register_verification_failed_then_login,
                                                                msg));
                                                loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(loginIntent);
                                                finish();
                                            }
                                        }))
                                .addOnFailureListener(e -> {
                                    String err = e.getMessage() != null ? e.getMessage() : "";
                                    authRepository.deleteCurrentUser().addOnCompleteListener(delTask -> {
                                        authRepository.signOut();
                                        if (delTask.isSuccessful()) {
                                            UiMessages.snackbar(
                                                    RegisterActivity.this,
                                                    getString(
                                                            R.string.register_profile_failed_rolled_back,
                                                            err),
                                                    Snackbar.LENGTH_LONG);
                                        } else {
                                            String delMsg = delTask.getException() != null
                                                    && delTask.getException().getMessage() != null
                                                    ? delTask.getException().getMessage()
                                                    : "";
                                            UiMessages.snackbar(
                                                    RegisterActivity.this,
                                                    getString(
                                                            R.string.register_profile_failed_account_cleanup_failed,
                                                            err,
                                                            delMsg),
                                                    Snackbar.LENGTH_LONG);
                                        }
                                    });
                                });

                    } else {
                        Exception ex = task.getException();
                        String err = ex != null && ex.getMessage() != null ? ex.getMessage() : "";
                        UiMessages.snackbar(
                                RegisterActivity.this,
                                getString(R.string.error_register_failed, err),
                                Snackbar.LENGTH_LONG);
                    }
                });
    }
}