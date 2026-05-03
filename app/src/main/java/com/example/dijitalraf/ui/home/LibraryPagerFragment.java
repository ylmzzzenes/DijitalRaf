package com.example.dijitalraf.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.dijitalraf.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

public class LibraryPagerFragment extends Fragment {

    private static final String ARG_INITIAL_PAGE = "initial_page";

    private static final long SEARCH_DEBOUNCE_MS = 320L;

    private TabLayout tabLayout;
    private int currentPage;
    private TextInputEditText etLibrarySearch;
    private MaterialButton btnLibraryClearFilters;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

    public static LibraryPagerFragment newInstance(int initialPage) {
        LibraryPagerFragment fragment = new LibraryPagerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_INITIAL_PAGE, Math.max(0, Math.min(1, initialPage)));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library_pager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tabLayout = view.findViewById(R.id.libraryTabLayout);

        int initial = 1;
        if (getArguments() != null) {
            initial = getArguments().getInt(ARG_INITIAL_PAGE, 1);
        }
        currentPage = Math.max(0, Math.min(1, initial));

        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText(R.string.library_tab_read));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.library_tab_to_read));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentPage = tab.getPosition();
                showLibraryPage(currentPage);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        etLibrarySearch = view.findViewById(R.id.etLibrarySearch);
        MaterialButton btnLibraryFilters = view.findViewById(R.id.btnLibraryFilters);
        btnLibraryClearFilters = view.findViewById(R.id.btnLibraryClearFilters);

        LibraryFilterViewModel filterViewModel =
                new ViewModelProvider(requireActivity()).get(LibraryFilterViewModel.class);

        TextWatcher searchWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (searchDebounceRunnable != null) {
                    searchHandler.removeCallbacks(searchDebounceRunnable);
                }
                final String q = s != null ? s.toString() : "";
                searchDebounceRunnable = () -> filterViewModel.updateQuickSearch(q);
                searchHandler.postDelayed(searchDebounceRunnable, SEARCH_DEBOUNCE_MS);
            }
        };
        etLibrarySearch.addTextChangedListener(searchWatcher);

        btnLibraryFilters.setOnClickListener(v ->
                LibraryFilterBottomSheet.newInstance()
                        .show(requireActivity().getSupportFragmentManager(), "library_filters"));

        btnLibraryClearFilters.setOnClickListener(v -> {
            filterViewModel.clearAll();
            etLibrarySearch.removeTextChangedListener(searchWatcher);
            etLibrarySearch.setText("");
            etLibrarySearch.addTextChangedListener(searchWatcher);
        });

        filterViewModel.getSpec().observe(getViewLifecycleOwner(), spec -> {
            if (btnLibraryClearFilters != null && spec != null) {
                btnLibraryClearFilters.setVisibility(spec.hasActiveFilters() ? View.VISIBLE : View.GONE);
            }
        });

        TabLayout.Tab tab = tabLayout.getTabAt(currentPage);
        if (tab != null) {
            tabLayout.selectTab(tab);
        } else {
            showLibraryPage(currentPage);
        }
    }

    private void showLibraryPage(int page) {
        if (!isAdded()) {
            return;
        }
        int p = Math.max(0, Math.min(1, page));
        getChildFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.libraryFragmentHost, LibraryFragment.newInstance(p == 0))
                .commit();
    }

    public int getCurrentItem() {
        return currentPage;
    }

    public void setCurrentItem(int page) {
        int p = Math.max(0, Math.min(1, page));
        currentPage = p;
        if (tabLayout != null) {
            TabLayout.Tab tab = tabLayout.getTabAt(p);
            if (tab != null) {
                if (tabLayout.getSelectedTabPosition() != p) {
                    tabLayout.selectTab(tab);
                } else {
                    showLibraryPage(p);
                }
            } else {
                showLibraryPage(p);
            }
        } else {
            showLibraryPage(p);
        }
    }

    /** Kitap ekleme ekranı kapanınca; kaydırmalı satır çiziminin takılı kalmaması için. */
    public void resetHostLibrarySwipeUi() {
        if (!isAdded()) {
            return;
        }
        Fragment host = getChildFragmentManager().findFragmentById(R.id.libraryFragmentHost);
        if (host instanceof LibraryFragment) {
            ((LibraryFragment) host).resetStuckSwipeUi();
        }
    }
}
