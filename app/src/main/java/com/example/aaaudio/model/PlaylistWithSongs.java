package com.example.aaaudio.model;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class PlaylistWithSongs {
    @Embedded
    public Playlist playlist;
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = @androidx.room.Junction(
            value = PlaylistSongCrossRef.class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    public List<Song> songs;

    public PlaylistWithSongs() {}

    public PlaylistWithSongs(Playlist playlist, List<Song> songs) {
        this.playlist = playlist;
        this.songs = songs;
    }

    public int getSongCount() {
        return songs != null ? songs.size() : 0;
    }

    @Override
    public String toString() {
        return "PlaylistWithSongs{" +
                "playlist=" + playlist +
                ", songs=" + (songs != null ? songs.size() : 0) +
                '}';
    }
}