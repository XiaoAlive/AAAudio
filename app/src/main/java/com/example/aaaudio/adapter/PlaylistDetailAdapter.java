package com.example.aaaudio.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aaaudio.R;
import com.example.aaaudio.model.Song;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistDetailAdapter extends RecyclerView.Adapter<PlaylistDetailAdapter.ViewHolder> {

    private List<Song> songList = new ArrayList<>();
    private boolean isBatchMode = false;
    private Set<Long> selectedIds = new HashSet<>();
    private long currentPlayingSongId = -1;
    private OnSongActionListener listener;

    public interface OnSongActionListener {
        void onSongClick(Song song, int position);
        void onDragStart(RecyclerView.ViewHolder viewHolder);
    }

    public void setOnSongActionListener(OnSongActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Song> songs) {
        this.songList = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Song> getCurrentList() {
        return new ArrayList<>(songList);
    }

    public void setCurrentPlayingSongId(long songId) {
        if (this.currentPlayingSongId != songId) {
            this.currentPlayingSongId = songId;
            notifyDataSetChanged();
        }
    }

    public void setBatchMode(boolean batchMode) {
        this.isBatchMode = batchMode;
        if (!batchMode) {
            selectedIds.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isBatchMode() {
        return isBatchMode;
    }

    public Set<Long> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    public void setSelectedIds(Set<Long> ids) {
        this.selectedIds = ids != null ? new HashSet<>(ids) : new HashSet<>();
        notifyDataSetChanged();
    }

    public void selectAll() {
        selectedIds.clear();
        for (Song song : songList) {
            selectedIds.add(song.getId());
        }
        notifyDataSetChanged();
    }

    public void deselectAll() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public boolean isAllSelected() {
        for (Song song : songList) {
            if (!selectedIds.contains(song.getId())) {
                return false;
            }
        }
        return !songList.isEmpty();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        Collections.swap(songList, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    public void onDragEnd() {
        notifyDataSetChanged();
    }

    public void removeSelected() {
        List<Song> toRemove = new ArrayList<>();
        for (Song song : songList) {
            if (selectedIds.contains(song.getId())) {
                toRemove.add(song);
            }
        }
        songList.removeAll(toRemove);
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public List<Song> getSelectedSongs() {
        List<Song> selected = new ArrayList<>();
        for (Song song : songList) {
            if (selectedIds.contains(song.getId())) {
                selected.add(song);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.bind(song, position, listener, isBatchMode, selectedIds, currentPlayingSongId);
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView artistText;
        private final CheckBox checkBox;
        private final ImageView dragHandle;
        private final MaterialCardView cardView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.song_title);
            artistText = itemView.findViewById(R.id.song_artist);
            checkBox = itemView.findViewById(R.id.checkbox_select);
            dragHandle = itemView.findViewById(R.id.drag_handle);
            cardView = (MaterialCardView) itemView;
        }

        void bind(final Song song, final int position, final OnSongActionListener listener,
                   boolean isBatchMode, Set<Long> selectedIds, long currentPlayingSongId) {
            titleText.setText(song.getTitle());
            artistText.setText(song.getArtist());

            checkBox.setVisibility(isBatchMode ? View.VISIBLE : View.GONE);
            dragHandle.setVisibility(isBatchMode ? View.VISIBLE : View.GONE);

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(selectedIds.contains(song.getId()));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedIds.add(song.getId());
                } else {
                    selectedIds.remove(song.getId());
                }
            });

            boolean isPlaying = !isBatchMode && song.getId() == currentPlayingSongId;
            if (isPlaying) {
                cardView.setCardBackgroundColor(itemView.getContext().getColor(R.color.highlight_bg));
                cardView.setStrokeColor(itemView.getContext().getColor(R.color.colorPrimary));
                cardView.setStrokeWidth(2);
                titleText.setTextColor(itemView.getContext().getColor(R.color.colorPrimaryDark));
            } else {
                cardView.setCardBackgroundColor(itemView.getContext().getColor(android.R.color.white));
                cardView.setStrokeColor(itemView.getContext().getColor(R.color.light_gray));
                cardView.setStrokeWidth(1);
                titleText.setTextColor(itemView.getContext().getColor(R.color.text_primary));
            }

            itemView.setOnClickListener(v -> {
                if (isBatchMode) {
                    checkBox.setChecked(!checkBox.isChecked());
                } else if (listener != null) {
                    listener.onSongClick(song, getAdapterPosition());
                }
            });

            dragHandle.setOnTouchListener((v, event) -> {
                if (isBatchMode && listener != null) {
                    listener.onDragStart(ViewHolder.this);
                }
                return false;
            });
        }
    }
}
