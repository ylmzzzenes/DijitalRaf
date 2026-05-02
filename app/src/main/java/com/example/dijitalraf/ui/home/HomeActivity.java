package com.example.dijitalraf.ui.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.dijitalraf.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BooksViewModel viewModel = new ViewModelProvider(this).get(BooksViewModel.class);
        viewModel.startListening();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                showFragment(new DashboardFragment());
                return true;
            } else if (itemId == R.id.nav_read_books || itemId == R.id.nav_to_read) {
                int page = itemId == R.id.nav_read_books ? 0 : 1;
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                if (current instanceof LibraryPagerFragment) {
                    LibraryPagerFragment pager = (LibraryPagerFragment) current;
                    if (pager.getCurrentItem() != page) {
                        pager.setCurrentItem(page);
                    }
                    return true;
                }
                showFragment(LibraryPagerFragment.newInstance(page));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                showFragment(new FavoritesFragment());
                return true;
            } else if (itemId == R.id.nav_add_book) {
                startActivity(new Intent(HomeActivity.this, KitapEkleActivity.class));
                return false;
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

    /** Ana sayfadaki kitap satırından Okunan / Okunacak sekmesine geçer. */
    public void openBookSection(boolean readBooks) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(readBooks ? R.id.nav_read_books : R.id.nav_to_read);
    }

    /**
     * Kütüphane ViewPager kaydırıldığında alt menüde Okunan / Okunacak ile senkron tutar.
     */
    public void syncLibraryBottomNavFromSwipe(int page) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        int targetId = page == 0 ? R.id.nav_read_books : R.id.nav_to_read;
        if (bottomNav.getSelectedItemId() != targetId) {
            bottomNav.setSelectedItemId(targetId);
        }
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
