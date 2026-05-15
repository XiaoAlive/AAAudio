package com.example.aaaudio.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aaaudio.R;

import java.util.ArrayList;
import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    private List<String> albumList = new ArrayList<>();

    public void submitList(List<String> albums) {
        this.albumList = albums != null ? albums : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        holder.bind(albumList.get(position));
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        private final TextView albumName;

        AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumName = itemView.findViewById(R.id.album_name);
        }

        void bind(String album) {
            albumName.setText(album);
        }
    }
}
