package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.data.ShiftRule;

public class HomeFragment extends Fragment {

    private static final String ARG_RULE = "shift_rule";
    private ShiftRule shiftRule;

    public static HomeFragment newInstance(ShiftRule rule) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RULE, rule);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            shiftRule = (ShiftRule) getArguments().getSerializable(ARG_RULE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        if (shiftRule != null) {
            TextView groupName = view.findViewById(R.id.group_name);
            groupName.setText(shiftRule.getName());

            TextView companyName = view.findViewById(R.id.company_name);
            companyName.setText(shiftRule.getCompanyName());
        }

        view.findViewById(R.id.btn_view_calendar).setOnClickListener(v -> {
            ((MainActivity) getActivity()).refreshCalendar();
        });

        view.findViewById(R.id.btn_view_rule).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RuleDetailActivity.class);
            intent.putExtra("rule", shiftRule);
            startActivity(intent);
        });

        return view;
    }
}