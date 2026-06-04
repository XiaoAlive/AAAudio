package com.example.aaaudio.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.aaaudio.MainActivity;
import com.example.aaaudio.R;
import com.example.aaaudio.model.Song;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioPlayerService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "AudioPlayerService";
    private static final String CHANNEL_ID = "audio_player_channel";
    private static final int NOTIFICATION_ID = 1;

    // 通知栏操作Action
    public static final String ACTION_PREVIOUS = "com.example.aaaudio.ACTION_PREVIOUS";
    public static final String ACTION_PLAY_PAUSE = "com.example.aaaudio.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.example.aaaudio.ACTION_NEXT";

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private PowerManager.WakeLock wakeLock;
    private MediaSessionCompat mediaSession;

    private List<Song> currentPlaylist;
    private int currentPosition;
    private PlayMode playMode;
    private boolean isPrepared;

    private final IBinder binder = new AudioPlayerBinder();

    public enum PlayMode {
        SEQUENTIAL, REPEAT_ONE, REPEAT_ALL, SHUFFLE
    }

    public class AudioPlayerBinder extends Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        
        initializeMediaPlayer();
        initializeAudioManager();
        initializeMediaSession();
        createNotificationChannel();
        acquireWakeLock();
        
        currentPlaylist = new ArrayList<>();
        playMode = PlayMode.SEQUENTIAL;
        currentPosition = -1;

        restoreState();
    }

    private void initializeMediaSession() {
        mediaSession = new MediaSessionCompat(this, TAG);
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                Log.d(TAG, "MediaSession: onPlay");
                if (!isPlaying() && isPrepared) {
                    mediaPlayer.start();
                    requestAudioFocus();
                    updateNotification();
                    updateMediaSessionState();
                }
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.d(TAG, "MediaSession: onPause");
                if (isPlaying()) {
                    mediaPlayer.pause();
                    updateNotification();
                    updateMediaSessionState();
                }
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                Log.d(TAG, "MediaSession: onSkipToNext");
                next();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                Log.d(TAG, "MediaSession: onSkipToPrevious");
                previous();
            }

            @Override
            public void onStop() {
                super.onStop();
                Log.d(TAG, "MediaSession: onStop");
                stop();
                updateNotification();
                updateMediaSessionState();
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
                Log.d(TAG, "MediaSession: onSeekTo " + pos);
                seekTo((int) pos);
            }
        });

        // 设置初始播放状态
        PlaybackStateCompat initialState = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build();
        mediaSession.setPlaybackState(initialState);
        mediaSession.setActive(true);
    }

    private void restoreState() {
        PlayerStateManager stateManager = PlayerStateManager.getInstance(this);
        if (stateManager.hasSavedState()) {
            List<Song> savedPlaylist = stateManager.getPlaylist();
            if (savedPlaylist != null && !savedPlaylist.isEmpty()) {
                currentPlaylist = savedPlaylist;
                currentPosition = stateManager.getPositionIndex();
                restorePlayProgress = stateManager.getProgress();
                restoreWasPlaying = stateManager.getWasPlaying();
                isRestoringFromSaved = true;
                String savedMode = stateManager.getPlayMode();

                try {
                    playMode = PlayMode.valueOf(savedMode);
                } catch (Exception e) {
                    playMode = PlayMode.SEQUENTIAL;
                }

                if (currentPosition >= 0 && currentPosition < currentPlaylist.size()) {
                    Song song = currentPlaylist.get(currentPosition);
                    try {
                        prepareAndPlay(song);
                    } catch (Exception e) {
                        Log.w(TAG, "Could not restore playback: " + e.getMessage());
                    }
                }
            }
        }
    }

    private int restorePlayProgress;
    private boolean restoreWasPlaying;
    private boolean isRestoringFromSaved;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "Received action: " + action);
            switch (action) {
                case ACTION_PREVIOUS:
                    previous();
                    break;
                case ACTION_PLAY_PAUSE:
                    playPause();
                    break;
                case ACTION_NEXT:
                    next();
                    break;
            }
        }

        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");

        saveState();
        
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        
        releaseMediaPlayer();
        releaseAudioFocus();
        releaseWakeLock();
    }

    private void initializeMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mediaPlayer.setAudioAttributes(audioAttributes);
    }

    private void initializeAudioManager() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "音频播放器",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("音频播放器通知频道");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        // 获取当前歌曲信息
        Song currentSong = getCurrentSong();
        String title = currentSong != null ? currentSong.getTitle() : "音频播放器";
        String artist = currentSong != null ? currentSong.getArtist() : "正在播放音乐";

        // 上一首按钮
        PendingIntent prevIntent = PendingIntent.getService(this, 1,
                new Intent(ACTION_PREVIOUS, null, this, AudioPlayerService.class),
                PendingIntent.FLAG_IMMUTABLE);

        // 播放/暂停按钮
        boolean isPlaying = isPlaying();
        int playIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        String playText = isPlaying ? "暂停" : "播放";
        PendingIntent playPauseIntent = PendingIntent.getService(this, 2,
                new Intent(ACTION_PLAY_PAUSE, null, this, AudioPlayerService.class),
                PendingIntent.FLAG_IMMUTABLE);

        // 下一首按钮
        PendingIntent nextIntent = PendingIntent.getService(this, 3,
                new Intent(ACTION_NEXT, null, this, AudioPlayerService.class),
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 确保锁屏可见
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(R.drawable.ic_previous, "上一首", prevIntent)
                .addAction(playIcon, playText, playPauseIntent)
                .addAction(R.drawable.ic_skip_next, "下一首", nextIntent);

        return builder.build();
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    // 播放控制方法
    public void playSong(Song song) {
        if (song == null) return;

        List<Song> oldPlaylist = currentPlaylist;
        int oldPosition = currentPosition;

        currentPlaylist = new ArrayList<>();
        currentPlaylist.add(song);
        currentPosition = 0;

        prepareAndPlay(song);
        savePlaylistState();
        saveOldPlaylistToHistory(oldPlaylist, oldPosition);
    }

    public void playPlaylist(List<Song> playlist, int position) {
        if (playlist == null || playlist.isEmpty()) return;

        if (isSamePlaylist(playlist)) {
            currentPosition = Math.max(0, Math.min(position, playlist.size() - 1));
            prepareAndPlay(currentPlaylist.get(currentPosition));
            savePlaylistState();
            return;
        }

        List<Song> oldPlaylist = currentPlaylist;
        int oldPosition = currentPosition;

        currentPlaylist = new ArrayList<>(playlist);
        currentPosition = Math.max(0, Math.min(position, playlist.size() - 1));

        prepareAndPlay(currentPlaylist.get(currentPosition));
        savePlaylistState();
        saveOldPlaylistToHistory(oldPlaylist, oldPosition);
    }

    private boolean isSamePlaylist(List<Song> playlist) {
        if (currentPlaylist == null || playlist == null) return false;
        if (currentPlaylist.size() != playlist.size()) return false;
        if (currentPlaylist.isEmpty()) return false;
        return currentPlaylist.get(0).getPath().equals(playlist.get(0).getPath());
    }

    private void saveOldPlaylistToHistory(List<Song> oldPlaylist, int oldPosition) {
        if (oldPlaylist != null && !oldPlaylist.isEmpty()) {
            PlayHistoryManager.getInstance(this).saveCurrentPlaylist(oldPlaylist, oldPosition);
        }
    }

    private void savePlaylistState() {
        PlayerStateManager stateManager = PlayerStateManager.getInstance(this);
        stateManager.savePlaylist(currentPlaylist);
        stateManager.savePositionIndex(currentPosition);
        stateManager.saveProgress(mediaPlayer != null && isPrepared ? mediaPlayer.getCurrentPosition() : 0);
        stateManager.savePlayMode(playMode.name());
        stateManager.saveIsPlaying(mediaPlayer != null && mediaPlayer.isPlaying());
        if (getCurrentSong() != null) {
            stateManager.saveCurrentSongInfo(getCurrentSong().getTitle(), getCurrentSong().getArtist());
        }
    }

    private void prepareAndPlay(Song song) {
        if (song == null) return;
        
        try {
            releaseMediaPlayer();
            initializeMediaPlayer();
            
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(song.getPath()));
            mediaPlayer.prepareAsync();
            
            requestAudioFocus();
        } catch (IOException e) {
            Log.e(TAG, "播放歌曲失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void playPause() {
        if (mediaPlayer == null) return;
        
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            saveState();
        } else if (isPrepared) {
            mediaPlayer.start();
            requestAudioFocus();
        }
        
        updateNotification();
    }

    private void saveState() {
        PlayerStateManager stateManager = PlayerStateManager.getInstance(this);
        stateManager.saveFullState(
                currentPlaylist,
                currentPosition,
                mediaPlayer != null && isPrepared ? mediaPlayer.getCurrentPosition() : 0,
                playMode.name(),
                getCurrentSong(),
                mediaPlayer != null && mediaPlayer.isPlaying()
        );
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPrepared = false;
        }
        releaseAudioFocus();
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(position);
            PlayerStateManager.getInstance(this).saveProgress(position);
        }
    }

    public void next() {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;
        
        switch (playMode) {
            case SHUFFLE:
                currentPosition = (int) (Math.random() * currentPlaylist.size());
                break;
            default:
                currentPosition = (currentPosition + 1) % currentPlaylist.size();
                break;
        }
        
        prepareAndPlay(currentPlaylist.get(currentPosition));
        savePlaylistState();
    }

    public void previous() {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;
        
        currentPosition = (currentPosition - 1 + currentPlaylist.size()) % currentPlaylist.size();
        prepareAndPlay(currentPlaylist.get(currentPosition));
        savePlaylistState();
    }

    // MediaPlayer 回调
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "MediaPlayer prepared");
        isPrepared = true;

        if (restorePlayProgress > 0 && restorePlayProgress < mp.getDuration()) {
            mp.seekTo(restorePlayProgress);
            restorePlayProgress = 0;
        }

        mp.start();

        if (isRestoringFromSaved && !restoreWasPlaying) {
            mp.pause();
        }
        isRestoringFromSaved = false;
        restoreWasPlaying = false;

        savePlaylistState();

        updateNotification();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "MediaPlayer completion");
        
        if (playMode == PlayMode.REPEAT_ONE) {
            mp.seekTo(0);
            mp.start();
        } else {
            next();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
        return false;
    }

    // AudioFocus 回调
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer != null && !mediaPlayer.isPlaying() && isPrepared) {
                    mediaPlayer.start();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }

    private boolean requestAudioFocus() {
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            result = audioManager.requestAudioFocus(this,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
        }
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification());
        }
        updateMediaSessionState();
    }

    private void updateMediaSessionState() {
        if (mediaSession == null) return;
        
        // 更新播放状态
        int state = PlaybackStateCompat.STATE_NONE;
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                state = PlaybackStateCompat.STATE_PLAYING;
            } else if (isPrepared) {
                state = PlaybackStateCompat.STATE_PAUSED;
            }
        }
        
        long position = mediaPlayer != null && isPrepared ? mediaPlayer.getCurrentPosition() : 0;
        float speed = mediaPlayer != null && mediaPlayer.isPlaying() ? 1.0f : 0f;
        
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setState(state, position, speed)
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build();
        mediaSession.setPlaybackState(playbackState);
        
        // 更新媒体元数据（歌曲信息）
        Song currentSong = getCurrentSong();
        if (currentSong != null) {
            MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.getTitle() != null ? currentSong.getTitle() : "")
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.getArtist() != null ? currentSong.getArtist() : "")
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.getAlbum() != null ? currentSong.getAlbum() : "")
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer != null && isPrepared ? mediaPlayer.getDuration() : 0L);
            
            mediaSession.setMetadata(metadataBuilder.build());
        }
        
        mediaSession.setActive(true);
    }

    // 获取播放状态
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer != null && isPrepared ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null && isPrepared ? mediaPlayer.getDuration() : 0;
    }

    public Song getCurrentSong() {
        if (currentPlaylist != null && currentPosition >= 0 && currentPosition < currentPlaylist.size()) {
            return currentPlaylist.get(currentPosition);
        }
        return null;
    }

    public List<Song> getCurrentPlaylist() {
        return currentPlaylist != null ? new ArrayList<>(currentPlaylist) : new ArrayList<>();
    }

    public int getCurrentPlaylistPosition() {
        return currentPosition;
    }

    public PlayMode getPlayMode() {
        return playMode;
    }

    public void setPlayMode(PlayMode playMode) {
        this.playMode = playMode;
        PlayerStateManager.getInstance(this).savePlayMode(playMode.name());
    }
}