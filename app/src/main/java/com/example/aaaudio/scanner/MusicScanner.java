package com.example.aaaudio.scanner;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.example.aaaudio.model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MusicScanner {
    private static final String TAG = "MusicScanner";

    private static final String[] AUDIO_EXTENSIONS = {
        ".mp3", ".flac", ".wav", ".ogg", ".aac", ".m4a", ".wma", ".opus",
        ".wv", ".ape", ".tta", ".dts", ".ac3", ".eac3", ".amr", ".awb",
        ".mid", ".midi", ".xmf", ".rtttl", ".rtx", ".ota", ".imy",
        ".3gp", ".mp4", ".m4b", ".m4p", ".mka", ".ra", ".ram"
    };

    private static final String[] COMMON_AUDIO_MIMES = {
        "audio/mpeg", "audio/mp3", "audio/flac", "audio/wav",
        "audio/wave", "audio/ogg", "audio/aac", "audio/x-m4a",
        "audio/mp4", "audio/x-wav", "audio/x-flac", "audio/x-aac",
        "audio/x-ms-wma", "audio/opus", "audio/3gpp", "audio/amr",
        "audio/x-mpeg", "audio/vorbis", "audio/x-ms-wax",
        "audio/x-pn-wma", "audio/x-pn-realaudio",
        "application/ogg"
    };

    private final Context context;
    private final Uri audioCollectionUri;

    public MusicScanner(Context context) {
        this.context = context.getApplicationContext();
        this.audioCollectionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }

    public interface ScanCallback {
        void onScanProgress(int current, int total);
        void onScanComplete(List<Song> songs);
        void onScanError(String error);
    }

    public List<Song> scanAllMusic() {
        triggerMediaStoreScan();

        List<Song> songList = new ArrayList<>();
        Set<String> pathSet = new HashSet<>();

        List<Song> mimeResults = scanMediaStoreWithMimeFilter(pathSet);
        songList.addAll(mimeResults);

        List<Song> noFilterResults = scanMediaStoreNoFilter(pathSet);
        songList.addAll(noFilterResults);

        if (songList.isEmpty()) {
            Log.d(TAG, "MediaStore returned nothing, trying file system scan");
            songList.addAll(scanFileSystem());
        }

        Log.d(TAG, "Scan complete. Found " + songList.size() + " songs.");
        return songList;
    }

    private void triggerMediaStoreScan() {
        try {
            String[] paths = {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS).getAbsolutePath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
                Environment.getExternalStorageDirectory().getAbsolutePath()
            };
            MediaScannerConnection.scanFile(context, paths, null, null);
            Log.d(TAG, "MediaScanner triggered for " + paths.length + " directories");
        } catch (Exception e) {
            Log.w(TAG, "Could not trigger MediaScanner: " + e.getMessage());
        }
    }

    private List<Song> scanMediaStoreWithMimeFilter(Set<String> pathSet) {
        List<Song> songList = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA
        };

        StringBuilder mimeFilter = new StringBuilder();
        for (int i = 0; i < COMMON_AUDIO_MIMES.length; i++) {
            if (i > 0) mimeFilter.append(" OR ");
            mimeFilter.append(MediaStore.Audio.Media.MIME_TYPE).append(" = ?");
        }

        try (Cursor cursor = contentResolver.query(
                musicUri, projection, mimeFilter.toString(), COMMON_AUDIO_MIMES,
                MediaStore.Audio.Media.TITLE + " ASC")) {

            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "MIME filter found " + cursor.getCount() + " files");
                scanCursor(cursor, songList, pathSet);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied: " + e.getMessage());
        }

        return songList;
    }

    private List<Song> scanMediaStoreNoFilter(Set<String> pathSet) {
        List<Song> songList = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA
        };

        try (Cursor cursor = contentResolver.query(
                musicUri, projection, null, null,
                MediaStore.Audio.Media.TITLE + " ASC")) {

            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "No-filter query found " + cursor.getCount() + " files");
                scanCursor(cursor, songList, pathSet);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied: " + e.getMessage());
        }

        return songList;
    }

    private void scanCursor(Cursor cursor, List<Song> songList, Set<String> pathSet) {
        do {
            try {
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                Uri contentUri = ContentUris.withAppendedId(audioCollectionUri, id);

                if (path == null || path.isEmpty()) continue;
                if (pathSet.contains(path)) continue;
                if (!isAudioFile(path)) continue;

                pathSet.add(path);

                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                long fileSize = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
                String mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));
                long dateAdded = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED));
                long lastModified = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED));

                if (title == null || title.isEmpty()) {
                    title = path.substring(path.lastIndexOf('/') + 1);
                    int dotIndex = title.lastIndexOf('.');
                    if (dotIndex > 0) {
                        title = title.substring(0, dotIndex);
                    }
                }
                if (artist == null || artist.isEmpty()) {
                    artist = "未知艺术家";
                }
                if (album == null || album.isEmpty()) {
                    album = "未知专辑";
                }
                if (duration <= 0) {
                    duration = extractDurationFromUri(contentUri);
                }

                Song song = new Song(title, artist, album, path, duration);
                song.setFileSize(fileSize);
                song.setMimeType(mimeType);
                song.setDateAdded(dateAdded);
                song.setLastModified(lastModified);

                songList.add(song);

            } catch (Exception e) {
                Log.w(TAG, "Error processing song: " + e.getMessage());
            }
        } while (cursor.moveToNext());
    }

    private List<Song> scanFileSystem() {
        List<Song> songList = new ArrayList<>();
        Set<String> pathSet = new HashSet<>();

        File[] storageDirs = getStorageDirectories();
        for (File dir : storageDirs) {
            if (dir != null && dir.exists()) {
                scanDirectoryRecursive(dir, songList, pathSet);
            }
        }

        return songList;
    }

    private File[] getStorageDirectories() {
        List<File> dirs = new ArrayList<>();

        File internalStorage = Environment.getExternalStorageDirectory();
        if (internalStorage != null) {
            dirs.add(internalStorage);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externalFilesDirs = context.getExternalFilesDirs(null);
            if (externalFilesDirs != null) {
                for (File dir : externalFilesDirs) {
                    if (dir != null) {
                        File parent = dir.getParentFile();
                        if (parent != null && !dirs.contains(parent)) {
                            dirs.add(parent);
                        }
                    }
                }
            }
        }

        return dirs.toArray(new File[0]);
    }

    private void scanDirectoryRecursive(File directory, List<Song> songList, Set<String> pathSet) {
        if (directory == null || !directory.exists() || !directory.canRead()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                if (!file.getName().startsWith(".") && !file.getName().equals("Android")) {
                    scanDirectoryRecursive(file, songList, pathSet);
                }
            } else if (file.isFile()) {
                String path = file.getAbsolutePath();
                if (pathSet.contains(path)) continue;
                if (!isAudioFile(path)) continue;

                pathSet.add(path);

                String title = file.getName();
                int dotIndex = title.lastIndexOf('.');
                if (dotIndex > 0) {
                    title = title.substring(0, dotIndex);
                }

                long duration = extractDurationWithRetriever(path);

                Song song = new Song(title, "未知艺术家", "未知专辑", path, duration);
                song.setFileSize(file.length());
                song.setLastModified(file.lastModified());

                songList.add(song);
            }
        }
    }

    public void scanAllMusicAsync(final ScanCallback callback) {
        new Thread(() -> {
            try {
                List<Song> songs = scanAllMusic();
                if (callback != null) {
                    callback.onScanComplete(songs);
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Permission error: " + e.getMessage());
                if (callback != null) {
                    callback.onScanError("权限不足，无法扫描音乐文件");
                }
            } catch (Exception e) {
                Log.e(TAG, "Scan error: " + e.getMessage());
                if (callback != null) {
                    callback.onScanError("扫描失败: " + e.getMessage());
                }
            }
        }).start();
    }

    private long extractDurationFromUri(Uri contentUri) {
        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, contentUri);
            String durationStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                return Long.parseLong(durationStr);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not extract duration from URI: " + contentUri);
        } finally {
            if (retriever != null) {
                try {
                    retriever.release();
                } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private long extractDurationWithRetriever(String path) {
        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);
            String durationStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                return Long.parseLong(durationStr);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not extract duration for: " + path);
        } finally {
            if (retriever != null) {
                try {
                    retriever.release();
                } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private boolean isAudioFile(String path) {
        if (path == null) return false;
        String lowerPath = path.toLowerCase();
        for (String ext : AUDIO_EXTENSIONS) {
            if (lowerPath.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
