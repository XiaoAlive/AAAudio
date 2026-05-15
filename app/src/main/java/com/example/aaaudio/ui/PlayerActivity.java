package com.example.aaaudio.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.aaaudio.R;
import com.example.aaaudio.model.Song;
import com.example.aaaudio.service.AudioPlayerService;
import com.example.aaaudio.service.SimpleAudioPlayerManager;

public class PlayerActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private ImageView playerAlbumArt;
    private TextView playerSongTitle;
    private TextView playerSongArtist;
    private SeekBar playerSeekBar;
    private TextView playerCurrentTime;
    private TextView playerTotalTime;

    private ImageButton btnPlayerPrevious;
    private ImageButton btnPlayerPlayPause;
    private ImageButton btnPlayerNext;
    private ImageButton btnPlayerPlayMode;
    private ImageButton btnPlayerPlaylist;

    private SimpleAudioPlayerManager audioPlayerManager;
    private AudioPlayerService audioPlayerService;
    private boolean isServiceBound;

    private Handler playbackUpdateHandler;
    private Runnable playbackUpdateRunnable;

    private boolean isSeeking;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioPlayerService.AudioPlayerBinder binder = (AudioPlayerService.AudioPlayerBinder) service;
            audioPlayerService = binder.getService();
            isServiceBound = true;
            updatePlayerState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            audioPlayerService = null;
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        audioPlayerManager = SimpleAudioPlayerManager.getInstance(this);
        audioPlayerManager.setPlaybackStateListener((isPlaying, currentSong) -> {
            if (isServiceBound) {
                updatePlayerState();
            }
        });
        bindService();

        initializeViews();
        setupToolbar();
        setupControls();
        setupSeekBar();
        startPlaybackUpdates();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_player);
        playerAlbumArt = findViewById(R.id.player_album_art);
        playerSongTitle = findViewById(R.id.player_song_title);
        playerSongArtist = findViewById(R.id.player_song_artist);
        playerSeekBar = findViewById(R.id.player_seek_bar);
        playerCurrentTime = findViewById(R.id.player_current_time);
        playerTotalTime = findViewById(R.id.player_total_time);

        btnPlayerPrevious = findViewById(R.id.btn_player_previous);
        btnPlayerPlayPause = findViewById(R.id.btn_player_play_pause);
        btnPlayerNext = findViewById(R.id.btn_player_next);
        btnPlayerPlayMode = findViewById(R.id.btn_player_play_mode);
        btnPlayerPlaylist = findViewById(R.id.btn_player_playlist);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupControls() {
        btnPlayerPlayPause.setOnClickListener(v -> {
            if (isServiceBound && audioPlayerService != null) {
                audioPlayerService.playPause();
                updatePlayButton();
            }
        });

        btnPlayerNext.setOnClickListener(v -> {
            if (isServiceBound && audioPlayerService != null) {
                audioPlayerService.next();
                updatePlayerState();
            }
        });

        btnPlayerPrevious.setOnClickListener(v -> {
            if (isServiceBound && audioPlayerService != null) {
                audioPlayerService.previous();
                updatePlayerState();
            }
        });

        btnPlayerPlayMode.setOnClickListener(v -> {
            if (audioPlayerService != null) {
                AudioPlayerService.PlayMode currentMode = audioPlayerService.getPlayMode();
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
                audioPlayerService.setPlayMode(nextMode);
                updatePlayModeButton();
            }
        });

        btnPlayerPlaylist.setOnClickListener(v -> {
            PlaylistQueueBottomSheet bottomSheet = new PlaylistQueueBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "PlaylistQueue");
        });
    }

    private void setupSeekBar() {
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    playerCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
                if (isServiceBound && audioPlayerService != null) {
                    audioPlayerService.seekTo(seekBar.getProgress());
                }
            }
        });
    }

    private void bindService() {
        Intent intent = new Intent(this, AudioPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void startPlaybackUpdates() {
        playbackUpdateHandler = new Handler();
        playbackUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updatePlaybackProgress();
                playbackUpdateHandler.postDelayed(this, 500);
            }
        };
        playbackUpdateHandler.postDelayed(playbackUpdateRunnable, 500);
    }

    private void updatePlaybackProgress() {
        if (!isServiceBound || audioPlayerService == null) return;

        if (!isSeeking) {
            int currentPosition = audioPlayerService.getCurrentPosition();
            int duration = audioPlayerService.getDuration();

            playerCurrentTime.setText(formatTime(currentPosition));
            playerTotalTime.setText(formatTime(duration));

            if (duration > 0) {
                playerSeekBar.setMax(duration);
                playerSeekBar.setProgress(currentPosition);
            }
        }

        updatePlayButton();
    }

    private void updatePlayerState() {
        if (audioPlayerService != null) {
            Song song = audioPlayerService.getCurrentSong();
            if (song != null) {
                playerSongTitle.setText(song.getTitle());
                playerSongArtist.setText(song.getArtist());

                int duration = audioPlayerService.getDuration();
                playerTotalTime.setText(formatTime(duration));
                if (duration > 0) {
                    playerSeekBar.setMax(duration);
                }
            }
            updatePlayButton();
            updatePlayModeButton();
        }
    }

    private void updatePlayButton() {
        if (isServiceBound && audioPlayerService != null && audioPlayerService.isPlaying()) {
            btnPlayerPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnPlayerPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    private void updatePlayModeButton() {
        if (audioPlayerService != null) {
            AudioPlayerService.PlayMode mode = audioPlayerService.getPlayMode();
            int primary = getResources().getColor(R.color.colorPrimary);

            switch (mode) {
                case SHUFFLE:
                    btnPlayerPlayMode.setImageResource(R.drawable.ic_shuffle);
                    btnPlayerPlayMode.setColorFilter(primary);
                    break;
                case REPEAT_ONE:
                    btnPlayerPlayMode.setImageResource(R.drawable.ic_repeat_one);
                    btnPlayerPlayMode.setColorFilter(primary);
                    break;
                default:
                    btnPlayerPlayMode.setImageResource(R.drawable.ic_repeat);
                    btnPlayerPlayMode.setColorFilter(primary);
                    break;
            }
        }
    }

    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playbackUpdateHandler != null && playbackUpdateRunnable != null) {
            playbackUpdateHandler.removeCallbacks(playbackUpdateRunnable);
        }
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}
