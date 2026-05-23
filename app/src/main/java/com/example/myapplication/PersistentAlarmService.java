package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.example.myapplication.data.AlarmManager;
import com.example.myapplication.data.AlarmSetting;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PersistentAlarmService extends Service {
    private static final String TAG = "PersistentAlarmService";
    private static final int NOTIFICATION_ID = 2000;
    private static final String CHANNEL_ID = "persistent_alarm_service_channel";
    private static final long UPDATE_INTERVAL = 60000; // 1分钟更新一次

    private Handler handler;
    private Runnable updateRunnable;
    private boolean isRunning = false;
    private BroadcastReceiver alarmChangedReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "持久闹钟服务创建");

        AlarmManager alarmManager = new AlarmManager(this);
        if (!alarmManager.isAlarmServiceEnabled()) {
            Log.d(TAG, "闹钟服务已关闭，不启动前台服务");
            stopSelf();
            return;
        }

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        handler = new Handler();
        isRunning = true;

        // 启动定时更新任务
        startUpdateRunnable();

        // 注册闹钟变化广播接收器
        registerAlarmChangedReceiver();
    }

    private void registerAlarmChangedReceiver() {
        alarmChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AlarmManager.ACTION_ALARM_CHANGED.equals(intent.getAction())) {
                    Log.d(TAG, "收到闹钟变化广播，立即更新通知");
                    updateNotification();
                }
            }
        };

        IntentFilter filter = new IntentFilter(AlarmManager.ACTION_ALARM_CHANGED);
        registerReceiver(alarmChangedReceiver, filter);
        Log.d(TAG, "闹钟变化广播接收器已注册");
    }

    private void startUpdateRunnable() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    updateNotification();
                    handler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        };
        handler.post(updateRunnable);
    }

    private void updateNotification() {
        try {
            AlarmManager alarmManager = new AlarmManager(this);
            if (!alarmManager.isAlarmServiceEnabled()) {
                Log.d(TAG, "闹钟服务已关闭，停止通知");
                stopSelf();
                return;
            }

            Notification notification = createNotification();
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "通知已更新");
            }
        } catch (Exception e) {
            Log.e(TAG, "更新通知失败", e);
        }
    }

    private Notification createNotification() {
        AlarmManager alarmManager = new AlarmManager(this);
        AlarmSetting nextAlarm = alarmManager.getNextAlarm();

        String title = "倒班人 - 闹钟服务运行中";
        String content = "后台服务已启动";

        if (nextAlarm != null) {
            long nextAlarmTime;
            if ("普通闹钟".equals(nextAlarm.getShiftType()) || "日历闹钟".equals(nextAlarm.getShiftType())) {
                nextAlarmTime = alarmManager.getNextCustomAlarmTime(nextAlarm);
            } else {
                nextAlarmTime = alarmManager.getNextAlarmTimeForShift(nextAlarm);
            }

            Calendar alarmCal = Calendar.getInstance();
            alarmCal.setTimeInMillis(nextAlarmTime);
            int hour = alarmCal.get(Calendar.HOUR_OF_DAY);
            int minute = alarmCal.get(Calendar.MINUTE);

            String relativeDate = getRelativeDateString(nextAlarmTime);
            content = String.format("下一个闹钟: %s %02d:%02d %s %s",
                relativeDate,
                hour,
                minute,
                nextAlarm.getShiftType(),
                nextAlarm.getReminderType());
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_LOW)
            .setStyle(new Notification.BigTextStyle().bigText(content))
            .build();
    }

    private String getRelativeDateString(long alarmTime) {
        Calendar alarmCalendar = Calendar.getInstance();
        alarmCalendar.setTimeInMillis(alarmTime);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);

        Calendar dayAfter = Calendar.getInstance();
        dayAfter.add(Calendar.DAY_OF_MONTH, 2);
        dayAfter.set(Calendar.HOUR_OF_DAY, 0);
        dayAfter.set(Calendar.MINUTE, 0);
        dayAfter.set(Calendar.SECOND, 0);
        dayAfter.set(Calendar.MILLISECOND, 0);

        if (alarmCalendar.before(tomorrow)) {
            return "今天";
        } else if (alarmCalendar.before(dayAfter)) {
            return "明天";
        } else if (alarmCalendar.before(dayAfter.getTimeInMillis() + 24 * 60 * 60 * 1000)) {
            return "后天";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日", Locale.getDefault());
        return sdf.format(new Date(alarmTime));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "持久闹钟服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持闹钟应用活跃的后台服务");
            channel.setSound(null, null);
            channel.setShowBadge(false);
            channel.setVibrationPattern(null);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "持久闹钟服务启动");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "持久闹钟服务销毁");
        isRunning = false;
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        if (alarmChangedReceiver != null) {
            unregisterReceiver(alarmChangedReceiver);
            Log.d(TAG, "闹钟变化广播接收器已取消注册");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}