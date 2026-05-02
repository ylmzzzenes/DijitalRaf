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

import com.example.dijitalraf.R;
import com.example.dijitalraf.ui.auth.GoogleSignInHelper;
import com.example.dijitalraf.ui.auth.LoginActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            String email = user.getEmail();
            if (!TextUtils.isEmpty(displayName) && !TextUtils.isEmpty(email)) {
                tvEmail.setText(getString(R.string.profile_name_email_line, displayName, email));
            } else if (!TextUtils.isEmpty(email)) {
                tvEmail.setText(email);
            } else if (!TextUtils.isEmpty(displayName)) {
                tvEmail.setText(displayName);
            } else {
                tvEmail.setText(R.string.profile_subtitle);
            }
        } else {
            tvEmail.setText(R.string.profile_subtitle);
        }

        btnLogout.setOnClickListener(v -> signOutEverywhere());
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
