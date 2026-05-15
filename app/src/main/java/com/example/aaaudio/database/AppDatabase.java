package com.example.aaaudio.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;
import com.example.aaaudio.model.Song;
import com.example.aaaudio.model.Playlist;
import com.example.aaaudio.model.PlaylistSongCrossRef;

@Database(
    entities = {Song.class, Playlist.class, PlaylistSongCrossRef.class},
    version = 1,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract SongDao songDao();
    public abstract PlaylistDao playlistDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "audio_player_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}