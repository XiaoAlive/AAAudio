package com.example.aaaudio.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aaaudio.R;
import com.example.aaaudio.model.Playlist;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlistList = new ArrayList<>();
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public void setOnPlaylistClickListener(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Playlist> playlists) {
        this.playlistList = playlists != null ? playlists : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlistList.get(position);
        holder.bind(playlist, listener);
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView songCountText;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.playlist_name);
            songCountText = itemView.findViewById(R.id.playlist_song_count);
        }

        void bind(final Playlist playlist, final OnPlaylistClickListener listener) {
            nameText.setText(playlist.getName());
            int count = playlist.getSongCount();
            songCountText.setText(count + " 首歌曲");

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });
        }
    }
}
