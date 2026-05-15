package com.example.aaaudio.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aaaudio.R;
import com.example.aaaudio.adapter.ArtistAdapter;
import com.example.aaaudio.data.SongRepository;

public class ArtistsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArtistAdapter artistAdapter;
    private SongRepository songRepository;

    public ArtistsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artists, container, false);

        recyclerView = view.findViewById(R.id.artists_recycler_view);
        artistAdapter = new ArtistAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(artistAdapter);

        songRepository = SongRepository.getInstance(requireContext());

        observeArtists();

        return view;
    }

    private void observeArtists() {
        songRepository.getAllArtists().observe(getViewLifecycleOwner(), artists -> {
            if (artists != null) {
                artistAdapter.submitList(artists);
            }
        });
    }
}
