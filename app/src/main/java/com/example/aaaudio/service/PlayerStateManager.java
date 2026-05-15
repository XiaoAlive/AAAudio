package com.example.aaaudio.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.aaaudio.model.Song;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlayerStateManager {

    private static final String PREFS_NAME = "player_state";
    private static final String KEY_PLAYLIST = "playlist";
    private static final String KEY_POSITION_INDEX = "position_index";
    private static final String KEY_PROGRESS = "progress";
    private static final String KEY_PLAY_MODE = "play_mode";
    private static final String KEY_CURRENT_SONG_TITLE = "current_song_title";
    private static final String KEY_CURRENT_SONG_ARTIST = "current_song_artist";
    private static final String KEY_IS_PLAYING = "is_playing";

    private static PlayerStateManager instance;
    private final SharedPreferences prefs;

    private PlayerStateManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PlayerStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new PlayerStateManager(context);
        }
        return instance;
    }

    public void savePlaylist(List<Song> playlist) {
        prefs.edit().putString(KEY_PLAYLIST, serializeSongs(playlist)).apply();
    }

    public List<Song> getPlaylist() {
        return deserializeSongs(prefs.getString(KEY_PLAYLIST, null));
    }

    public void savePositionIndex(int index) {
        prefs.edit().putInt(KEY_POSITION_INDEX, index).apply();
    }

    public int getPositionIndex() {
        return prefs.getInt(KEY_POSITION_INDEX, -1);
    }

    public void saveProgress(int progress) {
        prefs.edit().putInt(KEY_PROGRESS, progress).apply();
    }

    public int getProgress() {
        return prefs.getInt(KEY_PROGRESS, 0);
    }

    public boolean getWasPlaying() {
        return prefs.getBoolean(KEY_IS_PLAYING, false);
    }

    public void saveIsPlaying(boolean isPlaying) {
        prefs.edit().putBoolean(KEY_IS_PLAYING, isPlaying).apply();
    }

    public void savePlayMode(String mode) {
        prefs.edit().putString(KEY_PLAY_MODE, mode).apply();
    }

    public String getPlayMode() {
        return prefs.getString(KEY_PLAY_MODE, "SEQUENTIAL");
    }

    public void saveCurrentSongInfo(String title, String artist) {
        prefs.edit()
                .putString(KEY_CURRENT_SONG_TITLE, title != null ? title : "")
                .putString(KEY_CURRENT_SONG_ARTIST, artist != null ? artist : "")
                .apply();
    }

    public String getSavedSongTitle() {
        return prefs.getString(KEY_CURRENT_SONG_TITLE, null);
    }

    public String getSavedSongArtist() {
        return prefs.getString(KEY_CURRENT_SONG_ARTIST, null);
    }

    public void saveFullState(List<Song> playlist, int positionIndex, int progress, String playMode, Song currentSong, boolean isPlaying) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_PLAYLIST, serializeSongs(playlist));
        editor.putInt(KEY_POSITION_INDEX, positionIndex);
        editor.putInt(KEY_PROGRESS, progress);
        editor.putString(KEY_PLAY_MODE, playMode);
        editor.putBoolean(KEY_IS_PLAYING, isPlaying);
        if (currentSong != null) {
            editor.putString(KEY_CURRENT_SONG_TITLE, currentSong.getTitle() != null ? currentSong.getTitle() : "");
            editor.putString(KEY_CURRENT_SONG_ARTIST, currentSong.getArtist() != null ? currentSong.getArtist() : "");
        }
        editor.apply();
    }

    public void clearState() {
        prefs.edit().clear().apply();
    }

    public boolean hasSavedState() {
        return prefs.contains(KEY_PLAYLIST);
    }

    private String serializeSongs(List<Song> songs) {
        try {
            JSONArray array = new JSONArray();
            for (Song song : songs) {
                JSONObject obj = new JSONObject();
                obj.put("id", song.getId());
                obj.put("title", song.getTitle() != null ? song.getTitle() : "");
                obj.put("artist", song.getArtist() != null ? song.getArtist() : "");
                obj.put("album", song.getAlbum() != null ? song.getAlbum() : "");
                obj.put("path", song.getPath() != null ? song.getPath() : "");
                obj.put("duration", song.getDuration());
                array.put(obj);
            }
            return array.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Song> deserializeSongs(String json) {
        List<Song> songs = new ArrayList<>();
        if (json == null) return songs;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Song song = new Song(
                        obj.optString("title", ""),
                        obj.optString("artist", ""),
                        obj.optString("album", ""),
                        obj.optString("path", ""),
                        obj.optLong("duration", 0)
                );
                song.setId(obj.optLong("id", 0));
                songs.add(song);
            }
        } catch (Exception ignored) {
        }
        return songs;
    }
}
