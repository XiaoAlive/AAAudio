package com.example.aaaudio.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.aaaudio.ui.PlaylistQueueBottomSheet;
import com.example.aaaudio.ui.QueueCurrentFragment;
import com.example.aaaudio.ui.QueueHistoryFragment;

public class QueuePagerAdapter extends FragmentStateAdapter {

    private QueueCurrentFragment currentFragment;
    private QueueHistoryFragment lastFragment;
    private QueueHistoryFragment earlierFragment;
    private PlaylistQueueBottomSheet parentSheet;

    public QueuePagerAdapter(@NonNull FragmentActivity fragmentActivity, PlaylistQueueBottomSheet sheet) {
        super(fragmentActivity);
        this.parentSheet = sheet;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                currentFragment = QueueCurrentFragment.newInstance();
                currentFragment.setParentSheet(parentSheet);
                return currentFragment;
            case 1:
                lastFragment = QueueHistoryFragment.newInstance(QueueHistoryFragment.TYPE_LAST);
                lastFragment.setParentSheet(parentSheet);
                return lastFragment;
            case 2:
                earlierFragment = QueueHistoryFragment.newInstance(QueueHistoryFragment.TYPE_EARLIER);
                earlierFragment.setParentSheet(parentSheet);
                return earlierFragment;
            default:
                currentFragment = QueueCurrentFragment.newInstance();
                currentFragment.setParentSheet(parentSheet);
                return currentFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public QueueCurrentFragment getCurrentFragment() {
        return currentFragment;
    }

    public Fragment getHistoryFragment(int position) {
        if (position == 1) return lastFragment;
        if (position == 2) return earlierFragment;
        return null;
    }
}
