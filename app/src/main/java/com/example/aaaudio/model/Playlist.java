package com.example.aaaudio.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.aaaudio.database.Converters;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "playlists")
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String description;
    private long createTime;
    private long updateTime;
    private int songCount;
    private String coverPath;

    public Playlist() {}

    @Ignore
    public Playlist(String name) {
        this.name = name;
        this.createTime = System.currentTimeMillis();
        this.updateTime = this.createTime;
        this.songCount = 0;
    }

    @Ignore
    public Playlist(String name, String description) {
        this.name = name;
        this.description = description;
        this.createTime = System.currentTimeMillis();
        this.updateTime = this.createTime;
        this.songCount = 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }

    public int getSongCount() { return songCount; }
    public void setSongCount(int songCount) { this.songCount = songCount; }

    public String getCoverPath() { return coverPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }

    @Override
    public String toString() {
        return "Playlist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", songCount=" + songCount +
                '}';
    }
}