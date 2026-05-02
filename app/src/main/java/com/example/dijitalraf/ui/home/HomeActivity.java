package com.example.dijitalraf.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.dijitalraf.R;
import com.example.dijitalraf.auth.EmailVerificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private View bannerEmailVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bannerEmailVerification = findViewById(R.id.bannerEmailVerification);

        BooksViewModel viewModel = new ViewModelProvider(this).get(BooksViewModel.class);
        viewModel.startListening();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        FloatingActionButton fabAddBook = findViewById(R.id.fabAddBook);
        fabAddBook.setOnClickListener(v -> {
            if (blockIfEmailUnverified()) {
                return;
            }
            startActivity(new Intent(HomeActivity.this, KitapEkleActivity.class));
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_assistant && blockIfEmailUnverified()) {
                return false;
            }
            if (itemId == R.id.nav_home) {
                showFragment(new DashboardFragment());
                return true;
            } else if (itemId == R.id.nav_library) {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                if (current instanceof LibraryPagerFragment) {
                    return true;
                }
                showFragment(LibraryPagerFragment.newInstance(1));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                showFragment(new FavoritesFragment());
                return true;
            } else if (itemId == R.id.nav_assistant) {
                showFragment(new ChatAssistantFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                showFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            showFragment(new DashboardFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshEmailVerificationBanner();
    }

    private void refreshEmailVerificationBanner() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bannerEmailVerification == null) {
            if (bannerEmailVerification != null) {
                bannerEmailVerification.setVisibility(View.GONE);
            }
            return;
        }
        user.reload().addOnCompleteListener(task -> {
            FirebaseUser fresh = FirebaseAuth.getInstance().getCurrentUser();
            if (EmailVerificationHelper.mustVerifyEmail(fresh)) {
                bannerEmailVerification.setVisibility(View.VISIBLE);
            } else {
                bannerEmailVerification.setVisibility(View.GONE);
            }
        });
    }

    private boolean blockIfEmailUnverified() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (EmailVerificationHelper.mustVerifyEmail(u)) {
            Toast.makeText(this, R.string.feature_locked_email_unverified, Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    /** Ana sayfa (gösterge paneli); alt menü seçimini tetikler. */
    public void openHomeDashboard() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    /** Sohbet asistanı; alt menü seçimini tetikler. */
    public void openChatAssistant() {
        if (blockIfEmailUnverified()) {
            return;
        }
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_assistant);
    }

    /**
     * Kütüphane sekmesini açar: {@code readBooks true} → Okunan, {@code false} → Okunacak.
     */
    public void openBookSection(boolean readBooks) {
        int page = readBooks ? 0 : 1;
        showFragment(LibraryPagerFragment.newInstance(page));
        getSupportFragmentManager().executePendingTransactions();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_library);
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
