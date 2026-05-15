package com.example.aaaudio.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aaaudio.R;
import com.example.aaaudio.model.Song;

import java.util.ArrayList;
import java.util.List;

public class QueueSongAdapter extends RecyclerView.Adapter<QueueSongAdapter.ViewHolder> {

    private List<Song> songList = new ArrayList<>();
    private OnQueueSongClickListener listener;
    private boolean showActionButton;
    private int actionButtonIcon;
    private int highlightPosition = -1;
    private int lastPlayedPosition = -1;

    public interface OnQueueSongClickListener {
        void onSongClick(Song song, int position);
        void onActionClick(Song song, int position);
    }

    public void setOnQueueSongClickListener(OnQueueSongClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Song> songs) {
        this.songList = songs != null ? songs : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setShowActionButton(boolean show, int iconRes) {
        this.showActionButton = show;
        this.actionButtonIcon = iconRes;
        notifyDataSetChanged();
    }

    public void setHighlightPosition(int position) {
        this.highlightPosition = position;
        notifyDataSetChanged();
    }

    public void setLastPlayedPosition(int position) {
        this.lastPlayedPosition = position;
        notifyDataSetChanged();
    }

    public List<Song> getCurrentList() {
        return new ArrayList<>(songList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queue_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.titleText.setText(song.getTitle());
        holder.artistText.setText(song.getArtist());
        holder.indexText.setText(String.valueOf(position + 1));

        if (position == highlightPosition) {
            holder.titleText.setTextColor(holder.itemView.getContext().getColor(R.color.colorPrimary));
        } else {
            holder.titleText.setTextColor(holder.itemView.getContext().getColor(R.color.text_primary));
        }

        if (position == lastPlayedPosition && lastPlayedPosition >= 0) {
            holder.lastPlayedLabel.setVisibility(View.VISIBLE);
        } else {
            holder.lastPlayedLabel.setVisibility(View.GONE);
        }

        if (showActionButton) {
            holder.actionButton.setVisibility(View.VISIBLE);
            holder.actionButton.setImageResource(actionButtonIcon);
        } else {
            holder.actionButton.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(song, position);
            }
        });

        holder.actionButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActionClick(song, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView indexText;
        TextView titleText;
        TextView artistText;
        TextView lastPlayedLabel;
        ImageButton actionButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            indexText = itemView.findViewById(R.id.song_index);
            titleText = itemView.findViewById(R.id.song_title);
            artistText = itemView.findViewById(R.id.song_artist);
            lastPlayedLabel = itemView.findViewById(R.id.last_played_label);
            actionButton = itemView.findViewById(R.id.btn_item_action);
        }
    }
}
