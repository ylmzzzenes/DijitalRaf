package com.example.dijitalraf.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.dijitalraf.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LibraryPagerFragment extends Fragment {

    private static final String ARG_INITIAL_PAGE = "initial_page";

    private ViewPager2 viewPager;
    @Nullable
    private TabLayoutMediator tabLayoutMediator;

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
        TabLayout tabLayout = view.findViewById(R.id.libraryTabLayout);
        viewPager = view.findViewById(R.id.libraryViewPager);
        viewPager.setAdapter(new LibraryPagerAdapter(this));

        int initial = 0;
        if (getArguments() != null) {
            initial = getArguments().getInt(ARG_INITIAL_PAGE, 0);
        }

        tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.library_tab_read);
            } else {
                tab.setText(R.string.library_tab_to_read);
            }
        });
        tabLayoutMediator.attach();

        viewPager.setCurrentItem(Math.max(0, Math.min(1, initial)), false);
    }

    @Override
    public void onDestroyView() {
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
            tabLayoutMediator = null;
        }
        viewPager = null;
        super.onDestroyView();
    }

    public int getCurrentItem() {
        return viewPager != null ? viewPager.getCurrentItem() : 0;
    }

    public void setCurrentItem(int page) {
        if (viewPager == null) {
            return;
        }
        int p = Math.max(0, Math.min(1, page));
        if (viewPager.getCurrentItem() != p) {
            viewPager.setCurrentItem(p, true);
        }
    }
}
