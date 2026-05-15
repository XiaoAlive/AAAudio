package com.example.aaaudio.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.example.aaaudio.model.Song;
import java.util.List;

public class AudioPlayerManager {
    private static final String TAG = "AudioPlayerManager";
    
    private static AudioPlayerManager instance;
    private Context context;
    private AudioPlayerService audioPlayerService;
    private boolean isServiceBound;
    
    // LiveData用于观察播放状态
    public MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    public MutableLiveData<Song> currentSong = new MutableLiveData<>();
    public MutableLiveData<Integer> currentPosition = new MutableLiveData<>(0);
    public MutableLiveData<Integer> duration = new MutableLiveData<>(0);
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            AudioPlayerService.AudioPlayerBinder binder = (AudioPlayerService.AudioPlayerBinder) service;
            audioPlayerService = binder.getService();
            isServiceBound = true;
            
            // 更新初始状态
            updatePlaybackState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            audioPlayerService = null;
            isServiceBound = false;
        }
    };

    private AudioPlayerManager(Context context) {
        this.context = context.getApplicationContext();
        bindService();
    }

    public static synchronized AudioPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new AudioPlayerManager(context);
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
            updatePlaybackState();
        }
    }

    public void playPlaylist(List<Song> playlist, int position) {
        if (audioPlayerService != null && playlist != null && !playlist.isEmpty()) {
            audioPlayerService.playPlaylist(playlist, position);
            updatePlaybackState();
        }
    }

    public void playPause() {
        if (audioPlayerService != null) {
            audioPlayerService.playPause();
            updatePlaybackState();
        }
    }

    public void stop() {
        if (audioPlayerService != null) {
            audioPlayerService.stop();
            updatePlaybackState();
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
            updatePlaybackState();
        }
    }

    public void previous() {
        if (audioPlayerService != null) {
            audioPlayerService.previous();
            updatePlaybackState();
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

    public AudioPlayerService.PlayMode getPlayMode() {
        return audioPlayerService != null ? audioPlayerService.getPlayMode() : AudioPlayerService.PlayMode.SEQUENTIAL;
    }

    public void setPlayMode(AudioPlayerService.PlayMode playMode) {
        if (audioPlayerService != null) {
            audioPlayerService.setPlayMode(playMode);
        }
    }

    private void updatePlaybackState() {
        if (audioPlayerService != null) {
            isPlaying.postValue(audioPlayerService.isPlaying());
            currentSong.postValue(audioPlayerService.getCurrentSong());
            currentPosition.postValue(audioPlayerService.getCurrentPosition());
            duration.postValue(audioPlayerService.getDuration());
        }
    }

    public void refreshPlaybackState() {
        updatePlaybackState();
    }

    public boolean isServiceBound() {
        return isServiceBound;
    }
}