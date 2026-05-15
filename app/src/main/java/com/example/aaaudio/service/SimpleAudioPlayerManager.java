package com.example.aaaudio.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import com.example.aaaudio.model.Song;
import java.util.ArrayList;
import java.util.List;

public class SimpleAudioPlayerManager {
    private static final String TAG = "SimpleAudioMgr";
    
    private static SimpleAudioPlayerManager instance;
    private Context context;
    private AudioPlayerService audioPlayerService;
    private boolean isServiceBound;
    
    // 播放状态回调接口
    public interface PlaybackStateListener {
        void onPlaybackStateChanged(boolean isPlaying, Song currentSong);
    }
    
    private PlaybackStateListener playbackStateListener;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            AudioPlayerService.AudioPlayerBinder binder = (AudioPlayerService.AudioPlayerBinder) service;
            audioPlayerService = binder.getService();
            isServiceBound = true;
            
            // 通知监听器
            if (playbackStateListener != null) {
                playbackStateListener.onPlaybackStateChanged(
                    audioPlayerService.isPlaying(), 
                    audioPlayerService.getCurrentSong()
                );
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            audioPlayerService = null;
            isServiceBound = false;
        }
    };

    private SimpleAudioPlayerManager(Context context) {
        this.context = context.getApplicationContext();
        bindService();
    }

    public static synchronized SimpleAudioPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new SimpleAudioPlayerManager(context);
        }
        return instance;
    }

    private void bindService() {
        Intent intent = new Intent(context, AudioPlayerService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        context.startService(intent);
    }

    public void unbindService() {
        if (isServiceBound) {
            context.unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    // 播放控制方法
    public void playSong(Song song) {
        if (audioPlayerService != null) {
            audioPlayerService.playSong(song);
            notifyPlaybackStateChanged();
        }
    }

    public void playPlaylist(List<Song> playlist, int position) {
        if (audioPlayerService != null && playlist != null && !playlist.isEmpty()) {
            audioPlayerService.playPlaylist(playlist, position);
            notifyPlaybackStateChanged();
        }
    }

    public void playPause() {
        if (audioPlayerService != null) {
            audioPlayerService.playPause();
            notifyPlaybackStateChanged();
        }
    }

    public Song getSavedSong() {
        String title = PlayerStateManager.getInstance(context).getSavedSongTitle();
        if (title == null) return null;
        Song song = new Song(title,
                PlayerStateManager.getInstance(context).getSavedSongArtist() != null
                        ? PlayerStateManager.getInstance(context).getSavedSongArtist() : "",
                "", "", 0);
        return song;
    }

    public boolean hasSavedState() {
        return PlayerStateManager.getInstance(context).hasSavedState();
    }

    public void stop() {
        if (audioPlayerService != null) {
            audioPlayerService.stop();
            notifyPlaybackStateChanged();
        }
    }

    public void seekTo(int position) {
        if (audioPlayerService != null) {
            audioPlayerService.seekTo(position);
        }
    }

    public void next() {
        if (audioPlayerService != null) {
            audioPlayerService.next();
            notifyPlaybackStateChanged();
        }
    }

    public void previous() {
        if (audioPlayerService != null) {
            audioPlayerService.previous();
            notifyPlaybackStateChanged();
        }
    }

    // 获取播放状态
    public boolean isPlaying() {
        return audioPlayerService != null && audioPlayerService.isPlaying();
    }

    public int getCurrentPosition() {
        return audioPlayerService != null ? audioPlayerService.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return audioPlayerService != null ? audioPlayerService.getDuration() : 0;
    }

    public Song getCurrentSong() {
        return audioPlayerService != null ? audioPlayerService.getCurrentSong() : null;
    }

    public List<Song> getCurrentPlaylist() {
        return audioPlayerService != null ? audioPlayerService.getCurrentPlaylist() : new ArrayList<>();
    }

    public int getCurrentPlaylistPosition() {
        return audioPlayerService != null ? audioPlayerService.getCurrentPlaylistPosition() : -1;
    }

    public AudioPlayerService.PlayMode getPlayMode() {
        return audioPlayerService != null ? audioPlayerService.getPlayMode() : AudioPlayerService.PlayMode.SEQUENTIAL;
    }

    public void setPlayMode(AudioPlayerService.PlayMode playMode) {
        if (audioPlayerService != null) {
            audioPlayerService.setPlayMode(playMode);
        }
    }

    public void setPlaybackStateListener(PlaybackStateListener listener) {
        this.playbackStateListener = listener;
    }

    private void notifyPlaybackStateChanged() {
        if (playbackStateListener != null) {
            playbackStateListener.onPlaybackStateChanged(
                isPlaying(), 
                getCurrentSong()
            );
        }
    }

    public boolean isServiceBound() {
        return isServiceBound;
    }
}