package com.example.aaaudio.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aaaudio.R;
import com.example.aaaudio.adapter.QueuePagerAdapter;
import com.example.aaaudio.model.Song;
import com.example.aaaudio.service.AudioPlayerService;
import com.example.aaaudio.service.PlayHistoryManager;
import com.example.aaaudio.service.SimpleAudioPlayerManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class PlaylistQueueBottomSheet extends BottomSheetDialogFragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private View queueHeader;
    private TextView btnPlayMode, btnClose, btnResume;
    private ImageButton btnAddSongs;
    private ImageButton btnClearPlaylist;
    private SimpleAudioPlayerManager audioPlayerManager;
    private PlayHistoryManager historyManager;
    private QueuePagerAdapter pagerAdapter;

    private static final String[] TAB_TITLES = {"当前播放", "上次播放", "更早播放"};

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
                params.bottomMargin = 0;
                bottomSheet.setLayoutParams(params);
            }
            if (d.getWindow() != null) {
                d.getWindow().setDimAmount(0.5f);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_playlist_queue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        audioPlayerManager = SimpleAudioPlayerManager.getInstance(requireContext());
        historyManager = PlayHistoryManager.getInstance(requireContext());

        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.queue_view_pager);
        queueHeader = view.findViewById(R.id.queue_header);
        btnPlayMode = view.findViewById(R.id.btn_play_mode);
        btnAddSongs = view.findViewById(R.id.btn_add_songs);
        btnClearPlaylist = view.findViewById(R.id.btn_clear_playlist);
        btnResume = view.findViewById(R.id.btn_resume);
        btnClose = view.findViewById(R.id.btn_close);

        pagerAdapter = new QueuePagerAdapter(requireActivity(), this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(true);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                queueHeader.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                btnResume.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
                if (position == 0) {
                    if (pagerAdapter.getCurrentFragment() != null) {
                        pagerAdapter.getCurrentFragment().loadPlaylist();
                    }
                } else {
                    QueueHistoryFragment fragment = (QueueHistoryFragment) pagerAdapter.getHistoryFragment(position);
                    if (fragment != null) {
                        fragment.loadHistory();
                    }
                }
            }
        });

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();

        btnPlayMode.setOnClickListener(v -> cyclePlayMode());

        btnAddSongs.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SelectSongsActivity.class);
            startActivity(intent);
        });

        btnClearPlaylist.setOnClickListener(v -> {
            if (audioPlayerManager.isServiceBound()) {
                audioPlayerManager.stop();
            }
            if (pagerAdapter.getCurrentFragment() != null) {
                pagerAdapter.getCurrentFragment().loadPlaylist();
            }
        });

        btnResume.setOnClickListener(v -> {
            int currentTab = viewPager.getCurrentItem();
            if (currentTab < 1) return;

            QueueHistoryFragment fragment = (QueueHistoryFragment) pagerAdapter.getHistoryFragment(currentTab);
            if (fragment != null) {
                fragment.resumePlayback();
            }
        });

        btnClose.setOnClickListener(v -> dismiss());

        updatePlayModeText(0);
    }

    private void cyclePlayMode() {
        if (!audioPlayerManager.isServiceBound()) return;

        AudioPlayerService.PlayMode currentMode = audioPlayerManager.getPlayMode();
        AudioPlayerService.PlayMode nextMode;

        switch (currentMode) {
            case REPEAT_ALL:
                nextMode = AudioPlayerService.PlayMode.SHUFFLE;
                break;
            case SHUFFLE:
                nextMode = AudioPlayerService.PlayMode.REPEAT_ONE;
                break;
            case REPEAT_ONE:
                nextMode = AudioPlayerService.PlayMode.REPEAT_ALL;
                break;
            default:
                nextMode = AudioPlayerService.PlayMode.REPEAT_ALL;
                break;
        }

        audioPlayerManager.setPlayMode(nextMode);

        List<Song> playlist = audioPlayerManager.getCurrentPlaylist();
        updatePlayModeText(playlist.size());
    }

    public void updatePlayModeText(int songCount) {
        if (audioPlayerManager == null || !audioPlayerManager.isServiceBound()) {
            btnPlayMode.setText("全部循环(" + songCount + "首)");
            return;
        }

        AudioPlayerService.PlayMode mode = audioPlayerManager.getPlayMode();
        String modeText;
        switch (mode) {
            case REPEAT_ALL:
                modeText = "全部循环";
                break;
            case SHUFFLE:
                modeText = "随机循环";
                break;
            case REPEAT_ONE:
                modeText = "单曲循环";
                break;
            default:
                modeText = "顺序播放";
                break;
        }
        btnPlayMode.setText(modeText + "(" + songCount + "首)");
    }

    public void switchToCurrentTab() {
        viewPager.setCurrentItem(0, true);
        if (pagerAdapter.getCurrentFragment() != null) {
            pagerAdapter.getCurrentFragment().loadPlaylist();
        }
    }
}
