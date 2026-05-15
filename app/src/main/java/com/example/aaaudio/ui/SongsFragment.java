package com.example.aaaudio.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aaaudio.MainActivity;
import com.example.aaaudio.R;
import com.example.aaaudio.adapter.SongAdapter;
import com.example.aaaudio.data.SongRepository;
import com.example.aaaudio.model.Song;
import com.example.aaaudio.service.SimpleAudioPlayerManager;

import java.util.List;

public class SongsFragment extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView recyclerView;
    private ImageButton scanButton;
    private TextView songCountText;
    private SongAdapter songAdapter;
    private SongRepository songRepository;
    private SimpleAudioPlayerManager audioPlayerManager;

    public SongsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        recyclerView = view.findViewById(R.id.songs_recycler_view);
        scanButton = view.findViewById(R.id.btn_scan);
        songCountText = view.findViewById(R.id.tv_song_count);

        songAdapter = new SongAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(songAdapter);

        songRepository = SongRepository.getInstance(requireContext());

        if (getActivity() instanceof MainActivity) {
            audioPlayerManager = ((MainActivity) getActivity()).getAudioPlayerManager();
        }

        setupScanButton();
        setupSongClick();
        observeSongs();
        observeSongCount();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkAndScanOnFirstLaunch();
    }

    private void setupScanButton() {
        scanButton.setOnClickListener(v -> {
            if (hasStoragePermission()) {
                startScan();
            } else {
                requestStoragePermission();
            }
        });
    }

    private void setupSongClick() {
        songAdapter.setOnSongClickListener((song, position) -> {
            if (audioPlayerManager != null) {
                List<Song> currentList = songAdapter.getCurrentList();
                audioPlayerManager.playPlaylist(currentList, position);
            }
        });
    }

    private void observeSongs() {
        songRepository.getAllSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null && !songs.isEmpty()) {
                songAdapter.submitList(songs);
            }
        });
    }

    private void observeSongCount() {
        songRepository.getSongCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                songCountText.setText("共 " + count + " 首");
            }
        });
    }

    private void checkAndScanOnFirstLaunch() {
        songRepository.getSongCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count == 0 && hasStoragePermission()) {
                startScan();
            }
        });
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        requestPermissions(
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(getContext(),
                            "权限被永久拒绝，请在系统设置中手动授予存储权限",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "需要存储权限才能扫描音乐文件", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void startScan() {
        scanButton.setEnabled(false);
        Toast.makeText(getContext(), "正在扫描音乐文件...", Toast.LENGTH_SHORT).show();

        songRepository.scanMusic(new SongRepository.ScanResultCallback() {
            @Override
            public void onScanComplete(int totalFound, int inserted, int duplicates, int removed) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        scanButton.setEnabled(true);
                        String message;
                        if (inserted > 0 && removed > 0) {
                            message = "扫描完成，新增 " + inserted + " 首，移除 " + removed + " 首";
                        } else if (inserted > 0) {
                            message = "扫描完成，新增 " + inserted + " 首";
                        } else if (removed > 0) {
                            message = "扫描完成，移除 " + removed + " 首";
                        } else if (totalFound > 0) {
                            message = "歌曲已是最新，共 " + totalFound + " 首";
                        } else {
                            message = "未发现音乐文件";
                        }
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onScanError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        scanButton.setEnabled(true);
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
}
