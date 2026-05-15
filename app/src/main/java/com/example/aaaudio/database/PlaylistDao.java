package com.example.aaaudio.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;
import com.example.aaaudio.model.Playlist;
import com.example.aaaudio.model.PlaylistSongCrossRef;
import com.example.aaaudio.model.PlaylistWithSongs;
import com.example.aaaudio.model.Song;
import java.util.List;

@Dao
public interface PlaylistDao {
    @Insert
    long insert(Playlist playlist);

    @Update
    void update(Playlist playlist);

    @Delete
    void delete(Playlist playlist);

    @Query("SELECT * FROM playlists ORDER BY updateTime DESC")
    LiveData<List<Playlist>> getAllPlaylists();

    @Query("SELECT * FROM playlists WHERE id = :id")
    LiveData<Playlist> getPlaylistById(long id);

    @Query("UPDATE playlists SET songCount = songCount + 1, updateTime = :updateTime WHERE id = :playlistId")
    void incrementSongCount(long playlistId, long updateTime);

    @Query("UPDATE playlists SET songCount = songCount - 1, updateTime = :updateTime WHERE id = :playlistId")
    void decrementSongCount(long playlistId, long updateTime);

    // 关联表操作
    @Insert
    void insertPlaylistSongCrossRef(PlaylistSongCrossRef crossRef);

    @Delete
    void deletePlaylistSongCrossRef(PlaylistSongCrossRef crossRef);

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    void deletePlaylistSongCrossRef(long playlistId, long songId);

    @Query("SELECT * FROM playlist_song_cross_ref WHERE playlistId = :playlistId ORDER BY position ASC")
    List<PlaylistSongCrossRef> getPlaylistSongs(long playlistId);

    @Query("SELECT MAX(position) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    int getMaxPosition(long playlistId);

    @Query("UPDATE playlist_song_cross_ref SET position = position + 1 WHERE playlistId = :playlistId AND position >= :fromPosition")
    void shiftPositionsUp(long playlistId, int fromPosition);

    @Query("UPDATE playlist_song_cross_ref SET position = position - 1 WHERE playlistId = :playlistId AND position > :fromPosition")
    void shiftPositionsDown(long playlistId, int fromPosition);

    @Query("UPDATE playlist_song_cross_ref SET position = :newPosition WHERE playlistId = :playlistId AND songId = :songId")
    void updateSongPosition(long playlistId, long songId, int newPosition);

    // 关联查询
    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    LiveData<Playlist> getPlaylistByIdWithSongs(long playlistId);

    @Transaction
    @Query("SELECT songs.* FROM songs " +
           "INNER JOIN playlist_song_cross_ref ON songs.id = playlist_song_cross_ref.songId " +
           "WHERE playlist_song_cross_ref.playlistId = :playlistId " +
           "ORDER BY playlist_song_cross_ref.position ASC")
    LiveData<List<Song>> getSongsInPlaylist(long playlistId);

    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    LiveData<Integer> getSongCountInPlaylist(long playlistId);
}