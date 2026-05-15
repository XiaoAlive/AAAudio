package com.example.aaaudio.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;

@Entity(tableName = "playlist_song_cross_ref",
        primaryKeys = {"playlistId", "songId"},
        indices = {
            @Index("playlistId"),
            @Index("songId")
        },
        foreignKeys = {
            @ForeignKey(entity = Playlist.class,
                        parentColumns = "id",
                        childColumns = "playlistId",
                        onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = Song.class,
                        parentColumns = "id",
                        childColumns = "songId",
                        onDelete = ForeignKey.CASCADE)
        })
public class PlaylistSongCrossRef {
    private long playlistId;
    private long songId;
    private int position;
    private long addedTime;

    public PlaylistSongCrossRef() {}

    @Ignore
    public PlaylistSongCrossRef(long playlistId, long songId, int position) {
        this.playlistId = playlistId;
        this.songId = songId;
        this.position = position;
        this.addedTime = System.currentTimeMillis();
    }

    public long getPlaylistId() { return playlistId; }
    public void setPlaylistId(long playlistId) { this.playlistId = playlistId; }

    public long getSongId() { return songId; }
    public void setSongId(long songId) { this.songId = songId; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public long getAddedTime() { return addedTime; }
    public void setAddedTime(long addedTime) { this.addedTime = addedTime; }

    @Override
    public String toString() {
        return "PlaylistSongCrossRef{" +
                "playlistId=" + playlistId +
                ", songId=" + songId +
                ", position=" + position +
                ", addedTime=" + addedTime +
                '}';
    }
}