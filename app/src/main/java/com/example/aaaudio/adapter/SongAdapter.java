package com.example.aaaudio.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aaaudio.R;
import com.example.aaaudio.model.Song;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songList = new ArrayList<>();
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

    public void setOnSongClickListener(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Song> songs) {
        this.songList = songs != null ? songs : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Song> getCurrentList() {
        return new ArrayList<>(songList);
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.bind(song, listener);
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView artistText;
        private final TextView durationText;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.song_title);
            artistText = itemView.findViewById(R.id.song_artist);
            durationText = itemView.findViewById(R.id.song_duration);
        }

        void bind(final Song song, final OnSongClickListener listener) {
            titleText.setText(song.getTitle());
            artistText.setText(song.getArtist());
            durationText.setText(formatDuration(song.getDuration()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSongClick(song, getAdapterPosition());
                }
            });
        }

        private String formatDuration(long millis) {
            if (millis <= 0) return "00:00";
            int totalSeconds = (int) (millis / 1000);
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
