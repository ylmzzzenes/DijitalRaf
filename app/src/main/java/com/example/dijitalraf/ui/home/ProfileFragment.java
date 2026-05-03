package com.example.dijitalraf.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.dijitalraf.R;
import com.example.dijitalraf.auth.EmailVerificationHelper;
import com.example.dijitalraf.data.FirebaseRtdb;
import com.example.dijitalraf.ui.auth.GoogleSignInHelper;
import com.example.dijitalraf.locale.LanguagePreference;
import com.example.dijitalraf.theme.NightModePreference;
import com.example.dijitalraf.ui.auth.LoginActivity;
import com.example.dijitalraf.ui.util.UiMessages;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileFragment extends Fragment {

    private ShapeableImageView ivAvatar;
    private TextView tvProfileDisplayName;
    private TextView tvEmail;
    private View layoutEmailVerification;
    private MaterialButton btnResendVerificationEmail;
    private MaterialButton btnEditProfile;
    private MaterialButton btnBookStatistics;
    private MaterialButton btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvProfileDisplayName = view.findViewById(R.id.tvProfileDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        layoutEmailVerification = view.findViewById(R.id.layoutEmailVerification);
        btnResendVerificationEmail = view.findViewById(R.id.btnResendVerificationEmail);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnBookStatistics = view.findViewById(R.id.btnBookStatistics);
        btnLogout = view.findViewById(R.id.btnLogout);

        setupThemeToggle(view);
        setupLanguageToggle(view);

        btnResendVerificationEmail.setOnClickListener(v -> {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u == null || !EmailVerificationHelper.mustVerifyEmail(u)) {
                return;
            }
            btnResendVerificationEmail.setEnabled(false);
            u.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (!isAdded()) {
                            return;
                        }
                        btnResendVerificationEmail.setEnabled(true);
                        if (task.isSuccessful()) {
                            UiMessages.snackbar(ProfileFragment.this, R.string.email_verification_sent, Snackbar.LENGTH_LONG);
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "";
                            UiMessages.snackbar(
                                    ProfileFragment.this,
                                    getString(R.string.email_verification_send_failed, msg),
                                    Snackbar.LENGTH_LONG);
                        }
                    });
        });

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        btnBookStatistics.setOnClickListener(v -> BookStatisticsActivity.start(requireContext()));

        btnLogout.setOnClickListener(v -> signOutEverywhere());
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) {
            u.reload().addOnCompleteListener(t -> bindProfileUi());
        } else {
            bindProfileUi();
        }
    }

    private void bindProfileUi() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvProfileDisplayName.setText(R.string.profile_subtitle);
            tvEmail.setText("");
            layoutEmailVerification.setVisibility(View.GONE);
            ivAvatar.setImageResource(R.drawable.ic_person_24);
            ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.primary)));
            return;
        }

        layoutEmailVerification.setVisibility(
                EmailVerificationHelper.mustVerifyEmail(user) ? View.VISIBLE : View.GONE);

        String email = user.getEmail();
        tvEmail.setText(!TextUtils.isEmpty(email) ? email : getString(R.string.profile_email_unknown));

        FirebaseDatabase.getInstance(FirebaseRtdb.URL)
                .getReference("users")
                .child(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) {
                        return;
                    }
                    String first = snapshot.child("firstName").getValue(String.class);
                    String last = snapshot.child("lastName").getValue(String.class);
                    String full = snapshot.child("fullName").getValue(String.class);
                    String photoUrl = snapshot.child("photoUrl").getValue(String.class);

                    String display = joinName(first, last);
                    if (display.isEmpty() && !TextUtils.isEmpty(full)) {
                        display = full.trim();
                    }
                    if (display.isEmpty()) {
                        display = user.getDisplayName();
                    }
                    if (TextUtils.isEmpty(display)) {
                        display = getString(R.string.profile_subtitle);
                    }
                    tvProfileDisplayName.setText(display);

                    if (TextUtils.isEmpty(photoUrl) && user.getPhotoUrl() != null) {
                        photoUrl = user.getPhotoUrl().toString();
                    }
                    if (!TextUtils.isEmpty(photoUrl)) {
                        ivAvatar.setImageTintList(null);
                        Glide.with(this).load(photoUrl).centerCrop().into(ivAvatar);
                    } else {
                        ivAvatar.setImageResource(R.drawable.ic_person_24);
                        ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(
                                requireContext().getColor(R.color.primary)));
                    }
                })
                .addOnFailureListener(e -> applyAuthFallback(user));
    }

    private void applyAuthFallback(@NonNull FirebaseUser user) {
        if (!isAdded()) {
            return;
        }
        String dn = user.getDisplayName();
        tvProfileDisplayName.setText(!TextUtils.isEmpty(dn) ? dn : getString(R.string.profile_subtitle));
        if (user.getPhotoUrl() != null) {
            ivAvatar.setImageTintList(null);
            Glide.with(this).load(user.getPhotoUrl()).centerCrop().into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.ic_person_24);
            ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.primary)));
        }
    }

    @NonNull
    private static String joinName(@Nullable String first, @Nullable String last) {
        String f = first != null ? first.trim() : "";
        String l = last != null ? last.trim() : "";
        if (f.isEmpty() && l.isEmpty()) {
            return "";
        }
        if (f.isEmpty()) {
            return l;
        }
        if (l.isEmpty()) {
            return f;
        }
        return f + " " + l;
    }

    private void setupThemeToggle(@NonNull View root) {
        MaterialButtonToggleGroup toggle = root.findViewById(R.id.toggleTheme);
        if (toggle == null) {
            return;
        }
        String saved = NightModePreference.getSavedOrDefault(requireContext());
        int checkId = NightModePreference.DARK.equals(saved)
                ? R.id.btnThemeDark
                : R.id.btnThemeLight;
        toggle.check(checkId);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            String mode = checkedId == R.id.btnThemeDark
                    ? NightModePreference.DARK
                    : NightModePreference.LIGHT;
            if (mode.equals(NightModePreference.getSavedOrDefault(requireContext()))) {
                return;
            }
            NightModePreference.setAndApply(requireContext(), mode);
            UiMessages.snackbar(ProfileFragment.this, R.string.theme_changed, Snackbar.LENGTH_SHORT);
        });
    }

    private void setupLanguageToggle(@NonNull View root) {
        MaterialButtonToggleGroup toggle = root.findViewById(R.id.toggleLanguage);
        if (toggle == null) {
            return;
        }
        String saved = LanguagePreference.getSavedOrDefault(requireContext());
        int checkId = R.id.btnLangTr;
        if (LanguagePreference.TAG_ENGLISH.equals(saved)) {
            checkId = R.id.btnLangEn;
        } else if (LanguagePreference.TAG_GERMAN.equals(saved)) {
            checkId = R.id.btnLangDe;
        }
        toggle.check(checkId);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            String tag = LanguagePreference.TAG_TURKISH;
            if (checkedId == R.id.btnLangEn) {
                tag = LanguagePreference.TAG_ENGLISH;
            } else if (checkedId == R.id.btnLangDe) {
                tag = LanguagePreference.TAG_GERMAN;
            }
            if (tag.equals(LanguagePreference.getSavedOrDefault(requireContext()))) {
                return;
            }
            LanguagePreference.setAndApply(requireContext(), tag);
            UiMessages.snackbar(ProfileFragment.this, R.string.language_changed, Snackbar.LENGTH_SHORT);
        });
    }

    private void signOutEverywhere() {
        Context ctx = requireContext();
        Runnable goLogin = () -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ctx, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        };

        if (GoogleSignInHelper.hasWebClientIdConfigured(ctx)) {
            GoogleSignIn.getClient(ctx, GoogleSignInHelper.buildSignInOptions(ctx))
                    .signOut()
                    .addOnCompleteListener(requireActivity(), t -> goLogin.run());
        } else {
            goLogin.run();
        }
    }
}
