package com.example.aaaudio;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.aaaudio.adapter.MainPagerAdapter;
import com.example.aaaudio.service.SimpleAudioPlayerManager;
import com.example.aaaudio.ui.PlayerActivity;
import com.example.aaaudio.ui.PlaylistQueueBottomSheet;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private LinearLayout bottomPlayerControl;
    private ImageView albumArt;
    private ImageButton btnPlayPause, btnNext, btnPlaylist;
    private TextView songTitle, songArtist;

    private SimpleAudioPlayerManager audioPlayerManager;
    private Handler playbackUpdateHandler;
    private Runnable playbackUpdateRunnable;

    public SimpleAudioPlayerManager getAudioPlayerManager() {
        return audioPlayerManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupViewPager();
        setupBottomNavigation();
        setupPlayerControls();
        setupPlaybackUpdates();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);

        viewPager = findViewById(R.id.view_pager);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomPlayerControl = findViewById(R.id.bottom_player_control);
        albumArt = findViewById(R.id.album_art);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);
        btnPlaylist = findViewById(R.id.btn_playlist);
        songTitle = findViewById(R.id.song_title);
        songArtist = findViewById(R.id.song_artist);

        audioPlayerManager = SimpleAudioPlayerManager.getInstance(this);

        audioPlayerManager.setPlaybackStateListener(new SimpleAudioPlayerManager.PlaybackStateListener() {
            @Override
            public void onPlaybackStateChanged(boolean isPlaying, com.example.aaaudio.model.Song currentSong) {
                updatePlaybackUI(isPlaying, currentSong);
            }
        });
    }

    private void setupViewPager() {
        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // 禁用滑动切换
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_songs) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.navigation_playlists) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.navigation_albums) {
                viewPager.setCurrentItem(2);
                return true;
            } else if (itemId == R.id.navigation_artists) {
                viewPager.setCurrentItem(3);
                return true;
            } else if (itemId == R.id.navigation_settings) {
                viewPager.setCurrentItem(4);
                return true;
            }
            return false;
        });
    }

    private void setupPlayerControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (audioPlayerManager.isServiceBound()) {
                audioPlayerManager.playPause();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (audioPlayerManager.isServiceBound()) {
                audioPlayerManager.next();
            }
        });

        btnPlaylist.setOnClickListener(v -> {
            PlaylistQueueBottomSheet bottomSheet = new PlaylistQueueBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "PlaylistQueue");
        });

        bottomPlayerControl.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            startActivity(intent);
        });
    }

    private void setupPlaybackUpdates() {
        playbackUpdateHandler = new Handler();
        playbackUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updatePlaybackState();
                playbackUpdateHandler.postDelayed(this, 1000); // 每秒更新一次
            }
        };
        playbackUpdateHandler.postDelayed(playbackUpdateRunnable, 1000);
    }

    private void updatePlaybackState() {
        if (audioPlayerManager.isServiceBound()) {
            boolean isPlaying = audioPlayerManager.isPlaying();
            com.example.aaaudio.model.Song currentSong = audioPlayerManager.getCurrentSong();
            updatePlaybackUI(isPlaying, currentSong);
        }
    }

    private void updatePlaybackUI(boolean isPlaying, com.example.aaaudio.model.Song currentSong) {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewPager.getLayoutParams();
        int bottomNavHeight = bottomNavigation.getHeight();
        int playerBarHeight = getResources().getDimensionPixelSize(R.dimen.player_bar_height);

        bottomPlayerControl.setVisibility(View.VISIBLE);

        if (currentSong != null) {
            songTitle.setText(currentSong.getTitle());
            songArtist.setText(currentSong.getArtist());
        } else {
            songTitle.setText("未在播放");
            songArtist.setText("选择歌曲开始播放");
        }

        params.bottomMargin = bottomNavHeight + playerBarHeight;
        viewPager.setLayoutParams(params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 停止播放状态更新
        if (playbackUpdateHandler != null && playbackUpdateRunnable != null) {
            playbackUpdateHandler.removeCallbacks(playbackUpdateRunnable);
        }
        
        // 解绑服务
        if (audioPlayerManager != null) {
            audioPlayerManager.unbindService();
        }
    }
}