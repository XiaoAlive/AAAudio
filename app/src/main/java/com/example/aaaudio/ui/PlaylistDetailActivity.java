package com.example.aaaudio.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aaaudio.R;
import com.example.aaaudio.adapter.PlaylistDetailAdapter;
import com.example.aaaudio.database.AppDatabase;
import com.example.aaaudio.database.PlaylistDao;
import com.example.aaaudio.model.Playlist;
import com.example.aaaudio.model.PlaylistSongCrossRef;
import com.example.aaaudio.model.Song;
import com.example.aaaudio.service.SimpleAudioPlayerManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlaylistDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYLIST_ID = "playlist_id";

    private long playlistId;
    private Playlist playlist;
    private PlaylistDao playlistDao;

    private Toolbar toolbar;
    private TextView toolbarTitle;
    private TextView tvSongCount;
    private View emptyView;
    private LinearLayout batchModeBar;
    private CheckBox cbSelectAll;
    private MaterialButton btnDoneBatch;
    private RecyclerView recyclerView;

    private PlaylistDetailAdapter adapter;
    private SimpleAudioPlayerManager audioPlayerManager;
    private List<Song> currentSongs = new ArrayList<>();
    private ItemTouchHelper itemTouchHelper;

    private boolean isBatchMode = false;
    private List<Song> searchResultList;

    private ImageButton btnPlayPause, btnNext, btnPlaylist;
    private ImageView albumArt;
    private TextView bottomSongTitle, bottomSongArtist;
    private View bottomPlayerControl;

    private Handler playbackUpdateHandler;
    private Runnable playbackUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getLongExtra(EXTRA_PLAYLIST_ID, -1);
        if (playlistId == -1) {
            finish();
            return;
        }

        playlistDao = AppDatabase.getDatabase(this).playlistDao();
        audioPlayerManager = SimpleAudioPlayerManager.getInstance(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        setupBottomPlayer();
        startPlaybackUpdates();
        loadPlaylist();
        loadSongs();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        tvSongCount = findViewById(R.id.tv_song_count);
        emptyView = findViewById(R.id.empty_view);
        batchModeBar = findViewById(R.id.batch_mode_bar);
        cbSelectAll = findViewById(R.id.cb_select_all);
        btnDoneBatch = findViewById(R.id.btn_done_batch);
        recyclerView = findViewById(R.id.playlist_songs_recycler_view);
        bottomPlayerControl = findViewById(R.id.bottom_player_control);
        albumArt = findViewById(R.id.album_art);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);
        btnPlaylist = findViewById(R.id.btn_playlist);
        bottomSongTitle = findViewById(R.id.song_title);
        bottomSongArtist = findViewById(R.id.song_artist);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new PlaylistDetailAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                if (from >= 0 && to >= 0) {
                    adapter.onItemMove(from, to);
                }
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        };
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        adapter.setOnSongActionListener(new PlaylistDetailAdapter.OnSongActionListener() {
            @Override
            public void onSongClick(Song song, int position) {
                if (!currentSongs.isEmpty()) {
                    audioPlayerManager.playPlaylist(currentSongs, position);
                }
            }

            @Override
            public void onDragStart(RecyclerView.ViewHolder viewHolder) {
                itemTouchHelper.startDrag(viewHolder);
            }
        });
    }

    private void setupListeners() {
        findViewById(R.id.btn_play_all).setOnClickListener(v -> {
            if (currentSongs != null && !currentSongs.isEmpty()) {
                audioPlayerManager.playPlaylist(currentSongs, 0);
            }
        });

        findViewById(R.id.btn_batch).setOnClickListener(v -> toggleBatchMode());

        findViewById(R.id.btn_search).setOnClickListener(v -> showSearchDialog());

        findViewById(R.id.btn_manage).setOnClickListener(v -> showManageBottomSheet());

        btnDoneBatch.setOnClickListener(v -> exitBatchMode());

        findViewById(R.id.btn_batch_delete).setOnClickListener(v -> showBatchDeleteConfirm());

        cbSelectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    adapter.selectAll();
                } else {
                    adapter.deselectAll();
                }
            }
        });
    }

    private void setupBottomPlayer() {
        audioPlayerManager.setPlaybackStateListener(new SimpleAudioPlayerManager.PlaybackStateListener() {
            @Override
            public void onPlaybackStateChanged(boolean isPlaying, Song currentSong) {
                updateBottomPlayerUI(isPlaying, currentSong);
            }
        });

        boolean isPlaying = audioPlayerManager.isPlaying();
        Song currentSong = audioPlayerManager.getCurrentSong();
        updateBottomPlayerUI(isPlaying, currentSong);

        btnPlayPause.setOnClickListener(v -> {
            if (audioPlayerManager.isServiceBound()) {
                audioPlayerManager.playPause();
                updateBottomPlayerUI(audioPlayerManager.isPlaying(), audioPlayerManager.getCurrentSong());
            }
        });

        btnNext.setOnClickListener(v -> {
            if (audioPlayerManager.isServiceBound()) {
                audioPlayerManager.next();
            }
        });

        bottomPlayerControl.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
        });

        btnPlaylist.setOnClickListener(v -> {
            PlaylistQueueBottomSheet bottomSheet = new PlaylistQueueBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "PlaylistQueue");
        });
    }

    private void startPlaybackUpdates() {
        playbackUpdateHandler = new Handler();
        playbackUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (audioPlayerManager != null) {
                    updateBottomPlayerUI(audioPlayerManager.isPlaying(), audioPlayerManager.getCurrentSong());
                }
                playbackUpdateHandler.postDelayed(this, 500);
            }
        };
        playbackUpdateHandler.postDelayed(playbackUpdateRunnable, 500);
    }

    private void updateBottomPlayerUI(boolean isPlaying, Song currentSong) {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }

        if (currentSong != null) {
            bottomSongTitle.setText(currentSong.getTitle());
            bottomSongArtist.setText(currentSong.getArtist());
        } else {
            bottomSongTitle.setText("未在播放");
            bottomSongArtist.setText("选择歌曲开始播放");
        }
        bottomPlayerControl.setVisibility(View.VISIBLE);
    }

    private void loadPlaylist() {
        playlistDao.getPlaylistById(playlistId).observe(this, new Observer<Playlist>() {
            @Override
            public void onChanged(Playlist playlist) {
                if (playlist != null) {
                    PlaylistDetailActivity.this.playlist = playlist;
                    toolbarTitle.setText(playlist.getName());
                }
            }
        });
    }

    private void loadSongs() {
        LiveData<List<Song>> songsLiveData = playlistDao.getSongsInPlaylist(playlistId);
        songsLiveData.observe(this, new Observer<List<Song>>() {
            @Override
            public void onChanged(List<Song> songs) {
                if (songs != null) {
                    currentSongs = new ArrayList<>(songs);
                    adapter.submitList(currentSongs);
                    updateSongCount(songs.size());
                }
            }
        });
    }

    private void updateSongCount(int count) {
        if (count > 0) {
            tvSongCount.setVisibility(View.VISIBLE);
            tvSongCount.setText(count + " 首歌曲");
            emptyView.setVisibility(View.GONE);
        } else {
            tvSongCount.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    private void toggleBatchMode() {
        isBatchMode = !isBatchMode;
        adapter.setBatchMode(isBatchMode);
        batchModeBar.setVisibility(isBatchMode ? View.VISIBLE : View.GONE);
        if (isBatchMode) {
            cbSelectAll.setChecked(false);
        }
    }

    private void exitBatchMode() {
        isBatchMode = false;
        adapter.setBatchMode(false);
        batchModeBar.setVisibility(View.GONE);
        saveSortedOrder();
    }

    private void saveSortedOrder() {
        List<Song> sortedList = adapter.getCurrentList();
        if (sortedList.size() <= 1) {
            return;
        }

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    for (int i = 0; i < sortedList.size(); i++) {
                        playlistDao.updateSongPosition(playlistId, sortedList.get(i).getId(), i);
                    }
                    if (playlist != null) {
                        playlist.setUpdateTime(System.currentTimeMillis());
                        playlistDao.update(playlist);
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Toast.makeText(PlaylistDetailActivity.this, "排序已保存", Toast.LENGTH_SHORT).show();
                }
                currentSongs = new ArrayList<>(sortedList);
            }
        }.execute();
    }

    private void showBatchDeleteConfirm() {
        final Set<Long> selectedIds = adapter.getSelectedIds();
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "请选择要删除的歌曲", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("删除歌曲")
                .setMessage("确定要从歌单中删除选中的 " + selectedIds.size() + " 首歌曲吗？")
                .setPositiveButton("删除", (dialog, which) -> batchDeleteSongs(selectedIds))
                .setNegativeButton("取消", null)
                .show();
    }

    private void batchDeleteSongs(final Set<Long> songIds) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    for (Long songId : songIds) {
                        playlistDao.deletePlaylistSongCrossRef(playlistId, songId);
                        playlistDao.decrementSongCount(playlistId, System.currentTimeMillis());
                    }
                    List<PlaylistSongCrossRef> refs = playlistDao.getPlaylistSongs(playlistId);
                    for (int i = 0; i < refs.size(); i++) {
                        playlistDao.updateSongPosition(playlistId, refs.get(i).getSongId(), i);
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    adapter.removeSelected();
                    Toast.makeText(PlaylistDetailActivity.this, "已删除 " + songIds.size() + " 首歌曲", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PlaylistDetailActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("搜索歌曲");

        final EditText input = new EditText(this);
        input.setHint("输入歌曲名称或艺术家");
        input.setMaxLines(1);
        int paddingDp = dpToPx(16);
        input.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);
        builder.setView(input);

        builder.setPositiveButton("搜索", (dialog, which) -> {
            String query = input.getText().toString().trim();
            if (!query.isEmpty()) {
                searchResultList = filterSongs(query);
            }
        });
        builder.setNeutralButton("重置", (dialog, which) -> {
            resetSearch();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void resetSearch() {
        adapter.submitList(currentSongs);
        searchResultList = null;
        Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show();
    }

    private List<Song> filterSongs(String query) {
        List<Song> filtered = new ArrayList<>();
        for (Song song : currentSongs) {
            if (song.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    song.getArtist().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(song);
            }
        }
        adapter.submitList(filtered);
        if (filtered.isEmpty()) {
            Toast.makeText(this, "未找到匹配的歌曲", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "找到 " + filtered.size() + " 首匹配歌曲", Toast.LENGTH_SHORT).show();
        }
        return filtered;
    }

    private void showManageBottomSheet() {
        PlaylistManageBottomSheet bottomSheet = new PlaylistManageBottomSheet();
        bottomSheet.setOnManageActionListener(new PlaylistManageBottomSheet.OnManageActionListener() {
            @Override
            public void onAddSongs() {
                Intent intent = new Intent(PlaylistDetailActivity.this, SelectSongsActivity.class);
                intent.putExtra(SelectSongsActivity.EXTRA_PLAYLIST_ID, playlistId);
                startActivity(intent);
            }

            @Override
            public void onEditPlaylist() {
                showEditPlaylistDialog();
            }

            @Override
            public void onDeletePlaylist() {
                showDeletePlaylistConfirm();
            }
        });
        bottomSheet.show(getSupportFragmentManager(), "PlaylistManage");
    }

    private void showEditPlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑歌单名称");

        final EditText input = new EditText(this);
        input.setHint("歌单名称（最多20字）");
        input.setMaxLines(1);
        int paddingDp = dpToPx(16);
        input.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);
        if (playlist != null) {
            input.setText(playlist.getName());
            input.setSelection(input.getText().length());
        }
        builder.setView(input);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(PlaylistDetailActivity.this, "名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (name.length() > 20) {
                Toast.makeText(PlaylistDetailActivity.this, "名称不能超过20个字符", Toast.LENGTH_SHORT).show();
                return;
            }
            updatePlaylistName(name);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void updatePlaylistName(final String name) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    if (playlist != null) {
                        playlist.setName(name);
                        playlist.setUpdateTime(System.currentTimeMillis());
                        playlistDao.update(playlist);
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    toolbarTitle.setText(name);
                    Toast.makeText(PlaylistDetailActivity.this, "歌单已重命名", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PlaylistDetailActivity.this, "重命名失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void showDeletePlaylistConfirm() {
        String name = playlist != null ? playlist.getName() : "";
        new AlertDialog.Builder(this)
                .setTitle("删除歌单")
                .setMessage("确定要删除歌单\"" + name + "\"吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> deletePlaylist())
                .setNegativeButton("取消", null)
                .show();
    }

    private void deletePlaylist() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    if (playlist != null) {
                        playlistDao.delete(playlist);
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Toast.makeText(PlaylistDetailActivity.this, "歌单已删除", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PlaylistDetailActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playbackUpdateHandler != null && playbackUpdateRunnable != null) {
            playbackUpdateHandler.removeCallbacks(playbackUpdateRunnable);
        }
    }
}
