package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.data.ShiftCalendarUtil;

public class ProfileFragment extends Fragment {

    private TextView teamMemberView;

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        teamMemberView = view.findViewById(R.id.tv_team_member);
        updateTeamMemberText();

        view.findViewById(R.id.item_settings).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.item_help).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HelpActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.item_about).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AboutActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTeamMemberText();
    }

    private void updateTeamMemberText() {
        if (teamMemberView != null) {
            String currentTeam = getCurrentTeam();
            String displayName = ShiftCalendarUtil.removeHSM2Prefix(currentTeam);
            teamMemberView.setText(displayName + "成员");
        }
    }

    private String getCurrentTeam() {
        if (getActivity() != null) {
            SharedPreferences shiftPrefs = getActivity().getSharedPreferences("shift_prefs", getActivity().MODE_PRIVATE);
            return shiftPrefs.getString("current_group", "丁班");
        }
        return "丁班";
    }

    public void updateTeamDisplay(String teamName) {
        if (teamMemberView != null) {
            String displayName = ShiftCalendarUtil.removeHSM2Prefix(teamName);
            teamMemberView.setText(displayName + "成员");
        }
    }
}