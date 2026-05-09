package com.example.myapplication.data;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmNotificationHelper {
    private static final String CHANNEL_ID = "alarm_channel";
    private static final String NEXT_ALARM_CHANNEL_ID = "next_alarm_channel";
    private static final int NEXT_ALARM_ID = 1000;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "闹钟提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("闹钟提醒通知");

            NotificationChannel nextAlarmChannel = new NotificationChannel(
                    NEXT_ALARM_CHANNEL_ID,
                    "下一个闹钟",
                    NotificationManager.IMPORTANCE_LOW
            );
            nextAlarmChannel.setDescription("显示下一个闹钟的通知");
            nextAlarmChannel.setShowBadge(false);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                notificationManager.createNotificationChannel(nextAlarmChannel);
            }
        }
    }

    public static void showNextAlarmNotification(Context context, AlarmSetting alarm) {
        if (alarm == null) {
            cancelNextAlarmNotification(context);
            return;
        }

        createNotificationChannels(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = new AlarmManager(context);
        long nextAlarmTime = alarmManager.getNextAlarmTimeForShift(alarm);
        String relativeDate = getRelativeDateString(nextAlarmTime);

        Calendar alarmCal = Calendar.getInstance();
        alarmCal.setTimeInMillis(nextAlarmTime);
        int hour = alarmCal.get(Calendar.HOUR_OF_DAY);
        int minute = alarmCal.get(Calendar.MINUTE);

        String contentText = String.format(Locale.getDefault(), "Next: %s %02d:%02d %s %s",
                relativeDate, hour, minute, alarm.getShiftType(), alarm.getReminderType());

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, NEXT_ALARM_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_alarm_clock)
                .setContentTitle("倒班人")
                .setContentText(contentText)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_LOW)
                .setStyle(new Notification.BigTextStyle().bigText(contentText))
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NEXT_ALARM_ID, notification);
        }
    }

    private static String getRelativeDateString(long alarmTime) {
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

        Calendar thirdDay = Calendar.getInstance();
        thirdDay.add(Calendar.DAY_OF_MONTH, 3);
        thirdDay.set(Calendar.HOUR_OF_DAY, 0);
        thirdDay.set(Calendar.MINUTE, 0);
        thirdDay.set(Calendar.SECOND, 0);
        thirdDay.set(Calendar.MILLISECOND, 0);

        if (alarmCalendar.before(tomorrow)) {
            return "今天";
        } else if (alarmCalendar.before(dayAfter)) {
            return "明天";
        } else if (alarmCalendar.before(thirdDay)) {
            return "后天";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日", Locale.getDefault());
        return sdf.format(new Date(alarmTime));
    }

    public static void cancelNextAlarmNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NEXT_ALARM_ID);
        }
    }
}