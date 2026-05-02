package com.example.dijitalraf.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Ana sekme sırası: Ana sayfa, Kütüphane, Favoriler, Asistan, Profil.
 */
public class MainPagerAdapter extends FragmentStateAdapter {

    private static final int PAGE_COUNT = 5;

    private LibraryPagerFragment libraryPagerFragment;

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new DashboardFragment();
            case 1:
                libraryPagerFragment = LibraryPagerFragment.newInstance(1);
                return libraryPagerFragment;
            case 2:
                return new FavoritesFragment();
            case 3:
                return new ChatAssistantFragment();
            case 4:
                return new ProfileFragment();
            default:
                return new DashboardFragment();
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    @Nullable
    public LibraryPagerFragment getLibraryPagerFragment() {
        return libraryPagerFragment;
    }
}
