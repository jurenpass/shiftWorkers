package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        view.findViewById(R.id.item_settings).setOnClickListener(v -> {
            Toast.makeText(getContext(), "设置功能开发中", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.item_help).setOnClickListener(v -> {
            Toast.makeText(getContext(), "帮助与反馈功能开发中", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.item_about).setOnClickListener(v -> {
            Toast.makeText(getContext(), "关于我们功能开发中", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}