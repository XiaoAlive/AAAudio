package com.example.aaaudio.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aaaudio.R;
import com.example.aaaudio.adapter.PlaylistAdapter;
import com.example.aaaudio.database.AppDatabase;
import com.example.aaaudio.database.PlaylistDao;
import com.example.aaaudio.model.Playlist;

import java.util.List;

public class PlaylistsFragment extends Fragment {

    private RecyclerView recyclerView;
    private View btnAddPlaylist;
    private PlaylistAdapter adapter;
    private PlaylistDao playlistDao;

    public PlaylistsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlists, container, false);

        recyclerView = view.findViewById(R.id.playlists_recycler_view);
        btnAddPlaylist = view.findViewById(R.id.btn_add_playlist);

        playlistDao = AppDatabase.getDatabase(requireContext()).playlistDao();

        adapter = new PlaylistAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        setupAddButton();
        setupPlaylistClick();
        observePlaylists();

        return view;
    }

    private void setupAddButton() {
        btnAddPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
    }

    private void setupPlaylistClick() {
        adapter.setOnPlaylistClickListener(playlist -> {
            Intent intent = new Intent(getActivity(), PlaylistDetailActivity.class);
            intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.getId());
            startActivity(intent);
        });
    }

    private void observePlaylists() {
        playlistDao.getAllPlaylists().observe(getViewLifecycleOwner(), new Observer<List<Playlist>>() {
            @Override
            public void onChanged(List<Playlist> playlists) {
                if (playlists != null) {
                    adapter.submitList(playlists);
                }
            }
        });
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("新建歌单");

        final EditText input = new EditText(requireContext());
        input.setHint("歌单名称（最多20个字符）");
        input.setMaxLines(1);
        int paddingDp = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (name.length() > 20) {
                Toast.makeText(getContext(), "名称不能超过20个字符", Toast.LENGTH_SHORT).show();
                return;
            }
            createPlaylist(name);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void createPlaylist(final String name) {
        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... voids) {
                Playlist playlist = new Playlist(name);
                return playlistDao.insert(playlist);
            }

            @Override
            protected void onPostExecute(Long playlistId) {
                if (playlistId > 0) {
                    Toast.makeText(getContext(), "歌单\"" + name + "\"已创建", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getActivity(), PlaylistDetailActivity.class);
                    intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlistId);
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "创建失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }
}
