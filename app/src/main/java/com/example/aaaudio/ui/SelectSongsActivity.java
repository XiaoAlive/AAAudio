package com.example.aaaudio.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aaaudio.R;
import com.example.aaaudio.data.SongRepository;
import com.example.aaaudio.database.AppDatabase;
import com.example.aaaudio.database.PlaylistDao;
import com.example.aaaudio.model.PlaylistSongCrossRef;
import com.example.aaaudio.model.Song;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectSongsActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYLIST_ID = "playlist_id";

    private long playlistId;
    private PlaylistDao playlistDao;
    private SongRepository songRepository;

    private RecyclerView recyclerView;
    private SelectSongAdapter adapter;
    private MaterialButton btnAddSelected;
    private TextView tvSelectedCount;
    private EditText etSearch;
    private ImageButton btnSearch, btnReset;
    private View bottomBar;

    private List<Song> allSongs = new ArrayList<>();
    private Set<Long> existingSongIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_songs);

        playlistId = getIntent().getLongExtra(EXTRA_PLAYLIST_ID, -1);
        if (playlistId == -1) {
            finish();
            return;
        }

        playlistDao = AppDatabase.getDatabase(this).playlistDao();
        songRepository = SongRepository.getInstance(this);

        initViews();
        setupToolbar();
        setupSearch();
        setupRecyclerView();
        loadSongs();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.select_songs_recycler_view);
        btnAddSelected = findViewById(R.id.btn_add_selected);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        btnReset = findViewById(R.id.btn_reset);
        bottomBar = findViewById(R.id.bottom_bar);

        bottomBar.post(new Runnable() {
            @Override
            public void run() {
                int bottomBarHeight = bottomBar.getHeight();
                recyclerView.setPadding(
                        recyclerView.getPaddingLeft(),
                        recyclerView.getPaddingTop(),
                        recyclerView.getPaddingRight(),
                        bottomBarHeight + dpToPx(8)
                );
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_select_songs);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSearch() {
        btnSearch.setOnClickListener(v -> performSearch());

        btnReset.setOnClickListener(v -> resetSearch());

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        if (query.isEmpty()) {
            return;
        }

        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            String title = song.getTitle() != null ? song.getTitle().toLowerCase() : "";
            String artist = song.getArtist() != null ? song.getArtist().toLowerCase() : "";
            String path = song.getPath() != null ? song.getPath().toLowerCase() : "";
            String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;

            if (title.contains(query) || artist.contains(query) || fileName.contains(query)) {
                filtered.add(song);
            }
        }
        adapter.submitList(filtered, existingSongIds);
        if (filtered.isEmpty()) {
            Toast.makeText(this, "未找到匹配的歌曲", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetSearch() {
        etSearch.setText("");
        adapter.submitList(allSongs, existingSongIds);
    }

    private void setupRecyclerView() {
        adapter = new SelectSongAdapter();
        adapter.setOnSelectionChangedListener(new SelectSongAdapter.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(int count) {
                tvSelectedCount.setText("已选择 " + count + " 首");
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnAddSelected.setOnClickListener(v -> addSelectedSongsToPlaylist());
    }

    private void loadSongs() {
        songRepository.getAllSongs().observe(this, new Observer<List<Song>>() {
            @Override
            public void onChanged(List<Song> songs) {
                if (songs != null) {
                    allSongs = songs;
                    loadExistingSongIds();
                }
            }
        });
    }

    private void loadExistingSongIds() {
        new AsyncTask<Void, Void, Set<Long>>() {
            @Override
            protected Set<Long> doInBackground(Void... voids) {
                List<PlaylistSongCrossRef> existingRefs = playlistDao.getPlaylistSongs(playlistId);
                Set<Long> ids = new HashSet<>();
                for (PlaylistSongCrossRef ref : existingRefs) {
                    ids.add(ref.getSongId());
                }
                return ids;
            }

            @Override
            protected void onPostExecute(Set<Long> ids) {
                existingSongIds = ids;
                adapter.submitList(allSongs, existingSongIds);
            }
        }.execute();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void addSelectedSongsToPlaylist() {
        final List<Song> selectedSongs = adapter.getSelectedSongs();
        if (selectedSongs.isEmpty()) {
            Toast.makeText(this, "请选择要添加的歌曲", Toast.LENGTH_SHORT).show();
            return;
        }

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                int addedCount = 0;
                int maxPosition = playlistDao.getMaxPosition(playlistId);
                try {
                    for (int i = 0; i < selectedSongs.size(); i++) {
                        Song song = selectedSongs.get(i);
                        playlistDao.insertPlaylistSongCrossRef(
                                new PlaylistSongCrossRef(playlistId, song.getId(), maxPosition + i + 1)
                        );
                        addedCount++;
                    }
                    if (addedCount > 0) {
                        for (int i = 0; i < addedCount; i++) {
                            playlistDao.incrementSongCount(playlistId, System.currentTimeMillis());
                        }
                    }
                } catch (Exception e) {
                    return -1;
                }
                return addedCount;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result > 0) {
                    Toast.makeText(SelectSongsActivity.this, "成功添加 " + result + " 首歌曲", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (result == -1) {
                    Toast.makeText(SelectSongsActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private static class SelectSongAdapter extends RecyclerView.Adapter<SelectSongAdapter.ViewHolder> {

        private List<Song> songList = new ArrayList<>();
        private Set<Long> existingSongIds = new HashSet<>();
        private Set<Long> selectedIds = new HashSet<>();

        public interface OnSelectionChangedListener {
            void onSelectionChanged(int count);
        }

        private OnSelectionChangedListener selectionChangedListener;

        public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
            this.selectionChangedListener = listener;
        }

        public void submitList(List<Song> songs, Set<Long> existingIds) {
            this.songList = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
            this.existingSongIds = existingIds != null ? existingIds : new HashSet<>();
            this.selectedIds.clear();
            notifyDataSetChanged();
            notifySelectionChanged();
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

        private void notifySelectionChanged() {
            if (selectionChangedListener != null) {
                selectionChangedListener.onSelectionChanged(selectedIds.size());
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_select_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Song song = songList.get(position);
            boolean isExisting = existingSongIds.contains(song.getId());
            boolean isSelected = selectedIds.contains(song.getId());
            holder.bind(song, isExisting, isSelected);
        }

        @Override
        public int getItemCount() {
            return songList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView titleText;
            private final TextView artistText;
            private final CheckBox checkBox;
            private final TextView existingLabel;
            private final View container;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.song_title);
                artistText = itemView.findViewById(R.id.song_artist);
                checkBox = itemView.findViewById(R.id.checkbox_select);
                existingLabel = itemView.findViewById(R.id.tv_existing_label);
                container = itemView;
            }

            void bind(final Song song, boolean isExisting, boolean isSelected) {
                titleText.setText(song.getTitle());
                artistText.setText(song.getArtist());

                if (isExisting) {
                    checkBox.setVisibility(View.GONE);
                    existingLabel.setVisibility(View.VISIBLE);
                    container.setEnabled(false);
                    container.setAlpha(0.5f);
                } else {
                    checkBox.setVisibility(View.VISIBLE);
                    existingLabel.setVisibility(View.GONE);
                    container.setEnabled(true);
                    container.setAlpha(1.0f);
                    checkBox.setChecked(isSelected);

                    container.setOnClickListener(v -> {
                        checkBox.setChecked(!checkBox.isChecked());
                    });

                    checkBox.setOnCheckedChangeListener(null);
                    checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            selectedIds.add(song.getId());
                        } else {
                            selectedIds.remove(song.getId());
                        }
                        notifySelectionChanged();
                    });
                }
            }
        }
    }
}
