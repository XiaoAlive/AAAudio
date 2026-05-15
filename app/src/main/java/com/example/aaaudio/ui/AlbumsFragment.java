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
import com.example.aaaudio.adapter.AlbumAdapter;
import com.example.aaaudio.data.SongRepository;

public class AlbumsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AlbumAdapter albumAdapter;
    private SongRepository songRepository;

    public AlbumsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_albums, container, false);

        recyclerView = view.findViewById(R.id.albums_recycler_view);
        albumAdapter = new AlbumAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(albumAdapter);

        songRepository = SongRepository.getInstance(requireContext());

        observeAlbums();

        return view;
    }

    private void observeAlbums() {
        songRepository.getAllAlbums().observe(getViewLifecycleOwner(), albums -> {
            if (albums != null) {
                albumAdapter.submitList(albums);
            }
        });
    }
}
