package com.example.aaaudio.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aaaudio.R;
import com.example.aaaudio.adapter.QueueSongAdapter;
import com.example.aaaudio.model.Song;
import com.example.aaaudio.service.SimpleAudioPlayerManager;

import java.util.List;

public class QueueCurrentFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private QueueSongAdapter adapter;
    private SimpleAudioPlayerManager audioPlayerManager;
    private PlaylistQueueBottomSheet parentSheet;

    public static QueueCurrentFragment newInstance() {
        return new QueueCurrentFragment();
    }

    public void setParentSheet(PlaylistQueueBottomSheet sheet) {
        this.parentSheet = sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_queue_songs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.queue_recycler_view);
        emptyText = view.findViewById(R.id.empty_text);

        adapter = new QueueSongAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnQueueSongClickListener(new QueueSongAdapter.OnQueueSongClickListener() {
            @Override
            public void onSongClick(Song song, int position) {
                if (audioPlayerManager != null && audioPlayerManager.isServiceBound()) {
                    List<Song> currentList = audioPlayerManager.getCurrentPlaylist();
                    if (!currentList.isEmpty()) {
                        audioPlayerManager.playPlaylist(currentList, position);
                        loadPlaylist();
                    }
                }
            }

            @Override
            public void onActionClick(Song song, int position) {
            }
        });

        audioPlayerManager = SimpleAudioPlayerManager.getInstance(requireContext());
        loadPlaylist();
    }

    public void loadPlaylist() {
        if (audioPlayerManager != null && audioPlayerManager.isServiceBound()) {
            List<Song> playlist = audioPlayerManager.getCurrentPlaylist();
            int currentPos = audioPlayerManager.getCurrentPlaylistPosition();
            adapter.submitList(playlist);
            adapter.setHighlightPosition(currentPos);

            if (playlist.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
            } else {
                emptyText.setVisibility(View.GONE);
            }

            if (parentSheet != null) {
                parentSheet.updatePlayModeText(playlist.size());
            }
        }
    }
}
