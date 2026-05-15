package com.example.aaaudio.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.aaaudio.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class PlaylistManageBottomSheet extends BottomSheetDialogFragment {

    public interface OnManageActionListener {
        void onAddSongs();
        void onEditPlaylist();
        void onDeletePlaylist();
    }

    private OnManageActionListener listener;

    public void setOnManageActionListener(OnManageActionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_playlist_manage, container, false);

        MaterialButton btnAddSongs = view.findViewById(R.id.btn_add_songs);
        MaterialButton btnEditPlaylist = view.findViewById(R.id.btn_edit_playlist);
        MaterialButton btnDeletePlaylist = view.findViewById(R.id.btn_delete_playlist);

        btnAddSongs.setOnClickListener(v -> {
            if (listener != null) listener.onAddSongs();
            dismiss();
        });

        btnEditPlaylist.setOnClickListener(v -> {
            if (listener != null) listener.onEditPlaylist();
            dismiss();
        });

        btnDeletePlaylist.setOnClickListener(v -> {
            if (listener != null) listener.onDeletePlaylist();
            dismiss();
        });

        return view;
    }
}
