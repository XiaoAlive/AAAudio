package com.example.aaaudio.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "songs")
public class Song {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String artist;
    private String album;
    private String path;
    private long duration;
    private long fileSize;
    private String mimeType;
    private long dateAdded;
    private long lastModified;

    public Song() {}

    @Ignore
    public Song(String title, String artist, String album, String path, long duration) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.duration = duration;
        this.dateAdded = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    @Override
    public String toString() {
        return "Song{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", path='" + path + '\'' +
                ", duration=" + duration +
                '}';
    }
}