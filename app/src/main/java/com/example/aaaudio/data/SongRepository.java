package com.example.aaaudio.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.aaaudio.database.AppDatabase;
import com.example.aaaudio.database.SongDao;
import com.example.aaaudio.model.Song;
import com.example.aaaudio.scanner.MusicScanner;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SongRepository {
    private static final String TAG = "SongRepository";

    private static volatile SongRepository INSTANCE;

    private final SongDao songDao;
    private final MusicScanner musicScanner;
    private final ExecutorService executor;

    private SongRepository(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        this.songDao = database.songDao();
        this.musicScanner = new MusicScanner(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static SongRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SongRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SongRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<List<Song>> getAllSongs() {
        return songDao.getAllSongs();
    }

    public LiveData<List<String>> getAllArtists() {
        return songDao.getAllArtists();
    }

    public LiveData<List<String>> getAllAlbums() {
        return songDao.getAllAlbums();
    }

    public LiveData<Integer> getSongCount() {
        return songDao.getSongCount();
    }

    public void scanMusic(final ScanResultCallback callback) {
        executor.execute(() -> {
            try {
                List<Song> scannedSongs = musicScanner.scanAllMusic();

                Set<String> scannedPaths = new HashSet<>();
                for (Song song : scannedSongs) {
                    scannedPaths.add(song.getPath());
                }

                int insertedCount = 0;
                int duplicateCount = 0;

                for (Song song : scannedSongs) {
                    try {
                        Song existing = songDao.getSongByPath(song.getPath());
                        if (existing == null) {
                            songDao.insert(song);
                            insertedCount++;
                        } else {
                            duplicateCount++;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error inserting song: " + e.getMessage());
                    }
                }

                int removedCount = 0;
                List<Song> dbSongs = songDao.getAllSongsSync();
                for (Song dbSong : dbSongs) {
                    if (!scannedPaths.contains(dbSong.getPath())) {
                        File file = new File(dbSong.getPath());
                        if (!file.exists()) {
                            songDao.delete(dbSong);
                            removedCount++;
                        }
                    }
                }

                final int finalInserted = insertedCount;
                final int finalDuplicate = duplicateCount;
                final int total = scannedSongs.size();
                final int finalRemoved = removedCount;

                Log.d(TAG, "Scan result: " + total + " found, " +
                        finalInserted + " inserted, " + finalDuplicate + " duplicates, " +
                        finalRemoved + " removed");

                if (callback != null) {
                    callback.onScanComplete(total, finalInserted, finalDuplicate, finalRemoved);
                }

            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied during scan", e);
                if (callback != null) {
                    callback.onScanError("权限不足，请在设置中授予存储权限");
                }
            } catch (Exception e) {
                Log.e(TAG, "Scan failed", e);
                if (callback != null) {
                    callback.onScanError("扫描失败: " + e.getMessage());
                }
            }
        });
    }

    public interface ScanResultCallback {
        void onScanComplete(int totalFound, int inserted, int duplicates, int removed);
        void onScanError(String error);
    }
}
