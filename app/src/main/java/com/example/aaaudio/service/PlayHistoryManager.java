package com.example.aaaudio.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.aaaudio.model.Song;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlayHistoryManager {

    private static final String PREFS_NAME = "play_history";
    private static final String KEY_LAST_PLAYLIST = "last_playlist";
    private static final String KEY_EARLIER_PLAYLIST = "earlier_playlist";
    private static final String KEY_LAST_POSITION = "last_position";
    private static final String KEY_EARLIER_POSITION = "earlier_position";

    private static PlayHistoryManager instance;
    private final SharedPreferences prefs;

    private PlayHistoryManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PlayHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new PlayHistoryManager(context);
        }
        return instance;
    }

    public void saveCurrentPlaylist(List<Song> playlist, int lastPlayedPosition) {
        if (playlist == null || playlist.isEmpty()) return;

        String lastJson = prefs.getString(KEY_LAST_PLAYLIST, null);

        SharedPreferences.Editor editor = prefs.edit();

        if (lastJson != null) {
            editor.putString(KEY_EARLIER_PLAYLIST, lastJson);
            editor.putInt(KEY_EARLIER_POSITION, prefs.getInt(KEY_LAST_POSITION, 0));
        }

        editor.putString(KEY_LAST_PLAYLIST, serializeSongs(playlist));
        editor.putInt(KEY_LAST_POSITION, lastPlayedPosition);
        editor.apply();
    }

    public List<Song> getLastPlaylist() {
        return deserializeSongs(prefs.getString(KEY_LAST_PLAYLIST, null));
    }

    public int getLastPlayedPosition() {
        return prefs.getInt(KEY_LAST_POSITION, 0);
    }

    public List<Song> getEarlierPlaylist() {
        return deserializeSongs(prefs.getString(KEY_EARLIER_PLAYLIST, null));
    }

    public int getEarlierPlayedPosition() {
        return prefs.getInt(KEY_EARLIER_POSITION, 0);
    }

    public void clearHistory() {
        prefs.edit()
                .remove(KEY_LAST_PLAYLIST)
                .remove(KEY_EARLIER_PLAYLIST)
                .remove(KEY_LAST_POSITION)
                .remove(KEY_EARLIER_POSITION)
                .apply();
    }

    private String serializeSongs(List<Song> songs) {
        try {
            JSONArray array = new JSONArray();
            for (Song song : songs) {
                JSONObject obj = new JSONObject();
                obj.put("title", song.getTitle() != null ? song.getTitle() : "");
                obj.put("artist", song.getArtist() != null ? song.getArtist() : "");
                obj.put("album", song.getAlbum() != null ? song.getAlbum() : "");
                obj.put("path", song.getPath() != null ? song.getPath() : "");
                obj.put("duration", song.getDuration());
                obj.put("id", song.getId());
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
