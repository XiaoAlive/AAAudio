package com.example.aaaudio.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.aaaudio.model.Song;
import java.util.List;

@Dao
public interface SongDao {
    @Insert
    long insert(Song song);

    @Update
    void update(Song song);

    @Delete
    void delete(Song song);

    @Query("SELECT * FROM songs ORDER BY title ASC")
    LiveData<List<Song>> getAllSongs();

    @Query("SELECT * FROM songs")
    List<Song> getAllSongsSync();

    @Query("SELECT * FROM songs WHERE id = :id")
    LiveData<Song> getSongById(long id);

    @Query("SELECT * FROM songs WHERE path = :path")
    Song getSongByPath(String path);

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY title ASC")
    LiveData<List<Song>> getSongsByArtist(String artist);

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY title ASC")
    LiveData<List<Song>> getSongsByAlbum(String album);

    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    LiveData<List<String>> getAllArtists();

    @Query("SELECT DISTINCT album FROM songs ORDER BY album ASC")
    LiveData<List<String>> getAllAlbums();

    @Query("SELECT COUNT(*) FROM songs")
    LiveData<Integer> getSongCount();

    @Query("DELETE FROM songs")
    void deleteAllSongs();
}