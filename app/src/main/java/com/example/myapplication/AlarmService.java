package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "alarm_service_channel";

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AlarmService 创建");

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "AlarmService:WakeLock"
        );
        wakeLock.acquire(60000);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createForegroundNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AlarmService 启动, flags: " + flags);

        if (intent != null) {
            String alarmId = intent.getStringExtra("alarm_id");
            String shiftType = intent.getStringExtra("shift_type");
            String reminderType = intent.getStringExtra("reminder_type");
            int hour = intent.getIntExtra("hour", -1);
            int minute = intent.getIntExtra("minute", -1);

            Log.d(TAG, "收到闹钟信号: alarmId=" + alarmId + ", " + shiftType + " " + reminderType + " " + String.format("%02d:%02d", hour, minute));

            if (shiftType != null && reminderType != null && hour >= 0 && minute >= 0) {
                startAlarmActivity(alarmId, shiftType, reminderType, String.valueOf(hour), String.valueOf(minute));
            } else {
                Log.d(TAG, "缺少闹钟参数，不启动AlarmActivity");
                stopSelf();
            }
        } else {
            Log.d(TAG, "intent为null，不启动AlarmActivity");
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "AlarmService 任务被移除");
        stopSelf();
    }

    private void startAlarmActivity(String alarmId, String shiftType, String reminderType, String hour, String minute) {
        SharedPreferences prefs = getSharedPreferences("alarm_closed", MODE_PRIVATE);
        prefs.edit().putBoolean("is_alarm_closed", false).apply();
        Log.d(TAG, "闹钟关闭状态已重置");
        
        Intent alarmIntent = new Intent(this, AlarmActivity.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NO_USER_ACTION |
                Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        alarmIntent.putExtra("alarm_id", alarmId);
        alarmIntent.putExtra("shift_type", shiftType);
        alarmIntent.putExtra("reminder_type", reminderType);
        alarmIntent.putExtra("hour", hour);
        alarmIntent.putExtra("minute", minute);

        try {
            startActivity(alarmIntent);
            Log.d(TAG, "AlarmActivity 启动成功");
            
            new Handler().postDelayed(() -> {
                stopSelf();
                Log.d(TAG, "AlarmService 启动Activity后停止");
            }, 1000);
            
        } catch (Exception e) {
            Log.e(TAG, "AlarmActivity 启动失败，尝试显示高优先级通知", e);
            showHighPriorityNotification(alarmId, shiftType, reminderType, hour, minute);
            
            new Handler().postDelayed(() -> {
                stopSelf();
                Log.d(TAG, "AlarmService 显示通知后停止");
            }, 1000);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "闹钟服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("倒班闹钟服务");
            channel.setSound(null, null);
            channel.setShowBadge(false);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createForegroundNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setSmallIcon(R.drawable.ic_alarm_clock)
                .setContentTitle("倒班闹钟运行中")
                .setContentText("闹钟服务正在运行")
                .setPriority(Notification.PRIORITY_MIN)
                .setOngoing(true)
                .setSound(null)
                .build();
    }

    private void showHighPriorityNotification(String alarmId, String shiftType, String reminderType, String hour, String minute) {
        String channelId = "alarm_notification_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "闹钟提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("倒班闹钟提醒通知");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 1000, 1000});
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent alarmIntent = new Intent(this, AlarmActivity.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        alarmIntent.putExtra("alarm_id", alarmId);
        alarmIntent.putExtra("shift_type", shiftType);
        alarmIntent.putExtra("reminder_type", reminderType);
        alarmIntent.putExtra("hour", hour);
        alarmIntent.putExtra("minute", minute);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                alarmId != null ? alarmId.hashCode() : 0,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String message = "";
        if ("起床".equals(reminderType)) {
            message = shiftType + "该起床了！";
        } else if ("上班".equals(reminderType)) {
            message = shiftType + "该上班了！";
        } else if ("接班".equals(reminderType)) {
            message = shiftType + "该接班了！";
        } else if ("打卡".equals(reminderType)) {
            message = shiftType + "该打卡了！";
        }

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_alarm_clock)
                .setContentTitle(shiftType + " - " + reminderType + "提醒")
                .setContentText(message)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setSound(alarmUri)
                .setVibrate(new long[]{0, 1000, 1000, 1000})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID + 1, notification);
            Log.d(TAG, "高优先级通知已显示");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AlarmService 销毁");
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}