package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.example.myapplication.data.HolidayManager;
import com.example.myapplication.data.HolidayUpdateService;
import com.example.myapplication.data.ShiftRule;
import com.example.myapplication.data.ShiftCalendarUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements CalendarFragment.OnGroupSwitchListener {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private ShiftRule currentRule;
    private float startX, startY;
    private static final int SWIPE_THRESHOLD = 50;
    private Calendar currentCalendar = Calendar.getInstance();
    private static final String PREFS_NAME = "shift_prefs";
    private static final String PREF_CURRENT_GROUP = "current_group";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadCurrentGroup();
        if (currentRule == null) {
            currentRule = ShiftCalendarUtil.createDefaultRule();
        }

        initHolidayData();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        checkHolidayUpdate();
        requestNotificationPermission();
        requestExactAlarmPermission();
        requestOverlayPermission();
        requestBackgroundActivityPermission();
        startAlarmService();
        rescheduleAlarms();
        updateNextAlarmNotification();

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();
                Fragment fragment = null;

                if (id == R.id.nav_home) {
                    fragment = HomeFragment.newInstance(currentRule);
                } else if (id == R.id.nav_calendar) {
                    fragment = CalendarFragment.newInstance(currentRule);
                    ((CalendarFragment) fragment).setOnGroupSwitchListener(MainActivity.this);
                } else if (id == R.id.nav_alarm) {
                    fragment = AlarmFragment.newInstance();
                } else if (id == R.id.nav_profile) {
                    fragment = ProfileFragment.newInstance();
                }

                if (fragment != null) {
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.fragment_container, fragment);
                    transaction.commit();
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_calendar);
        }
    }

    private void loadCurrentGroup() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String groupName = prefs.getString(PREF_CURRENT_GROUP, "丁班");
        currentRule = ShiftCalendarUtil.createDefaultRule();
        currentRule.setName(groupName);
    }

    private void saveCurrentGroup(String groupName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_CURRENT_GROUP, groupName).apply();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
                float diffX = ev.getX() - startX;
                float diffY = ev.getY() - startY;

                if (Math.abs(diffX) > SWIPE_THRESHOLD || Math.abs(diffY) > SWIPE_THRESHOLD) {
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (diffX > SWIPE_THRESHOLD) {
                            currentCalendar.add(Calendar.MONTH, -1);
                        } else {
                            currentCalendar.add(Calendar.MONTH, 1);
                        }
                    } else {
                        if (diffY > SWIPE_THRESHOLD) {
                            currentCalendar.add(Calendar.YEAR, -1);
                        } else {
                            currentCalendar.add(Calendar.YEAR, 1);
                        }
                    }
                    Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof CalendarFragment) {
                        ((CalendarFragment) currentFragment).updateCalendarByDate(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH) + 1);
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onGroupSwitched(ShiftRule newRule) {
        currentRule = newRule;
        saveCurrentGroup(newRule.getName());
        rescheduleAlarms();
        updateNextAlarmNotification();
    }

    private void initHolidayData() {
        HolidayManager.getInstance().loadFromPreferences(this);
    }

    private void checkHolidayUpdate() {
        HolidayUpdateService.checkForYearUpdate(this, new HolidayUpdateService.UpdateCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailed(String error) {
            }
        });
    }

    public ShiftRule getCurrentRule() {
        return currentRule;
    }

    public void setCurrentRule(ShiftRule rule) {
        this.currentRule = rule;
        saveCurrentGroup(rule.getName());
    }

    public void refreshCalendar() {
        bottomNavigationView.setSelectedItemId(R.id.nav_calendar);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    private void requestBackgroundActivityPermission() {
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                Intent intent = new Intent("android.settings.MANAGE_APP_ACTIVITY");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
            }
        }
    }

    private void startAlarmService() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void rescheduleAlarms() {
        com.example.myapplication.data.AlarmManager alarmManager = new com.example.myapplication.data.AlarmManager(this);
        alarmManager.scheduleAllAlarms();
    }

    private void updateNextAlarmNotification() {
        com.example.myapplication.data.AlarmManager alarmManager = new com.example.myapplication.data.AlarmManager(this);
        com.example.myapplication.data.AlarmSetting nextAlarm = alarmManager.getNextAlarm();
        com.example.myapplication.data.AlarmNotificationHelper.showNextAlarmNotification(this, nextAlarm);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要通知权限才能接收闹钟提醒", Toast.LENGTH_SHORT).show();
            }
        }
    }
}