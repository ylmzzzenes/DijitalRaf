package com.example.dijitalraf.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.dijitalraf.R;
import com.example.dijitalraf.auth.EmailVerificationHelper;
import com.example.dijitalraf.ui.util.UiMessages;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_ADD_BOOK = "extra_open_add_book";

    private static final int[] NAV_IDS = {
            R.id.nav_home,
            R.id.nav_library,
            R.id.nav_favorites,
            R.id.nav_assistant,
            R.id.nav_profile,
    };

    private View bannerEmailVerification;
    private ViewPager2 mainViewPager;
    private BottomNavigationView bottomNav;
    private View kitapEkleOverlay;
    private MainPagerAdapter pagerAdapter;
    private int lastValidPagerPosition;
    @Nullable
    private Integer pendingLibraryTab;
    private FloatingActionButton fabAddBook;
    private BooksViewModel booksViewModel;
    /** Avoids ViewPager2 ↔ BottomNavigationView feedback when {@link BottomNavigationView#setSelectedItemId(int)} runs from {@link ViewPager2.OnPageChangeCallback}. */
    private boolean syncingBottomNavFromPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initComponents();
        registerEventHandlers();
        observeViewModel();
        restoreInitialState(savedInstanceState);
        handleOpenAddBookIntent(getIntent());
    }

    private void initComponents() {
        bannerEmailVerification = findViewById(R.id.bannerEmailVerification);
        mainViewPager = findViewById(R.id.mainViewPager);
        bottomNav = findViewById(R.id.bottomNav);
        kitapEkleOverlay = findViewById(R.id.kitapEkleOverlay);
        fabAddBook = findViewById(R.id.fabAddBook);

        booksViewModel = new ViewModelProvider(this).get(BooksViewModel.class);
        booksViewModel.startListening();

        pagerAdapter = new MainPagerAdapter(this);
        mainViewPager.setAdapter(pagerAdapter);
        mainViewPager.setOffscreenPageLimit(4);
        mainViewPager.setUserInputEnabled(true);

        lastValidPagerPosition = 0;
    }

    private void registerEventHandlers() {
        bottomNav.setOnItemSelectedListener(item -> {
            if (syncingBottomNavFromPager) {
                return true;
            }
            int itemId = item.getItemId();
            if (itemId == R.id.nav_assistant && blockIfEmailUnverified()) {
                return false;
            }
            int index = menuIdToIndex(itemId);
            if (index < 0) {
                return false;
            }
            if (mainViewPager.getCurrentItem() != index) {
                mainViewPager.setCurrentItem(index, false);
            }
            return true;
        });

        mainViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 3 && EmailVerificationHelper.mustVerifyEmail(FirebaseAuth.getInstance().getCurrentUser())) {
                    mainViewPager.setCurrentItem(lastValidPagerPosition, false);
                    UiMessages.snackbar(
                            HomeActivity.this,
                            R.string.feature_locked_email_unverified,
                            Snackbar.LENGTH_LONG,
                            fabAddBook);
                    applyBottomNavSelectionForPagerIndex(lastValidPagerPosition);
                    return;
                }
                if (position != 3) {
                    lastValidPagerPosition = position;
                }
                applyPendingLibraryTabIfNeeded(position);
                applyBottomNavSelectionForPagerIndex(position);
            }
        });

        fabAddBook.setOnClickListener(v -> {
            if (blockIfEmailUnverified()) {
                return;
            }
            showKitapEkleOverlay();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (kitapEkleOverlay.getVisibility() == View.VISIBLE) {
                    dismissKitapEkleOverlay();
                } else {
                    finish();
                }
            }
        });
    }

    private void observeViewModel() {
        booksViewModel.getBooksError().observe(this, event -> {
            if (event == null) {
                return;
            }
            String msg = event.getContentIfNotHandled();
            if (msg != null && !msg.isEmpty()) {
                UiMessages.snackbar(
                        HomeActivity.this,
                        getString(R.string.books_sync_failed, msg),
                        Snackbar.LENGTH_LONG,
                        fabAddBook);
            }
        });
    }

    private void restoreInitialState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            mainViewPager.setCurrentItem(0, false);
            applyBottomNavSelectionForPagerIndex(0);
        } else {
            mainViewPager.post(() -> applyBottomNavSelectionForPagerIndex(mainViewPager.getCurrentItem()));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenAddBookIntent(intent);
    }

    private void handleOpenAddBookIntent(@Nullable Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_OPEN_ADD_BOOK, false)) {
            return;
        }
        intent.removeExtra(EXTRA_OPEN_ADD_BOOK);
        mainViewPager.post(() -> {
            if (!blockIfEmailUnverified()) {
                showKitapEkleOverlay();
            }
        });
    }

    public void showKitapEkleOverlay() {
        kitapEkleOverlay.setVisibility(View.VISIBLE);
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.kitapEkleOverlay, new KitapEkleFragment())
                .commit();
    }

    public void dismissKitapEkleOverlay() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.kitapEkleOverlay);
        if (f != null) {
            getSupportFragmentManager().beginTransaction().remove(f).commit();
        }
        kitapEkleOverlay.setVisibility(View.GONE);
        mainViewPager.post(() -> {
            LibraryPagerFragment lib = pagerAdapter != null ? pagerAdapter.getLibraryPagerFragment() : null;
            if (lib != null) {
                lib.resetHostLibrarySwipeUi();
            }
        });
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
            UiMessages.snackbar(this, R.string.feature_locked_email_unverified, Snackbar.LENGTH_LONG, fabAddBook);
            return true;
        }
        return false;
    }

    private static int menuIdToIndex(int menuItemId) {
        for (int i = 0; i < NAV_IDS.length; i++) {
            if (NAV_IDS[i] == menuItemId) {
                return i;
            }
        }
        return -1;
    }

    private void applyBottomNavSelectionForPagerIndex(int pagerIndex) {
        if (pagerIndex < 0 || pagerIndex >= NAV_IDS.length) {
            return;
        }
        int menuId = NAV_IDS[pagerIndex];
        if (bottomNav.getSelectedItemId() == menuId) {
            return;
        }
        syncingBottomNavFromPager = true;
        try {
            bottomNav.setSelectedItemId(menuId);
        } finally {
            syncingBottomNavFromPager = false;
        }
    }

    private void applyPendingLibraryTabIfNeeded(int position) {
        if (position != 1 || pendingLibraryTab == null) {
            return;
        }
        mainViewPager.post(() -> {
            LibraryPagerFragment lib = pagerAdapter.getLibraryPagerFragment();
            if (lib != null && lib.getView() != null) {
                lib.setCurrentItem(pendingLibraryTab);
            }
            pendingLibraryTab = null;
        });
    }

    /** Ana sayfa (gösterge paneli); alt menü ve ViewPager senkron. */
    public void openHomeDashboard() {
        mainViewPager.setCurrentItem(0, false);
        applyBottomNavSelectionForPagerIndex(0);
    }

    /** Sohbet asistanı sekmesi. */
    public void openChatAssistant() {
        if (blockIfEmailUnverified()) {
            return;
        }
        mainViewPager.setCurrentItem(3, false);
        applyBottomNavSelectionForPagerIndex(3);
    }

    /**
     * Kütüphane sekmesi: {@code readBooks true} → Okunan, {@code false} → Okunacak.
     */
    public void openBookSection(boolean readBooks) {
        pendingLibraryTab = readBooks ? 0 : 1;
        if (mainViewPager.getCurrentItem() == 1) {
            LibraryPagerFragment lib = pagerAdapter.getLibraryPagerFragment();
            if (lib != null) {
                lib.setCurrentItem(pendingLibraryTab);
            }
            pendingLibraryTab = null;
        } else {
            mainViewPager.setCurrentItem(1, false);
        }
        applyBottomNavSelectionForPagerIndex(1);
    }
}
