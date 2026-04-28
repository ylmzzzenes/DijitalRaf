package com.example.dijitalraf.ui.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.dijitalraf.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BooksViewModel viewModel = new ViewModelProvider(this).get(BooksViewModel.class);
        viewModel.startListening();

        FloatingActionButton fab = findViewById(R.id.fabAddBook);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        fab.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, KitapEkleActivity.class)));

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                showFragment(new DashboardFragment());
                fab.show();
                return true;
            } else if (itemId == R.id.nav_library) {
                showFragment(new LibraryFragment());
                fab.hide();
                return true;
            } else if (itemId == R.id.nav_favorites) {
                showFragment(new FavoritesFragment());
                fab.hide();
                return true;
            } else if (itemId == R.id.nav_profile) {
                showFragment(new ProfileFragment());
                fab.hide();
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            showFragment(new DashboardFragment());
            fab.show();
            bottomNav.setSelectedItemId(R.id.nav_home);
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
