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
    private TextView groupNameView;
    private TextView companyNameView;

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

        groupNameView = view.findViewById(R.id.group_name);
        companyNameView = view.findViewById(R.id.company_name);

        updateRuleDisplay();

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

    private void updateRuleDisplay() {
        if (shiftRule != null) {
            if (groupNameView != null) {
                groupNameView.setText(shiftRule.getName());
            }
            if (companyNameView != null) {
                companyNameView.setText(shiftRule.getCompanyName());
            }
        }
    }

    public void updateShiftRule(ShiftRule newRule) {
        this.shiftRule = newRule;
        updateRuleDisplay();
    }

    public ShiftRule getShiftRule() {
        return shiftRule;
    }
}