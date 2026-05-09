package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.data.AlarmManager;
import com.example.myapplication.data.AlarmNotificationHelper;
import com.example.myapplication.data.AlarmSetting;

public class AlarmFragment extends Fragment {

    private ListView listView;
    private AlarmManager alarmManager;
    private View emptyView;
    private AlarmAdapter adapter;

    public static AlarmFragment newInstance() {
        return new AlarmFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        alarmManager = new AlarmManager(getContext());

        listView = view.findViewById(R.id.lv_alarms);
        emptyView = view.findViewById(R.id.empty_view);

        adapter = new AlarmAdapter();
        listView.setAdapter(adapter);
        listView.setEmptyView(emptyView);

        view.findViewById(R.id.btn_add_alarm).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AlarmSettingActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        alarmManager = new AlarmManager(getContext());
        adapter = new AlarmAdapter();
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        updateNextAlarmNotification();
    }

    private void updateNextAlarmNotification() {
        AlarmSetting nextAlarm = alarmManager.getNextAlarm();
        AlarmNotificationHelper.showNextAlarmNotification(getContext(), nextAlarm);
    }

    private void showPopupMenu(View anchorView, final AlarmSetting alarm) {
        final View itemView = anchorView;
        itemView.setSelected(true);

        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.popup_menu_alarm, null);

        int width = getResources().getDimensionPixelSize(R.dimen.popup_menu_width);
        final PopupWindow popupWindow = new PopupWindow(popupView, width, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        popupWindow.setElevation(16);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        View editView = popupView.findViewById(R.id.tv_edit);
        View deleteView = popupView.findViewById(R.id.tv_delete);

        editView.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AlarmSettingActivity.class);
            intent.putExtra("alarm", alarm);
            startActivity(intent);
            popupWindow.dismiss();
        });

        deleteView.setOnClickListener(v -> {
            alarmManager.deleteAlarm(alarm.getId());
            adapter.notifyDataSetChanged();
            updateNextAlarmNotification();
            Toast.makeText(getContext(), "闹钟已删除", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });

        popupWindow.setOnDismissListener(() -> itemView.setSelected(false));

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int anchorWidth = anchorView.getWidth();
        int anchorHeight = anchorView.getHeight();
        
        int x = location[0] + anchorWidth - width;
        int y = location[1];

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
    }

    private class AlarmAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return alarmManager.getAllAlarms().size();
        }

        @Override
        public AlarmSetting getItem(int position) {
            return alarmManager.getAllAlarms().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_alarm, parent, false);
            }

            final AlarmSetting alarm = getItem(position);

            TextView tvShiftType = view.findViewById(R.id.tv_shift_type);
            TextView tvTime = view.findViewById(R.id.tv_time);
            TextView tvReminderType = view.findViewById(R.id.tv_reminder_type);
            Switch switchAlarm = view.findViewById(R.id.switch_alarm);

            tvShiftType.setText(alarm.getShiftType());
            tvTime.setText(alarm.getTimeString());
            tvReminderType.setText(alarm.getReminderType() + "提醒");
            switchAlarm.setChecked(alarm.isEnabled());

            switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                alarmManager.toggleAlarm(alarm.getId());
                notifyDataSetChanged();
                updateNextAlarmNotification();
            });

            view.setOnLongClickListener(v -> {
                showPopupMenu(v, alarm);
                return true;
            });

            return view;
        }
    }
}