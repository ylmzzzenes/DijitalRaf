package com.example.dijitalraf.ui.home;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.dijitalraf.R;
import com.example.dijitalraf.data.FirebaseRtdb;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class EditProfileActivity extends AppCompatActivity {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final int MAX_BIO = 280;

    private MaterialToolbar toolbar;
    private ShapeableImageView ivAvatar;
    private TextInputLayout tilFirstName;
    private TextInputLayout tilLastName;
    private TextInputLayout tilUsername;
    private TextInputLayout tilBio;
    private TextInputEditText etFirstName;
    private TextInputEditText etLastName;
    private TextInputEditText etUsername;
    private TextInputEditText etBio;
    private TextView tvEmailReadOnly;
    private MaterialButton btnSaveProfile;
    private ProgressBar progressOverlay;

    @Nullable
    private Uri pendingImageUri;
    @Nullable
    private String currentPhotoUrl;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pendingImageUri = uri;
                    ivAvatar.setImageTintList(null);
                    Glide.with(this).load(uri).centerCrop().into(ivAvatar);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        toolbar = findViewById(R.id.toolbar);
        ivAvatar = findViewById(R.id.ivAvatar);
        tilFirstName = findViewById(R.id.tilFirstName);
        tilLastName = findViewById(R.id.tilLastName);
        tilUsername = findViewById(R.id.tilUsername);
        tilBio = findViewById(R.id.tilBio);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etUsername = findViewById(R.id.etUsername);
        etBio = findViewById(R.id.etBio);
        tvEmailReadOnly = findViewById(R.id.tvEmailReadOnly);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressOverlay = findViewById(R.id.progressOverlay);

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.chat_error_not_signed_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvEmailReadOnly.setText(!TextUtils.isEmpty(user.getEmail())
                ? user.getEmail()
                : getString(R.string.profile_email_unknown));

        ivAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnSaveProfile.setOnClickListener(v -> attemptSave(user));

        setLoading(true);
        loadProfile(user);
    }

    private void loadProfile(@NonNull FirebaseUser authUser) {
        DatabaseReference ref = FirebaseDatabase.getInstance(FirebaseRtdb.URL)
                .getReference("users")
                .child(authUser.getUid());

        ref.get().addOnCompleteListener(task -> {
            setLoading(false);
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(this, R.string.profile_load_failed, Toast.LENGTH_LONG).show();
                prefillFromAuth(authUser);
                return;
            }

            String first = task.getResult().child("firstName").getValue(String.class);
            String last = task.getResult().child("lastName").getValue(String.class);
            String full = task.getResult().child("fullName").getValue(String.class);
            String username = task.getResult().child("username").getValue(String.class);
            String bio = task.getResult().child("bio").getValue(String.class);
            currentPhotoUrl = task.getResult().child("photoUrl").getValue(String.class);

            if (TextUtils.isEmpty(first) && TextUtils.isEmpty(last) && !TextUtils.isEmpty(full)) {
                String[] parts = splitFullName(full);
                first = parts[0];
                last = parts[1];
            }

            etFirstName.setText(first != null ? first : "");
            etLastName.setText(last != null ? last : "");
            etUsername.setText(username != null ? username : "");
            etBio.setText(bio != null ? bio : "");

            if (TextUtils.isEmpty(currentPhotoUrl) && authUser.getPhotoUrl() != null) {
                currentPhotoUrl = authUser.getPhotoUrl().toString();
            }

            applyAvatarPreview(authUser);
        });
    }

    private void prefillFromAuth(@NonNull FirebaseUser authUser) {
        String dn = authUser.getDisplayName();
        if (!TextUtils.isEmpty(dn)) {
            String[] p = splitFullName(dn);
            etFirstName.setText(p[0]);
            etLastName.setText(p[1]);
        }
        if (authUser.getPhotoUrl() != null) {
            currentPhotoUrl = authUser.getPhotoUrl().toString();
        }
        applyAvatarPreview(authUser);
    }

    private void applyAvatarPreview(@NonNull FirebaseUser authUser) {
        if (pendingImageUri != null) {
            return;
        }
        if (!TextUtils.isEmpty(currentPhotoUrl)) {
            ivAvatar.setImageTintList(null);
            Glide.with(this).load(currentPhotoUrl).centerCrop().into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.ic_person_24);
            ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(
                    getColor(R.color.primary)));
        }
    }

    private void attemptSave(@NonNull FirebaseUser authUser) {
        tilFirstName.setError(null);
        tilLastName.setError(null);
        tilUsername.setError(null);
        tilBio.setError(null);

        String first = textOf(etFirstName);
        String last = textOf(etLastName);
        String username = textOf(etUsername);
        String bio = textOf(etBio);

        if (TextUtils.isEmpty(first)) {
            tilFirstName.setError(getString(R.string.error_first_name_empty));
            etFirstName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(last)) {
            tilLastName.setError(getString(R.string.error_last_name_empty));
            etLastName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(username)) {
            tilUsername.setError(getString(R.string.error_username_empty));
            etUsername.requestFocus();
            return;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            tilUsername.setError(getString(R.string.error_username_invalid));
            etUsername.requestFocus();
            return;
        }
        if (bio.length() > MAX_BIO) {
            tilBio.setError(getString(R.string.error_bio_too_long));
            etBio.requestFocus();
            return;
        }

        String fullName = (first + " " + last).trim();

        setLoading(true);
        if (pendingImageUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReference()
                    .child("profiles")
                    .child(authUser.getUid())
                    .child("avatar.jpg");
            storageRef.putFile(pendingImageUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException() != null
                                    ? task.getException()
                                    : new IllegalStateException("upload");
                        }
                        return storageRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            setLoading(false);
                            Toast.makeText(this, R.string.profile_photo_upload_failed, Toast.LENGTH_LONG).show();
                            return;
                        }
                        pendingImageUri = null;
                        currentPhotoUrl = task.getResult().toString();
                        persistProfile(authUser, fullName, first, last, username, bio, currentPhotoUrl);
                    });
        } else {
            persistProfile(authUser, fullName, first, last, username, bio, currentPhotoUrl);
        }
    }

    private void persistProfile(
            @NonNull FirebaseUser authUser,
            @NonNull String fullName,
            @NonNull String first,
            @NonNull String last,
            @NonNull String username,
            @NonNull String bio,
            @Nullable String photoUrl) {

        DatabaseReference ref = FirebaseDatabase.getInstance(FirebaseRtdb.URL)
                .getReference("users")
                .child(authUser.getUid());

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", first);
        updates.put("lastName", last);
        updates.put("fullName", fullName);
        updates.put("username", username);
        updates.put("bio", bio);
        updates.put("photoUrl", photoUrl != null ? photoUrl : "");
        updates.put("updatedAt", System.currentTimeMillis());

        ref.updateChildren(updates).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                setLoading(false);
                Toast.makeText(
                        this,
                        getString(R.string.profile_save_failed, task.getException() != null
                                ? task.getException().getMessage()
                                : ""),
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            UserProfileChangeRequest.Builder b = new UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName);
            if (!TextUtils.isEmpty(photoUrl)) {
                b.setPhotoUri(Uri.parse(photoUrl));
            }
            authUser.updateProfile(b.build()).addOnCompleteListener(t2 -> {
                setLoading(false);
                if (t2.isSuccessful()) {
                    Toast.makeText(this, R.string.profile_save_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(
                            this,
                            getString(R.string.profile_auth_update_failed,
                                    t2.getException() != null ? t2.getException().getMessage() : ""),
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                }
            });
        });
    }

    private void setLoading(boolean loading) {
        progressOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!loading);
        toolbar.setEnabled(!loading);
    }

    @NonNull
    private static String textOf(@Nullable TextInputEditText et) {
        if (et == null || et.getText() == null) {
            return "";
        }
        return et.getText().toString().trim();
    }

    @NonNull
    private static String[] splitFullName(@Nullable String fullName) {
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
}
