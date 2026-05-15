package com.example.aaaudio.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.aaaudio.ui.SongsFragment;
import com.example.aaaudio.ui.PlaylistsFragment;
import com.example.aaaudio.ui.AlbumsFragment;
import com.example.aaaudio.ui.ArtistsFragment;
import com.example.aaaudio.ui.SettingsFragment;

public class MainPagerAdapter extends FragmentStateAdapter {
    
    private static final int TAB_COUNT = 5;

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new SongsFragment();
            case 1:
                return new PlaylistsFragment();
            case 2:
                return new AlbumsFragment();
            case 3:
                return new ArtistsFragment();
            case 4:
                return new SettingsFragment();
            default:
                return new SongsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}