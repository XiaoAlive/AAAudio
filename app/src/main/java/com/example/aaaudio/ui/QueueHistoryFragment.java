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
import com.example.aaaudio.service.PlayHistoryManager;
import com.example.aaaudio.service.SimpleAudioPlayerManager;

import java.util.List;

public class QueueHistoryFragment extends Fragment {

    private static final String ARG_TYPE = "history_type";
    public static final int TYPE_LAST = 0;
    public static final int TYPE_EARLIER = 1;

    private RecyclerView recyclerView;
    private TextView emptyText;
    private QueueSongAdapter adapter;
    private int historyType;
    private PlaylistQueueBottomSheet parentSheet;

    public static QueueHistoryFragment newInstance(int type) {
        QueueHistoryFragment fragment = new QueueHistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    public void setParentSheet(PlaylistQueueBottomSheet sheet) {
        this.parentSheet = sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            historyType = getArguments().getInt(ARG_TYPE, TYPE_LAST);
        }
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
                continuePlaylist(position);
            }

            @Override
            public void onActionClick(Song song, int position) {
            }
        });

        loadHistory();
    }

    public void resumePlayback() {
        PlayHistoryManager historyManager = PlayHistoryManager.getInstance(requireContext());
        int resumePosition = historyType == TYPE_LAST
                ? historyManager.getLastPlayedPosition()
                : historyManager.getEarlierPlayedPosition();
        continuePlaylist(resumePosition);
    }

    private void continuePlaylist(int position) {
        SimpleAudioPlayerManager manager = SimpleAudioPlayerManager.getInstance(requireContext());
        List<Song> historySongs = adapter.getCurrentList();
        if (!historySongs.isEmpty()) {
            int safePosition = Math.max(0, Math.min(position, historySongs.size() - 1));
            manager.playPlaylist(historySongs, safePosition);
            if (parentSheet != null) {
                parentSheet.switchToCurrentTab();
            }
        }
    }

    public void loadHistory() {
        PlayHistoryManager historyManager = PlayHistoryManager.getInstance(requireContext());
        List<Song> historySongs;
        int lastPlayedPos;

        if (historyType == TYPE_LAST) {
            historySongs = historyManager.getLastPlaylist();
            lastPlayedPos = historyManager.getLastPlayedPosition();
        } else {
            historySongs = historyManager.getEarlierPlaylist();
            lastPlayedPos = historyManager.getEarlierPlayedPosition();
        }

        adapter.submitList(historySongs);
        adapter.setLastPlayedPosition(lastPlayedPos);

        if (historySongs.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(historyType == TYPE_LAST ? "暂无上次播放记录" : "暂无更早播放记录");
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }
}
