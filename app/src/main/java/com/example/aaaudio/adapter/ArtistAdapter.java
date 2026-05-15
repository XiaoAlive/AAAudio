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

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {

    private List<String> artistList = new ArrayList<>();

    public void submitList(List<String> artists) {
        this.artistList = artists != null ? artists : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artist, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        holder.bind(artistList.get(position));
    }

    @Override
    public int getItemCount() {
        return artistList.size();
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        private final TextView artistName;

        ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artist_name);
        }

        void bind(String artist) {
            artistName.setText(artist);
        }
    }
}
