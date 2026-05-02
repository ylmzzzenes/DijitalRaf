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

public class LibraryPagerFragment extends Fragment {

    private static final String ARG_INITIAL_PAGE = "initial_page";

    private ViewPager2 viewPager;

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
        viewPager = view.findViewById(R.id.libraryViewPager);
        viewPager.setAdapter(new LibraryPagerAdapter(this));

        int initial = 0;
        if (getArguments() != null) {
            initial = getArguments().getInt(ARG_INITIAL_PAGE, 0);
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).syncLibraryBottomNavFromSwipe(position);
                }
            }
        });

        viewPager.setCurrentItem(Math.max(0, Math.min(1, initial)), false);
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
