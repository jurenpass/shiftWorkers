package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.data.AlarmManager;
import com.example.myapplication.data.AlarmSetting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmFragment extends Fragment {

    private ListView listView;
    private AlarmManager alarmManager;
    private View emptyView;
    private View serviceDisabledView;
    private AlarmAdapter adapter;
    private long lastClickTime = 0;

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
        serviceDisabledView = view.findViewById(R.id.service_disabled_view);

        adapter = new AlarmAdapter();
        listView.setAdapter(adapter);

        view.findViewById(R.id.iv_add_alarm).setOnClickListener(v -> {
            showAddAlarmPopup(v);
        });

        Switch switchAllAlarm = view.findViewById(R.id.switch_all_alarm);
        switchAllAlarm.setChecked(alarmManager.isAlarmServiceEnabled());
        switchAllAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            alarmManager.setAlarmServiceEnabled(isChecked);
            updateView();
            adapter.notifyDataSetChanged();
        });

        updateView();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        alarmManager = new AlarmManager(getContext());
        adapter.notifyDataSetChanged();
        updateView();
    }

    private void updateView() {
        boolean serviceEnabled = alarmManager.isAlarmServiceEnabled();
        if (serviceEnabled) {
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
            serviceDisabledView.setVisibility(View.GONE);
        } else {
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            serviceDisabledView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddAlarmPopup(View anchorView) {
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.popup_menu_add_alarm, null);

        int width = getResources().getDimensionPixelSize(R.dimen.popup_menu_width);
        final PopupWindow popupWindow = new PopupWindow(popupView, width, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        popupWindow.setElevation(16);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        popupView.findViewById(R.id.btn_shift_alarm).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AlarmSettingActivity.class);
            startActivity(intent);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.btn_custom_alarm).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CustomAlarmActivity.class);
            startActivity(intent);
            popupWindow.dismiss();
        });

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int anchorWidth = anchorView.getWidth();
        int anchorHeight = anchorView.getHeight();

        int x = location[0] + anchorWidth - width;
        int y = location[1] + anchorHeight;

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
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

        View btnEdit = popupView.findViewById(R.id.tv_edit);
        View btnDelete = popupView.findViewById(R.id.tv_delete);

        btnEdit.setOnClickListener(v -> {
            Intent intent;
            if ("日历闹钟".equals(alarm.getShiftType())) {
                intent = new Intent(getContext(), DateAlarmActivity.class);
            } else if ("普通闹钟".equals(alarm.getShiftType())) {
                intent = new Intent(getContext(), CustomAlarmActivity.class);
            } else {
                intent = new Intent(getContext(), AlarmSettingActivity.class);
            }
            intent.putExtra("alarm", alarm);
            startActivity(intent);
            popupWindow.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            alarmManager.removeAlarm(alarm);
            alarmManager = new AlarmManager(getContext());
            adapter.notifyDataSetChanged();
            updateView();
            Toast.makeText(getContext(), "闹钟已删除", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        popupWindow.setOnDismissListener(() -> itemView.setSelected(false));

        int x = location[0] + anchorView.getWidth() - width;
        int y = location[1];

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
    }

    private class AlarmAdapter extends BaseAdapter {

        private List<AlarmSetting> getShiftAlarms() {
            List<AlarmSetting> result = new ArrayList<>();
            for (AlarmSetting alarm : alarmManager.getAllAlarms()) {
                if (!"普通闹钟".equals(alarm.getShiftType()) && !"日历闹钟".equals(alarm.getShiftType())) {
                    result.add(alarm);
                }
            }
            return result;
        }

        private List<AlarmSetting> getNormalAlarms() {
            List<AlarmSetting> result = new ArrayList<>();
            for (AlarmSetting alarm : alarmManager.getAllAlarms()) {
                if ("普通闹钟".equals(alarm.getShiftType()) || "日历闹钟".equals(alarm.getShiftType())) {
                    result.add(alarm);
                }
            }
            return result;
        }

        @Override
        public int getCount() {
            return getShiftAlarms().size() + getNormalAlarms().size();
        }

        @Override
        public AlarmSetting getItem(int position) {
            List<AlarmSetting> shiftAlarms = getShiftAlarms();
            if (position < shiftAlarms.size()) {
                return shiftAlarms.get(position);
            }
            return getNormalAlarms().get(position - shiftAlarms.size());
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_alarm, parent, false);
            }

            AlarmSetting alarm = getItem(position);

            ImageView ivAlarmIcon = convertView.findViewById(R.id.iv_alarm_icon);
            TextView tvShiftType = convertView.findViewById(R.id.tv_shift_type);
            TextView tvTime = convertView.findViewById(R.id.tv_time);
            TextView tvReminderType = convertView.findViewById(R.id.tv_reminder_type);
            Switch switchAlarm = convertView.findViewById(R.id.switch_alarm);

            tvShiftType.setText(alarm.getShiftType());
            tvTime.setText(alarm.getTimeString());
            
            if ("日历闹钟".equals(alarm.getShiftType()) || 
                ("普通闹钟".equals(alarm.getShiftType()) && alarm.getRepeatType() == 3)) {
                long repeatDate = alarm.getRepeatDate();
                if (repeatDate > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(repeatDate);
                    String dateStr = String.format("%d年%d月%d日",
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH));
                    tvReminderType.setText(dateStr);
                } else {
                    tvReminderType.setText(alarm.getReminderType());
                }
            } else {
                tvReminderType.setText(alarm.getReminderType() + "提醒");
            }
            
            switchAlarm.setChecked(alarm.isEnabled());

            if ("日历闹钟".equals(alarm.getShiftType())) {
                ivAlarmIcon.setImageResource(R.drawable.ic_alarm_clock_calendar);
            } else if ("普通闹钟".equals(alarm.getShiftType())) {
                ivAlarmIcon.setImageResource(R.drawable.ic_alarm_clock_normal);
            } else {
                ivAlarmIcon.setImageResource(R.drawable.ic_alarm_clock);
            }

            final String alarmId = alarm.getId();
            switchAlarm.setOnCheckedChangeListener(null);
            switchAlarm.setChecked(alarm.isEnabled());
            switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                alarmManager.toggleAlarm(alarmId);
                alarmManager = new AlarmManager(getContext());
                adapter.notifyDataSetChanged();
            });

            convertView.setOnLongClickListener(v -> {
                showPopupMenu(v, alarm);
                return true;
            });

            convertView.setOnClickListener(v -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < 300) {
                    Intent intent;
                    if ("日历闹钟".equals(alarm.getShiftType())) {
                        intent = new Intent(getContext(), DateAlarmActivity.class);
                    } else if ("普通闹钟".equals(alarm.getShiftType())) {
                        intent = new Intent(getContext(), CustomAlarmActivity.class);
                    } else {
                        intent = new Intent(getContext(), AlarmSettingActivity.class);
                    }
                    intent.putExtra("alarm", alarm);
                    startActivity(intent);
                }
                lastClickTime = currentTime;
            });

            return convertView;
        }
    }
}