package com.example.dijitalraf.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dijitalraf.R;
import com.google.android.material.tabs.TabLayout;

public class LibraryPagerFragment extends Fragment {

    private static final String ARG_INITIAL_PAGE = "initial_page";

    private TabLayout tabLayout;
    private int currentPage;

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
}
